package com.nexus.hotelpms.aplicacion.puertos;

import com.nexus.hotelpms.dominio.modelo.Factura;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/** Puerto de persistencia de Facturas (Spring Data genera la implementación). */
public interface IFacturaRepository extends MongoRepository<Factura, String> {
    Optional<Factura> findByReservaId(String reservaId);
}
