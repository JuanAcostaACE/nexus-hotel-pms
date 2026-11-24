package com.nexus.hotelpms.aplicacion.reembolso;

import com.nexus.hotelpms.dominio.modelo.Reserva;

/** Patrón Strategy */
public interface PoliticaReembolso {

    /** Calcula el resultado de la cancelación para la reserva dada. */
    ResultadoReembolso calcular(Reserva reserva);
}
