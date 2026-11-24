package com.nexus.hotelpms.aplicacion.reembolso;

import com.nexus.hotelpms.dominio.modelo.EstadoReserva;
import com.nexus.hotelpms.dominio.modelo.Reserva;

import java.math.BigDecimal;

/**
 * <b>Patrón Strategy – Concrete Strategy #2.</b> Cancelación con menos de 48 horas: se retiene una noche
 * (precio base de la habitación, sin consumos) y se devuelve el resto. La reserva queda
 * {@code CanceladaConPenalidad}.
 *
 * @see PoliticaReembolso
 */
public class ReembolsoParcial implements PoliticaReembolso {

    @Override
    public ResultadoReembolso calcular(Reserva reserva) {
        BigDecimal penalidad = reserva.getPrecioNoche().min(reserva.getValorTotal());
        return new ResultadoReembolso(reserva.getValorTotal().subtract(penalidad), penalidad,
                EstadoReserva.CanceladaConPenalidad, "REEMBOLSO_PARCIAL");
    }
}
