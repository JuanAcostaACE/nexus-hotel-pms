package com.nexus.hotelpms.infraestructura.persistencia;

import com.nexus.hotelpms.aplicacion.puertos.IHabitacionRepository;
import com.nexus.hotelpms.dominio.modelo.EstadoHabitacion;
import com.nexus.hotelpms.dominio.modelo.Habitacion;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class HabitacionRepository implements IHabitacionRepository {
    private final HabitacionMongoRepository delegate;
    private final MongoTemplate mongo;

    public HabitacionRepository(HabitacionMongoRepository delegate, MongoTemplate mongo) {
        this.delegate = delegate;
        this.mongo = mongo;
    }

    @Override public Habitacion guardar(Habitacion h) { return delegate.save(h); }
    @Override public Optional<Habitacion> buscarPorId(String id) { return delegate.findById(id); }
    @Override public List<Habitacion> buscarPorSede(String sedeId) { return delegate.findBySedeId(sedeId); }

    @Override public List<Habitacion> buscarPorSedeYEstado(String sedeId, EstadoHabitacion estado) {
        return delegate.findBySedeIdAndEstado(sedeId, estado);
    }

    /** findAndModify con filtro por estado: operación atómica en el servidor MongoDB (sin condiciones de carrera). */
    @Override public boolean bloquearSiDisponible(String habitacionId) {
        Habitacion previa = mongo.findAndModify(
                Query.query(Criteria.where("id").is(habitacionId).and("estado").is(EstadoHabitacion.Disponible)),
                new Update().set("estado", EstadoHabitacion.Bloqueada),
                Habitacion.class);
        return previa != null;
    }
}
