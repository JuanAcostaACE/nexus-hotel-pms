package com.nexus.hotelpms.infraestructura.pdf;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DocumentoPdfMongoRepository extends MongoRepository<DocumentoPdf, String> {
    Optional<DocumentoPdf> findByTipoAndReservaId(String tipo, String reservaId);
}
