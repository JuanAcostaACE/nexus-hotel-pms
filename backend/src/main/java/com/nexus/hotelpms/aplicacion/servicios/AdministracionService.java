package com.nexus.hotelpms.aplicacion.servicios;

import com.nexus.hotelpms.aplicacion.puertos.ICatalogoItemRepository;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Fase 2 (HU-13..HU-16): administración de sedes, usuarios, inventario y catálogo (los reportes de HU-17 están en ReporteService).
 * Todas las operaciones son exclusivas del Administrador; el control de rol vive en el controlador.
 */
@Service
public class AdministracionService {

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

    /** HU-13: listado completo para el administrador (incluye sedes inactivas). */
    // Llamado desde: AdminController.listarSedes
    public List<Sede> listarTodasLasSedes() {
        return sedes.findAll();
    }

    // Llamado desde: AdminController.crearSede
    public Sede crearSede(String nombre, String direccion) {
        return sedes.save(new Sede(nombre, direccion));
    }

    // Llamado desde: AdminController.actualizarSede
    public Sede actualizarSede(String id, String nombre, String direccion) {
        Sede s = sedes.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Sede no encontrada"));
        s.actualizar(nombre, direccion);
        return sedes.save(s);
    }

    // Llamado desde: AdminController.activarSede
    public Sede cambiarActivaSede(String id, boolean activa) {
        Sede s = sedes.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Sede no encontrada"));
        if (activa) s.activar(); else s.inactivar();
        return sedes.save(s);
    }

    /** HU-13: sede activa del administrador. Sin sede elegida devuelve {@code sedeId: null}. */
    // Llamado desde: AdminController.sedeActiva
    public Map<String, Object> sedeActiva(String adminId) {
        Administrador a = buscarAdministrador(adminId);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("sedeId", a.getSedeActivaId());
        r.put("sede", a.getSedeActivaId() == null ? null : sedes.findById(a.getSedeActivaId()).orElse(null));
        return r;
    }

    /** HU-13: id de la sede activa, o error si el administrador aún no eligió ninguna. */
    public String sedeActivaId(String adminId) {
        String id = buscarAdministrador(adminId).getSedeActivaId();
        if (id == null) {
            throw new ReglaNegocioException("Debe seleccionar una sede para continuar");
        }
        return id;
    }

    /** HU-13: el administrador elige (y se recuerda) la sede sobre la que va a operar. */
    // Llamado desde: AdminController.cambiarSedeActiva
    public Map<String, Object> cambiarSedeActiva(String adminId, String sedeId) {
        validarSede(sedeId);
        Administrador a = buscarAdministrador(adminId);
        a.cambiarSedeActiva(sedeId);
        usuarios.save(a);
        return sedeActiva(adminId);
    }

    private Administrador buscarAdministrador(String id) {
        if (buscarUsuario(id) instanceof Administrador a) return a;
        throw new ReglaNegocioException("Solo el administrador tiene sede activa");
    }

    // ---------- HU-14: usuarios y roles ----------

    /** HU-13/14: con {@code sedeId} lista solo los recepcionistas de esa sede; sin él, todos los usuarios. */
    // Llamado desde: AdminController.listarUsuarios
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
    // Llamado desde: AdminController.actualizarUsuario
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
    // Llamado desde: AdminController.reservasDeSede
    public List<Reserva> listarReservas(String sedeId) {
        if (sedeId == null || sedeId.isBlank()) {
            throw new ReglaNegocioException("Debe seleccionar una sede para continuar");
        }
        validarSede(sedeId);
        return reservas.buscarPorSede(sedeId);
    }

    // Llamado desde: AdminController.crearRecepcionista
    public Usuario crearRecepcionista(String nombre, String email, String password, String sedeId) {
        validarSede(sedeId);
        String correo = normalizarCorreo(email);
        if (usuarios.findByEmail(correo).isPresent()) {
            throw new ReglaNegocioException("Ya existe un usuario con ese correo");
        }
        return usuarios.save(new Recepcionista(nombre.trim(), correo, encoder.encode(password), sedeId));
    }

    // Llamado desde: AdminController.reasignarSede
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
    // Llamado desde: AdminController.inactivarUsuario
    public Usuario inactivarUsuario(String usuarioId) {
        Usuario u = buscarUsuario(usuarioId);
        u.inactivar();
        return usuarios.save(u);
    }

    // ---------- HU-15: inventario de habitaciones ----------

    // Llamado desde: AdminController.crearHabitacion
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

    // Llamado desde: AdminController.actualizarHabitacion
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
    // Llamado desde: AdminController.cambiarEstado
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
    // Llamado desde: AdminController.listarCatalogo
    public List<CatalogoItem> listarCatalogoCompleto(String sedeId) {
        if (sedeId == null || sedeId.isBlank()) {
            throw new ReglaNegocioException("Debe seleccionar una sede para continuar");
        }
        validarSede(sedeId);
        return catalogo.findBySedeId(sedeId);
    }

    // Llamado desde: AdminController.crearItem
    public CatalogoItem crearItem(String sedeId, String nombre, BigDecimal precio, CategoriaConsumo categoria) {
        validarSede(sedeId);
        return catalogo.save(new CatalogoItem(sedeId, nombre, precio, categoria));
    }

    // Llamado desde: AdminController.actualizarItem
    public CatalogoItem actualizarItem(String id, String nombre, BigDecimal precio, CategoriaConsumo categoria) {
        CatalogoItem it = catalogo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ítem no encontrado"));
        it.actualizar(nombre, precio, categoria);
        return catalogo.save(it);
    }

    // Llamado desde: AdminController.cambiarActivo
    public CatalogoItem cambiarActivoItem(String id, boolean activo) {
        CatalogoItem it = catalogo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ítem no encontrado"));
        if (activo) it.activar(); else it.inactivar();
        return catalogo.save(it);
    }

    // ---------- auxiliares ----------

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
