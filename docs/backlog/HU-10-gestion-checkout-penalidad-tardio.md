# HU-10: Gestión de check-out con penalidad por salida tardía

**Como** recepcionista  
**Quiero** registrar el check-out de un huésped y cerrar su cuenta con el total de estadía más consumos  
**Para** formalizar la salida y cobrar los cargos correctos incluyendo recargos por salida tardía

---

## Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | El check-out puede desarrollarse una vez que check-in y consumos existan como datos; es independiente como historia. |
| **N — Negotiable** | ✅ | No impone el mecanismo de cálculo del recargo ni el método de cobro final. |
| **V — Valuable** | ✅ | Cierra la cuenta del huésped correctamente y protege los ingresos del hotel por salidas tardías. |
| **E — Estimable** | ✅ | La fórmula (estadía + consumos + recargo tardío) y el umbral de hora límite (13:00) están definidos. |
| **S — Small** | ✅ | Solo cubre el check-out; la transición a limpieza y la facturación son historias separadas. |
| **T — Testable** | ✅ | El cálculo del recargo, el umbral de hora y el cierre de cuenta son verificables objetivamente. |

---

## Criterios de aceptación (Gherkin)

```gherkin
# language: es
Característica: Gestión de check-out con penalidad por salida tardía

  Antecedentes:
    Dado que el recepcionista tiene sesión activa en la sede correspondiente

  Escenario: Check-out exitoso antes de la hora límite sin recargo
    Dado que la habitación "205" tiene estado "Ocupada" con la reserva "RSV-20261001-001"
    Y el check-out se realiza antes de las 13:00 del día de salida
    Cuando el recepcionista registra el check-out de la habitación "205"
    Entonces el sistema calcula el total final sumando el costo de estadía más los consumos registrados
    Y no aplica ningún recargo por salida tardía
    Y muestra el resumen de cuenta al recepcionista para confirmación

  Escenario: Check-out con recargo automático por salida tardía
    Dado que la habitación "312" tiene estado "Ocupada" con la reserva "RSV-20261001-002"
    Y el check-out se realiza después de las 13:00 del día de salida
    Cuando el recepcionista registra el check-out de la habitación "312"
    Entonces el sistema detecta automáticamente que la salida supera la hora límite de las 13:00
    Y calcula y agrega el recargo por Late Check-out al total de la cuenta
    Y muestra el desglose: costo de estadía + consumos + recargo por salida tardía
    Y el recargo queda registrado en la cuenta para su inclusión en la factura final

  Escenario: Intento de check-out en habitación que no está ocupada
    Dado que la habitación "101" tiene estado distinto a "Ocupada"
    Cuando el recepcionista intenta registrar el check-out de esa habitación
    Entonces el sistema muestra el mensaje "No se puede realizar check-out: la habitación no está en estado Ocupada"
    Y no realiza ningún cambio de estado

  Escenario: Confirmación del total antes de finalizar el check-out
    Dado que el recepcionista ha revisado el resumen de cuenta del huésped
    Cuando confirma el check-out
    Entonces el sistema registra el timestamp de salida tomado del servidor
    Y cambia el estado de la reserva a "Pendiente de cierre"
    Y activa el flujo de transición de la habitación al estado "En Limpieza"
```

---

## Supuestos / preguntas abiertas

- No se especificó el valor exacto del recargo por Late Check-out (monto fijo, porcentaje de la noche, etc.); se asume que es un valor configurable por el administrador. Esta información es necesaria para completar los escenarios con datos de ejemplo reales.
- Se asume que la "hora límite" de las 13:00 puede ser configurable por sede en el módulo de administración.
- No se especificó si el check-out genera automáticamente la factura final o si eso requiere un paso adicional; se asumió que la facturación es un paso separado (HU-12).
