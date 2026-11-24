package com.nexus.hotelpms.aplicacion.puertos;

import java.math.BigDecimal;

/** Puerto hacia la pasarela de pagos. Implementado por el Adapter {@code PseGatewayAdapter}. */
public interface IPagoGateway {
    /**
     * @param resultadoSimulado solo para el simulador: "Aprobado", "Rechazado" o "Pendiente" (null = Aprobado)
     */
    RespuestaPasarela iniciarPago(String referencia, BigDecimal monto, String banco, String resultadoSimulado);
}
