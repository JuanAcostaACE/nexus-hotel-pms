package com.nexus.hotelpms.dominio.modelo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Document("catalogo_items")
public class CatalogoItem {
    @Id private String id;
    private String sedeId;
    private String nombre;
    private BigDecimal precio;
    private CategoriaConsumo categoria;
    private boolean activo = true;

    protected CatalogoItem() { }

    public CatalogoItem(String sedeId, String nombre, BigDecimal precio, CategoriaConsumo categoria) {
        this.sedeId = sedeId;
        this.nombre = nombre;
        this.precio = precio;
        this.categoria = categoria;
    }

    /** HU-16: edita un ítem sin borrar el histórico (los consumos guardan su precio original). */
    public void actualizar(String nombre, BigDecimal precio, CategoriaConsumo categoria) {
        this.nombre = nombre;
        this.precio = precio;
        this.categoria = categoria;
    }
    public void activar() { this.activo = true; }
    public void inactivar() { this.activo = false; }

    public String getId() { return id; }
    public String getSedeId() { return sedeId; }
    public String getNombre() { return nombre; }
    public BigDecimal getPrecio() { return precio; }
    public CategoriaConsumo getCategoria() { return categoria; }
    public boolean isActivo() { return activo; }
}
