package com.nexus.hotelpms.presentacion.api;

import com.nexus.hotelpms.aplicacion.servicios.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Funciones exclusivas del Administrador (HU-06: un recepcionista recibe 403). CRUD de sedes/usuarios llega en la Fase 2. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('Administrador')")
public class AdminController {
    private final AuthService auth;

    public AdminController(AuthService auth) { this.auth = auth; }

    @GetMapping("/usuarios")
    public java.util.List<com.nexus.hotelpms.dominio.modelo.Usuario> usuarios() {
        return auth.listarUsuarios();
    }

    @PostMapping("/usuarios/{id}/desbloquear")
    public ResponseEntity<Map<String, String>> desbloquear(@PathVariable String id) {
        auth.desbloquear(id);
        return ResponseEntity.ok(Map.of("mensaje", "Cuenta desbloqueada"));
    }
}
