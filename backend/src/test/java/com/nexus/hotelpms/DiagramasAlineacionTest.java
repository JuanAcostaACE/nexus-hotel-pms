package com.nexus.hotelpms;

import com.nexus.hotelpms.aplicacion.puertos.IConsumoRepository;
import com.nexus.hotelpms.aplicacion.puertos.IDocumentoRepository;
import com.nexus.hotelpms.aplicacion.puertos.IEmailSender;
import com.nexus.hotelpms.aplicacion.puertos.IHabitacionRepository;
import com.nexus.hotelpms.aplicacion.puertos.IPdfGenerator;
import com.nexus.hotelpms.aplicacion.puertos.IReservaRepository;
import com.nexus.hotelpms.aplicacion.puertos.ISedeRepository;
import com.nexus.hotelpms.aplicacion.puertos.IUsuarioRepository;
import com.nexus.hotelpms.aplicacion.puertos.ICatalogoItemRepository;
import com.nexus.hotelpms.aplicacion.servicios.AdministracionService;
import com.nexus.hotelpms.dominio.excepcion.ReglaNegocioException;
import com.nexus.hotelpms.dominio.modelo.Administrador;
import com.nexus.hotelpms.dominio.modelo.Sede;
import com.nexus.hotelpms.infraestructura.correo.CorreoOutbox;
import com.nexus.hotelpms.infraestructura.correo.CorreoOutboxRepository;
import com.nexus.hotelpms.infraestructura.correo.SmtpEmailSender;
import com.nexus.hotelpms.infraestructura.jobs.PublicadorOutbox;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Piezas que los diagramas de clases y de despliegue piden: sede activa del administrador, outbox de correo y PDF guardados. */
class DiagramasAlineacionTest {

    // ---------------------------------------------------- Administrador.cambiarSedeActiva (clases)
    @Test
    void elAdministradorRecuerdaSuSedeActiva() {
        ISedeRepository sedes = mock(ISedeRepository.class);
        IUsuarioRepository usuarios = mock(IUsuarioRepository.class);
        Administrador admin = new Administrador("Admin", "admin@hotel.com", "hash");
        ReflectionTestUtils.setField(admin, "id", "adm-1");
        when(sedes.findById("sede-1")).thenReturn(Optional.of(new Sede("Norte", "Calle 1")));
        when(usuarios.findById("adm-1")).thenReturn(Optional.of(admin));
        when(usuarios.save(any())).thenAnswer(i -> i.getArgument(0));
        AdministracionService servicio = new AdministracionService(sedes, usuarios,
                mock(IHabitacionRepository.class), mock(ICatalogoItemRepository.class),
                mock(IReservaRepository.class), mock(PasswordEncoder.class));

        Map<String, Object> r = servicio.cambiarSedeActiva("adm-1", "sede-1");

        assertThat(r.get("sedeId")).isEqualTo("sede-1");
        assertThat(servicio.sedeActivaId("adm-1")).isEqualTo("sede-1");
    }

    @Test
    void sinSedeActivaLasConsultasPidenElegirUna() {
        IUsuarioRepository usuarios = mock(IUsuarioRepository.class);
        Administrador admin = new Administrador("Admin", "admin@hotel.com", "hash");
        when(usuarios.findById("adm-1")).thenReturn(Optional.of(admin));
        AdministracionService servicio = new AdministracionService(mock(ISedeRepository.class), usuarios,
                mock(IHabitacionRepository.class), mock(ICatalogoItemRepository.class),
                mock(IReservaRepository.class), mock(PasswordEncoder.class));

        assertThatThrownBy(() -> servicio.sedeActivaId("adm-1"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Debe seleccionar una sede para continuar");
    }

    // ---------------------------------------------------- Outbox de correo (despliegue)
    @Test
    @SuppressWarnings("unchecked")
    void elPublicadorEnviaLosPendientesYLosMarcaEnviados() {
        CorreoOutboxRepository cola = mock(CorreoOutboxRepository.class);
        SmtpEmailSender smtp = mock(SmtpEmailSender.class);
        CorreoOutbox c = new CorreoOutbox("a@b.com", "Asunto", "Cuerpo", "x.pdf", new byte[]{1, 2}, java.time.Instant.now());
        when(cola.findByEstadoOrderByCreadoAtAsc(eq(CorreoOutbox.Estado.Pendiente), any(Pageable.class))).thenReturn(List.of(c));

        new PublicadorOutbox(cola, smtp, Clock.systemUTC()).publicarPendientes();

        verify(smtp).enviar("a@b.com", "Asunto", "Cuerpo", "x.pdf", new byte[]{1, 2});
        assertThat(c.getEstado()).isEqualTo(CorreoOutbox.Estado.Enviado);
        assertThat(c.getAdjunto()).isNull();
        verify(cola).save(c);
    }

    @Test
    void siElEnvioFallaElCorreoQuedaPendienteHastaAgotarIntentos() {
        CorreoOutboxRepository cola = mock(CorreoOutboxRepository.class);
        SmtpEmailSender smtp = mock(SmtpEmailSender.class);
        CorreoOutbox c = new CorreoOutbox("a@b.com", "Asunto", "Cuerpo", null, null, java.time.Instant.now());
        when(cola.findByEstadoOrderByCreadoAtAsc(eq(CorreoOutbox.Estado.Pendiente), any(Pageable.class))).thenReturn(List.of(c));
        org.mockito.Mockito.doThrow(new IllegalStateException("sin servidor")).when(smtp).enviar(anyString(), anyString(), anyString(), any(), any());

        new PublicadorOutbox(cola, smtp, Clock.systemUTC()).publicarPendientes();

        assertThat(c.getEstado()).isEqualTo(CorreoOutbox.Estado.Pendiente);
        assertThat(c.getIntentos()).isEqualTo(1);
        for (int i = 1; i < CorreoOutbox.MAX_INTENTOS; i++) c.registrarFallo("sin servidor");
        assertThat(c.getEstado()).isEqualTo(CorreoOutbox.Estado.Fallido);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sinServidorSmtpElCorreoSoloSeEscribeEnElLog() {
        ObjectProvider<org.springframework.mail.javamail.JavaMailSender> proveedor = mock(ObjectProvider.class);
        when(proveedor.getIfAvailable()).thenReturn(null);
        SmtpEmailSender smtp = new SmtpEmailSender(proveedor, "no-reply@x.com");

        assertThat(smtp.smtpConfigurado()).isFalse();
        smtp.enviar("a@b.com", "Asunto", "Cuerpo", null, null); // no lanza excepción
    }

    // ---------------------------------------------------- PDF guardado (despliegue)
    @Test
    void elComprobanteSeGeneraUnaVezYDespuesSeLeeDelAlmacenamiento() {
        IDocumentoRepository docs = mock(IDocumentoRepository.class);
        IPdfGenerator pdf = mock(IPdfGenerator.class);
        IReservaRepository reservas = mock(IReservaRepository.class);
        com.nexus.hotelpms.dominio.modelo.Habitacion h = new com.nexus.hotelpms.dominio.modelo.Habitacion(
                "sede-1", "101", "Doble", 2, new java.math.BigDecimal("100000"));
        ReflectionTestUtils.setField(h, "id", "hab-1");
        com.nexus.hotelpms.dominio.modelo.Reserva r = new com.nexus.hotelpms.dominio.modelo.Reserva("RSV-1", h, "Ana", "123",
                "a@b.com", java.time.LocalDate.now().plusDays(5), java.time.LocalDate.now().plusDays(7), 2);
        ReflectionTestUtils.setField(r, "id", "res-1");
        r.bloquear(java.time.ZonedDateTime.now(), 15);
        r.confirmarPago();
        when(reservas.buscarPorCodigo("RSV-1")).thenReturn(Optional.of(r));
        when(reservas.buscarPorId("RSV-1")).thenReturn(Optional.of(r));
        ISedeRepository sedes = mock(ISedeRepository.class);
        when(sedes.findById(any())).thenReturn(Optional.of(new Sede("Norte", "Calle 1")));
        when(pdf.generarComprobante(any(), anyString())).thenReturn(new byte[]{9});
        com.nexus.hotelpms.aplicacion.servicios.ReservaService servicio = new com.nexus.hotelpms.aplicacion.servicios.ReservaService(
                reservas, mock(IHabitacionRepository.class), sedes, mock(IEmailSender.class), pdf, docs,
                mock(com.nexus.hotelpms.infraestructura.config.HotelProperties.class), Clock.systemDefaultZone());

        // 1.ª descarga: no hay PDF guardado -> se genera y se escribe
        when(docs.leer(IDocumentoRepository.COMPROBANTE, "res-1")).thenReturn(Optional.empty());
        assertThat(servicio.comprobante("RSV-1")).containsExactly(9);
        verify(docs).guardar(eq(IDocumentoRepository.COMPROBANTE), eq("res-1"), anyString(), any());

        // 2.ª descarga: ya existe -> se lee y no se vuelve a generar
        when(docs.leer(IDocumentoRepository.COMPROBANTE, "res-1")).thenReturn(Optional.of(new byte[]{7}));
        assertThat(servicio.comprobante("RSV-1")).containsExactly(7);
        verify(pdf, org.mockito.Mockito.times(1)).generarComprobante(any(), anyString());
        verify(docs, org.mockito.Mockito.times(1)).guardar(anyString(), anyString(), anyString(), any());
        verify(pdf, never()).generarFactura(any(), any(), anyString(), any());
    }
}
