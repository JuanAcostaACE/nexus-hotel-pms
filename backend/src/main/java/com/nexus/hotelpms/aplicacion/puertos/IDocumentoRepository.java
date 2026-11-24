package com.nexus.hotelpms.aplicacion.puertos;

import java.util.Optional;

/** Puerto de almacenamiento de los PDF emitidos (comprobantes y facturas): escritura al generarlos, lectura al descargarlos. */
public interface IDocumentoRepository {
    String COMPROBANTE = "comprobante";
    String FACTURA = "factura";

    Optional<byte[]> leer(String tipo, String reservaId);

    void guardar(String tipo, String reservaId, String nombreArchivo, byte[] contenido);
}
