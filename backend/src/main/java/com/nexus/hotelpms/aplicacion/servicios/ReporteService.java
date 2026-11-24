package com.nexus.hotelpms.aplicacion.servicios;

import com.nexus.hotelpms.aplicacion.puertos.IConsumoRepository;
import com.nexus.hotelpms.aplicacion.puertos.IHabitacionRepository;
import com.nexus.hotelpms.aplicacion.puertos.IReservaRepository;
import com.nexus.hotelpms.aplicacion.puertos.ISedeRepository;
import com.nexus.hotelpms.dominio.excepcion.RecursoNoEncontradoException;
import com.nexus.hotelpms.dominio.excepcion.ReglaNegocioException;
import com.nexus.hotelpms.dominio.modelo.Consumo;
import com.nexus.hotelpms.dominio.modelo.EstadoReserva;
import com.nexus.hotelpms.dominio.modelo.Reserva;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * HU-17: reportes operativos de ocupación e ingresos por sede y rango de fechas.
 * Solo los consume el Administrador; el control de rol vive en el controlador.
 */
@Service
public class ReporteService {

    /** Estados que cuentan como ocupación/ingreso confirmado. */
    private static final Set<EstadoReserva> CONFIRMADAS = EnumSet.of(
            EstadoReserva.Pagada, EstadoReserva.EnCurso, EstadoReserva.PendienteDeCierre, EstadoReserva.Cerrada);

    private final ISedeRepository sedes;
    private final IHabitacionRepository habitaciones;
    private final IReservaRepository reservas;
    private final IConsumoRepository consumos;

    public ReporteService(ISedeRepository sedes, IHabitacionRepository habitaciones,
                          IReservaRepository reservas, IConsumoRepository consumos) {
        this.sedes = sedes;
        this.habitaciones = habitaciones;
        this.reservas = reservas;
        this.consumos = consumos;
    }

    /** Ocupación = noches vendidas dentro del rango / (habitaciones de la sede × noches del rango). */
    public Map<String, Object> ocupacion(String sedeId, LocalDate inicio, LocalDate fin) {
        validarParametrosReporte(sedeId, inicio, fin);
        long totalHabitaciones = habitaciones.buscarPorSede(sedeId).size();
        long nochesRango = ChronoUnit.DAYS.between(inicio, fin);
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

    private List<Reserva> reservasConfirmadas(String sedeId) {
        return reservas.buscarPorSede(sedeId).stream()
                .filter(r -> CONFIRMADAS.contains(r.getEstado()))
                .collect(Collectors.toList());
    }

    private static long nochesEnRango(Reserva r, LocalDate inicio, LocalDate fin) {
        LocalDate desde = r.getFechaEntrada().isAfter(inicio) ? r.getFechaEntrada() : inicio;
        LocalDate hasta = r.getFechaSalida().isBefore(fin) ? r.getFechaSalida() : fin;
        long n = ChronoUnit.DAYS.between(desde, hasta);
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
        if (sedes.findById(sedeId).isEmpty()) {
            throw new RecursoNoEncontradoException("Sede no encontrada");
        }
    }
}
