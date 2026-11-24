package com.nexus.hotelpms.aplicacion.puertos;

import com.nexus.hotelpms.aplicacion.sesion.AuthPrincipal;

/** Emisión y validación de tokens de sesión (implementado con JWT). */
public interface ITokenService {
    String generar(AuthPrincipal principal);
    /** @throws RuntimeException si el token es inválido o expiró */
    AuthPrincipal validar(String token);
}
