package com.nexus.hotelpms.aplicacion.puertos;

import com.nexus.hotelpms.dominio.modelo.Pago;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/** Puerto de persistencia de Pagos (Spring Data genera la implementación). */
public interface IPagoRepository extends MongoRepository<Pago, String> {
    Optional<Pago> findByTransaccionId(String transaccionId);
    List<Pago> findByReservaId(String reservaId);
}