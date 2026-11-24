package com.nexus.hotelpms.presentacion.api;

import com.nexus.hotelpms.aplicacion.sesion.AuthPrincipal;
import com.nexus.hotelpms.aplicacion.servicios.FacturacionService;
import com.nexus.hotelpms.aplicacion.servicios.ReservaService;
import com.nexus.hotelpms.dominio.modelo.Factura;
import com.nexus.hotelpms.dominio.modelo.Reserva;
import com.nexus.hotelpms.presentacion.dto.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api")
public class ReservaController {
    private final ReservaService reservas;
    private final FacturacionService facturacion;

    public ReservaController(ReservaService reservas, FacturacionService facturacion) {
        this.reservas = reservas;
        this.facturacion = facturacion;
    }

    @GetMapping("/sedes")
    public java.util.List<com.nexus.hotelpms.dominio.modelo.Sede> sedes() {
        return reservas.listarSedesActivas();
    }

    /** HU-01 (público). Sin sede devuelve el mensaje de error de la historia. */
    @GetMapping({"/sedes/{sedeId}/disponibilidad", "/disponibilidad"})
    public DisponibilidadResponse disponibilidad(
            @PathVariable(value = "sedeId", required = false) String sedeEnRuta,
            @RequestParam(value = "sedeId", required = false) String sedeEnQuery,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaEntrada,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaSalida,
            @RequestParam(required = false) Integer huespedes) {
        return reservas.consultarDisponibilidad(sedeEnRuta != null ? sedeEnRuta : sedeEnQuery,
                fechaEntrada, fechaSalida, huespedes);
    }

    /** HU-02 (público): crea la reserva y bloquea la habitación 15 minutos. */
    @PostMapping("/reservas")
    public ResponseEntity<Reserva> crear(@Valid @RequestBody CrearReservaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservas.crear(req));
    }

    /** HU-07 (personal). */
    @GetMapping("/reservas/buscar")
    public BusquedaResponse buscar(@AuthenticationPrincipal AuthPrincipal p,
                                   @RequestParam(required = false) String cedula,
                                   @RequestParam(required = false) String id,
                                   @RequestParam(required = false) String sedeId) {
        return reservas.buscar(p, cedula, id, sedeId);
    }

    /** Lectura por ID o código (también es el destino del QR escaneado en mostrador, HU-05/HU-08). */
    @GetMapping("/reservas/{id}")
    public Reserva obtener(@AuthenticationPrincipal AuthPrincipal p, @PathVariable String id) {
        return reservas.obtenerParaPersonal(id, p);
    }

    /** HU-04 (público). */
    @PostMapping("/reservas/{id}/cancelar")
    public CancelacionResponse cancelar(@PathVariable String id, @Valid @RequestBody CancelarRequest req) {
        return reservas.cancelar(id, req.documento());
    }

    /** HU-05 (público): PDF con QR. */
    @GetMapping("/reservas/{id}/comprobante")
    public ResponseEntity<byte[]> comprobante(@PathVariable String id,
                                              @RequestParam(required = false) String documento) {
        return pdf(reservas.comprobanteCliente(id, documento), "comprobante-" + id + ".pdf");
    }

    /** HU-08. */
    @PostMapping("/reservas/{id}/checkin")
    public Reserva checkIn(@AuthenticationPrincipal AuthPrincipal p, @PathVariable String id) {
        return reservas.checkIn(id, p);
    }

    @PostMapping("/reservas/{id}/no-show")
    public Reserva noShow(@AuthenticationPrincipal AuthPrincipal p, @PathVariable String id) {
        return reservas.registrarNoShow(id, p);
    }

    /** HU-12. */
    @GetMapping("/reservas/{id}/factura")
    public Factura factura(@AuthenticationPrincipal AuthPrincipal p, @PathVariable String id) {
        return facturacion.consultar(id, p);
    }

    @GetMapping("/reservas/{id}/factura/pdf")
    public ResponseEntity<byte[]> facturaPdf(@AuthenticationPrincipal AuthPrincipal p, @PathVariable String id) {
        return pdf(facturacion.pdf(id, p), "factura-" + id + ".pdf");
    }

    @PostMapping("/reservas/{id}/cerrar")
    public Factura cerrar(@AuthenticationPrincipal AuthPrincipal p, @PathVariable String id) {
        return facturacion.reintentarCierre(id, p);
    }

    private ResponseEntity<byte[]> pdf(byte[] contenido, String nombre) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nombre + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(contenido);
    }
}
