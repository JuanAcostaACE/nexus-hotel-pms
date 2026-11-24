package com.nexus.hotelpms.aplicacion.servicios;

import com.nexus.hotelpms.aplicacion.puertos.*;
import com.nexus.hotelpms.aplicacion.sesion.AuthPrincipal;
import com.nexus.hotelpms.dominio.excepcion.RecursoNoEncontradoException;
import com.nexus.hotelpms.dominio.excepcion.ReglaNegocioException;
import com.nexus.hotelpms.dominio.modelo.*;
import com.nexus.hotelpms.infraestructura.config.HotelProperties;
import com.nexus.hotelpms.presentacion.dto.Dtos.CuentaHabitacion;
import com.nexus.hotelpms.presentacion.dto.Dtos.ResumenCuenta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;

/** Operación diaria de recepción: consumos (HU-09), check-out (HU-10) y limpieza (HU-11). */
@Service
public class OperacionService {
    private static final Logger log = LoggerFactory.getLogger(OperacionService.class);

    private final IHabitacionRepository habitaciones;
    private final IReservaRepository reservas;
    private final IConsumoRepository consumos;
    private final ICatalogoItemRepository catalogo;
    private final FacturacionService facturacion;
    private final HotelProperties props;
    private final Clock clock;

    public OperacionService(IHabitacionRepository habitaciones, IReservaRepository reservas, IConsumoRepository consumos,
                            ICatalogoItemRepository catalogo, FacturacionService facturacion, HotelProperties props,
                            Clock clock) {
        this.habitaciones = habitaciones;
        this.reservas = reservas;
        this.consumos = consumos;
        this.catalogo = catalogo;
        this.facturacion = facturacion;
        this.props = props;
        this.clock = clock;
    }

    // ------------------------------------------------------------ lecturas de apoyo
    public List<Habitacion> listarHabitaciones(AuthPrincipal p, String sedeIdParam, EstadoHabitacion estado) {
        String sede = p.esAdministrador() ? sedeIdParam : p.sedeId();
        if (sede == null || sede.isBlank()) {
            throw new ReglaNegocioException("Debe indicar la sede (sedeId)");
        }
        return estado == null ? habitaciones.buscarPorSede(sede) : habitaciones.buscarPorSedeYEstado(sede, estado);
    }

    /** Solo ítems activos de la sede (HU-09: los inactivos no se ofrecen). */
    public List<CatalogoItem> listarCatalogo(AuthPrincipal p, String sedeIdParam) {
        String sede = p.esAdministrador() ? sedeIdParam : p.sedeId();
        if (sede == null || sede.isBlank()) {
            throw new ReglaNegocioException("Debe indicar la sede (sedeId)");
        }
        return catalogo.findBySedeIdAndActivoTrue(sede);
    }

    private Habitacion habitacion(String id, AuthPrincipal p) {
        return habitaciones.buscarPorId(id).filter(h -> p.puedeAccederASede(h.getSedeId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Habitación no encontrada en esta sede"));
    }

    private Reserva reservaEnCurso(Habitacion h) {
        return reservas.buscarEnCursoPorHabitacion(h.getId())
                .orElseThrow(() -> new ReglaNegocioException("La habitación no tiene una reserva en curso"));
    }

    // ------------------------------------------------------------ HU-09
    public Consumo registrarConsumo(String habitacionId, String itemId, int cantidad, AuthPrincipal p) {
        Habitacion h = habitacion(habitacionId, p);
        if (h.getEstado() != EstadoHabitacion.Ocupada) {
            throw new ReglaNegocioException("Solo se pueden registrar consumos en habitaciones con estado Ocupada");
        }
        CatalogoItem item = catalogo.findById(itemId)
                .filter(i -> i.isActivo() && i.getSedeId().equals(h.getSedeId()))
                .orElseThrow(() -> new ReglaNegocioException("El ítem no está disponible en el catálogo de la sede"));
        Reserva r = reservaEnCurso(h);
        r.asegurarModificable();
        return consumos.save(new Consumo(r, item, cantidad, p.userId(), ZonedDateTime.now(clock).toInstant()));
    }

    public CuentaHabitacion cuenta(String habitacionId, AuthPrincipal p) {
        Habitacion h = habitacion(habitacionId, p);
        Reserva r = reservaEnCurso(h);
        List<Consumo> lista = consumos.findByReservaId(r.getId());
        return new CuentaHabitacion(h.getNumero(), r.getCodigo(), lista, sumaActivos(lista));
    }

    public Consumo anularConsumo(String consumoId, String motivo, AuthPrincipal p) {
        Consumo c = consumos.findById(consumoId).filter(x -> p.puedeAccederASede(x.getSedeId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Consumo no encontrado en esta sede"));
        Reserva r = reservas.buscarPorId(c.getReservaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada"));
        r.asegurarModificable();
        c.anular(motivo, p.userId(), ZonedDateTime.now(clock).toInstant());
        log.info("AUDITORIA anulación consumo={} reserva={} motivo='{}' usuario={}", c.getId(), r.getCodigo(),
                c.getMotivoAnulacion(), p.userId());
        return consumos.save(c);
    }

    private BigDecimal sumaActivos(List<Consumo> lista) {
        return lista.stream().filter(c -> !c.isAnulado()).map(Consumo::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ------------------------------------------------------------ HU-10 / HU-11 / HU-12
    /**
     * @param confirmar false = solo muestra el resumen (sin cambiar estados); true = registra el check-out,
     *                  pasa la habitación a En Limpieza y dispara la facturación/cierre.
     */
    public ResumenCuenta checkout(String habitacionId, boolean confirmar, AuthPrincipal p) {
        Habitacion h = habitacion(habitacionId, p);
        if (h.getEstado() != EstadoHabitacion.Ocupada) {
            throw new ReglaNegocioException("No se puede realizar check-out: la habitación no está en estado Ocupada");
        }
        Reserva r = reservaEnCurso(h);
        ZonedDateTime ahora = ZonedDateTime.now(clock);
        boolean tardia = ahora.isAfter(r.getFechaSalida().atTime(props.horaLimiteCheckout(), 0).atZone(ahora.getZone()));
        BigDecimal recargo = tardia ? props.recargoLateCheckout() : BigDecimal.ZERO;
        List<Consumo> lista = consumos.findByReservaId(r.getId());
        BigDecimal totalConsumos = sumaActivos(lista);
        BigDecimal total = r.getValorTotal().add(totalConsumos).add(recargo);

        if (!confirmar) {
            return new ResumenCuenta(r.getCodigo(), h.getNumero(), r.getValorTotal(), lista, totalConsumos, tardia,
                    recargo, total, false, r.getEstado().name(),
                    tardia ? "Salida posterior a las " + props.horaLimiteCheckout() + ":00: se aplica recargo por Late Check-out"
                           : "Sin recargo. Confirme el check-out con confirmar=true");
        }
        r.checkOut(ahora, recargo);
        reservas.guardar(r);
        h.cambiarEstado(EstadoHabitacion.EnLimpieza); // HU-11: transición automática
        habitaciones.guardar(h);
        String mensaje = "Check-out registrado";
        try {
            facturacion.cerrarReserva(r); // HU-12
            mensaje += "; factura generada y reserva cerrada";
        } catch (RuntimeException e) {
            log.error("Fallo al facturar la reserva {}; queda PendienteDeCierre", r.getCodigo(), e);
            mensaje += "; la facturación falló y la reserva queda Pendiente de cierre (reintente en /cerrar)";
        }
        return new ResumenCuenta(r.getCodigo(), h.getNumero(), r.getValorTotal(), lista, totalConsumos, tardia,
                recargo, total, true, r.getEstado().name(), mensaje);
    }

    /** HU-11: habilitación manual tras la limpieza. */
    public Habitacion habilitar(String habitacionId, AuthPrincipal p) {
        Habitacion h = habitacion(habitacionId, p);
        switch (h.getEstado()) {
            case EnLimpieza -> { }
            case Ocupada -> throw new ReglaNegocioException("Debe completar el check-out antes de habilitar la habitación");
            default -> throw new ReglaNegocioException("Solo se pueden habilitar habitaciones en estado EnLimpieza");
        }
        h.cambiarEstado(EstadoHabitacion.Disponible);
        return habitaciones.guardar(h);
    }
}
