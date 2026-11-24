package com.nexus.hotelpms.aplicacion.servicios;

import com.nexus.hotelpms.aplicacion.puertos.ICatalogoItemRepository;
import com.nexus.hotelpms.aplicacion.puertos.IHabitacionRepository;
import com.nexus.hotelpms.aplicacion.puertos.IReservaRepository;
import com.nexus.hotelpms.aplicacion.puertos.ISedeRepository;
import com.nexus.hotelpms.aplicacion.puertos.IUsuarioRepository;
import com.nexus.hotelpms.dominio.excepcion.RecursoNoEncontradoException;
import com.nexus.hotelpms.dominio.excepcion.ReglaNegocioException;
import com.nexus.hotelpms.dominio.modelo.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fase 2 (HU-13..HU-17): administración de sedes, usuarios, inventario, catálogo y reportes.
 * Todas las operaciones son exclusivas del Administrador; el control de rol vive en el controlador.
 */
@Service
public class AdministracionService {

    /** Estados que cuentan como ocupación/ingreso confirmado. */
    private static final Set<EstadoReserva> CONFIRMADAS = EnumSet.of(
            EstadoReserva.Pagada, EstadoReserva.EnCurso, EstadoReserva.PendienteDeCierre, EstadoReserva.Cerrada);

    private final ISedeRepository sedes;
    private final IUsuarioRepository usuarios;
    private final IHabitacionRepository habitaciones;
    private final ICatalogoItemRepository catalogo;
    private final IReservaRepository reservas;
    private final PasswordEncoder encoder;

    public AdministracionService(ISedeRepository sedes, IUsuarioRepository usuarios,
                                 IHabitacionRepository habitaciones, ICatalogoItemRepository catalogo,
                                 IReservaRepository reservas, PasswordEncoder encoder) {
        this.sedes = sedes;
        this.usuarios = usuarios;
        this.habitaciones = habitaciones;
        this.catalogo = catalogo;
        this.reservas = reservas;
        this.encoder = encoder;
    }

    // ---------- HU-13: sedes ----------

    public Sede crearSede(String nombre, String direccion) {
        return sedes.save(new Sede(nombre, direccion));
    }

    public Sede actualizarSede(String id, String nombre, String direccion) {
        Sede s = sedes.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Sede no encontrada"));
        s.actualizar(nombre, direccion);
        return sedes.save(s);
    }

    public Sede cambiarActivaSede(String id, boolean activa) {
        Sede s = sedes.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Sede no encontrada"));
        if (activa) s.activar(); else s.inactivar();
        return sedes.save(s);
    }

    // ---------- HU-14: usuarios y roles ----------

    public Usuario crearRecepcionista(String nombre, String email, String password, String sedeId) {
        validarSede(sedeId);
        if (usuarios.findByEmail(email).isPresent()) {
            throw new ReglaNegocioException("Ya existe un usuario con ese correo");
        }
        return usuarios.save(new Recepcionista(nombre, email, encoder.encode(password), sedeId));
    }

    public Usuario reasignarSede(String usuarioId, String nuevaSedeId) {
        Usuario u = buscarUsuario(usuarioId);
        if (!(u instanceof Recepcionista r)) {
            throw new ReglaNegocioException("Solo los recepcionistas tienen sede asignada");
        }
        validarSede(nuevaSedeId);
        r.cambiarSede(nuevaSedeId);
        return usuarios.save(r);
    }

    /** Baja lógica: el usuario se inactiva y no puede iniciar sesión (conserva auditoría). */
    public Usuario inactivarUsuario(String usuarioId) {
        Usuario u = buscarUsuario(usuarioId);
        u.inactivar();
        return usuarios.save(u);
    }

    // ---------- HU-15: inventario de habitaciones ----------

    public Habitacion crearHabitacion(String sedeId, String numero, String tipo, int capacidad, BigDecimal precio) {
        validarSede(sedeId);
        return habitaciones.guardar(new Habitacion(sedeId, numero, tipo, capacidad, precio));
    }

    public Habitacion actualizarHabitacion(String id, String tipo, int capacidad, BigDecimal precio) {
        Habitacion h = habitaciones.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Habitación no encontrada"));
        h.actualizar(tipo, capacidad, precio);
        return habitaciones.guardar(h);
    }

    /**
     * Cambio de estado administrativo (p. ej. Mantenimiento). No se puede sacar de servicio una
     * habitación ocupada; las reservas futuras se devuelven en la respuesta como advertencia.
     */
    public Map<String, Object> cambiarEstadoHabitacion(String id, EstadoHabitacion nuevo) {
        Habitacion h = habitaciones.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Habitación no encontrada"));
        if (h.getEstado() == EstadoHabitacion.Ocupada && nuevo == EstadoHabitacion.Mantenimiento) {
            throw new ReglaNegocioException("No se puede enviar a mantenimiento una habitación ocupada");
        }
        h.cambiarEstado(nuevo);
        habitaciones.guardar(h);
        long futuras = reservas.buscarPorSede(h.getSedeId()).stream()
                .filter(r -> h.getId().equals(r.getHabitacionId()))
                .filter(r -> CONFIRMADAS.contains(r.getEstado()))
                .filter(r -> r.getFechaEntrada().isAfter(LocalDate.now()))
                .count();
        return Map.of("habitacion", h, "reservasFuturasAfectadas", futuras);
    }

    // ---------- HU-16: catálogo de consumos ----------

    public CatalogoItem crearItem(String sedeId, String nombre, BigDecimal precio, CategoriaConsumo categoria) {
        validarSede(sedeId);
        return catalogo.save(new CatalogoItem(sedeId, nombre, precio, categoria));
    }

    public CatalogoItem actualizarItem(String id, String nombre, BigDecimal precio, CategoriaConsumo categoria) {
        CatalogoItem it = catalogo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ítem no encontrado"));
        it.actualizar(nombre, precio, categoria);
        return catalogo.save(it);
    }

    public CatalogoItem cambiarActivoItem(String id, boolean activo) {
        CatalogoItem it = catalogo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ítem no encontrado"));
        if (activo) it.activar(); else it.inactivar();
        return catalogo.save(it);
    }

    // ---------- HU-17: reportes operativos ----------

    /** Ocupación = noches vendidas dentro del rango / (habitaciones de la sede × noches del rango). */
    public Map<String, Object> ocupacion(String sedeId, LocalDate inicio, LocalDate fin) {
        validarRango(inicio, fin);
        validarSede(sedeId);
        long totalHabitaciones = habitaciones.buscarPorSede(sedeId).size();
        long nochesRango = java.time.temporal.ChronoUnit.DAYS.between(inicio, fin);
        long nochesVendidas = reservasConfirmadas(sedeId).stream()
                .mapToLong(r -> nochesEnRango(r, inicio, fin))
                .sum();
        double capacidad = (double) totalHabitaciones * nochesRango;
        double porcentaje = capacidad == 0 ? 0 : Math.round(nochesVendidas * 1000.0 / capacidad) / 10.0;
        return Map.of("sedeId", sedeId, "fechaInicio", inicio, "fechaFin", fin,
                "nochesVendidas", nochesVendidas, "nochesDisponibles", (long) capacidad,
                "porcentajeOcupacion", porcentaje);
    }

    /** Ingresos = valor de las reservas confirmadas cuyo check-out cae dentro del rango. */
    public Map<String, Object> ingresos(String sedeId, LocalDate inicio, LocalDate fin) {
        validarRango(inicio, fin);
        validarSede(sedeId);
        BigDecimal total = reservasConfirmadas(sedeId).stream()
                .filter(r -> !r.getFechaSalida().isBefore(inicio) && !r.getFechaSalida().isAfter(fin))
                .map(Reserva::getValorTotal)
                .map(v -> v == null ? BigDecimal.ZERO : v)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Map.of("sedeId", sedeId, "fechaInicio", inicio, "fechaFin", fin, "ingresos", total);
    }

    // ---------- auxiliares ----------

    private List<Reserva> reservasConfirmadas(String sedeId) {
        return reservas.buscarPorSede(sedeId).stream()
                .filter(r -> CONFIRMADAS.contains(r.getEstado()))
                .collect(Collectors.toList());
    }

    private static long nochesEnRango(Reserva r, LocalDate inicio, LocalDate fin) {
        LocalDate desde = r.getFechaEntrada().isAfter(inicio) ? r.getFechaEntrada() : inicio;
        LocalDate hasta = r.getFechaSalida().isBefore(fin) ? r.getFechaSalida() : fin;
        long n = java.time.temporal.ChronoUnit.DAYS.between(desde, hasta);
        return Math.max(n, 0);
    }

    private void validarRango(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null || !fin.isAfter(inicio)) {
            throw new ReglaNegocioException("El rango de fechas es inválido (fin debe ser posterior a inicio)");
        }
    }

    private void validarSede(String sedeId) {
        if (sedeId == null || sedes.findById(sedeId).isEmpty()) {
            throw new RecursoNoEncontradoException("Sede no encontrada");
        }
    }

    private Usuario buscarUsuario(String id) {
        return usuarios.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }
}
