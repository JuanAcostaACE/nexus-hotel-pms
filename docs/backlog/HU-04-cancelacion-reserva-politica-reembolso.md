# HU-04: Cancelación de reserva y aplicación de política de reembolso

**Como** cliente  
**Quiero** cancelar mi reserva antes del check-in  
**Para** recuperar mi dinero total o parcialmente según las políticas de cancelación vigentes

---

## Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | La lógica de cancelación y reembolso puede desarrollarse de forma autónoma. |
| **N — Negotiable** | ✅ | No impone el canal de reembolso ni los plazos bancarios; el equipo define la implementación. |
| **V — Valuable** | ✅ | Otorga transparencia y confianza al cliente sobre las condiciones de cancelación. |
| **E — Estimable** | ✅ | Las dos ramas de reembolso (>48 h y <48 h) y la liberación de habitación están completamente definidas. |
| **S — Small** | ✅ | Solo cubre el flujo de cancelación; el procesamiento del pago original es una historia separada. |
| **T — Testable** | ✅ | El umbral de 48 horas, la penalidad de una noche y la liberación inmediata son verificables. |

---

## Criterios de aceptación (Gherkin)

```gherkin
# language: es
Característica: Cancelación de reserva y política de reembolso

  Escenario: Cancelación con más de 48 horas de anticipación — reembolso completo
    Dado que el cliente tiene una reserva en estado "Pagada"
    Y faltan más de 48 horas para la fecha de check-in
    Cuando el cliente solicita la cancelación de la reserva
    Entonces el sistema procesa el reembolso del valor total pagado
    Y cambia el estado de la reserva a "Cancelada"
    Y libera la habitación cambiando su estado a "Disponible" de inmediato
    Y notifica al cliente que el reembolso completo fue procesado

  Escenario: Cancelación con menos de 48 horas de anticipación — penalidad de una noche
    Dado que el cliente tiene una reserva en estado "Pagada"
    Y faltan menos de 48 horas para la fecha de check-in
    Cuando el cliente solicita la cancelación de la reserva
    Entonces el sistema retiene el valor equivalente a una noche como penalidad
    Y procesa el reembolso del valor restante (total pagado menos una noche)
    Y cambia el estado de la reserva a "Cancelada con penalidad"
    Y libera la habitación cambiando su estado a "Disponible" de inmediato
    Y notifica al cliente el valor retenido y el valor a reembolsar

  Escenario: Cancelación exactamente a las 48 horas del check-in
    Dado que el cliente tiene una reserva en estado "Pagada"
    Y faltan exactamente 48 horas para la fecha de check-in
    Cuando el cliente solicita la cancelación de la reserva
    Entonces el sistema aplica la política de reembolso completo
    Y libera la habitación de inmediato

  Escenario: Intento de cancelación de una reserva no pagada
    Dado que el cliente tiene una reserva en estado distinto a "Pagada"
    Cuando intenta cancelarla
    Entonces el sistema muestra el mensaje "Solo se pueden cancelar reservas en estado Pagada"
    Y no realiza ningún cambio de estado ni reembolso

  Escenario: Liberación inmediata de la habitación tras cancelación
    Dado que el cliente cancela una reserva con habitación en estado "Reservada"
    Cuando el sistema procesa la cancelación (con o sin penalidad)
    Entonces la habitación cambia a estado "Disponible" en el mismo instante del procesamiento
    Y queda visible en consultas de disponibilidad a partir de ese momento
```

---

## Supuestos / preguntas abiertas

- Se asume que "valor de una noche" equivale al precio base de la habitación por una noche, sin incluir consumos adicionales.
- No se especificó el canal ni el plazo del reembolso bancario (ej. 3–5 días hábiles); se asume que el sistema registra la solicitud de reembolso y el procesamiento bancario ocurre en segundo plano.
- No se especificó si el cliente puede cancelar reservas que ya pasaron el check-in; se asume que esta historia solo aplica antes del check-in.
