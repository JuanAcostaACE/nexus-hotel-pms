package com.nexus.hotelpms;

import com.nexus.hotelpms.dominio.excepcion.*;
import com.nexus.hotelpms.dominio.modelo.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reglas de negocio del dominio (HU-02, 06, 08, 09, 10, 12) sin necesidad de base de datos. */
class ReservaDominioTest {
    private final ZoneId zona = ZoneId.of("America/Bogota");
    private final LocalDate entrada = LocalDate.of(2026, 10, 10);

    private Reserva pagada() {
        Habitacion h = new Habitacion("sede1", "101", "Doble", 2, new BigDecimal("100000"));
        Reserva r = new Reserva("RSV-1", h, "Ana", "123", "a@x.co", entrada, entrada.plusDays(3), 2);
        r.bloquear(ZonedDateTime.of(2026, 10, 1, 9, 0, 0, 0, zona), 15);
        r.confirmarPago();
        return r;
    }

    @Test
    void bloqueoExpiraALos15Minutos() {
        Habitacion h = new Habitacion("s", "1", "Doble", 2, BigDecimal.TEN);
        Reserva r = new Reserva("R", h, "A", "1", "a@x.co", entrada, entrada.plusDays(1), 1);
        ZonedDateTime t0 = ZonedDateTime.of(2026, 10, 1, 9, 0, 0, 0, zona);
        r.bloquear(t0, 15);
        assertThat(r.bloqueoExpirado(t0.plusMinutes(14))).isFalse();
        assertThat(r.bloqueoExpirado(t0.plusMinutes(15))).isTrue();
    }

    @Test
    void noShowNoPermitidoAntesDeLas2359() {
        assertThatThrownBy(() -> pagada().marcarNoShow(entrada.atTime(20, 0).atZone(zona)))
                .hasMessage("No es posible marcar como No-Show antes de las 23:59 del día de entrada");
    }

    @Test
    void noShowPermitidoDesdeLas2359() {
        Reserva r = pagada();
        r.marcarNoShow(entrada.atTime(23, 59).atZone(zona));
        assertThat(r.getEstado()).isEqualTo(EstadoReserva.NoShow);
    }

    @Test
    void checkInSoloSiPagada() {
        Habitacion h = new Habitacion("s", "1", "Doble", 2, BigDecimal.TEN);
        Reserva r = new Reserva("R", h, "A", "1", "a@x.co", entrada, entrada.plusDays(1), 1);
        assertThatThrownBy(() -> r.checkIn(ZonedDateTime.now(zona)))
                .hasMessage("No se puede realizar el check-in: la reserva no está en estado Pagada");
    }

    @Test
    void checkInAntesDeLaFechaDeEntradaSeRechaza() {
        Reserva r = pagada();
        assertThatThrownBy(() -> r.checkIn(entrada.minusDays(1).atTime(15, 0).atZone(zona)))
                .isInstanceOf(com.nexus.hotelpms.dominio.excepcion.ReglaNegocioException.class)
                .hasMessageContaining("antes de la fecha de entrada");
    }

    @Test
    void cicloCheckInCheckOutCierreYReservaInmutable() {
        Reserva r = pagada();
        r.checkIn(entrada.atTime(15, 0).atZone(zona));
        assertThat(r.getCheckinAt()).isNotNull();
        r.checkOut(entrada.plusDays(3).atTime(14, 0).atZone(zona), new BigDecimal("50000"));
        assertThat(r.getEstado()).isEqualTo(EstadoReserva.PendienteDeCierre);
        r.cerrar();
        assertThatThrownBy(r::asegurarModificable)
                .hasMessage("No se pueden realizar modificaciones en una reserva cerrada");
    }

    @Test
    void anularConsumoExigeMotivo() {
        Reserva r = pagada();
        Consumo c = new Consumo(r, new CatalogoItem("s", "Agua", new BigDecimal("5000"), CategoriaConsumo.Minibar), 2, "u1", Instant.now());
        assertThat(c.getSubtotal()).isEqualByComparingTo("10000");
        assertThatThrownBy(() -> c.anular(" ", "u1", Instant.now()))
                .hasMessage("Debe ingresar un motivo para anular el consumo");
        c.anular("Error de habitación", "u1", Instant.now());
        assertThat(c.isAnulado()).isTrue();
        assertThat(c.getAnuladoPorId()).isEqualTo("u1");
    }

    @Test
    void tresIntentosFallidosBloqueanLaCuenta() {
        Recepcionista rec = new Recepcionista("R", "r@x.co", "hash", "sede1");
        assertThatThrownBy(() -> rec.autenticar(false)).isInstanceOf(CredencialesInvalidasException.class);
        assertThatThrownBy(() -> rec.autenticar(false)).isInstanceOf(CredencialesInvalidasException.class);
        assertThatThrownBy(() -> rec.autenticar(false)).isInstanceOf(CuentaBloqueadaException.class);
        assertThatThrownBy(() -> rec.autenticar(true)).hasMessage("Su cuenta está bloqueada. Contacte al administrador");
        rec.desbloquear();
        rec.autenticar(true);
    }

    @Test
    void recepcionistaSoloOperaEnSuSede() {
        Recepcionista rec = new Recepcionista("R", "r@x.co", "h", "sede1");
        assertThat(rec.tienePermisoSobre("sede1")).isTrue();
        assertThat(rec.tienePermisoSobre("sede2")).isFalse();
        assertThat(new Administrador("A", "a@x.co", "h").tienePermisoSobre("sede2")).isTrue();
    }
}
