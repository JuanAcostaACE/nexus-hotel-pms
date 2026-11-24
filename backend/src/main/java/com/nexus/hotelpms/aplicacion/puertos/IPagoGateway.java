package com.nexus.hotelpms.aplicacion.puertos;

import java.math.BigDecimal;

/** Patrón Adapter */
public interface IPagoGateway {
    /**
     * @param resultadoSimulado solo para el simulador: "Aprobado", "Rechazado" o "Pendiente" (null = Aprobado)
     */
    RespuestaPasarela iniciarPago(String referencia, BigDecimal monto, String banco, String resultadoSimulado);
}
