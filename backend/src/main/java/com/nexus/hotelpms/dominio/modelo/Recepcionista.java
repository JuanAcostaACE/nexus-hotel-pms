package com.nexus.hotelpms.dominio.modelo;

/** Opera únicamente sobre la sede a la que está asignado. */
public class Recepcionista extends Usuario {
    private String sedeId;

    protected Recepcionista() { }

    public Recepcionista(String nombre, String email, String passwordHash, String sedeId) {
        super(nombre, email, passwordHash);
        this.sedeId = sedeId;
    }

    /** HU-14: reasigna al recepcionista a otra sede. */
    public void cambiarSede(String nuevaSede) { this.sedeId = nuevaSede; }

    public String getSedeId() { return sedeId; }
    @Override public String getRol() { return "Recepcionista"; }
    @Override public boolean tienePermisoSobre(String sede) { return sedeId != null && sedeId.equals(sede); }
}
