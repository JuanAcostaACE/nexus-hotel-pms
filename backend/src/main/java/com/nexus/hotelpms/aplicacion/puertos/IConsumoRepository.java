package com.nexus.hotelpms.aplicacion.puertos;

import com.nexus.hotelpms.dominio.modelo.Consumo;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/** Puerto de persistencia de Consumos (Spring Data genera la implementación). */
public interface IConsumoRepository extends MongoRepository<Consumo, String> {
    List<Consumo> findByReservaId(String reservaId);
}
