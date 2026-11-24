package com.nexus.hotelpms.infraestructura.pse;

import com.nexus.hotelpms.aplicacion.puertos.IPagoGateway;
import com.nexus.hotelpms.aplicacion.puertos.RespuestaPasarela;
import com.nexus.hotelpms.dominio.modelo.EstadoPago;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * <b>Patrón ADAPTER.</b> Adapta la API de PSE ({@link PseClienteSimulado}: centavos, OK/FAILED/PENDING) a la
 * interfaz que espera la aplicación ({@link IPagoGateway}: {@link BigDecimal} y {@link EstadoPago}). Así el dominio
 * no conoce los detalles del proveedor.
 */
@Component
public class PseGatewayAdapter implements IPagoGateway {
    private final PseClienteSimulado cliente = new PseClienteSimulado();

    @Override
    public RespuestaPasarela iniciarPago(String referencia, BigDecimal monto, String banco, String resultadoSimulado) {
        long centavos = monto.movePointRight(2).longValue();
        PseClienteSimulado.TransaccionPse trx = cliente.crearTransaccion(referencia, centavos, banco, resultadoSimulado);
        EstadoPago estado = switch (trx.estadoPse()) {
            case "OK" -> EstadoPago.Aprobado;
            case "FAILED" -> EstadoPago.Rechazado;
            default -> EstadoPago.Pendiente;
        };
        return new RespuestaPasarela(trx.trxId(), estado);
    }
}
