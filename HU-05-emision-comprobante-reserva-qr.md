# HU-05: Emisión automática de comprobante de reserva con código QR

**Como** sistema  
**Quiero** generar y entregar automáticamente un comprobante en PDF con código QR tras un pago exitoso  
**Para** que el cliente tenga un soporte formal de su reserva que agilice el check-in en el mostrador

---

## Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | El proceso de generación del PDF puede desarrollarse independientemente como componente de salida. |
| **N — Negotiable** | ✅ | No impone la biblioteca de generación de PDF ni el diseño visual del comprobante. |
| **V — Valuable** | ✅ | Reduce el tiempo de check-in y provee al cliente un soporte formal descargable. |
| **E — Estimable** | ✅ | El disparador (pago exitoso) y el contenido obligatorio (QR + datos de reserva) están definidos. |
| **S — Small** | ✅ | Solo cubre la generación y entrega del PDF; el pago y el check-in son historias separadas. |
| **T — Testable** | ✅ | Se puede verificar que el PDF se genera, que contiene el QR y que se entrega al cliente. |

---

## Criterios de aceptación (Gherkin)

```gherkin
# language: es
Característica: Emisión automática de comprobante de reserva con código QR

  Escenario: Generación automática del PDF tras pago aprobado
    Dado que el sistema ha recibido confirmación de pago aprobado para la reserva "RSV-20261001-001"
    Cuando el sistema procesa el evento de pago exitoso
    Entonces el sistema genera automáticamente un PDF de comprobante de reserva
    Y el PDF incluye el código QR asociado al ID de reserva
    Y el PDF incluye los datos de la reserva: nombre del huésped, sede, habitación, fechas y valor total pagado
    Y el sistema pone el PDF a disposición del cliente para descarga o envío

  Escenario: El comprobante se entrega al cliente tras la generación
    Dado que el sistema ha generado el PDF de comprobante de la reserva
    Cuando el proceso de entrega se ejecuta
    Entonces el cliente recibe el comprobante por el canal configurado
    Y el comprobante es accesible de inmediato sin necesidad de solicitarlo manualmente

  Escenario: El código QR del comprobante es válido y escaneable en el mostrador
    Dado que el recepcionista en el mostrador escanea el código QR del comprobante del huésped
    Cuando el sistema decodifica el QR
    Entonces el sistema muestra los datos de la reserva correspondiente al ID codificado en el QR
    Y permite iniciar el proceso de check-in directamente

  Escenario: No se genera comprobante si el pago no fue aprobado
    Dado que el pago de una reserva fue rechazado o está pendiente
    Cuando el sistema procesa el resultado del pago
    Entonces el sistema no genera ningún PDF de comprobante
    Y no entrega ningún documento al cliente
```

---

## Supuestos / preguntas abiertas

- Se asume que el canal de entrega del comprobante es el correo electrónico registrado por el cliente al hacer la reserva; no se especificó si también se ofrece descarga directa desde la plataforma.
- No se definió el contenido exacto y la diagramación visual del PDF; se asume que incluye como mínimo: ID de reserva, nombre del huésped, sede, número de habitación, fechas, valor total y código QR.
- No se especificó si el QR debe expirar o si es válido mientras la reserva esté activa; se asume que es válido hasta el check-out.
