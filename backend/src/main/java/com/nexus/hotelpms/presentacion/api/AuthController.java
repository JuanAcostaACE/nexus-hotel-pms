package com.nexus.hotelpms.presentacion.api;

import com.nexus.hotelpms.aplicacion.servicios.AuthService;
import com.nexus.hotelpms.presentacion.dto.Dtos.LoginRequest;
import com.nexus.hotelpms.presentacion.dto.Dtos.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

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
}
