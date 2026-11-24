package com.nexus.hotelpms.dominio.excepcion;

/** Recurso inexistente (o fuera de la sede del usuario) (HTTP 404). Se traduce en {@code ManejadorExcepciones}. */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
