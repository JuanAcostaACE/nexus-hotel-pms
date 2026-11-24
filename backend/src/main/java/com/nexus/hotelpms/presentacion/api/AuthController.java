package com.nexus.hotelpms.presentacion.api;

import com.nexus.hotelpms.aplicacion.servicios.AuthService;
import com.nexus.hotelpms.aplicacion.sesion.AuthPrincipal;
import com.nexus.hotelpms.presentacion.dto.Dtos.CambiarPasswordRequest;
import com.nexus.hotelpms.presentacion.dto.Dtos.LoginRequest;
import com.nexus.hotelpms.presentacion.dto.Dtos.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** HU-06. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req.email(), req.password());
    }

    /** Cambio de la propia contraseña (requiere sesión). */
    @PostMapping("/cambiar-password")
    public Map<String, String> cambiarPassword(@AuthenticationPrincipal AuthPrincipal p,
                                               @Valid @RequestBody CambiarPasswordRequest req) {
        auth.cambiarPassword(p.userId(), req.passwordActual(), req.passwordNueva());
        return Map.of("mensaje", "Contraseña actualizada");
    }
}
