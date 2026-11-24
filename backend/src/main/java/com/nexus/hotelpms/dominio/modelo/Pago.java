package com.nexus.hotelpms.dominio.modelo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document("pagos")
public class Pago {
    @Id private String id;
    @Indexed private String reservaId;
    private BigDecimal monto;
    private MetodoPago metodo = MetodoPago.PSE;
    private EstadoPago estado;
    @Indexed private String transaccionId;
    private String banco;
    private Instant creadoAt;
    private Instant webhookRecibidoAt;

    protected Pago() { }

    public Pago(String reservaId, BigDecimal monto, String banco, String transaccionId,
                EstadoPago estado, Instant ahora) {
        this.reservaId = reservaId;
        this.monto = monto;
        this.banco = banco;
        this.transaccionId = transaccionId;
        this.estado = estado;
        this.creadoAt = ahora;
    }

    /** Aplica la respuesta definitiva de PSE (síncrona o por webhook). */
    public void procesarRespuesta(EstadoPago resultado, Instant webhookAt) {
        this.estado = resultado;
        if (webhookAt != null) {
            this.webhookRecibidoAt = webhookAt;
        }
    }

    public String getId() { return id; }
    public String getReservaId() { return reservaId; }
    public BigDecimal getMonto() { return monto; }
    public MetodoPago getMetodo() { return metodo; }
    public EstadoPago getEstado() { return estado; }
    public String getTransaccionId() { return transaccionId; }
    public String getBanco() { return banco; }
    public Instant getCreadoAt() { return creadoAt; }
    public Instant getWebhookRecibidoAt() { return webhookRecibidoAt; }
}
