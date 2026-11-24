package com.nexus.hotelpms.dominio.modelo;

/** Acceso global a todas las sedes. */
public class Administrador extends Usuario {
    protected Administrador() { }

    public Administrador(String nombre, String email, String passwordHash) {
        super(nombre, email, passwordHash);
    }

    @Override public String getRol() { return "Administrador"; }
    @Override public boolean tienePermisoSobre(String sedeId) { return true; }
}
