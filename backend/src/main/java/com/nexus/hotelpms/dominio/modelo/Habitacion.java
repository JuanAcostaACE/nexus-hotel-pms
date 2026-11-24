package com.nexus.hotelpms.dominio.modelo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Document("habitaciones")
@CompoundIndex(name = "sede_numero", def = "{'sedeId':1,'numero':1}", unique = true)
public class Habitacion {
    @Id private String id;
    private String sedeId;
    private String numero;
    private String tipo;
    private int capacidadMax;
    private BigDecimal precioBase;
    private EstadoHabitacion estado = EstadoHabitacion.Disponible;

    protected Habitacion() { }

    public Habitacion(String sedeId, String numero, String tipo, int capacidadMax, BigDecimal precioBase) {
        this.sedeId = sedeId;
        this.numero = numero;
        this.tipo = tipo;
        this.capacidadMax = capacidadMax;
        this.precioBase = precioBase;
    }

    /** HU-15: actualiza datos del inventario (el número de habitación no cambia). */
    public void actualizar(String tipo, int capacidadMax, BigDecimal precioBase) {
        this.tipo = tipo;
        this.capacidadMax = capacidadMax;
        this.precioBase = precioBase;
    }

    public void cambiarEstado(EstadoHabitacion nuevo) { this.estado = nuevo; }
    public boolean estaDisponible() { return estado == EstadoHabitacion.Disponible; }

    public String getId() { return id; }
    public String getSedeId() { return sedeId; }
    public String getNumero() { return numero; }
    public String getTipo() { return tipo; }
    public int getCapacidadMax() { return capacidadMax; }
    public BigDecimal getPrecioBase() { return precioBase; }
    public EstadoHabitacion getEstado() { return estado; }
}
