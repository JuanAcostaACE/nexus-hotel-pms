package com.nexus.hotelpms.presentacion.api;

import com.nexus.hotelpms.aplicacion.servicios.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Funciones exclusivas del Administrador (HU-06: un recepcionista recibe 403). El resto de la administración (HU-13 a HU-17) está en AdministracionController. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('Administrador')")
public class AdminController {
    private final AuthService auth;

    public AdminController(AuthService auth) { this.auth = auth; }

    @PostMapping("/usuarios/{id}/desbloquear")
    public ResponseEntity<Map<String, String>> desbloquear(@PathVariable String id) {
        auth.desbloquear(id);
        return ResponseEntity.ok(Map.of("mensaje", "Cuenta desbloqueada"));
    }
}
