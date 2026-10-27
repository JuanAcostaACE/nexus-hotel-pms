# HU-07: Búsqueda y localización de reservas en el mostrador

**Como** recepcionista  
**Quiero** buscar la reserva de un huésped utilizando su cédula o ID de reserva  
**Para** localizar rápidamente la información necesaria durante la atención en el mostrador

---

## Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | La búsqueda de reservas puede desarrollarse independientemente del check-in; son operaciones distintas. |
| **N — Negotiable** | ✅ | No impone el motor de búsqueda ni el diseño de la pantalla de resultados. |
| **V — Valuable** | ✅ | Reduce el tiempo de atención en el mostrador y mejora la experiencia del huésped. |
| **E — Estimable** | ✅ | Los dos criterios de búsqueda (cédula, ID reserva) y el filtro por sede están claramente definidos. |
| **S — Small** | ✅ | Solo cubre la búsqueda; el check-in y la gestión de la reserva son historias separadas. |
| **T — Testable** | ✅ | Los resultados esperados y el aislamiento por sede son verificables. |

---

## Criterios de aceptación (Gherkin)

```gherkin
# language: es
Característica: Búsqueda y localización de reservas por recepcionista

  Antecedentes:
    Dado que el recepcionista tiene sesión activa en la "Sede Norte"

  Escenario: Búsqueda exitosa por cédula del huésped
    Dado que existe una reserva asociada al huésped con cédula "1098765432" en la "Sede Norte"
    Cuando el recepcionista ingresa la cédula "1098765432" en el buscador
    Entonces el sistema muestra la(s) reserva(s) activa(s) del huésped en la "Sede Norte"
    Y presenta los datos de la reserva: ID, habitación, fechas, estado y nombre del huésped

  Escenario: Búsqueda exitosa por ID de reserva
    Dado que existe la reserva "RSV-20261001-001" en la "Sede Norte"
    Cuando el recepcionista ingresa el ID "RSV-20261001-001" en el buscador
    Entonces el sistema muestra la información completa de esa reserva
    Y presenta los datos del huésped, habitación, fechas y estado actual

  Escenario: Búsqueda sin resultados en la sede activa
    Dado que no existe ninguna reserva asociada a la cédula "9999999999" en la "Sede Norte"
    Cuando el recepcionista busca por esa cédula
    Entonces el sistema muestra el mensaje "No se encontraron reservas para los criterios ingresados en esta sede"

  Escenario: Aislamiento de datos por sede — no se muestran reservas de otras sedes
    Dado que existe una reserva del huésped con cédula "1098765432" en la "Sede Sur"
    Y el recepcionista tiene sesión activa en la "Sede Norte"
    Cuando busca por la cédula "1098765432"
    Entonces el sistema no muestra la reserva de la "Sede Sur"
    Y solo muestra reservas correspondientes a la "Sede Norte"

  Escenario: Intento de búsqueda con campo vacío
    Cuando el recepcionista intenta realizar la búsqueda sin ingresar ningún criterio
    Entonces el sistema muestra el mensaje "Ingrese una cédula o ID de reserva para realizar la búsqueda"
    Y no ejecuta ninguna consulta
```

---

## Supuestos / preguntas abiertas

- Se asume que la búsqueda puede retornar múltiples reservas si un huésped tiene más de una reserva activa en la misma sede.
- No se especificó si se pueden buscar reservas en estados históricos (canceladas, cerradas); se asume que la búsqueda incluye todos los estados para facilitar la consulta en mostrador.
- No se especificó si el recepcionista puede hacer búsqueda parcial (ej. los primeros dígitos de la cédula); se asume búsqueda exacta.
