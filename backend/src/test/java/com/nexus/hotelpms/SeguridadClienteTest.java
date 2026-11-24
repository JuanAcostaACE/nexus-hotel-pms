package com.nexus.hotelpms;

import com.nexus.hotelpms.aplicacion.puertos.IDocumentoRepository;
import com.nexus.hotelpms.aplicacion.puertos.IEmailSender;
import com.nexus.hotelpms.aplicacion.puertos.IHabitacionRepository;
import com.nexus.hotelpms.aplicacion.puertos.IPdfGenerator;
import com.nexus.hotelpms.aplicacion.puertos.IReservaRepository;
import com.nexus.hotelpms.aplicacion.puertos.ISedeRepository;
import com.nexus.hotelpms.aplicacion.puertos.ITokenService;
import com.nexus.hotelpms.aplicacion.puertos.IUsuarioRepository;
import com.nexus.hotelpms.aplicacion.servicios.ReservaService;
import com.nexus.hotelpms.aplicacion.sesion.AuthPrincipal;
import com.nexus.hotelpms.dominio.excepcion.RecursoNoEncontradoException;
import com.nexus.hotelpms.dominio.modelo.Habitacion;
import com.nexus.hotelpms.dominio.modelo.Recepcionista;
import com.nexus.hotelpms.dominio.modelo.Reserva;
import com.nexus.hotelpms.infraestructura.config.HotelProperties;
import com.nexus.hotelpms.infraestructura.seguridad.JwtAuthFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Acceso del cliente por código + documento, y revalidación de la sesión del personal. */
class SeguridadClienteTest {

    // ------------------------------------------------------------ operaciones públicas del cliente
    private IReservaRepository reservas;
    private ReservaService servicio;

    @BeforeEach
    void preparar() {
        reservas = mock(IReservaRepository.class);
        servicio = new ReservaService(reservas, mock(IHabitacionRepository.class), mock(ISedeRepository.class),
                mock(IEmailSender.class), mock(IPdfGenerator.class), mock(IDocumentoRepository.class), mock(HotelProperties.class), Clock.systemDefaultZone());
        Habitacion h = new Habitacion("sede", "101", "Doble", 2, BigDecimal.TEN);
        ReflectionTestUtils.setField(h, "id", "hab");
        Reserva r = new Reserva("RSV-20261201-001", h, "Ana", "1098765432", "a@b.com",
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(7), 2);
        when(reservas.buscarPorCodigo("RSV-20261201-001")).thenReturn(Optional.of(r));
        when(reservas.buscarPorCodigo("NO-EXISTE")).thenReturn(Optional.empty());
        when(reservas.buscarPorId(any())).thenReturn(Optional.empty());
    }

    @Test
    void conCodigoYDocumentoCorrectosSeAccede() {
        assertThat(servicio.obtenerParaCliente("RSV-20261201-001", " 1098765432 ").getCodigo())
                .isEqualTo("RSV-20261201-001");
    }

    @Test
    void conDocumentoIncorrectoVacioONuloSeRechaza() {
        for (String documento : new String[]{"999", "", "   ", null}) {
            assertThatThrownBy(() -> servicio.obtenerParaCliente("RSV-20261201-001", documento))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }
    }

    @Test
    void elMensajeNoRevelaSiElCodigoExiste() {
        String conCodigoValido = catchMensaje("RSV-20261201-001", "000");
        String conCodigoInexistente = catchMensaje("NO-EXISTE", "000");
        assertThat(conCodigoValido).isEqualTo(conCodigoInexistente);
    }

    private String catchMensaje(String codigo, String documento) {
        try {
            servicio.obtenerParaCliente(codigo, documento);
            return "sin error";
        } catch (RecursoNoEncontradoException e) {
            return e.getMessage();
        }
    }

    // ------------------------------------------------------------ sesión del personal
    private IUsuarioRepository usuarios;
    private ITokenService tokens;
    private JwtAuthFilter filtro;

    private void prepararFiltro() {
        usuarios = mock(IUsuarioRepository.class);
        tokens = mock(ITokenService.class);
        when(tokens.generar(any())).thenReturn("renovado");
        filtro = new JwtAuthFilter(tokens, usuarios);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void limpiar() { SecurityContextHolder.clearContext(); }

    private Recepcionista recepcionista(String sede, boolean activo) {
        Recepcionista u = new Recepcionista("Pedro", "pedro@hotel.com", "hash", sede);
        ReflectionTestUtils.setField(u, "id", "u1");
        if (!activo) u.inactivar();
        return u;
    }

    private MockHttpServletRequest conToken() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer abc");
        return req;
    }

    @Test
    void laSedeSalDelRegistroActualNoDelToken() throws Exception {
        prepararFiltro();
        // el token dice "sede-A", pero el administrador ya reasignó al usuario a "sede-B"
        when(tokens.validar("abc")).thenReturn(new AuthPrincipal("u1", "Pedro", "Recepcionista", "sede-A"));
        when(usuarios.findById("u1")).thenReturn(Optional.of(recepcionista("sede-B", true)));

        filtro.doFilter(conToken(), new MockHttpServletResponse(), mock(FilterChain.class));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(((AuthPrincipal) auth.getPrincipal()).sedeId()).isEqualTo("sede-B");
    }

    @Test
    void unaCuentaDesactivadaPierdeLaSesionAbierta() throws Exception {
        prepararFiltro();
        when(tokens.validar("abc")).thenReturn(new AuthPrincipal("u1", "Pedro", "Recepcionista", "sede-A"));
        when(usuarios.findById("u1")).thenReturn(Optional.of(recepcionista("sede-A", false)));
        MockHttpServletRequest req = conToken();
        MockHttpServletResponse res = new MockHttpServletResponse();

        filtro.doFilter(req, res, mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(req.getAttribute(JwtAuthFilter.ATTR_INACTIVA)).isEqualTo(true);
        assertThat(res.getHeader("X-Token-Renovado")).isNull();
    }

    @Test
    void unUsuarioQueYaNoExisteNoAutentica() throws Exception {
        prepararFiltro();
        when(tokens.validar("abc")).thenReturn(new AuthPrincipal("u1", "Pedro", "Recepcionista", "sede-A"));
        when(usuarios.findById("u1")).thenReturn(Optional.empty());

        filtro.doFilter(conToken(), new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
