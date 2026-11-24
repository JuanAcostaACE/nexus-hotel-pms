package com.nexus.hotelpms.aplicacion.reembolso;

import com.nexus.hotelpms.dominio.modelo.EstadoReserva;

import java.math.BigDecimal;

/** Resultado de aplicar una {@link PoliticaReembolso}: cuánto se devuelve, cuánto se retiene y el estado final. */
public record ResultadoReembolso(BigDecimal reembolso, BigDecimal retenido, EstadoReserva estadoFinal, String politica) { }
