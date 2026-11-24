package com.nexus.hotelpms.dominio.excepcion;

/** Regla de negocio incumplida (HTTP 422). Se traduce en {@code ManejadorExcepciones}. */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
