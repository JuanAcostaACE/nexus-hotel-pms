# HU-11: Transición de habitación a estado de limpieza tras check-out

**Como** recepcionista  
**Quiero** que tras el check-out la habitación pase automáticamente a estado "En Limpieza" y poder habilitarla manualmente una vez el aseo esté confirmado  
**Para** controlar el ciclo físico de preparación de la habitación antes de ofrecerla nuevamente

---

## Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | La gestión del ciclo de limpieza es independiente del check-out como transacción financiera. |
| **N — Negotiable** | ✅ | No impone cómo se comunica el equipo de aseo; el equipo puede decidir si hay módulo de housekeeping. |
| **V — Valuable** | ✅ | Evita que una habitación sucia aparezca como disponible, protegiendo la reputación del hotel. |
| **E — Estimable** | ✅ | La transición automática al check-out y la habilitación manual están claramente definidas. |
| **S — Small** | ✅ | Solo cubre el ciclo de estados post check-out; el check-out financiero es una historia separada. |
| **T — Testable** | ✅ | Los cambios de estado (Ocupada → En Limpieza → Disponible) son verificables objetivamente. |

---

## Criterios de aceptación (Gherkin)

```gherkin
# language: es
Característica: Transición de habitación a estado de limpieza tras check-out

  Antecedentes:
    Dado que el recepcionista tiene sesión activa en la sede correspondiente

  Escenario: Transición automática a "En Limpieza" tras confirmar el check-out
    Dado que la habitación "205" tiene estado "Ocupada"
    Cuando el recepcionista confirma el check-out de la habitación "205"
    Entonces el sistema cambia automáticamente el estado de la habitación a "En Limpieza"
    Y la habitación deja de aparecer como "Disponible" en las consultas de disponibilidad
    Y la habitación no puede ser asignada a nuevas reservas mientras esté "En Limpieza"

  Escenario: Habitación "En Limpieza" no visible en consultas de disponibilidad
    Dado que la habitación "205" está en estado "En Limpieza"
    Cuando un cliente consulta disponibilidad en la sede para las fechas correspondientes
    Entonces la habitación "205" no aparece en los resultados de disponibilidad

  Escenario: Habilitación manual de habitación tras confirmación de limpieza
    Dado que la habitación "205" está en estado "En Limpieza"
    Y el personal de aseo ha confirmado que la habitación está lista
    Cuando el recepcionista habilita manualmente la habitación "205"
    Entonces el sistema cambia el estado de la habitación a "Disponible"
    Y la habitación vuelve a aparecer en las consultas de disponibilidad

  Escenario: Intento de habilitar habitación sin pasar por estado "En Limpieza"
    Dado que la habitación "101" tiene estado "Ocupada"
    Cuando el recepcionista intenta cambiar directamente su estado a "Disponible" sin registrar check-out
    Entonces el sistema no permite el cambio directo de "Ocupada" a "Disponible"
    Y muestra el mensaje "Debe completar el check-out antes de habilitar la habitación"

  Escenario: Vista de habitaciones en limpieza para gestión del recepcionista
    Dado que existen varias habitaciones en estado "En Limpieza" en la sede activa
    Cuando el recepcionista accede al panel de gestión de habitaciones
    Entonces el sistema muestra las habitaciones en estado "En Limpieza" diferenciadas visualmente
    Y permite seleccionar cada una para habilitarla individualmente
```

---

## Supuestos / preguntas abiertas

- Se asume que el personal de aseo no tiene acceso propio al sistema para confirmar la limpieza; la confirmación la realiza el recepcionista de forma manual. Si existe un rol o módulo de housekeeping, este alcance podría ampliarse.
- No se especificó si hay un tiempo máximo en estado "En Limpieza" antes de generar una alerta; se registra como pregunta abierta.
- Se asume que el administrador también puede habilitar habitaciones desde el módulo de administración.
