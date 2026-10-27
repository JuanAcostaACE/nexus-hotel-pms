# HU-01: Consulta de disponibilidad de habitaciones

**Como** cliente  
**Quiero** consultar las habitaciones disponibles en una sede para un rango de fechas y número de huéspedes  
**Para** verificar qué opciones existen antes de realizar una reserva

---

## Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | No depende de ninguna otra historia para entregarse; el cliente puede consultar sin necesidad de iniciar sesión ni reservar. |
| **N — Negotiable** | ✅ | No impone tecnología de búsqueda ni interfaz específica; el equipo puede decidir el mecanismo de filtrado. |
| **V — Valuable** | ✅ | Permite al cliente tomar decisiones informadas antes de comprometerse a una compra. |
| **E — Estimable** | ✅ | Las reglas de negocio (sede obligatoria, fechas lógicas, filtro por capacidad) están claramente definidas. |
| **S — Small** | ✅ | Se limita a la consulta; la reserva es una historia separada. |
| **T — Testable** | ✅ | Los filtros y resultados esperados son verificables objetivamente. |

---

## Criterios de aceptación (Gherkin)

```gherkin
# language: es
Característica: Consulta de disponibilidad de habitaciones

  Antecedentes:
    Dado que el sistema tiene registradas múltiples sedes con habitaciones en distintos estados

  Escenario: Consulta exitosa con resultados disponibles
    Dado que el cliente selecciona la sede "Sede Norte"
    Y ingresa la fecha de entrada "2026-10-01" y la fecha de salida "2026-10-05"
    Y especifica 2 huéspedes
    Cuando solicita la consulta de disponibilidad
    Entonces el sistema muestra únicamente las habitaciones con estado "Disponible" de la "Sede Norte"
    Y excluye habitaciones cuya capacidad máxima sea menor a 2 huéspedes
    Y muestra el tipo, la capacidad y el precio base de cada habitación disponible

  Escenario: Sin resultados por falta de capacidad suficiente
    Dado que el cliente selecciona la sede "Sede Sur"
    Y ingresa fechas válidas
    Y especifica 3 huéspedes
    Cuando solicita la consulta de disponibilidad
    Entonces el sistema no muestra habitaciones de tipo "Simple" ni otras con capacidad menor a 3
    Y si no hay habitaciones con capacidad suficiente, muestra el mensaje "No hay habitaciones disponibles para los criterios seleccionados"

  Escenario: Intento de consulta sin seleccionar sede
    Dado que el cliente no ha seleccionado ninguna sede
    Cuando intenta solicitar la consulta de disponibilidad
    Entonces el sistema muestra el mensaje de error "Debe seleccionar una sede para continuar"
    Y no ejecuta la búsqueda

  Escenario: Fecha de salida anterior o igual a la fecha de entrada
    Dado que el cliente selecciona la sede "Sede Norte"
    Y ingresa la fecha de entrada "2026-10-05" y la fecha de salida "2026-10-03"
    Cuando intenta solicitar la consulta de disponibilidad
    Entonces el sistema muestra el mensaje de error "La fecha de salida debe ser posterior a la fecha de entrada"
    Y no ejecuta la búsqueda

  Escenario: Fecha de entrada en el pasado
    Dado que el cliente ingresa la fecha de entrada "2024-01-01"
    Cuando intenta solicitar la consulta de disponibilidad
    Entonces el sistema muestra el mensaje de error "La fecha de entrada no puede ser anterior a la fecha actual"
    Y no ejecuta la búsqueda

  Escenario: Habitaciones en estado distinto a Disponible son excluidas
    Dado que la sede "Sede Norte" tiene habitaciones en estado "Mantenimiento", "En Limpieza" y "Ocupada"
    Cuando el cliente consulta disponibilidad para esa sede con fechas y capacidad válidas
    Entonces el sistema solo muestra habitaciones con estado "Disponible"
    Y excluye las habitaciones en estado "Mantenimiento", "En Limpieza" y "Ocupada"
```

---

## Supuestos / preguntas abiertas

- Se asume que la consulta no requiere autenticación del cliente.
- Se asume que el número de huéspedes mínimo es 1 y que el sistema valida que sea un valor positivo entero; el límite máximo no fue especificado.
- No se especificó si las habitaciones bloqueadas temporalmente (en proceso de reserva) deben o no mostrarse; se asume que se excluyen de la consulta.
