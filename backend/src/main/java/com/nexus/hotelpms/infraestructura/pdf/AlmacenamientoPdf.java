package com.nexus.hotelpms.infraestructura.pdf;

import com.nexus.hotelpms.aplicacion.puertos.IDocumentoRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Optional;

/** Almacenamiento de PDF emitidos sobre MongoDB (colección documentos_pdf). */
@Component
public class AlmacenamientoPdf implements IDocumentoRepository {
    private final DocumentoPdfMongoRepository repo;
    private final Clock clock;

    public AlmacenamientoPdf(DocumentoPdfMongoRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    @Override
    public Optional<byte[]> leer(String tipo, String reservaId) {
        return repo.findByTipoAndReservaId(tipo, reservaId).map(DocumentoPdf::getContenido);
    }

    @Override
    public void guardar(String tipo, String reservaId, String nombreArchivo, byte[] contenido) {
        DocumentoPdf doc = repo.findByTipoAndReservaId(tipo, reservaId)
                .orElseGet(() -> new DocumentoPdf(tipo, reservaId, nombreArchivo, contenido, clock.instant()));
        doc.reemplazar(nombreArchivo, contenido, clock.instant());
        repo.save(doc);
    }
}
