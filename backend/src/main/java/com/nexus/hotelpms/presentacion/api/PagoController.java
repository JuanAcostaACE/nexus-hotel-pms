package com.nexus.hotelpms.presentacion.api;

import com.nexus.hotelpms.aplicacion.servicios.PagoService;
import com.nexus.hotelpms.dominio.excepcion.AccesoDenegadoException;
import com.nexus.hotelpms.infraestructura.config.HotelProperties;
import com.nexus.hotelpms.presentacion.dto.Dtos.PagoRequest;
import com.nexus.hotelpms.presentacion.dto.Dtos.PagoResponse;
import com.nexus.hotelpms.presentacion.dto.Dtos.WebhookPseRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** HU-03. */
@RestController
@RequestMapping("/api")
public class PagoController {
    private final PagoService pagos;
    private final HotelProperties props;

    public PagoController(PagoService pagos, HotelProperties props) {
        this.pagos = pagos;
        this.props = props;
    }

    /** Inicia el pago por PSE. {@code simulacion}: Aprobado | Rechazado | Pendiente (solo simulador). */
    // Front: Cliente, 'Pagar ahora' → 'Pagar' (cliente.js · pagoModal) y 'Mis reservas' → 'Pagar con PSE' (cliente.js · pagar)
    @PostMapping("/reservas/{id}/pago")
    public PagoResponse pagar(@PathVariable String id, @Valid @RequestBody PagoRequest req) {
        return pagos.iniciarPago(id, req);
    }

    /** Webhook asíncrono de PSE; se autentica con la cabecera X-PSE-Secret compartida. */
    // Front: sin botón; lo llama la pasarela PSE (simulada)
    @PostMapping("/webhooks/pse")
    public PagoResponse webhook(@RequestHeader(value = "X-PSE-Secret", required = false) String secreto,
                                @Valid @RequestBody WebhookPseRequest req) {
        if (!props.pseSecret().equals(secreto)) {
            throw new AccesoDenegadoException("Firma del webhook inválida");
        }
        return pagos.procesarWebhook(req.transaccionId(), req.resultado());
    }
}
