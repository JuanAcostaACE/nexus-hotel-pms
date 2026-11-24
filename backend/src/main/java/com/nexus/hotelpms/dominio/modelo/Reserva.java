package com.nexus.hotelpms.dominio.modelo;

import com.nexus.hotelpms.dominio.excepcion.ReglaNegocioException;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Entidad central del dominio. Concentra las transiciones de estado y sus reglas (HU-02, 03, 04, 08, 10, 12).
 * Los timestamps oficiales (check-in/out) vienen del reloj del servidor y, una vez fijados, no se modifican.
 */
@Document("reservas")
public class Reserva {
    @Id private String id;
    @Indexed(unique = true) private String codigo;
    private String sedeId;
    private String habitacionId;
    private String habitacionNumero;
    private String clienteNombre;
    @Indexed private String clienteDocumento;
    private String clienteEmail;
    private LocalDate fechaEntrada;
    private LocalDate fechaSalida;
    private int numHuespedes;
    private EstadoReserva estado = EstadoReserva.Pendiente;
    private BigDecimal precioNoche;
    private BigDecimal valorTotal;
    private Instant creadaAt;
    private Instant bloqueoExpiraAt;
    private Instant checkinAt;
    private Instant checkoutAt;
    private BigDecimal recargoLateCheckout = BigDecimal.ZERO;
    private BigDecimal valorReembolsado;
    private BigDecimal valorRetenido;

    protected Reserva() { }

    public Reserva(String codigo, Habitacion habitacion, String clienteNombre, String clienteDocumento,
                   String clienteEmail, LocalDate fechaEntrada, LocalDate fechaSalida, int numHuespedes) {
        this.codigo = codigo;
        this.sedeId = habitacion.getSedeId();
        this.habitacionId = habitacion.getId();
        this.habitacionNumero = habitacion.getNumero();
        this.clienteNombre = clienteNombre;
        this.clienteDocumento = clienteDocumento;
        this.clienteEmail = clienteEmail;
        this.fechaEntrada = fechaEntrada;
        this.fechaSalida = fechaSalida;
        this.numHuespedes = numHuespedes;
        this.precioNoche = habitacion.getPrecioBase();
        this.valorTotal = calcularTotal();
    }

    // ---------------------------------------------------------------- cálculo
    public long noches() { return ChronoUnit.DAYS.between(fechaEntrada, fechaSalida); }

    public BigDecimal calcularTotal() { return precioNoche.multiply(BigDecimal.valueOf(noches())); }

    // ---------------------------------------------------------------- HU-02 / HU-03
    /** Inicia el bloqueo temporal de la habitación (temporizador de pago). */
    public void bloquear(ZonedDateTime ahora, int minutos) {
        this.estado = EstadoReserva.Bloqueada;
        this.creadaAt = ahora.toInstant();
        this.bloqueoExpiraAt = ahora.toInstant().plusSeconds(minutos * 60L);
    }

    /** PSE respondió "Pendiente": se espera el webhook y se suspende el temporizador. */
    public void marcarPendienteConfirmacion() {
        exigirEstado(EstadoReserva.Bloqueada, "Solo se puede pagar una reserva en estado Bloqueada");
        this.estado = EstadoReserva.PendienteConfirmacion;
        this.bloqueoExpiraAt = null;
    }

    public void confirmarPago() {
        if (estado != EstadoReserva.Bloqueada && estado != EstadoReserva.PendienteConfirmacion) {
            throw new ReglaNegocioException("La reserva no está pendiente de pago");
        }
        this.estado = EstadoReserva.Pagada;
        this.bloqueoExpiraAt = null;
    }

    /** Pago rechazado: la reserva se anula. */
    public void anularPorPagoRechazado() {
        this.estado = EstadoReserva.Cancelada;
        this.bloqueoExpiraAt = null;
    }

    /** Expiró el temporizador de 15 minutos. */
    public void cancelarPorTiempo() {
        exigirEstado(EstadoReserva.Bloqueada, "Solo se libera por tiempo una reserva Bloqueada");
        this.estado = EstadoReserva.CanceladaPorTiempo;
        this.bloqueoExpiraAt = null;
    }

    public boolean bloqueoExpirado(ZonedDateTime ahora) {
        return estado == EstadoReserva.Bloqueada && bloqueoExpiraAt != null
                && !ahora.toInstant().isBefore(bloqueoExpiraAt);
    }

    // ---------------------------------------------------------------- HU-04
    /** Cancela una reserva Pagada con el resultado calculado por la política de reembolso (patrón Strategy). */
    public void cancelar(EstadoReserva estadoFinal, BigDecimal reembolso, BigDecimal retenido) {
        exigirEstado(EstadoReserva.Pagada, "Solo se pueden cancelar reservas en estado Pagada");
        this.estado = estadoFinal;
        this.valorReembolsado = reembolso;
        this.valorRetenido = retenido;
    }

    // ---------------------------------------------------------------- HU-08
    public void checkIn(ZonedDateTime ahora) {
        exigirEstado(EstadoReserva.Pagada, "No se puede realizar el check-in: la reserva no está en estado Pagada");
        if (ahora.toLocalDate().isBefore(fechaEntrada)) {
            throw new ReglaNegocioException("No se puede realizar el check-in antes de la fecha de entrada (" + fechaEntrada + ")");
        }
        if (checkinAt != null) {
            throw new ReglaNegocioException("El timestamp de check-in es inalterable");
        }
        this.checkinAt = ahora.toInstant();
        this.estado = EstadoReserva.EnCurso;
    }

    public void marcarNoShow(ZonedDateTime ahora) {
        exigirEstado(EstadoReserva.Pagada, "Solo se puede marcar No-Show una reserva en estado Pagada");
        ZonedDateTime limite = fechaEntrada.atTime(23, 59).atZone(ahora.getZone());
        if (ahora.isBefore(limite)) {
            throw new ReglaNegocioException("No es posible marcar como No-Show antes de las 23:59 del día de entrada");
        }
        this.estado = EstadoReserva.NoShow;
    }

    // ---------------------------------------------------------------- HU-10 / HU-12
    public void checkOut(ZonedDateTime ahora, BigDecimal recargo) {
        exigirEstado(EstadoReserva.EnCurso, "No se puede realizar check-out: la reserva no está En curso");
        this.checkoutAt = ahora.toInstant();
        this.recargoLateCheckout = recargo;
        this.estado = EstadoReserva.PendienteDeCierre;
    }

    public void cerrar() {
        exigirEstado(EstadoReserva.PendienteDeCierre, "La reserva no ha completado el check-out");
        this.estado = EstadoReserva.Cerrada;
    }

    /** Una reserva Cerrada es inmutable (HU-12). */
    public void asegurarModificable() {
        if (estado == EstadoReserva.Cerrada) {
            throw new ReglaNegocioException("No se pueden realizar modificaciones en una reserva cerrada");
        }
    }

    private void exigirEstado(EstadoReserva requerido, String mensaje) {
        if (estado != requerido) {
            throw new ReglaNegocioException(mensaje);
        }
    }

    public String getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getSedeId() { return sedeId; }
    public String getHabitacionId() { return habitacionId; }
    public String getHabitacionNumero() { return habitacionNumero; }
    public String getClienteNombre() { return clienteNombre; }
    public String getClienteDocumento() { return clienteDocumento; }
    public String getClienteEmail() { return clienteEmail; }
    public LocalDate getFechaEntrada() { return fechaEntrada; }
    public LocalDate getFechaSalida() { return fechaSalida; }
    public int getNumHuespedes() { return numHuespedes; }
    public EstadoReserva getEstado() { return estado; }
    public BigDecimal getPrecioNoche() { return precioNoche; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public Instant getCreadaAt() { return creadaAt; }
    public Instant getBloqueoExpiraAt() { return bloqueoExpiraAt; }
    public Instant getCheckinAt() { return checkinAt; }
    public Instant getCheckoutAt() { return checkoutAt; }
    public BigDecimal getRecargoLateCheckout() { return recargoLateCheckout; }
    public BigDecimal getValorReembolsado() { return valorReembolsado; }
    public BigDecimal getValorRetenido() { return valorRetenido; }
}
