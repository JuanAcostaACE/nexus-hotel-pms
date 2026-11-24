package com.nexus.hotelpms.aplicacion.reembolso;

import com.nexus.hotelpms.dominio.modelo.Reserva;

import java.time.ZonedDateTime;

/** Patrón Strategy */
public class SelectorPoliticaReembolso {
    public static final long UMBRAL_HORAS = 48;

    private final PoliticaReembolso total = new ReembolsoTotal();
    private final PoliticaReembolso parcial = new ReembolsoParcial();

    public PoliticaReembolso seleccionar(Reserva reserva, ZonedDateTime ahora) {
        ZonedDateTime checkin = reserva.getFechaEntrada().atStartOfDay(ahora.getZone());
        // Con 48 h exactas (o más) de anticipación aplica el reembolso total (HU-04).
        boolean cuarentaYOchoOMas = !ahora.plusHours(UMBRAL_HORAS).isAfter(checkin);
        return cuarentaYOchoOMas ? total : parcial;
    }
}
