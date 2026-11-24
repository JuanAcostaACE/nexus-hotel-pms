package com.nexus.hotelpms.aplicacion.reembolso;

import com.nexus.hotelpms.dominio.modelo.EstadoReserva;
import com.nexus.hotelpms.dominio.modelo.Reserva;

import java.math.BigDecimal;

/**
 * <b>Patrón Strategy – Concrete Strategy #1.</b> Cancelación con 48 horas o más de anticipación (el límite exacto de
 * 48 h también cae aquí): devuelve el 100 % de lo pagado y la reserva queda {@code Cancelada}.
 *
 * @see PoliticaReembolso
 */
public class ReembolsoTotal implements PoliticaReembolso {

    @Override
    public ResultadoReembolso calcular(Reserva reserva) {
        return new ResultadoReembolso(reserva.getValorTotal(), BigDecimal.ZERO,
                EstadoReserva.Cancelada, "REEMBOLSO_TOTAL");
    }
}
