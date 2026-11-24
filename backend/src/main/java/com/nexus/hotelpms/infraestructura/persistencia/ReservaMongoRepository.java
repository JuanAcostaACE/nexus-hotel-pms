package com.nexus.hotelpms.infraestructura.persistencia;

import com.nexus.hotelpms.dominio.modelo.EstadoReserva;
import com.nexus.hotelpms.dominio.modelo.Reserva;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Acceso Spring Data a la colección {@code reservas} (detalle interno de {@link ReservaRepository}). */
public interface ReservaMongoRepository extends MongoRepository<Reserva, String> {
    Optional<Reserva> findByCodigo(String codigo);
    List<Reserva> findBySedeIdAndClienteDocumento(String sedeId, String clienteDocumento);
    List<Reserva> findByEstadoAndBloqueoExpiraAtBefore(EstadoReserva estado, Instant limite);
    List<Reserva> findBySedeId(String sedeId);
        Optional<Reserva> findFirstByHabitacionIdAndEstado(String habitacionId, EstadoReserva estado);
}
