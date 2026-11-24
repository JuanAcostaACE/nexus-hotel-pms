package com.nexus.hotelpms.dominio.excepcion;

/** Conflicto de concurrencia o estado (HTTP 409). Se traduce en {@code ManejadorExcepciones}. */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
