package com.nexus.hotelpms.dominio.excepcion;

/** Credenciales incorrectas (HTTP 401). Se traduce en {@code ManejadorExcepciones}. */
public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}
