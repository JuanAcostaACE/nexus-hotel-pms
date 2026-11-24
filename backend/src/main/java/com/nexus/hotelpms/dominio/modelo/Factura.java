package com.nexus.hotelpms.dominio.modelo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Factura final consolidada. {@code pdfUrl} apunta al endpoint que genera el PDF bajo demanda. */
@Document("facturas")
public class Factura {
    @Id private String id;
    @Indexed(unique = true) private String reservaId;
    @Indexed(unique = true) private String numeroFactura;
    private String pdfUrl;
    private BigDecimal subtotalEstadia;
    private BigDecimal subtotalConsumos;
    private BigDecimal recargoLateCheckout;
    private List<LineaImpuesto> impuestosDetalle;
    private BigDecimal totalSinImpuestos;
    private BigDecimal totalImpuestos;
    private BigDecimal total;
    private BigDecimal pagadoAnticipado;
    private BigDecimal saldoPendiente;
    private Instant generadaAt;

    protected Factura() { }

    public Factura(String reservaId, String numeroFactura, String pdfUrl, BigDecimal subtotalEstadia,
                   BigDecimal subtotalConsumos, BigDecimal recargoLateCheckout, List<LineaImpuesto> impuestos,
                   BigDecimal pagadoAnticipado, Instant generadaAt) {
        this.reservaId = reservaId;
        this.numeroFactura = numeroFactura;
        this.pdfUrl = pdfUrl;
        this.subtotalEstadia = subtotalEstadia;
        this.subtotalConsumos = subtotalConsumos;
        this.recargoLateCheckout = recargoLateCheckout;
        this.impuestosDetalle = impuestos;
        this.totalSinImpuestos = subtotalEstadia.add(subtotalConsumos).add(recargoLateCheckout);
        this.totalImpuestos = impuestos.stream().map(LineaImpuesto::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
        this.total = totalSinImpuestos.add(totalImpuestos);
        this.pagadoAnticipado = pagadoAnticipado;
        this.saldoPendiente = total.subtract(pagadoAnticipado).max(BigDecimal.ZERO);
        this.generadaAt = generadaAt;
    }

    public String getId() { return id; }
    public String getReservaId() { return reservaId; }
    public String getNumeroFactura() { return numeroFactura; }
    public String getPdfUrl() { return pdfUrl; }
    public BigDecimal getSubtotalEstadia() { return subtotalEstadia; }
    public BigDecimal getSubtotalConsumos() { return subtotalConsumos; }
    public BigDecimal getRecargoLateCheckout() { return recargoLateCheckout; }
    public List<LineaImpuesto> getImpuestosDetalle() { return impuestosDetalle; }
    public BigDecimal getTotalSinImpuestos() { return totalSinImpuestos; }
    public BigDecimal getTotalImpuestos() { return totalImpuestos; }
    public BigDecimal getTotal() { return total; }
    public BigDecimal getPagadoAnticipado() { return pagadoAnticipado; }
    public BigDecimal getSaldoPendiente() { return saldoPendiente; }
    public Instant getGeneradaAt() { return generadaAt; }
}
