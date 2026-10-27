# HU-03: Procesamiento de pago seguro mediante PSE

**Como** cliente  
**Quiero** pagar mi reserva a través de la pasarela de pagos PSE  
**Para** confirmar y asegurar mi reserva de forma segura sin manejar efectivo

---

## Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | Depende lógicamente de que exista una reserva, pero como historia es desarrollable independientemente con datos de prueba. |
| **N — Negotiable** | ✅ | No impone detalles técnicos de integración con PSE; el equipo decide el mecanismo de webhook. |
| **V — Valuable** | ✅ | Convierte la intención de compra en un pago real y cambia el estado de la reserva a pagada. |
| **E — Estimable** | ✅ | Los tres estados de respuesta de PSE y sus consecuencias están claramente definidos. |
| **S — Small** | ✅ | Se limita al ciclo de pago; el registro y el comprobante son historias separadas. |
| **T — Testable** | ✅ | Cada respuesta de PSE (Aprobado, Rechazado, Pendiente) tiene un resultado esperado verificable. |

---

## Criterios de aceptación (Gherkin)

```gherkin
# language: es
Característica: Procesamiento de pago seguro mediante PSE

  Escenario: Pago aprobado por PSE
    Dado que el cliente tiene una reserva en estado "Bloqueada" pendiente de pago
    Y inicia el proceso de pago a través de PSE
    Cuando PSE responde con el estado "Aprobado"
    Entonces el sistema actualiza el estado de la reserva a "Pagada"
    Y libera el bloqueo temporal convirtiendo la habitación a estado "Reservada"
    Y notifica al cliente que el pago fue procesado exitosamente

  Escenario: Pago rechazado por PSE
    Dado que el cliente tiene una reserva en estado "Bloqueada" pendiente de pago
    Y inicia el proceso de pago a través de PSE
    Cuando PSE responde con el estado "Rechazado"
    Entonces el sistema anula la reserva
    Y libera la habitación cambiando su estado a "Disponible"
    Y notifica al cliente que el pago fue rechazado y la reserva cancelada

  Escenario: Pago pendiente en espera de webhook de PSE
    Dado que el cliente tiene una reserva en estado "Bloqueada" pendiente de pago
    Y inicia el proceso de pago a través de PSE
    Cuando PSE responde con el estado "Pendiente"
    Entonces el sistema mantiene la habitación en estado "Bloqueada"
    Y mantiene la reserva en estado "Pendiente de confirmación"
    Y espera el webhook de confirmación de PSE para actualizar el estado final

  Escenario: Recepción del webhook de confirmación aprobado
    Dado que existe una reserva en estado "Pendiente de confirmación"
    Y la habitación permanece bloqueada
    Cuando el sistema recibe el webhook de PSE con resultado "Aprobado"
    Entonces el sistema actualiza el estado de la reserva a "Pagada"
    Y actualiza el estado de la habitación a "Reservada"
    Y notifica al cliente la confirmación del pago

  Escenario: Recepción del webhook de confirmación rechazado
    Dado que existe una reserva en estado "Pendiente de confirmación"
    Y la habitación permanece bloqueada
    Cuando el sistema recibe el webhook de PSE con resultado "Rechazado"
    Entonces el sistema anula la reserva
    Y libera la habitación cambiando su estado a "Disponible"
    Y notifica al cliente que el pago fue rechazado y la reserva cancelada
```

---

## Supuestos / preguntas abiertas

- Se asume que mientras el estado sea "Pendiente", el temporizador de 15 minutos del bloqueo se suspende o extiende, ya que la regla de negocio indica que el sistema mantiene la habitación bloqueada hasta recibir el webhook. Si el temporizador sigue activo en paralelo, deberá confirmarse el comportamiento.
- No se especificó el canal de notificación al cliente (correo, SMS, pantalla); se asume notificación en pantalla y correo electrónico.
- No se especificó un tiempo máximo de espera del webhook antes de liberar la habitación; queda como pregunta abierta.
