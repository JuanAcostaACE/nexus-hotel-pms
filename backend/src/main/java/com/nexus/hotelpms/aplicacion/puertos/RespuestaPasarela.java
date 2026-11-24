package com.nexus.hotelpms.aplicacion.puertos;

import com.nexus.hotelpms.dominio.modelo.EstadoPago;

/** Respuesta normalizada de la pasarela de pagos. */
public record RespuestaPasarela(String transaccionId, EstadoPago estado) { }
