package com.nexus.hotelpms.presentacion.api;

import com.nexus.hotelpms.aplicacion.servicios.AdministracionService;
import com.nexus.hotelpms.dominio.modelo.CategoriaConsumo;
import com.nexus.hotelpms.dominio.modelo.CatalogoItem;
import com.nexus.hotelpms.dominio.modelo.EstadoHabitacion;
import com.nexus.hotelpms.dominio.modelo.Habitacion;
import com.nexus.hotelpms.dominio.modelo.Reserva;
import com.nexus.hotelpms.dominio.modelo.Sede;
import com.nexus.hotelpms.dominio.modelo.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Fase 2 (HU-13..HU-17). Todo el controlador es exclusivo del Administrador:
 * un recepcionista recibe 403 (HU-06 / HU-14). Los textos de validación salen de las historias de usuario.
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
    public record SedeRequest(@NotBlank(message = "El nombre de la sede es obligatorio") String nombre,
                              @NotBlank(message = "La dirección de la sede es obligatoria") String direccion) { }
    public record ActivaRequest(boolean activa) { }

    @GetMapping("/sedes")
    public List<Sede> listarSedes() {
        return admin.listarTodasLasSedes();
    }

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

    /** HU-13: vista de reservas de una sede (el administrador elige la sede en cada consulta). */
    @GetMapping("/reservas")
    public List<Reserva> reservasDeSede(@RequestParam(required = false) String sedeId) {
        return admin.listarReservas(sedeId);
    }

    // ---- HU-14: usuarios y roles ----
    public record RecepcionistaRequest(
            @NotBlank(message = "El nombre es obligatorio") String nombre,
            @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo no es válido") String email,
            @NotBlank(message = "La contraseña es obligatoria")
            @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres") String password,
            @NotBlank(message = "Debe asignar una sede al usuario para continuar") String sedeId) { }
    public record UsuarioUpdateRequest(
            @NotBlank(message = "El nombre es obligatorio") String nombre,
            @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo no es válido") String email) { }
    public record SedeAsignadaRequest(
            @NotBlank(message = "Debe asignar una sede al usuario para continuar") String sedeId) { }

    /** Con {@code sedeId} solo devuelve los usuarios de esa sede (HU-13); sin él, todos. */
    @GetMapping("/usuarios")
    public List<Usuario> listarUsuarios(@RequestParam(required = false) String sedeId) {
        return admin.listarUsuarios(sedeId);
    }

    @PostMapping("/usuarios")
    public ResponseEntity<Usuario> crearRecepcionista(@Valid @RequestBody RecepcionistaRequest r) {
        return creado(admin.crearRecepcionista(r.nombre(), r.email(), r.password(), r.sedeId()));
    }

    @PutMapping("/usuarios/{id}")
    public Usuario actualizarUsuario(@PathVariable String id, @Valid @RequestBody UsuarioUpdateRequest r) {
        return admin.actualizarUsuario(id, r.nombre(), r.email());
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
    public record HabitacionRequest(
            @NotBlank(message = "La sede es obligatoria") String sedeId,
            @NotBlank(message = "El número de habitación es obligatorio") String numero,
            @NotBlank(message = "El tipo de habitación es obligatorio") String tipo,
            @NotNull(message = "La capacidad máxima es obligatoria") @Min(value = 1, message = "La capacidad máxima debe ser mayor a cero") Integer capacidadMax,
            @NotNull(message = "El precio base es obligatorio") @Positive(message = "El precio base debe ser mayor a cero") BigDecimal precioBase) { }
    public record HabitacionUpdateRequest(
            @NotBlank(message = "El tipo de habitación es obligatorio") String tipo,
            @NotNull(message = "La capacidad máxima es obligatoria") @Min(value = 1, message = "La capacidad máxima debe ser mayor a cero") Integer capacidadMax,
            @NotNull(message = "El precio base es obligatorio") @Positive(message = "El precio base debe ser mayor a cero") BigDecimal precioBase) { }
    /** {@code confirmar}: necesario para enviar a Mantenimiento una habitación con reservas pagadas vigentes. */
    public record EstadoHabitacionRequest(@NotNull(message = "El estado es obligatorio") EstadoHabitacion estado,
                                          Boolean confirmar) { }

    @PostMapping("/habitaciones")
    public ResponseEntity<Habitacion> crearHabitacion(@Valid @RequestBody HabitacionRequest r) {
        return creado(admin.crearHabitacion(r.sedeId(), r.numero(), r.tipo(), r.capacidadMax(), r.precioBase()));
    }

    @PutMapping("/habitaciones/{id}")
    public Habitacion actualizarHabitacion(@PathVariable String id, @Valid @RequestBody HabitacionUpdateRequest r) {
        return admin.actualizarHabitacion(id, r.tipo(), r.capacidadMax(), r.precioBase());
    }

    @PostMapping("/habitaciones/{id}/estado")
    public Map<String, Object> cambiarEstado(@PathVariable String id, @Valid @RequestBody EstadoHabitacionRequest r) {
        return admin.cambiarEstadoHabitacion(id, r.estado(), Boolean.TRUE.equals(r.confirmar()));
    }

    // ---- HU-16: catálogo de consumos ----
    public record ItemRequest(
            @NotBlank(message = "La sede es obligatoria") String sedeId,
            @NotBlank(message = "El nombre del ítem es obligatorio") String nombre,
            @NotNull(message = "El precio es obligatorio") @Positive(message = "El precio debe ser mayor a cero") BigDecimal precio,
            @NotNull(message = "La categoría es obligatoria") CategoriaConsumo categoria) { }
    public record ItemUpdateRequest(
            @NotBlank(message = "El nombre del ítem es obligatorio") String nombre,
            @NotNull(message = "El precio es obligatorio") @Positive(message = "El precio debe ser mayor a cero") BigDecimal precio,
            @NotNull(message = "La categoría es obligatoria") CategoriaConsumo categoria) { }

    @GetMapping("/catalogo")
    public List<CatalogoItem> listarCatalogo(@RequestParam(required = false) String sedeId) {
        return admin.listarCatalogoCompleto(sedeId);
    }

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
    public Map<String, Object> ocupacion(
            @RequestParam(required = false) String sedeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return admin.ocupacion(sedeId, fechaInicio, fechaFin);
    }

    @GetMapping("/reportes/ingresos")
    public Map<String, Object> ingresos(
            @RequestParam(required = false) String sedeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return admin.ingresos(sedeId, fechaInicio, fechaFin);
    }
}
