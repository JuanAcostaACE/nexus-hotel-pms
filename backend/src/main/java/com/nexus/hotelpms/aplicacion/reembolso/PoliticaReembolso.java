package com.nexus.hotelpms.aplicacion.reembolso;

import com.nexus.hotelpms.dominio.modelo.Reserva;

/**
 * <h2>PATRÓN DE DISEÑO: STRATEGY (HU-04)</h2>
 *
 * <p><b>Rol: interfaz Strategy.</b> Define el contrato común de las distintas políticas con las que se
 * calcula el reembolso al cancelar una reserva pagada.</p>
 *
 * <pre>
 *   ReservaService (Context) ──usa──▶ SelectorPoliticaReembolso ──elige──▶ PoliticaReembolso (Strategy)
 *                                                                              ▲            ▲
 *                                                                   ReembolsoTotal   ReembolsoParcial
 *                                                                   (&gt;= 48 h)        (&lt; 48 h)
 * </pre>
 *
 * <p><b>Por qué Strategy:</b> la regla de reembolso es un algoritmo que varía según el tiempo restante al
 * check-in. Encapsular cada variante en su clase evita un {@code if/else} dentro de {@code ReservaService},
 * cumple Open/Closed (una política nueva, p.ej. "no reembolsable en temporada alta", es una clase nueva y no
 * toca el servicio) y permite probar cada política por separado.</p>
 *
 * <p>Clases del patrón: {@link PoliticaReembolso} (Strategy), {@link ReembolsoTotal} y
 * {@link ReembolsoParcial} (Concrete Strategies), {@link SelectorPoliticaReembolso} (selección de la estrategia)
 * y {@code ReservaService#cancelar} (Context).</p>
 */
public interface PoliticaReembolso {

    /** Calcula el resultado de la cancelación para la reserva dada. */
    ResultadoReembolso calcular(Reserva reserva);
}
