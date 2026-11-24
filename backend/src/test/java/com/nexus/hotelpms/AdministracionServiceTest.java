package com.nexus.hotelpms;

import com.nexus.hotelpms.aplicacion.puertos.ICatalogoItemRepository;
import com.nexus.hotelpms.aplicacion.puertos.IConsumoRepository;
import com.nexus.hotelpms.aplicacion.puertos.IHabitacionRepository;
import com.nexus.hotelpms.aplicacion.puertos.IReservaRepository;
import com.nexus.hotelpms.aplicacion.puertos.ISedeRepository;
import com.nexus.hotelpms.aplicacion.puertos.IUsuarioRepository;
import com.nexus.hotelpms.aplicacion.servicios.AdministracionService;
import com.nexus.hotelpms.aplicacion.servicios.ReporteService;
import com.nexus.hotelpms.dominio.excepcion.ConflictoException;
import com.nexus.hotelpms.dominio.excepcion.ReglaNegocioException;
import com.nexus.hotelpms.dominio.modelo.Administrador;
import com.nexus.hotelpms.dominio.modelo.EstadoHabitacion;
import com.nexus.hotelpms.dominio.modelo.EstadoReserva;
import com.nexus.hotelpms.dominio.modelo.Habitacion;
import com.nexus.hotelpms.dominio.modelo.Reserva;
import com.nexus.hotelpms.dominio.modelo.Sede;
import com.nexus.hotelpms.dominio.modelo.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU-13 a HU-17: reglas del servicio de administración (usuarios, habitaciones y reportes). */
class AdministracionServiceTest {
    private ISedeRepository sedes;
    private IUsuarioRepository usuarios;
    private IHabitacionRepository habitaciones;
    private IReservaRepository reservas;
    private IConsumoRepository consumos;
    private AdministracionService servicio;
    private ReporteService reportes;

    @BeforeEach
    void preparar() {
        sedes = mock(ISedeRepository.class);
        usuarios = mock(IUsuarioRepository.class);
        habitaciones = mock(IHabitacionRepository.class);
        reservas = mock(IReservaRepository.class);
        consumos = mock(IConsumoRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(any())).thenReturn("hash");
        when(sedes.findById(any())).thenReturn(Optional.of(new Sede("Norte", "Calle 1")));
        when(usuarios.save(any())).thenAnswer(i -> i.getArgument(0));
        when(habitaciones.guardar(any())).thenAnswer(i -> i.getArgument(0));
        servicio = new AdministracionService(sedes, usuarios, habitaciones, mock(ICatalogoItemRepository.class),
                reservas, encoder);
        reportes = new ReporteService(sedes, habitaciones, reservas, consumos);
    }

    // ------------------------------------------------------------ HU-14: correos
    @Test
    void elCorreoSeGuardaNormalizadoEnMinusculas() {
        servicio.crearRecepcionista("Juan", "  Juan@Hotel.COM ", "clave1234", "sede-1");
        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(guardado.capture());
        assertThat(guardado.getValue().getEmail()).isEqualTo("juan@hotel.com");
    }

    @Test
    void elCorreoRepetidoSeDetectaSinImportarMayusculas() {
        when(usuarios.findByEmail("juan@hotel.com"))
                .thenReturn(Optional.of(new Administrador("Otro", "juan@hotel.com", "x")));
        assertThatThrownBy(() -> servicio.crearRecepcionista("Juan", "JUAN@hotel.com", "clave1234", "sede-1"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Ya existe un usuario con ese correo");
    }

    // ------------------------------------------------------------ HU-15: habitaciones
    @Test
    void numeroDeHabitacionRepetidoEnLaSedeDaConflicto() {
        Habitacion existente = new Habitacion("sede-1", "101", "Doble", 2, BigDecimal.TEN);
        when(habitaciones.buscarPorSede("sede-1")).thenReturn(List.of(existente));
        assertThatThrownBy(() -> servicio.crearHabitacion("sede-1", "101", "Suite", 3, BigDecimal.ONE))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("Ya existe una habitación con el número 101");
    }

    private Habitacion habitacionReservada() {
        Habitacion h = new Habitacion("sede-1", "205", "Doble", 2, BigDecimal.TEN);
        ReflectionTestUtils.setField(h, "id", "hab-1");
        h.cambiarEstado(EstadoHabitacion.Reservada);
        when(habitaciones.buscarPorId("hab-1")).thenReturn(Optional.of(h));
        return h;
    }

    private Reserva reservaPagada(Habitacion h) {
        Reserva r = new Reserva("RSV-1", h, "Ana", "123", "a@b.com",
                java.time.LocalDate.now().plusDays(10), java.time.LocalDate.now().plusDays(12), 2);
        r.bloquear(ZonedDateTime.now(ZoneId.of("America/Bogota")), 15);
        r.confirmarPago();
        return r;
    }

    @Test
    void mantenimientoConReservaPagadaExigeConfirmacion() {
        Habitacion h = habitacionReservada();
        when(reservas.buscarPorSede("sede-1")).thenReturn(List.of(reservaPagada(h)));

        assertThatThrownBy(() -> servicio.cambiarEstadoHabitacion("hab-1", EstadoHabitacion.Mantenimiento, false))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("1 reserva(s) pagada(s)");
        assertThat(h.getEstado()).isEqualTo(EstadoHabitacion.Reservada);

        Map<String, Object> r = servicio.cambiarEstadoHabitacion("hab-1", EstadoHabitacion.Mantenimiento, true);
        assertThat(r.get("reservasFuturasAfectadas")).isEqualTo(1L);
        assertThat(h.getEstado()).isEqualTo(EstadoHabitacion.Mantenimiento);
    }

    @Test
    void alVolverDeMantenimientoConReservaPagadaQuedaReservadaYNoDisponible() {
        Habitacion h = habitacionReservada();
        when(reservas.buscarPorSede("sede-1")).thenReturn(List.of(reservaPagada(h)));
        servicio.cambiarEstadoHabitacion("hab-1", EstadoHabitacion.Mantenimiento, true);

        servicio.cambiarEstadoHabitacion("hab-1", EstadoHabitacion.Disponible, false);

        assertThat(h.getEstado()).isEqualTo(EstadoHabitacion.Reservada); // evita la doble reserva
    }

    @Test
    void sinReservasVigentesNoPideConfirmacion() {
        Habitacion h = new Habitacion("sede-1", "301", "Doble", 2, BigDecimal.TEN);
        ReflectionTestUtils.setField(h, "id", "hab-2");
        when(habitaciones.buscarPorId("hab-2")).thenReturn(Optional.of(h));
        when(reservas.buscarPorSede("sede-1")).thenReturn(List.of());

        servicio.cambiarEstadoHabitacion("hab-2", EstadoHabitacion.Mantenimiento, false);

        assertThat(h.getEstado()).isEqualTo(EstadoHabitacion.Mantenimiento);
    }

    // ------------------------------------------------------------ HU-17: ingresos
    @Test
    void losIngresosIncluyenLoRetenidoEnCancelacionesConPenalidad() {
        Habitacion h = new Habitacion("sede-1", "101", "Doble", 2, new BigDecimal("100000"));
        ReflectionTestUtils.setField(h, "id", "hab-3");
        Reserva r = new Reserva("RSV-2", h, "Ana", "123", "a@b.com",
                java.time.LocalDate.now().plusDays(1), java.time.LocalDate.now().plusDays(3), 2);
        r.bloquear(ZonedDateTime.now(ZoneId.of("America/Bogota")), 15);
        r.confirmarPago();
        r.cancelar(EstadoReserva.CanceladaConPenalidad, new BigDecimal("100000"), new BigDecimal("100000"));
        when(reservas.buscarPorSede("sede-1")).thenReturn(List.of(r));
        when(habitaciones.buscarPorSede("sede-1")).thenReturn(List.of(h));

        Map<String, Object> rep = reportes.ingresos("sede-1", java.time.LocalDate.now(), java.time.LocalDate.now().plusDays(10));

        assertThat((BigDecimal) rep.get("ingresosPenalidades")).isEqualByComparingTo("100000");
        assertThat((BigDecimal) rep.get("total")).isEqualByComparingTo("100000");
        assertThat(rep.get("cancelacionesConPenalidad")).isEqualTo(1);
        assertThat(rep).doesNotContainKey("mensaje");
    }

    @Test
    void sinDatosElReporteSaleEnCeroConMensaje() {
        when(reservas.buscarPorSede("sede-1")).thenReturn(List.of());
        Map<String, Object> rep = reportes.ingresos("sede-1", java.time.LocalDate.now(), java.time.LocalDate.now().plusDays(5));
        assertThat((BigDecimal) rep.get("total")).isEqualByComparingTo("0");
        assertThat(rep.get("mensaje")).isEqualTo("No hay datos para el período seleccionado");
    }
}
