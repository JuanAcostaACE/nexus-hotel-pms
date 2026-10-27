# HU-08: Gestión de check-in y registro de no-shows

**Como** recepcionista  
**Quiero** registrar el ingreso oficial de un huésped o marcarlo como no-show según su llegada  
**Para** controlar con exactitud la ocupación real del hotel y mantener las métricas íntegras

---

## Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | El check-in puede desarrollarse independientemente del check-out y del registro de consumos. |
| **N — Negotiable** | ✅ | No impone el mecanismo de captura de timestamp ni la interfaz de registro. |
| **V — Valuable** | ✅ | Formaliza el ingreso del huésped y protege las métricas de ocupación del hotel. |
| **E — Estimable** | ✅ | El timestamp inalterable del servidor y la condición de no-show (23:59) están definidos. |
| **S — Small** | ✅ | Cubre solo el check-in y el no-show; el check-out es una historia separada. |
| **T — Testable** | ✅ | El timestamp del servidor, el cambio de estado y el criterio de no-show (23:59) son verificables. |

---

## Criterios de aceptación (Gherkin)

```gherkin
# language: es
Característica: Gestión de check-in y registro de no-shows

  Antecedentes:
    Dado que el recepcionista tiene sesión activa en la sede correspondiente

  Escenario: Check-in exitoso del huésped
    Dado que existe la reserva "RSV-20261001-001" en estado "Pagada"
    Y el huésped se presenta en el mostrador en la fecha de entrada programada
    Cuando el recepcionista registra el check-in de la reserva
    Entonces el sistema captura el timestamp exacto del servidor como hora oficial de ingreso
    Y cambia el estado de la reserva a "En curso"
    Y cambia el estado de la habitación a "Ocupada"
    Y el timestamp registrado es inalterable y no puede ser modificado manualmente

  Escenario: Registro de no-show cuando el huésped no llega en el día de entrada
    Dado que existe la reserva "RSV-20261001-002" en estado "Pagada"
    Y han pasado las 23:59 del día de entrada sin que el huésped se haya presentado
    Cuando el recepcionista marca la reserva como "No-Show"
    Entonces el sistema cambia el estado de la reserva a "No-Show"
    Y la habitación queda disponible para su proceso de limpieza o reasignación
    Y el evento queda registrado en el sistema sin alterar las métricas de ocupación real

  Escenario: Intento de check-in de una reserva no pagada
    Dado que la reserva "RSV-20261001-003" tiene estado distinto a "Pagada"
    Cuando el recepcionista intenta registrar el check-in
    Entonces el sistema muestra el mensaje "No se puede realizar el check-in: la reserva no está en estado Pagada"
    Y no realiza ningún cambio de estado

  Escenario: Intento de marcar como no-show antes de las 23:59 del día de entrada
    Dado que existe una reserva en estado "Pagada" para el día de hoy
    Y aún no son las 23:59
    Cuando el recepcionista intenta marcar la reserva como "No-Show"
    Entonces el sistema muestra el mensaje "No es posible marcar como No-Show antes de las 23:59 del día de entrada"
    Y no cambia el estado de la reserva

  Escenario: Check-in mediante escaneo del código QR del comprobante
    Dado que el huésped presenta el código QR de su comprobante de reserva
    Y la reserva asociada está en estado "Pagada"
    Cuando el recepcionista escanea el QR
    Entonces el sistema carga automáticamente los datos de la reserva correspondiente
    Y el recepcionista puede proceder a registrar el check-in desde esa vista
```

---

## Supuestos / preguntas abiertas

- Se asume que "inalterable" significa que el campo de timestamp de check-in no tiene ningún control de edición en la interfaz ni en el backend una vez guardado.
- No se especificó si el no-show aplica políticas de reembolso o retención; se asume que la gestión económica del no-show está cubierta por la historia de cancelación (HU-04) o es una política operativa fuera del alcance de esta historia.
- No se especificó si el sistema envía una alerta automática al recepcionista a las 23:59 para recordar marcar los no-shows; se registra como supuesto menor a confirmar.
