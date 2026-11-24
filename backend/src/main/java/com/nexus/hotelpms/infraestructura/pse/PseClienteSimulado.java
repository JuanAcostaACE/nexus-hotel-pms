package com.nexus.hotelpms.infraestructura.pse;

import java.util.UUID;

/** Patrón Adapter */
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
