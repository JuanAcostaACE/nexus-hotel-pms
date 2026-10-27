# HU-13: Configuración y aislamiento multi-sede (Multitenancy)

**Como** administrador  
**Quiero** cambiar entre sedes y que todas las vistas, reservas y consumos se filtren exclusivamente por la sede activa  
**Para** gestionar distintas sucursales de manera segura e independiente sin mezclar información entre ellas

---

## Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | El mecanismo de aislamiento por sede puede desarrollarse como capa transversal independiente. |
| **N — Negotiable** | ✅ | No impone el modelo de datos de multi-tenancy; el equipo decide la arquitectura de separación. |
| **V — Valuable** | ✅ | Permite al administrador operar varias sedes sin riesgo de cruzar información ni comprometer la privacidad de cada sucursal. |
| **E — Estimable** | ✅ | La regla de filtrado estricto por ID de sede y el cambio de sede están claramente definidos. |
| **S — Small** | ✅ | Cubre solo la lógica de selección y aislamiento de sede; la gestión de usuarios y habitaciones son historias separadas. |
| **T — Testable** | ✅ | El filtrado por sede es verificable: ningún dato de otra sede debe aparecer en la sesión activa. |

---

## Criterios de aceptación (Gherkin)

```gherkin
# language: es
Característica: Configuración y aislamiento multi-sede

  Antecedentes:
    Dado que el administrador global tiene sesión activa
    Y existen al menos dos sedes configuradas: "Sede Norte" y "Sede Sur"

  Escenario: Cambio de sede activa por el administrador global
    Dado que el administrador tiene seleccionada la "Sede Norte" como sede activa
    Cuando el administrador cambia la sede activa a "Sede Sur"
    Entonces el sistema actualiza todas las vistas con la información correspondiente a la "Sede Sur"
    Y no muestra datos de la "Sede Norte" en ninguna vista del panel

  Escenario: Todas las vistas se filtran estrictamente por la sede activa
    Dado que el administrador tiene seleccionada la "Sede Norte" como sede activa
    Cuando accede a las vistas de reservas, habitaciones, consumos y usuarios
    Entonces cada vista muestra exclusivamente los datos asociados al ID de la "Sede Norte"
    Y no aparece ningún registro cuyo ID de sede sea diferente al de la sede activa

  Escenario: El recepcionista no puede cambiar de sede
    Dado que el usuario con rol "Recepcionista" está asignado a la "Sede Norte"
    Y tiene sesión activa
    Cuando accede al sistema
    Entonces el sistema no muestra ninguna opción para cambiar de sede
    Y todas sus operaciones están restringidas a los datos de la "Sede Norte"

  Escenario: Aislamiento de reportes por sede activa
    Dado que el administrador tiene seleccionada la "Sede Sur" como sede activa
    Cuando genera un reporte de ocupación o ingresos
    Entonces el reporte incluye únicamente datos de la "Sede Sur"
    Y no incluye ningún dato de la "Sede Norte" ni de otras sedes

  Escenario: Intento de acceso directo a datos de otra sede mediante URL o API
    Dado que el administrador tiene seleccionada la "Sede Norte" como sede activa
    Cuando intenta acceder directamente a recursos de la "Sede Sur" mediante una URL o llamada de API
    Entonces el sistema rechaza la solicitud
    Y muestra el mensaje "No tiene acceso a los recursos de la sede solicitada"
```

---

## Supuestos / preguntas abiertas

- Se asume que "administrador global" puede ver y gestionar todas las sedes, mientras que los administradores locales (si existen) estarían limitados a una sede específica. Este nivel de jerarquía no fue especificado y se registra como pregunta abierta.
- No se especificó si existe una "vista consolidada" que muestre datos de todas las sedes simultáneamente para el administrador global; se asume que solo se ve una sede a la vez.
