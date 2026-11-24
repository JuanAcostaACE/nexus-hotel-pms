package com.nexus.hotelpms.aplicacion.reembolso;

import com.nexus.hotelpms.dominio.modelo.EstadoReserva;
import com.nexus.hotelpms.dominio.modelo.Reserva;

import java.math.BigDecimal;

/** Patrón Strategy */
public class ReembolsoTotal implements PoliticaReembolso {

    @Override
    public ResultadoReembolso calcular(Reserva reserva) {
        return new ResultadoReembolso(reserva.getValorTotal(), BigDecimal.ZERO,
                EstadoReserva.Cancelada, "REEMBOLSO_TOTAL");
    }
}
