package com.nexus.hotelpms.dominio.modelo;

/** Acceso global a todas las sedes. */
public class Administrador extends Usuario {
    /** HU-13: sede sobre la que el administrador está operando (null si aún no eligió una). */
    private String sedeActivaId;

    protected Administrador() { }

    public Administrador(String nombre, String email, String passwordHash) {
        super(nombre, email, passwordHash);
    }

    @Override public String getRol() { return "Administrador"; }
    @Override public boolean tienePermisoSobre(String sedeId) { return true; }

    /** HU-13: cambia la sede activa; las consultas sin sede explícita se resuelven contra ella. */
    public void cambiarSedeActiva(String sedeId) { this.sedeActivaId = sedeId; }

    public String getSedeActivaId() { return sedeActivaId; }
}
