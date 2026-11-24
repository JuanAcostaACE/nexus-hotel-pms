package com.nexus.hotelpms.presentacion.api;

import com.nexus.hotelpms.aplicacion.servicios.OperacionService;
import com.nexus.hotelpms.aplicacion.sesion.AuthPrincipal;
import com.nexus.hotelpms.dominio.modelo.CatalogoItem;
import com.nexus.hotelpms.dominio.modelo.Consumo;
import com.nexus.hotelpms.dominio.modelo.EstadoHabitacion;
import com.nexus.hotelpms.dominio.modelo.Habitacion;
import com.nexus.hotelpms.presentacion.dto.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Operación de recepción: HU-09 (consumos), HU-10 (check-out), HU-11 (limpieza). Requiere sesión. */
@RestController
@RequestMapping("/api")
public class OperacionController {
    private final OperacionService operacion;

    public OperacionController(OperacionService operacion) { this.operacion = operacion; }

    @GetMapping("/habitaciones")
    public List<Habitacion> habitaciones(@AuthenticationPrincipal AuthPrincipal p,
                                         @RequestParam(required = false) String sedeId,
                                         @RequestParam(required = false) EstadoHabitacion estado) {
        return operacion.listarHabitaciones(p, sedeId, estado);
    }

    @GetMapping("/catalogo")
    public List<CatalogoItem> catalogo(@AuthenticationPrincipal AuthPrincipal p,
                                       @RequestParam(required = false) String sedeId) {
        return operacion.listarCatalogo(p, sedeId);
    }

    @PostMapping("/habitaciones/{id}/consumos")
    public ResponseEntity<Consumo> registrarConsumo(@AuthenticationPrincipal AuthPrincipal p, @PathVariable String id,
                                                    @Valid @RequestBody ConsumoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(operacion.registrarConsumo(id, req.catalogoItemId(), req.cantidad(), p));
    }

    @GetMapping("/habitaciones/{id}/consumos")
    public CuentaHabitacion cuenta(@AuthenticationPrincipal AuthPrincipal p, @PathVariable String id) {
        return operacion.cuenta(id, p);
    }

    @PostMapping("/consumos/{id}/anular")
    public Consumo anular(@AuthenticationPrincipal AuthPrincipal p, @PathVariable String id,
                          @RequestBody(required = false) AnularConsumoRequest req) {
        return operacion.anularConsumo(id, req == null ? null : req.motivo(), p);
    }

    /** confirmar=false (defecto) solo muestra el resumen; confirmar=true registra el check-out. */
    @PostMapping("/habitaciones/{id}/checkout")
    public ResumenCuenta checkout(@AuthenticationPrincipal AuthPrincipal p, @PathVariable String id,
                                  @RequestParam(defaultValue = "false") boolean confirmar) {
        return operacion.checkout(id, confirmar, p);
    }

    @PostMapping("/habitaciones/{id}/habilitar")
    public Habitacion habilitar(@AuthenticationPrincipal AuthPrincipal p, @PathVariable String id) {
        return operacion.habilitar(id, p);
    }
}
