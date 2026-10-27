# HU-02: Registro de reserva con bloqueo de concurrencia

**Como** cliente  
**Quiero** registrar una reserva capturando mis datos y los de los huéspedes para asegurar la habitación seleccionada  
**Para** garantizar que la habitación quede reservada a mi nombre mientras completo el pago

---

## Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | Puede desarrollarse independientemente de la historia de pago; el bloqueo temporal es parte del registro. |
| **N — Negotiable** | ✅ | No especifica la interfaz de captura de datos ni el mecanismo interno de bloqueo; el equipo decide la implementación. |
| **V — Valuable** | ✅ | Protege al cliente de perder la habitación mientras gestiona el pago y evita doble reserva. |
| **E — Estimable** | ✅ | Reglas de tiempo de bloqueo (15 min) y liberación automática están claramente definidas. |
| **S — Small** | ✅ | Se limita al registro y bloqueo; el procesamiento del pago es una historia separada. |
| **T — Testable** | ✅ | El ID generado, el bloqueo y la liberación automática son verificables objetivamente. |

---

## Criterios de aceptación (Gherkin)

```gherkin
# language: es
Característica: Registro de reserva con bloqueo de concurrencia

  Escenario: Registro exitoso de reserva y bloqueo de habitación
    Dado que el cliente ha seleccionado una habitación disponible de la "Sede Norte"
    Y ha ingresado todos los datos requeridos del huésped principal
    Cuando confirma el registro de la reserva
    Entonces el sistema genera un ID de reserva único
    Y cambia el estado de la habitación a "Bloqueada"
    Y el sistema inicia un temporizador de 15 minutos para completar el pago
    Y redirige al cliente al flujo de pago

  Escenario: Liberación automática por tiempo de pago expirado
    Dado que el cliente tiene una reserva con habitación bloqueada
    Y han transcurrido 15 minutos sin que se complete el pago
    Cuando el temporizador expira
    Entonces el sistema libera la habitación y cambia su estado a "Disponible"
    Y cambia el estado de la reserva a "Cancelada por tiempo expirado"
    Y notifica al cliente que la reserva fue liberada por inactividad

  Escenario: Intento de registrar una reserva sobre habitación ya bloqueada
    Dado que la habitación "101" de la "Sede Norte" tiene estado "Bloqueada"
    Cuando otro cliente intenta registrar una reserva sobre la misma habitación
    Entonces el sistema muestra el mensaje "La habitación ya no está disponible, por favor seleccione otra"
    Y no crea una nueva reserva

  Escenario: Intento de registro con datos obligatorios incompletos
    Dado que el cliente ha seleccionado una habitación disponible
    Y no ha ingresado todos los datos requeridos del huésped
    Cuando intenta confirmar el registro de la reserva
    Entonces el sistema muestra un mensaje indicando los campos obligatorios faltantes
    Y no genera ninguna reserva ni bloquea la habitación
```

---

## Supuestos / preguntas abiertas

- Se asume que los datos mínimos del huésped son: nombre completo, documento de identidad y correo electrónico. El conjunto exacto de campos no fue especificado.
- Se asume que el cliente recibe notificación (correo o pantalla) cuando la reserva es liberada por expiración.
- No se especificó si el cliente puede reintentar el registro tras la expiración; se asume que sí, eligiendo nuevamente la habitación.
