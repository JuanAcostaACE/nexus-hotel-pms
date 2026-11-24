package com.nexus.hotelpms.dominio.excepcion;

/** Rol sin permisos (HTTP 403). Se traduce en {@code ManejadorExcepciones}. */
public class AccesoDenegadoException extends RuntimeException {
    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
