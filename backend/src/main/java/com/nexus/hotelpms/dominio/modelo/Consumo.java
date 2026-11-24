package com.nexus.hotelpms.dominio.modelo;

import com.nexus.hotelpms.dominio.excepcion.ReglaNegocioException;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Consumo cargado a la cuenta de una habitación (HU-09). Los campos de anulación hacen de registro de
 * auditoría: consumo anulado, motivo, recepcionista responsable y timestamp del servidor.
 */
@Document("consumos")
public class Consumo {
    @Id private String id;
    @Indexed private String reservaId;
    private String habitacionId;
    private String sedeId;
    private String catalogoItemId;
    private String nombreItem;
    private int cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
    private Instant registradoAt;
    private String registradoPorId;
    private boolean anulado;
    private String motivoAnulacion;
    private Instant anuladoAt;
    private String anuladoPorId;

    protected Consumo() { }

    public Consumo(Reserva reserva, CatalogoItem item, int cantidad, String usuarioId, Instant ahora) {
        this.reservaId = reserva.getId();
        this.habitacionId = reserva.getHabitacionId();
        this.sedeId = reserva.getSedeId();
        this.catalogoItemId = item.getId();
        this.nombreItem = item.getNombre();
        this.cantidad = cantidad;
        this.precioUnitario = item.getPrecio();
        this.subtotal = item.getPrecio().multiply(BigDecimal.valueOf(cantidad));
        this.registradoAt = ahora;
        this.registradoPorId = usuarioId;
    }

    public void anular(String motivo, String usuarioId, Instant ahora) {
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioException("Debe ingresar un motivo para anular el consumo");
        }
        if (anulado) {
            throw new ReglaNegocioException("El consumo ya fue anulado");
        }
        this.anulado = true;
        this.motivoAnulacion = motivo.trim();
        this.anuladoPorId = usuarioId;
        this.anuladoAt = ahora;
    }

    public String getId() { return id; }
    public String getReservaId() { return reservaId; }
    public String getHabitacionId() { return habitacionId; }
    public String getSedeId() { return sedeId; }
    public String getCatalogoItemId() { return catalogoItemId; }
    public String getNombreItem() { return nombreItem; }
    public int getCantidad() { return cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public BigDecimal getSubtotal() { return subtotal; }
    public Instant getRegistradoAt() { return registradoAt; }
    public String getRegistradoPorId() { return registradoPorId; }
    public boolean isAnulado() { return anulado; }
    public String getMotivoAnulacion() { return motivoAnulacion; }
    public Instant getAnuladoAt() { return anuladoAt; }
    public String getAnuladoPorId() { return anuladoPorId; }
}
