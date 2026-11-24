package com.nexus.hotelpms.dominio.excepcion;

/** Cuenta bloqueada tras intentos fallidos (HTTP 423). Se traduce en {@code ManejadorExcepciones}. */
public class CuentaBloqueadaException extends RuntimeException {
    public CuentaBloqueadaException(String mensaje) {
        super(mensaje);
    }
}
