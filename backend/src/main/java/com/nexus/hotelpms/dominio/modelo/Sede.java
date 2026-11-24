package com.nexus.hotelpms.dominio.modelo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Sede del hotel. Compone habitaciones y catálogo (referenciados por {@code sedeId}). */
@Document("sedes")
public class Sede {
    @Id private String id;
    private String nombre;
    private String direccion;
    private boolean activa;

    protected Sede() { }

    public Sede(String nombre, String direccion) {
        this.nombre = nombre;
        this.direccion = direccion;
        this.activa = true;
    }

    /** HU-13: actualiza nombre y dirección. */
    public void actualizar(String nombre, String direccion) { this.nombre = nombre; this.direccion = direccion; }
    public void activar() { this.activa = true; }
    public void inactivar() { this.activa = false; }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDireccion() { return direccion; }
    public boolean isActiva() { return activa; }
}
