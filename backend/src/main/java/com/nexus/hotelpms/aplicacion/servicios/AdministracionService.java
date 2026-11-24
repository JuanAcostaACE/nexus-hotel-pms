package com.nexus.hotelpms.aplicacion.servicios;

import com.nexus.hotelpms.aplicacion.puertos.ICatalogoItemRepository;
import com.nexus.hotelpms.aplicacion.puertos.IConsumoRepository;
import com.nexus.hotelpms.aplicacion.puertos.IHabitacionRepository;
import com.nexus.hotelpms.aplicacion.puertos.IReservaRepository;
import com.nexus.hotelpms.aplicacion.puertos.ISedeRepository;
import com.nexus.hotelpms.aplicacion.puertos.IUsuarioRepository;
import com.nexus.hotelpms.dominio.excepcion.ConflictoException;
import com.nexus.hotelpms.dominio.excepcion.RecursoNoEncontradoException;
import com.nexus.hotelpms.dominio.excepcion.ReglaNegocioException;
import com.nexus.hotelpms.dominio.modelo.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.LinkedHashMap;
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
    private final IConsumoRepository consumos;
    private final PasswordEncoder encoder;

    public AdministracionService(ISedeRepository sedes, IUsuarioRepository usuarios,
                                 IHabitacionRepository habitaciones, ICatalogoItemRepository catalogo,
                                 IReservaRepository reservas, IConsumoRepository consumos, PasswordEncoder encoder) {
        this.sedes = sedes;
        this.usuarios = usuarios;
        this.habitaciones = habitaciones;
        this.catalogo = catalogo;
        this.reservas = reservas;
        this.consumos = consumos;
        this.encoder = encoder;
    }

    // ---------- HU-13: sedes ----------

    /** HU-13: listado completo para el administrador (incluye sedes inactivas). */
    public List<Sede> listarTodasLasSedes() {
        return sedes.findAll();
    }

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

    /** HU-13/14: con {@code sedeId} lista solo los recepcionistas de esa sede; sin él, todos los usuarios. */
    public List<Usuario> listarUsuarios(String sedeId) {
        List<Usuario> todos = usuarios.findAll();
        if (sedeId == null || sedeId.isBlank()) {
            return todos;
        }
        validarSede(sedeId);
        return todos.stream()
                .filter(u -> u instanceof Recepcionista r && sedeId.equals(r.getSedeId()))
                .collect(Collectors.toList());
    }

    /** HU-14: edita nombre y correo; el rol y la sede no cambian. */
    public Usuario actualizarUsuario(String usuarioId, String nombre, String email) {
        Usuario u = buscarUsuario(usuarioId);
        String correo = normalizarCorreo(email);
        usuarios.findByEmail(correo).filter(otro -> !otro.getId().equals(usuarioId)).ifPresent(otro -> {
            throw new ReglaNegocioException("Ya existe un usuario con ese correo");
        });
        u.actualizarDatos(nombre.trim(), correo);
        return usuarios.save(u);
    }

    /** HU-13: vista de reservas de la sede elegida (nunca mezcla sedes). */
    public List<Reserva> listarReservas(String sedeId) {
        if (sedeId == null || sedeId.isBlank()) {
            throw new ReglaNegocioException("Debe seleccionar una sede para continuar");
        }
        validarSede(sedeId);
        return reservas.buscarPorSede(sedeId);
    }

    public Usuario crearRecepcionista(String nombre, String email, String password, String sedeId) {
        validarSede(sedeId);
        String correo = normalizarCorreo(email);
        if (usuarios.findByEmail(correo).isPresent()) {
            throw new ReglaNegocioException("Ya existe un usuario con ese correo");
        }
        return usuarios.save(new Recepcionista(nombre.trim(), correo, encoder.encode(password), sedeId));
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
        String num = numero.trim();
        String mensaje = "Ya existe una habitación con el número " + num + " en esta sede";
        if (habitaciones.buscarPorSede(sedeId).stream().anyMatch(h -> h.getNumero().equalsIgnoreCase(num))) {
            throw new ConflictoException(mensaje);
        }
        try {
            return habitaciones.guardar(new Habitacion(sedeId, num, tipo.trim(), capacidad, precio));
        } catch (DuplicateKeyException e) {
            throw new ConflictoException(mensaje);
        }
    }

    public Habitacion actualizarHabitacion(String id, String tipo, int capacidad, BigDecimal precio) {
        Habitacion h = habitaciones.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Habitación no encontrada"));
        h.actualizar(tipo, capacidad, precio);
        return habitaciones.guardar(h);
    }

    /**
     * Cambio de estado administrativo (HU-11 / HU-15). Solo Disponible y Mantenimiento, con las transiciones que admite
     * {@link Habitacion#cambiarEstadoAdministrativo}. Si la habitación tiene reservas pagadas vigentes, enviarla a
     * Mantenimiento exige confirmación explícita ({@code confirmar=true}); y al volver a Disponible una habitación con
     * reserva pagada queda Reservada, para que nadie más pueda reservarla.
     */
    public Map<String, Object> cambiarEstadoHabitacion(String id, EstadoHabitacion nuevo, boolean confirmar) {
        Habitacion h = habitaciones.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Habitación no encontrada"));
        long vigentes = reservasPagadasDe(h);
        if (nuevo == EstadoHabitacion.Mantenimiento && h.getEstado() != EstadoHabitacion.Mantenimiento
                && vigentes > 0 && !confirmar) {
            throw new ConflictoException("La habitación tiene " + vigentes
                    + " reserva(s) pagada(s) vigente(s) que se verían afectadas. Confirme para continuar");
        }
        EstadoHabitacion antes = h.getEstado();
        h.cambiarEstadoAdministrativo(nuevo);
        if (nuevo == EstadoHabitacion.Disponible && antes != EstadoHabitacion.Disponible && vigentes > 0) {
            h.cambiarEstado(EstadoHabitacion.Reservada);
        }
        habitaciones.guardar(h);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("habitacion", h);
        r.put("reservasFuturasAfectadas", vigentes);
        return r;
    }

    private long reservasPagadasDe(Habitacion h) {
        return reservas.buscarPorSede(h.getSedeId()).stream()
                .filter(r -> h.getId().equals(r.getHabitacionId()))
                .filter(r -> r.getEstado() == EstadoReserva.Pagada)
                .count();
    }

    // ---------- HU-16: catálogo de consumos ----------

    /** HU-16: catálogo completo de una sede (activos e inactivos) para poder reactivar ítems. */
    public List<CatalogoItem> listarCatalogoCompleto(String sedeId) {
        if (sedeId == null || sedeId.isBlank()) {
            throw new ReglaNegocioException("Debe seleccionar una sede para continuar");
        }
        validarSede(sedeId);
        return catalogo.findBySedeId(sedeId);
    }

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
        validarParametrosReporte(sedeId, inicio, fin);
        long totalHabitaciones = habitaciones.buscarPorSede(sedeId).size();
        long nochesRango = java.time.temporal.ChronoUnit.DAYS.between(inicio, fin);
        long nochesVendidas = reservasConfirmadas(sedeId).stream()
                .mapToLong(r -> nochesEnRango(r, inicio, fin))
                .sum();
        double capacidad = (double) totalHabitaciones * nochesRango;
        double porcentaje = capacidad == 0 ? 0 : Math.round(nochesVendidas * 1000.0 / capacidad) / 10.0;
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("sedeId", sedeId);
        r.put("fechaInicio", inicio);
        r.put("fechaFin", fin);
        r.put("nochesVendidas", nochesVendidas);
        r.put("nochesDisponibles", (long) capacidad);
        r.put("porcentajeOcupacion", porcentaje);
        if (nochesVendidas == 0) {
            r.put("mensaje", "No hay datos para el período seleccionado");
        }
        return r;
    }

    /**
     * Ingresos de la sede en el rango: reservas confirmadas cuya fecha de salida cae dentro del período, más lo
     * retenido en cancelaciones con penalidad (generaron ingreso parcial, HU-17). Se detallan estadía, recargo por
     * salida tardía, consumos (sin los anulados) y penalidades, y el total general.
     */
    public Map<String, Object> ingresos(String sedeId, LocalDate inicio, LocalDate fin) {
        validarParametrosReporte(sedeId, inicio, fin);
        List<Reserva> enRango = reservasConfirmadas(sedeId).stream()
                .filter(r -> salidaEnRango(r, inicio, fin))
                .collect(Collectors.toList());
        List<Reserva> conPenalidad = reservas.buscarPorSede(sedeId).stream()
                .filter(r -> r.getEstado() == EstadoReserva.CanceladaConPenalidad)
                .filter(r -> salidaEnRango(r, inicio, fin))
                .collect(Collectors.toList());
        BigDecimal estadia = enRango.stream().map(Reserva::getValorTotal)
                .map(v -> v == null ? BigDecimal.ZERO : v).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal recargo = enRango.stream().map(Reserva::getRecargoLateCheckout)
                .map(v -> v == null ? BigDecimal.ZERO : v).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal porConsumos = enRango.stream()
                .flatMap(r -> consumos.findByReservaId(r.getId()).stream())
                .filter(c -> !c.isAnulado())
                .map(Consumo::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal penalidades = conPenalidad.stream().map(Reserva::getValorRetenido)
                .map(v -> v == null ? BigDecimal.ZERO : v).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = estadia.add(recargo).add(porConsumos).add(penalidades);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("sedeId", sedeId);
        r.put("fechaInicio", inicio);
        r.put("fechaFin", fin);
        r.put("ingresosEstadia", estadia);
        r.put("ingresosRecargoSalidaTardia", recargo);
        r.put("ingresosConsumos", porConsumos);
        r.put("ingresosPenalidades", penalidades);
        r.put("total", total);
        r.put("ingresos", total);
        r.put("reservasIncluidas", enRango.size());
        r.put("cancelacionesConPenalidad", conPenalidad.size());
        if (enRango.isEmpty() && conPenalidad.isEmpty()) {
            r.put("mensaje", "No hay datos para el período seleccionado");
        }
        return r;
    }

    private static boolean salidaEnRango(Reserva r, LocalDate inicio, LocalDate fin) {
        return !r.getFechaSalida().isBefore(inicio) && !r.getFechaSalida().isAfter(fin);
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

    private void validarParametrosReporte(String sedeId, LocalDate inicio, LocalDate fin) {
        if (sedeId == null || sedeId.isBlank()) {
            throw new ReglaNegocioException("Debe seleccionar una sede para generar el reporte");
        }
        if (inicio == null || fin == null) {
            throw new ReglaNegocioException("Debe indicar la fecha de inicio y la fecha de fin");
        }
        if (!inicio.isBefore(fin)) {
            throw new ReglaNegocioException("La fecha de inicio debe ser anterior a la fecha de fin");
        }
        validarSede(sedeId);
    }

    private void validarSede(String sedeId) {
        if (sedeId == null || sedes.findById(sedeId).isEmpty()) {
            throw new RecursoNoEncontradoException("Sede no encontrada");
        }
    }

    /** El inicio de sesión busca el correo en minúsculas, así que se guarda siempre normalizado. */
    private static String normalizarCorreo(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private Usuario buscarUsuario(String id) {
        return usuarios.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }
}
