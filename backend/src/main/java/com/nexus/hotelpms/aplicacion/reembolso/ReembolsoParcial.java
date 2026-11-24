package com.nexus.hotelpms.aplicacion.reembolso;

import com.nexus.hotelpms.dominio.modelo.EstadoReserva;
import com.nexus.hotelpms.dominio.modelo.Reserva;

import java.math.BigDecimal;

/** Patrón Strategy */
public class ReembolsoParcial implements PoliticaReembolso {

    @Override
    public ResultadoReembolso calcular(Reserva reserva) {
        BigDecimal penalidad = reserva.getPrecioNoche().min(reserva.getValorTotal());
        return new ResultadoReembolso(reserva.getValorTotal().subtract(penalidad), penalidad,
                EstadoReserva.CanceladaConPenalidad, "REEMBOLSO_PARCIAL");
    }
}
