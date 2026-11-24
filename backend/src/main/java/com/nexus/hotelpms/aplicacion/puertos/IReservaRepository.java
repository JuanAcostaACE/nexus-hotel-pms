package com.nexus.hotelpms.aplicacion.puertos;

import com.nexus.hotelpms.dominio.modelo.Reserva;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Puerto de persistencia de reservas (implementado por {@code ReservaRepository}). */
public interface IReservaRepository {
    Reserva guardar(Reserva reserva);
    Optional<Reserva> buscarPorId(String id);
    Optional<Reserva> buscarPorCodigo(String codigo);
    List<Reserva> buscarPorSedeYDocumento(String sedeId, String documento);
    /** Reservas Bloqueadas cuyo temporizador de pago ya venció. */
    List<Reserva> buscarBloqueosExpirados(Instant ahora);
    Optional<Reserva> buscarEnCursoPorHabitacion(String habitacionId);
    /** Todas las reservas de una sede (reportes HU-17). */
    List<Reserva> buscarPorSede(String sedeId);

    /** Contador atómico por clave (códigos de reserva y números de factura). */
    long siguienteConsecutivo(String clave);
}
