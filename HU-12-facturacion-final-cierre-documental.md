# HU-12: Facturación final y cierre documental de la reserva

**Como** sistema  
**Quiero** generar el PDF de factura final consolidada y bloquear la reserva en estado "Cerrada" al momento del check-out  
**Para** proveer al huésped un soporte financiero oficial y evitar modificaciones posteriores a la cuenta

---

## Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | La generación de la factura final puede desarrollarse como componente independiente de salida documental. |
| **N — Negotiable** | ✅ | No impone la plantilla fiscal ni el software de generación de PDF; el equipo lo decide. |
| **V — Valuable** | ✅ | Provee soporte legal y fiscal al huésped y cierra formalmente la transacción del hotel. |
| **E — Estimable** | ✅ | El contenido de la factura (pagos, impuestos, estado "Cerrada") y el bloqueo de modificaciones están definidos. |
| **S — Small** | ✅ | Solo cubre la generación del documento de cierre; el check-out y los consumos son historias separadas. |
| **T — Testable** | ✅ | El PDF, el detalle de impuestos, el estado "Cerrada" y el bloqueo de modificaciones son verificables. |

---

## Criterios de aceptación (Gherkin)

```gherkin
# language: es
Característica: Facturación final y cierre documental de la reserva

  Escenario: Generación automática de factura al confirmar el check-out
    Dado que la reserva "RSV-20261001-001" ha completado el proceso de check-out
    Y la cuenta incluye costo de estadía, consumos registrados e impuestos locales aplicables
    Cuando el sistema procesa el cierre de la reserva
    Entonces el sistema genera automáticamente un PDF de factura final
    Y el PDF consolida: detalle de pagos, consumos, impuestos locales desglosados y total a pagar
    Y el sistema cambia el estado de la reserva a "Cerrada"
    Y entrega el PDF al huésped por el canal configurado

  Escenario: La reserva cerrada no permite modificaciones
    Dado que la reserva "RSV-20261001-001" está en estado "Cerrada"
    Cuando el recepcionista o el sistema intenta agregar, modificar o anular un consumo en esa reserva
    Entonces el sistema muestra el mensaje "No se pueden realizar modificaciones en una reserva cerrada"
    Y rechaza cualquier cambio en la cuenta

  Escenario: La factura incluye el desglose de impuestos locales
    Dado que la reserva "RSV-20261001-002" tiene estado "Cerrada"
    Y el PDF de factura ha sido generado
    Cuando se revisa el contenido del PDF
    Entonces el documento incluye una línea separada por cada impuesto local aplicable con su nombre, tasa y valor
    Y muestra el total antes de impuestos y el total con impuestos claramente diferenciados

  Escenario: La factura no se genera si la reserva no ha completado el check-out
    Dado que la reserva "RSV-20261001-003" tiene estado "En curso" o "Pagada"
    Cuando el sistema evalúa si debe generar la factura final
    Entonces el sistema no genera ningún PDF de factura
    Y no cambia el estado de la reserva a "Cerrada"

  Escenario: El PDF de factura es accesible para el administrador
    Dado que la reserva "RSV-20261001-001" está en estado "Cerrada"
    Y su factura PDF fue generada
    Cuando el administrador accede al historial de reservas de la sede
    Entonces puede visualizar y descargar el PDF de factura de esa reserva
```

---

## Supuestos / preguntas abiertas

- No se especificaron los impuestos locales aplicables (IVA, ICA, impuesto al consumo, etc.) ni sus tasas. Se asume que son configurables por el administrador y se detalla solo la lógica de inclusión en el documento.
- Se asume que el canal de entrega del PDF es el correo electrónico registrado del huésped; si se requiere entrega en mostrador (impresión), es una extensión de alcance a confirmar.
- No se especificó si la factura puede ser reenvíada al huésped tras su generación; se deja como pregunta abierta.
