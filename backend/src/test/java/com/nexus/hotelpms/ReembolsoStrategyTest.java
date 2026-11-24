package com.nexus.hotelpms;

import com.nexus.hotelpms.aplicacion.reembolso.*;
import com.nexus.hotelpms.dominio.modelo.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifica el patrón Strategy de HU-04: elección de política y cálculo del reembolso. */
class ReembolsoStrategyTest {
    private final ZoneId zona = ZoneId.of("America/Bogota");
    private final LocalDate entrada = LocalDate.of(2026, 10, 10);
    private final SelectorPoliticaReembolso selector = new SelectorPoliticaReembolso();

    private Reserva reserva() {
        Habitacion h = new Habitacion("sede1", "101", "Doble", 2, new BigDecimal("100000"));
        return new Reserva("RSV-1", h, "Ana", "123", "a@x.co", entrada, entrada.plusDays(3), 2);
    }

    @Test
    void masDe48HorasAplicaReembolsoTotal() {
        ZonedDateTime checkin = entrada.atStartOfDay(zona);
        assertThat(selector.seleccionar(reserva(), checkin.minusHours(72))).isInstanceOf(ReembolsoTotal.class);
    }

    @Test
    void exactamente48HorasAplicaReembolsoTotal() {
        ZonedDateTime checkin = entrada.atStartOfDay(zona);
        assertThat(selector.seleccionar(reserva(), checkin.minusHours(48))).isInstanceOf(ReembolsoTotal.class);
    }

    @Test
    void menosDe48HorasAplicaReembolsoParcial() {
        ZonedDateTime checkin = entrada.atStartOfDay(zona);
        assertThat(selector.seleccionar(reserva(), checkin.minusHours(47))).isInstanceOf(ReembolsoParcial.class);
    }

    @Test
    void parcialRetieneUnaNoche() {
        ResultadoReembolso r = new ReembolsoParcial().calcular(reserva());
        assertThat(r.retenido()).isEqualByComparingTo("100000");
        assertThat(r.reembolso()).isEqualByComparingTo("200000");
        assertThat(r.estadoFinal()).isEqualTo(EstadoReserva.CanceladaConPenalidad);
    }

    @Test
    void totalDevuelveTodo() {
        ResultadoReembolso r = new ReembolsoTotal().calcular(reserva());
        assertThat(r.reembolso()).isEqualByComparingTo("300000");
        assertThat(r.retenido()).isEqualByComparingTo("0");
        assertThat(r.estadoFinal()).isEqualTo(EstadoReserva.Cancelada);
    }
}
