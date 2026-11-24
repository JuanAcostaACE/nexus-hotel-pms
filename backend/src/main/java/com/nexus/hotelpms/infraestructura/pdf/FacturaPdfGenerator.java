package com.nexus.hotelpms.infraestructura.pdf;

import com.nexus.hotelpms.aplicacion.puertos.IPdfGenerator;
import com.nexus.hotelpms.dominio.modelo.Consumo;
import com.nexus.hotelpms.dominio.modelo.Factura;
import com.nexus.hotelpms.dominio.modelo.LineaImpuesto;
import com.nexus.hotelpms.dominio.modelo.Reserva;
import org.springframework.stereotype.Component;

import java.util.List;

/** Genera los PDF del sistema: comprobante de reserva con QR (HU-05) y factura final (HU-12). */
@Component
public class FacturaPdfGenerator implements IPdfGenerator {

    @Override
    public byte[] generarComprobante(Reserva r, String nombreSede) {
        MiniPdf pdf = new MiniPdf();
        pdf.titulo("Nexus Hotel PMS - Comprobante de reserva");
        pdf.negrita("Reserva " + r.getCodigo());
        pdf.linea("Huésped: " + r.getClienteNombre() + " (doc. " + r.getClienteDocumento() + ")");
        pdf.linea("Sede: " + nombreSede);
        pdf.linea("Habitación: " + r.getHabitacionNumero());
        pdf.linea("Entrada: " + r.getFechaEntrada() + "   Salida: " + r.getFechaSalida());
        pdf.linea("Huéspedes: " + r.getNumHuespedes());
        pdf.linea("Valor total pagado: COP " + r.getValorTotal());
        pdf.espacio(15);
        pdf.linea("Presente este código QR en recepción para el check-in:");
        pdf.espacio(5);
        pdf.qr(r.getCodigo(), 150);
        return pdf.construir();
    }

    @Override
    public byte[] generarFactura(Factura f, Reserva r, String nombreSede, List<Consumo> consumos) {
        MiniPdf pdf = new MiniPdf();
        pdf.titulo("Nexus Hotel PMS - Factura " + f.getNumeroFactura());
        pdf.linea("Reserva " + r.getCodigo() + "  |  Sede: " + nombreSede + "  |  Habitación " + r.getHabitacionNumero());
        pdf.linea("Huésped: " + r.getClienteNombre() + " (doc. " + r.getClienteDocumento() + ")");
        pdf.linea("Estadía: " + r.getFechaEntrada() + " a " + r.getFechaSalida());
        pdf.espacio(10);
        pdf.negrita("Detalle de pagos");
        pdf.linea("Pago anticipado por PSE: COP " + f.getPagadoAnticipado());
        pdf.espacio(6);
        pdf.negrita("Consumos");
        if (consumos.isEmpty()) {
            pdf.linea("(sin consumos)");
        }
        for (Consumo c : consumos) {
            pdf.linea(c.getCantidad() + " x " + c.getNombreItem() + "  COP " + c.getSubtotal());
        }
        pdf.espacio(6);
        pdf.negrita("Resumen");
        pdf.linea("Estadía: COP " + f.getSubtotalEstadia());
        pdf.linea("Consumos: COP " + f.getSubtotalConsumos());
        pdf.linea("Recargo Late Check-out: COP " + f.getRecargoLateCheckout());
        pdf.linea("Total antes de impuestos: COP " + f.getTotalSinImpuestos());
        for (LineaImpuesto i : f.getImpuestosDetalle()) {
            pdf.linea(i.nombre() + " (" + i.tasa().movePointRight(2).stripTrailingZeros().toPlainString()
                    + "% sobre " + i.base() + "): COP " + i.valor());
        }
        pdf.negrita("Total con impuestos: COP " + f.getTotal());
        pdf.linea("Pagado anticipadamente: COP " + f.getPagadoAnticipado()
                + "   Saldo a pagar: COP " + f.getSaldoPendiente());
        pdf.linea("Generada: " + f.getGeneradaAt());
        return pdf.construir();
    }
}
