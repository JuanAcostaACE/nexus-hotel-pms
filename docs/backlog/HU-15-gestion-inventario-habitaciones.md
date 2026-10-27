# HU-15: Gestión del inventario de habitaciones (CRUD)

**Como** administrador  
**Quiero** crear, editar y cambiar el estado de las habitaciones del hotel  
**Para** mantener el inventario actualizado y controlar qué habitaciones están disponibles para reservas

---

## Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | La gestión del inventario de habitaciones puede desarrollarse independientemente de reservas y consumos. |
| **N — Negotiable** | ✅ | No impone la interfaz de gestión ni los campos adicionales opcionales de una habitación. |
| **V — Valuable** | ✅ | Permite mantener el catálogo de habitaciones exacto, incluyendo las temporalmente fuera de servicio. |
| **E — Estimable** | ✅ | Las operaciones CRUD y el efecto del estado "Mantenimiento" en la disponibilidad están claramente definidos. |
| **S — Small** | ✅ | Cubre solo el CRUD de habitaciones; la consulta de disponibilidad y las reservas son historias separadas. |
| **T — Testable** | ✅ | La ocultación de habitaciones en mantenimiento y la configuración de capacidad y precio son verificables. |

---

## Criterios de aceptación (Gherkin)

```gherkin
# language: es
Característica: Gestión del inventario de habitaciones

  Antecedentes:
    Dado que el administrador tiene sesión activa con la sede "Sede Norte" activa

  Escenario: Creación exitosa de una nueva habitación
    Dado que el administrador ingresa los datos de la nueva habitación: número, tipo, capacidad máxima y precio base
    Cuando confirma la creación de la habitación
    Entonces el sistema registra la habitación en el inventario de la "Sede Norte"
    Y la habitación queda en estado "Disponible" por defecto
    Y aparece en las consultas de disponibilidad para los filtros correspondientes

  Escenario: Edición de capacidad y precio base de una habitación existente
    Dado que la habitación "305" de la "Sede Norte" tiene capacidad 2 y precio base $150.000
    Cuando el administrador cambia la capacidad a 3 y el precio base a $180.000 y confirma los cambios
    Entonces el sistema actualiza los datos de la habitación "305"
    Y las consultas de disponibilidad futuras reflejan la nueva capacidad y precio

  Escenario: Cambio de estado de habitación a "Mantenimiento"
    Dado que la habitación "102" de la "Sede Norte" está en estado "Disponible"
    Cuando el administrador cambia su estado a "Mantenimiento"
    Entonces el sistema actualiza el estado de la habitación a "Mantenimiento"
    Y la habitación queda oculta de las consultas de disponibilidad para clientes
    Y no puede ser seleccionada para nuevas reservas mientras permanezca en ese estado

  Escenario: Restauración de habitación desde estado "Mantenimiento"
    Dado que la habitación "102" de la "Sede Norte" está en estado "Mantenimiento"
    Cuando el administrador cambia su estado a "Disponible"
    Entonces el sistema actualiza el estado a "Disponible"
    Y la habitación vuelve a aparecer en las consultas de disponibilidad

  Escenario: Intento de crear habitación con datos obligatorios incompletos
    Dado que el administrador no ha ingresado el precio base o la capacidad máxima
    Cuando intenta confirmar la creación de la habitación
    Entonces el sistema muestra el mensaje indicando los campos obligatorios faltantes
    Y no crea la habitación

  Escenario: Habitación en "Mantenimiento" no aparece en consultas de disponibilidad de clientes
    Dado que la habitación "102" está en estado "Mantenimiento"
    Cuando un cliente consulta disponibilidad en la "Sede Norte" para cualquier fecha
    Entonces la habitación "102" no aparece en los resultados de disponibilidad
```

---

## Supuestos / preguntas abiertas

- No se especificó si una habitación con reservas futuras puede pasar a "Mantenimiento"; se asume que el sistema debe advertir al administrador y requerir confirmación si hay reservas futuras afectadas.
- No se especificó el listado completo de campos de una habitación (ej. descripción, fotografías, amenidades); se trabajó con los campos mencionados: número, tipo, capacidad y precio base.
- Se asume que cada habitación pertenece a una única sede y no puede moverse entre sedes.
