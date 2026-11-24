package com.nexus.hotelpms.dominio.modelo;

import com.nexus.hotelpms.dominio.excepcion.ReglaNegocioException;
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

    /**
     * Cambios de estado que puede ordenar el administrador (HU-11 y HU-15). Solo se permite pasar a Disponible o
     * Mantenimiento, y únicamente desde estados que lo admiten: Ocupada, Bloqueada y Reservada los gobierna el flujo
     * de la reserva (check-in, pago, cancelación), así que no se pueden forzar a mano.
     */
    public void cambiarEstadoAdministrativo(EstadoHabitacion nuevo) {
        if (nuevo != EstadoHabitacion.Disponible && nuevo != EstadoHabitacion.Mantenimiento) {
            throw new ReglaNegocioException("El administrador solo puede cambiar una habitación a Disponible o Mantenimiento");
        }
        if (estado == nuevo) {
            return;
        }
        switch (estado) {
            case Ocupada -> throw new ReglaNegocioException(nuevo == EstadoHabitacion.Disponible
                    ? "Debe completar el check-out antes de habilitar la habitación"
                    : "No se puede enviar a mantenimiento una habitación ocupada");
            case Bloqueada -> throw new ReglaNegocioException(
                    "La habitación está bloqueada por una reserva en proceso de pago");
            case Reservada -> {
                if (nuevo == EstadoHabitacion.Disponible) {
                    throw new ReglaNegocioException("La habitación tiene una reserva pagada: cancele la reserva para liberarla");
                }
            }
            default -> { }
        }
        this.estado = nuevo;
    }
    public boolean estaDisponible() { return estado == EstadoHabitacion.Disponible; }

    public String getId() { return id; }
    public String getSedeId() { return sedeId; }
    public String getNumero() { return numero; }
    public String getTipo() { return tipo; }
    public int getCapacidadMax() { return capacidadMax; }
    public BigDecimal getPrecioBase() { return precioBase; }
    public EstadoHabitacion getEstado() { return estado; }
}
