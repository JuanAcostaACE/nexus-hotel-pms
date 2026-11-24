package com.nexus.hotelpms.infraestructura.correo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CorreoOutboxRepository extends MongoRepository<CorreoOutbox, String> {
    List<CorreoOutbox> findByEstadoOrderByCreadoAtAsc(CorreoOutbox.Estado estado, Pageable limite);
}
