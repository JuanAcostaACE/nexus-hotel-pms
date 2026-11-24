package com.nexus.hotelpms.infraestructura.pse;

import java.util.UUID;

/**
 * "Servicio externo" que se adapta (Adaptee del patrón Adapter). Simula el SDK/API de PSE con SU propio vocabulario:
 * montos en centavos y estados {@code OK}, {@code FAILED}, {@code PENDING}. En producción se reemplaza por el
 * cliente HTTP real sin tocar el dominio ni los servicios.
 */
public class PseClienteSimulado {

    public record TransaccionPse(String trxId, String estadoPse) { }

    public TransaccionPse crearTransaccion(String referencia, long montoCentavos, String codigoBanco, String modo) {
        String estado = switch (modo == null ? "APROBADO" : modo.toUpperCase()) {
            case "RECHAZADO" -> "FAILED";
            case "PENDIENTE" -> "PENDING";
            default -> "OK";
        };
        return new TransaccionPse("PSE-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(), estado);
    }
}
