package com.nexus.hotelpms.aplicacion.servicios;

import com.nexus.hotelpms.aplicacion.puertos.*;
import com.nexus.hotelpms.dominio.excepcion.ConflictoException;
import com.nexus.hotelpms.dominio.excepcion.RecursoNoEncontradoException;
import com.nexus.hotelpms.dominio.excepcion.ReglaNegocioException;
import com.nexus.hotelpms.dominio.modelo.*;
import com.nexus.hotelpms.presentacion.dto.Dtos.PagoRequest;
import com.nexus.hotelpms.presentacion.dto.Dtos.PagoResponse;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.ZonedDateTime;

/** HU-03: pago por PSE (respuesta inmediata y webhook asíncrono) y consecuencias sobre reserva y habitación. */
@Service
public class PagoService {
    private final ReservaService reservaService;
    private final IReservaRepository reservas;
    private final IHabitacionRepository habitaciones;
    private final IPagoRepository pagos;
    private final IPagoGateway gateway;
    private final IEmailSender email;
    private final Clock clock;

    public PagoService(ReservaService reservaService, IReservaRepository reservas, IHabitacionRepository habitaciones,
                       IPagoRepository pagos, IPagoGateway gateway, IEmailSender email, Clock clock) {
        this.reservaService = reservaService;
        this.reservas = reservas;
        this.habitaciones = habitaciones;
        this.pagos = pagos;
        this.gateway = gateway;
        this.email = email;
        this.clock = clock;
    }

    public PagoResponse iniciarPago(String idOCodigo, PagoRequest req) {
        Reserva r = reservaService.obtener(idOCodigo);
        if (r.getEstado() != EstadoReserva.Bloqueada) {
            throw new ReglaNegocioException("Solo se puede pagar una reserva en estado Bloqueada");
        }
        ZonedDateTime ahora = ZonedDateTime.now(clock);
        if (r.bloqueoExpirado(ahora)) {
            throw new ReglaNegocioException("El tiempo para completar el pago expiró; la reserva será liberada");
        }
        RespuestaPasarela resp = gateway.iniciarPago(r.getCodigo(), r.getValorTotal(), req.banco(), req.simulacion());
        Pago pago = pagos.save(new Pago(r.getId(), r.getValorTotal(), req.banco(), resp.transaccionId(),
                resp.estado(), ahora.toInstant()));
        aplicarResultado(r, resp.estado());
        return new PagoResponse(pago.getTransaccionId(), resp.estado().name(), r.getEstado().name(), mensaje(resp.estado()));
    }

    /** Webhook de PSE: confirma o rechaza un pago que había quedado Pendiente. */
    public PagoResponse procesarWebhook(String transaccionId, String resultado) {
        EstadoPago res;
        try {
            res = EstadoPago.valueOf(resultado);
        } catch (IllegalArgumentException e) {
            throw new ReglaNegocioException("Resultado de PSE no reconocido: " + resultado);
        }
        if (res == EstadoPago.Pendiente) {
            throw new ReglaNegocioException("El webhook debe informar un resultado final (Aprobado o Rechazado)");
        }
        Pago pago = pagos.findByTransaccionId(transaccionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Transacción PSE desconocida"));
        if (pago.getEstado() != EstadoPago.Pendiente) {
            throw new ConflictoException("La transacción ya fue procesada");
        }
        Reserva r = reservas.buscarPorId(pago.getReservaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada"));
        if (r.getEstado() != EstadoReserva.PendienteConfirmacion) {
            throw new ConflictoException("La reserva no está pendiente de confirmación");
        }
        pago.procesarRespuesta(res, ZonedDateTime.now(clock).toInstant());
        pagos.save(pago);
        aplicarResultado(r, res);
        return new PagoResponse(transaccionId, res.name(), r.getEstado().name(), mensaje(res));
    }

    private void aplicarResultado(Reserva r, EstadoPago resultado) {
        switch (resultado) {
            case Aprobado -> {
                r.confirmarPago();
                cambiarHabitacion(r, EstadoHabitacion.Reservada);
                reservas.guardar(r);
                reservaService.emitirComprobante(r); // HU-05: solo con pago aprobado
            }
            case Rechazado -> {
                r.anularPorPagoRechazado();
                cambiarHabitacion(r, EstadoHabitacion.Disponible);
                reservas.guardar(r);
                email.enviar(r.getClienteEmail(), "Pago rechazado - reserva " + r.getCodigo(),
                        "El pago fue rechazado y la reserva fue cancelada.", null, null);
            }
            case Pendiente -> {
                r.marcarPendienteConfirmacion(); // la habitación sigue Bloqueada
                reservas.guardar(r);
            }
        }
    }

    private void cambiarHabitacion(Reserva r, EstadoHabitacion estado) {
        habitaciones.buscarPorId(r.getHabitacionId()).ifPresent(h -> {
            h.cambiarEstado(estado);
            habitaciones.guardar(h);
        });
    }

    private String mensaje(EstadoPago e) {
        return switch (e) {
            case Aprobado -> "Pago aprobado: reserva confirmada y comprobante enviado";
            case Rechazado -> "Pago rechazado: la reserva fue cancelada y la habitación liberada";
            case Pendiente -> "Pago pendiente: se espera la confirmación de PSE (webhook)";
        };
    }
}
