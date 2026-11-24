package com.nexus.hotelpms.infraestructura.pdf;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** PDF emitido (comprobante o factura) guardado en MongoDB: una sola copia por tipo y reserva. */
@Document("documentos_pdf")
@CompoundIndex(name = "tipo_reserva_unico", def = "{'tipo': 1, 'reservaId': 1}", unique = true)
public class DocumentoPdf {
    @Id private String id;
    private String tipo;
    private String reservaId;
    private String nombreArchivo;
    private byte[] contenido;
    private Instant generadoAt;

    protected DocumentoPdf() { }

    public DocumentoPdf(String tipo, String reservaId, String nombreArchivo, byte[] contenido, Instant generadoAt) {
        this.tipo = tipo;
        this.reservaId = reservaId;
        this.nombreArchivo = nombreArchivo;
        this.contenido = contenido;
        this.generadoAt = generadoAt;
    }

    public void reemplazar(String nombreArchivo, byte[] contenido, Instant generadoAt) {
        this.nombreArchivo = nombreArchivo;
        this.contenido = contenido;
        this.generadoAt = generadoAt;
    }

    public byte[] getContenido() { return contenido; }
}
