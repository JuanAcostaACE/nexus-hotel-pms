package com.nexus.hotelpms.aplicacion.servicios;

import com.nexus.hotelpms.aplicacion.puertos.*;
import com.nexus.hotelpms.aplicacion.reembolso.PoliticaReembolso;
import com.nexus.hotelpms.aplicacion.reembolso.ResultadoReembolso;
import com.nexus.hotelpms.aplicacion.reembolso.SelectorPoliticaReembolso;
import com.nexus.hotelpms.aplicacion.sesion.AuthPrincipal;
import com.nexus.hotelpms.dominio.excepcion.AccesoDenegadoException;
import com.nexus.hotelpms.dominio.excepcion.ConflictoException;
import com.nexus.hotelpms.dominio.excepcion.ReglaNegocioException;
import com.nexus.hotelpms.dominio.excepcion.RecursoNoEncontradoException;
import com.nexus.hotelpms.dominio.modelo.*;
import com.nexus.hotelpms.infraestructura.config.HotelProperties;
import com.nexus.hotelpms.presentacion.dto.Dtos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Casos de uso de reservas: HU-01 (disponibilidad), HU-02 (registro + bloqueo), HU-04 (cancelación),
 * HU-05 (comprobante), HU-07 (búsqueda) y HU-08 (check-in / no-show).
 *
 * <p>Actúa como <b>Context del patrón Strategy</b>: delega el cálculo del reembolso en la
 * {@link PoliticaReembolso} que elige el {@link SelectorPoliticaReembolso} (ver Javadoc de {@code PoliticaReembolso}).</p>
 */
@Service
public class ReservaService {
    private static final Logger log = LoggerFactory.getLogger(ReservaService.class);
    private static final Set<EstadoReserva> CON_COMPROBANTE =
            EnumSet.of(EstadoReserva.Pagada, EstadoReserva.EnCurso, EstadoReserva.PendienteDeCierre, EstadoReserva.Cerrada);

    private final IReservaRepository reservas;
    private final IHabitacionRepository habitaciones;
    private final ISedeRepository sedes;
    private final IEmailSender email;
    private final IPdfGenerator pdf;
    private final HotelProperties props;
    private final Clock clock;
    private final SelectorPoliticaReembolso selectorReembolso = new SelectorPoliticaReembolso();

    public ReservaService(IReservaRepository reservas, IHabitacionRepository habitaciones, ISedeRepository sedes,
                          IEmailSender email, IPdfGenerator pdf, HotelProperties props, Clock clock) {
        this.reservas = reservas;
        this.habitaciones = habitaciones;
        this.sedes = sedes;
        this.email = email;
        this.pdf = pdf;
        this.props = props;
        this.clock = clock;
    }

    private ZonedDateTime ahora() { return ZonedDateTime.now(clock); }

    /** Sedes activas para que el cliente elija en HU-01. */
    public List<Sede> listarSedesActivas() {
        return sedes.findAll().stream().filter(Sede::isActiva).toList();
    }

    // ------------------------------------------------------------------ HU-01
    public DisponibilidadResponse consultarDisponibilidad(String sedeId, LocalDate entrada, LocalDate salida,
                                                          Integer huespedes) {
        if (sedeId == null || sedeId.isBlank()) {
            throw new ReglaNegocioException("Debe seleccionar una sede para continuar");
        }
        validarFechas(entrada, salida);
        if (huespedes == null || huespedes < 1) {
            throw new ReglaNegocioException("El número de huéspedes debe ser un entero positivo");
        }
        sedes.findById(sedeId).orElseThrow(() -> new RecursoNoEncontradoException("La sede no existe"));
        List<Habitacion> disponibles = habitaciones.buscarPorSedeYEstado(sedeId, EstadoHabitacion.Disponible).stream()
                .filter(h -> h.getCapacidadMax() >= huespedes)
                .toList();
        String mensaje = disponibles.isEmpty()
                ? "No hay habitaciones disponibles para los criterios seleccionados" : null;
        return new DisponibilidadResponse(disponibles, mensaje);
    }

    private void validarFechas(LocalDate entrada, LocalDate salida) {
        if (entrada == null || salida == null) {
            throw new ReglaNegocioException("Debe indicar la fecha de entrada y la fecha de salida");
        }
        if (entrada.isBefore(LocalDate.now(clock))) {
            throw new ReglaNegocioException("La fecha de entrada no puede ser anterior a la fecha actual");
        }
        if (!salida.isAfter(entrada)) {
            throw new ReglaNegocioException("La fecha de salida debe ser posterior a la fecha de entrada");
        }
    }

    // ------------------------------------------------------------------ HU-02
    public Reserva crear(CrearReservaRequest req) {
        validarFechas(req.fechaEntrada(), req.fechaSalida());
        Sede sede = sedes.findById(req.sedeId()).orElseThrow(() -> new RecursoNoEncontradoException("La sede no existe"));
        if (!sede.isActiva()) {
            throw new ReglaNegocioException("La sede seleccionada no está activa");
        }
        Habitacion hab = habitaciones.buscarPorId(req.habitacionId())
                .filter(h -> h.getSedeId().equals(req.sedeId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("La habitación no existe en la sede indicada"));
        if (hab.getCapacidadMax() < req.numHuespedes()) {
            throw new ReglaNegocioException("La habitación seleccionada no tiene capacidad para " + req.numHuespedes() + " huéspedes");
        }
        // Bloqueo atómico: si dos clientes compiten, solo uno obtiene la habitación.
        if (!habitaciones.bloquearSiDisponible(hab.getId())) {
            throw new ConflictoException("La habitación ya no está disponible, por favor seleccione otra");
        }
        try {
            String prefijo = "RSV-" + req.fechaEntrada().format(DateTimeFormatter.BASIC_ISO_DATE);
            String codigo = String.format("%s-%03d", prefijo, reservas.siguienteConsecutivo(prefijo));
            Reserva r = new Reserva(codigo, hab, req.clienteNombre().trim(), req.clienteDocumento().trim(),
                    req.clienteEmail().trim(), req.fechaEntrada(), req.fechaSalida(), req.numHuespedes());
            r.bloquear(ahora(), props.bloqueoMinutos());
            return reservas.guardar(r);
        } catch (RuntimeException e) {
            liberarHabitacion(hab.getId()); // no dejar la habitación bloqueada si falló el registro
            throw e;
        }
    }

    /** Liberación automática (job): reservas Bloqueadas con el temporizador vencido. */
    public int liberarBloqueosExpirados() {
        int n = 0;
        for (Reserva r : reservas.buscarBloqueosExpirados(ahora().toInstant())) {
            try {
                r.cancelarPorTiempo();
                reservas.guardar(r);
                liberarHabitacion(r.getHabitacionId());
                email.enviar(r.getClienteEmail(), "Reserva " + r.getCodigo() + " liberada",
                        "Su reserva fue liberada por inactividad: no se completó el pago en "
                                + props.bloqueoMinutos() + " minutos.", null, null);
                n++;
            } catch (RuntimeException e) {
                log.warn("No se pudo liberar la reserva {}: {}", r.getCodigo(), e.getMessage());
            }
        }
        return n;
    }

    void liberarHabitacion(String habitacionId) {
        habitaciones.buscarPorId(habitacionId).ifPresent(h -> {
            h.cambiarEstado(EstadoHabitacion.Disponible);
            habitaciones.guardar(h);
        });
    }

    // ------------------------------------------------------------------ consultas
    /** Acceso público por ID interno o código (flujo del cliente). */
    public Reserva obtener(String idOCodigo) {
        return reservas.buscarPorCodigo(idOCodigo).or(() -> reservas.buscarPorId(idOCodigo))
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada"));
    }

    /** Acceso del personal: respeta el aislamiento por sede (HU-07). */
    public Reserva obtenerParaPersonal(String idOCodigo, AuthPrincipal p) {
        Reserva r = obtener(idOCodigo);
        if (!p.puedeAccederASede(r.getSedeId())) {
            throw new AccesoDenegadoException("No tiene permisos para acceder a los datos de esta sede");
        }
        return r;
    }

    // ------------------------------------------------------------------ HU-07
    public BusquedaResponse buscar(AuthPrincipal p, String cedula, String id, String sedeId) {
        boolean sinCedula = cedula == null || cedula.isBlank();
        boolean sinId = id == null || id.isBlank();
        if (sinCedula && sinId) {
            throw new ReglaNegocioException("Ingrese una cédula o ID de reserva para realizar la búsqueda");
        }
        String sede = p.esAdministrador() ? sedeId : p.sedeId();
        List<Reserva> encontradas;
        if (!sinId) {
            encontradas = reservas.buscarPorCodigo(id.trim()).or(() -> reservas.buscarPorId(id.trim()))
                    .filter(r -> p.puedeAccederASede(r.getSedeId()) && (sede == null || sede.equals(r.getSedeId())))
                    .map(List::of).orElse(List.of());
        } else if (sede != null) {
            encontradas = reservas.buscarPorSedeYDocumento(sede, cedula.trim());
        } else {
            throw new ReglaNegocioException("El administrador debe indicar sedeId para buscar por cédula");
        }
        String mensaje = encontradas.isEmpty()
                ? "No se encontraron reservas para los criterios ingresados en esta sede" : null;
        return new BusquedaResponse(encontradas, mensaje);
    }

    // ------------------------------------------------------------------ HU-04 (Context del Strategy)
    public CancelacionResponse cancelar(String idOCodigo) {
        Reserva r = obtener(idOCodigo);
        PoliticaReembolso politica = selectorReembolso.seleccionar(r, ahora());
        if (r.getEstado() != EstadoReserva.Pagada) {
            throw new ReglaNegocioException("Solo se pueden cancelar reservas en estado Pagada");
        }
        ResultadoReembolso res = politica.calcular(r);
        r.cancelar(res.estadoFinal(), res.reembolso(), res.retenido());
        reservas.guardar(r);
        liberarHabitacion(r.getHabitacionId()); // liberación inmediata
        String mensaje = res.retenido().signum() == 0
                ? "Reembolso completo procesado: " + res.reembolso()
                : "Se retuvo " + res.retenido() + " por penalidad y se reembolsará " + res.reembolso();
        email.enviar(r.getClienteEmail(), "Cancelación de la reserva " + r.getCodigo(), mensaje, null, null);
        return new CancelacionResponse(r.getCodigo(), r.getEstado().name(), res.politica(), res.reembolso(),
                res.retenido(), mensaje);
    }

    // ------------------------------------------------------------------ HU-05
    public byte[] comprobante(String idOCodigo) {
        Reserva r = obtener(idOCodigo);
        if (!CON_COMPROBANTE.contains(r.getEstado())) {
            throw new ReglaNegocioException("No hay comprobante: el pago de la reserva no ha sido aprobado");
        }
        return pdf.generarComprobante(r, nombreSede(r.getSedeId()));
    }

    /** Genera el comprobante y lo envía por correo (se invoca al aprobarse el pago). */
    void emitirComprobante(Reserva r) {
        byte[] doc = comprobante(r.getCodigo());
        email.enviar(r.getClienteEmail(), "Comprobante de reserva " + r.getCodigo(),
                "Su pago fue procesado exitosamente. Adjuntamos su comprobante con código QR.",
                "comprobante-" + r.getCodigo() + ".pdf", doc);
    }

    String nombreSede(String sedeId) {
        return sedes.findById(sedeId).map(Sede::getNombre).orElse(sedeId);
    }

    // ------------------------------------------------------------------ HU-08
    public Reserva checkIn(String idOCodigo, AuthPrincipal p) {
        Reserva r = obtenerParaPersonal(idOCodigo, p);
        r.checkIn(ahora());
        Habitacion h = habitaciones.buscarPorId(r.getHabitacionId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Habitación no encontrada"));
        h.cambiarEstado(EstadoHabitacion.Ocupada);
        habitaciones.guardar(h);
        return reservas.guardar(r);
    }

    public Reserva registrarNoShow(String idOCodigo, AuthPrincipal p) {
        Reserva r = obtenerParaPersonal(idOCodigo, p);
        r.marcarNoShow(ahora());
        habitaciones.buscarPorId(r.getHabitacionId()).ifPresent(h -> {
            // Queda disponible para limpieza o reasignación; no cuenta como ocupación real.
            h.cambiarEstado(EstadoHabitacion.Disponible);
            habitaciones.guardar(h);
        });
        return reservas.guardar(r);
    }
}
