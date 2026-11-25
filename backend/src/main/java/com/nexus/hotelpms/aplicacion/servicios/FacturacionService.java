package com.nexus.hotelpms.aplicacion.servicios;

import com.nexus.hotelpms.aplicacion.puertos.*;
import com.nexus.hotelpms.aplicacion.sesion.AuthPrincipal;
import com.nexus.hotelpms.dominio.excepcion.RecursoNoEncontradoException;
import com.nexus.hotelpms.dominio.excepcion.ReglaNegocioException;
import com.nexus.hotelpms.dominio.modelo.*;
import com.nexus.hotelpms.infraestructura.config.HotelProperties;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/** HU-12: factura final con impuestos desglosados y cierre documental (la reserva pasa a Cerrada, inmutable). */
@Service
public class FacturacionService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FacturacionService.class);
    private final IReservaRepository reservas;
    private final IConsumoRepository consumos;
    private final IFacturaRepository facturas;
    private final ReservaService reservaService;
    private final IPdfGenerator pdf;
    private final IDocumentoRepository documentos;
    private final IEmailSender email;
    private final HotelProperties props;
    private final Clock clock;

    public FacturacionService(IReservaRepository reservas, IConsumoRepository consumos, IFacturaRepository facturas,
                              ReservaService reservaService, IPdfGenerator pdf, IDocumentoRepository documentos, IEmailSender email,
                              HotelProperties props, Clock clock) {
        this.reservas = reservas;
        this.consumos = consumos;
        this.facturas = facturas;
        this.reservaService = reservaService;
        this.pdf = pdf;
        this.documentos = documentos;
        this.email = email;
        this.props = props;
        this.clock = clock;
    }

    /** Genera la factura y cierra la reserva. Solo aplica a reservas con check-out completado. */
    public Factura cerrarReserva(Reserva reserva) {
        if (reserva.getEstado() != EstadoReserva.PendienteDeCierre) {
            throw new ReglaNegocioException("No se genera factura: la reserva no ha completado el check-out");
        }
        List<Consumo> activos = consumos.findByReservaId(reserva.getId()).stream().filter(c -> !c.isAnulado()).toList();
        BigDecimal totalConsumos = activos.stream().map(Consumo::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal baseHospedaje = reserva.getValorTotal().add(reserva.getRecargoLateCheckout());

        List<LineaImpuesto> impuestos = new ArrayList<>();
        impuestos.add(linea("IVA", props.ivaTasa(), baseHospedaje));
        if (totalConsumos.signum() > 0) {
            impuestos.add(linea("Impuesto al consumo", props.impuestoConsumoTasa(), totalConsumos));
        }
        ZonedDateTime ahora = ZonedDateTime.now(clock);
        String prefijo = "FAC-" + ahora.getYear();
        String numero = String.format("%s-%06d", prefijo, reservas.siguienteConsecutivo(prefijo));
        Factura f = new Factura(reserva.getId(), numero, "/api/reservas/" + reserva.getCodigo() + "/factura/pdf",
                reserva.getValorTotal(), totalConsumos, reserva.getRecargoLateCheckout(), impuestos,
                reserva.getValorTotal(), ahora.toInstant());
        f = facturas.save(f);
        reserva.cerrar();
        reservas.guardar(reserva);

        try { // la entrega del PDF no debe deshacer el cierre ya registrado
            byte[] doc = pdf.generarFactura(f, reserva, reservaService.nombreSede(reserva.getSedeId()), activos);
            documentos.guardar(IDocumentoRepository.FACTURA, reserva.getId(), "factura-" + f.getNumeroFactura() + ".pdf", doc);
            email.enviar(reserva.getClienteEmail(), "Factura " + f.getNumeroFactura(),
                    "Gracias por su estadía. Adjuntamos su factura final.", "factura-" + f.getNumeroFactura() + ".pdf", doc);
        } catch (RuntimeException e) {
            log.warn("Factura {} generada pero no entregada: {}", f.getNumeroFactura(), e.getMessage());
        }
        return f;
    }

    private LineaImpuesto linea(String nombre, BigDecimal tasa, BigDecimal base) {
        return new LineaImpuesto(nombre, tasa, base, base.multiply(tasa).setScale(2, RoundingMode.HALF_UP));
    }

    // Llamado desde: ReservaController.factura
    public Factura consultar(String idOCodigo, AuthPrincipal p) {
        Reserva r = reservaService.obtenerParaPersonal(idOCodigo, p);
        return facturas.findByReservaId(r.getId()).orElseThrow(() -> new RecursoNoEncontradoException(
                "La reserva aún no tiene factura: no ha completado el check-out"));
    }

    // Llamado desde: ReservaController.facturaPdf
    public byte[] pdf(String idOCodigo, AuthPrincipal p) {
        Reserva r = reservaService.obtenerParaPersonal(idOCodigo, p);
        Factura f = consultar(idOCodigo, p);
        // La factura es inmutable una vez emitida: se lee el PDF guardado y, si falta, se genera y se guarda.
        return documentos.leer(IDocumentoRepository.FACTURA, r.getId()).orElseGet(() -> {
            List<Consumo> activos = consumos.findByReservaId(r.getId()).stream().filter(c -> !c.isAnulado()).toList();
            byte[] doc = pdf.generarFactura(f, r, reservaService.nombreSede(r.getSedeId()), activos);
            documentos.guardar(IDocumentoRepository.FACTURA, r.getId(), "factura-" + f.getNumeroFactura() + ".pdf", doc);
            return doc;
        });
    }

    /** Reintento manual del cierre si la generación falló y la reserva quedó PendienteDeCierre. */
    // Llamado desde: ReservaController.cerrar
    public Factura reintentarCierre(String idOCodigo, AuthPrincipal p) {
        return cerrarReserva(reservaService.obtenerParaPersonal(idOCodigo, p));
    }
}
