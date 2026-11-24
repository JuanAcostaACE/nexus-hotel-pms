package com.nexus.hotelpms.aplicacion.sesion;

/** Identidad del usuario autenticado (sale del JWT). {@code sedeId} es null para administradores. */
public record AuthPrincipal(String userId, String nombre, String rol, String sedeId) {

    public boolean esAdministrador() { return "Administrador".equals(rol); }

    /** Aislamiento multi-sede: el administrador ve todo; el recepcionista solo su sede. */
    public boolean puedeAccederASede(String sede) {
        return esAdministrador() || (sedeId != null && sedeId.equals(sede));
    }
}
