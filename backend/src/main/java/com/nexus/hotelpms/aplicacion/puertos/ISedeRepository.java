package com.nexus.hotelpms.aplicacion.puertos;

import com.nexus.hotelpms.dominio.modelo.Sede;
import org.springframework.data.mongodb.repository.MongoRepository;

/** Puerto de persistencia de Sedes (Spring Data genera la implementación). */
public interface ISedeRepository extends MongoRepository<Sede, String> {
}
