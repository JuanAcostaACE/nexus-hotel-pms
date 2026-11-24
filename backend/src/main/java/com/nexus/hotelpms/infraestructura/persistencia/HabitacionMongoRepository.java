package com.nexus.hotelpms.infraestructura.persistencia;

import com.nexus.hotelpms.dominio.modelo.EstadoHabitacion;
import com.nexus.hotelpms.dominio.modelo.Habitacion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface HabitacionMongoRepository extends MongoRepository<Habitacion, String> {
    List<Habitacion> findBySedeId(String sedeId);
    List<Habitacion> findBySedeIdAndEstado(String sedeId, EstadoHabitacion estado);
}
