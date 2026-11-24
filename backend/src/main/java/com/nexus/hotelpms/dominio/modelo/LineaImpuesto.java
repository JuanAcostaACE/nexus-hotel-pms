package com.nexus.hotelpms.dominio.modelo;

import java.math.BigDecimal;

/** Una línea de impuesto local en la factura: nombre, tasa y valor (HU-12). */
public record LineaImpuesto(String nombre, BigDecimal tasa, BigDecimal base, BigDecimal valor) { }
