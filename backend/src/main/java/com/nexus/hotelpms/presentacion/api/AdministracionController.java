package com.nexus.hotelpms.presentacion.api;

import com.nexus.hotelpms.aplicacion.servicios.AdministracionService;
import com.nexus.hotelpms.dominio.modelo.CategoriaConsumo;
import com.nexus.hotelpms.dominio.modelo.CatalogoItem;
import com.nexus.hotelpms.dominio.modelo.EstadoHabitacion;
import com.nexus.hotelpms.dominio.modelo.Habitacion;
import com.nexus.hotelpms.dominio.modelo.Sede;
import com.nexus.hotelpms.dominio.modelo.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Fase 2 (HU-13..HU-17). Todo el controlador es exclusivo del Administrador:
 * un recepcionista recibe 403 (HU-06 / HU-14).
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('Administrador')")
public class AdministracionController {

    private final AdministracionService admin;

    public AdministracionController(AdministracionService admin) { this.admin = admin; }

    private static <T> ResponseEntity<T> creado(T cuerpo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuerpo);
    }

    // ---- HU-13: sedes ----
    public record SedeRequest(@NotBlank String nombre, @NotBlank String direccion) { }
    public record ActivaRequest(boolean activa) { }

    @PostMapping("/sedes")
    public ResponseEntity<Sede> crearSede(@Valid @RequestBody SedeRequest r) {
        return creado(admin.crearSede(r.nombre(), r.direccion()));
    }

    @PutMapping("/sedes/{id}")
    public Sede actualizarSede(@PathVariable String id, @Valid @RequestBody SedeRequest r) {
        return admin.actualizarSede(id, r.nombre(), r.direccion());
    }

    @PostMapping("/sedes/{id}/estado")
    public Sede activarSede(@PathVariable String id, @RequestBody ActivaRequest r) {
        return admin.cambiarActivaSede(id, r.activa());
    }

    // ---- HU-14: usuarios y roles ----
    public record RecepcionistaRequest(@NotBlank String nombre, @NotBlank @Email String email,
                                       @NotBlank String password, @NotBlank String sedeId) { }
    public record SedeAsignadaRequest(@NotBlank String sedeId) { }

    @PostMapping("/usuarios")
    public ResponseEntity<Usuario> crearRecepcionista(@Valid @RequestBody RecepcionistaRequest r) {
        return creado(admin.crearRecepcionista(r.nombre(), r.email(), r.password(), r.sedeId()));
    }

    @PutMapping("/usuarios/{id}/sede")
    public Usuario reasignarSede(@PathVariable String id, @Valid @RequestBody SedeAsignadaRequest r) {
        return admin.reasignarSede(id, r.sedeId());
    }

    @DeleteMapping("/usuarios/{id}")
    public Usuario inactivarUsuario(@PathVariable String id) {
        return admin.inactivarUsuario(id);
    }

    // ---- HU-15: inventario de habitaciones ----
    public record HabitacionRequest(@NotBlank String sedeId, @NotBlank String numero, @NotBlank String tipo,
                                    @NotNull @Min(1) Integer capacidadMax, @NotNull BigDecimal precioBase) { }
    public record HabitacionUpdateRequest(@NotBlank String tipo, @NotNull @Min(1) Integer capacidadMax,
                                          @NotNull BigDecimal precioBase) { }
    public record EstadoHabitacionRequest(@NotNull EstadoHabitacion estado) { }

    @PostMapping("/habitaciones")
    public ResponseEntity<Habitacion> crearHabitacion(@Valid @RequestBody HabitacionRequest r) {
        return creado(admin.crearHabitacion(r.sedeId(), r.numero(), r.tipo(),
                r.capacidadMax(), r.precioBase()));
    }

    @PutMapping("/habitaciones/{id}")
    public Habitacion actualizarHabitacion(@PathVariable String id, @Valid @RequestBody HabitacionUpdateRequest r) {
        return admin.actualizarHabitacion(id, r.tipo(), r.capacidadMax(), r.precioBase());
    }

    @PostMapping("/habitaciones/{id}/estado")
    public Map<String, Object> cambiarEstado(@PathVariable String id, @Valid @RequestBody EstadoHabitacionRequest r) {
        return admin.cambiarEstadoHabitacion(id, r.estado());
    }

    // ---- HU-16: catálogo de consumos ----
    public record ItemRequest(@NotBlank String sedeId, @NotBlank String nombre, @NotNull BigDecimal precio,
                              @NotNull CategoriaConsumo categoria) { }
    public record ItemUpdateRequest(@NotBlank String nombre, @NotNull BigDecimal precio,
                                    @NotNull CategoriaConsumo categoria) { }

    @PostMapping("/catalogo")
    public ResponseEntity<CatalogoItem> crearItem(@Valid @RequestBody ItemRequest r) {
        return creado(admin.crearItem(r.sedeId(), r.nombre(), r.precio(), r.categoria()));
    }

    @PutMapping("/catalogo/{id}")
    public CatalogoItem actualizarItem(@PathVariable String id, @Valid @RequestBody ItemUpdateRequest r) {
        return admin.actualizarItem(id, r.nombre(), r.precio(), r.categoria());
    }

    @PostMapping("/catalogo/{id}/estado")
    public CatalogoItem cambiarActivo(@PathVariable String id, @RequestBody ActivaRequest r) {
        return admin.cambiarActivoItem(id, r.activa());
    }

    // ---- HU-17: reportes operativos ----
    @GetMapping("/reportes/ocupacion")
    public Map<String, Object> ocupacion(@RequestParam String sedeId,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return admin.ocupacion(sedeId, fechaInicio, fechaFin);
    }

    @GetMapping("/reportes/ingresos")
    public Map<String, Object> ingresos(@RequestParam String sedeId,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return admin.ingresos(sedeId, fechaInicio, fechaFin);
    }
}
