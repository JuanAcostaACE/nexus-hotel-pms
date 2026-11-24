package com.nexus.hotelpms.infraestructura.persistencia;

import com.nexus.hotelpms.aplicacion.puertos.IReservaRepository;
import com.nexus.hotelpms.dominio.modelo.EstadoReserva;
import com.nexus.hotelpms.dominio.modelo.Reserva;
import org.bson.Document;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Implementación MongoDB del puerto {@link IReservaRepository}. */
@Repository
public class ReservaRepository implements IReservaRepository {
    private final ReservaMongoRepository delegate;
    private final MongoTemplate mongo;

    public ReservaRepository(ReservaMongoRepository delegate, MongoTemplate mongo) {
        this.delegate = delegate;
        this.mongo = mongo;
    }

    @Override public Reserva guardar(Reserva reserva) { return delegate.save(reserva); }
    @Override public Optional<Reserva> buscarPorId(String id) { return delegate.findById(id); }
    @Override public Optional<Reserva> buscarPorCodigo(String codigo) { return delegate.findByCodigo(codigo); }

    @Override public List<Reserva> buscarPorSedeYDocumento(String sedeId, String documento) {
        return delegate.findBySedeIdAndClienteDocumento(sedeId, documento);
    }

    @Override public List<Reserva> buscarPorSede(String sedeId) { return delegate.findBySedeId(sedeId); }

    @Override public List<Reserva> buscarBloqueosExpirados(Instant ahora) {
        return delegate.findByEstadoAndBloqueoExpiraAtBefore(EstadoReserva.Bloqueada, ahora);
    }

    @Override public Optional<Reserva> buscarEnCursoPorHabitacion(String habitacionId) {
        return delegate.findFirstByHabitacionIdAndEstado(habitacionId, EstadoReserva.EnCurso);
    }

    @Override public long siguienteConsecutivo(String clave) {
        Document d = mongo.findAndModify(
                Query.query(Criteria.where("_id").is(clave)),
                new Update().inc("valor", 1),
                FindAndModifyOptions.options().upsert(true).returnNew(true),
                Document.class, "contadores");
        return ((Number) d.get("valor")).longValue();
    }
}
