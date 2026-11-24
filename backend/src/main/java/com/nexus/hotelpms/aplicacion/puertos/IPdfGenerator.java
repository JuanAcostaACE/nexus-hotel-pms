package com.nexus.hotelpms.aplicacion.puertos;

import com.nexus.hotelpms.dominio.modelo.Consumo;
import com.nexus.hotelpms.dominio.modelo.Factura;
import com.nexus.hotelpms.dominio.modelo.Reserva;

import java.util.List;

public interface IPdfGenerator {
    /** Comprobante de reserva con código QR del código de reserva (HU-05). */
    byte[] generarComprobante(Reserva reserva, String nombreSede);

    /** Factura final con detalle de pagos, consumos e impuestos (HU-12). */
    byte[] generarFactura(Factura factura, Reserva reserva, String nombreSede, List<Consumo> consumos);
}
