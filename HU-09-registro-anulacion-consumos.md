# HU-09: Registro y anulación de consumos durante la estadía

**Como** recepcionista  
**Quiero** registrar productos y servicios consumidos por el huésped y anular cargos incorrectos con trazabilidad  
**Para** mantener la cuenta de la habitación precisa y auditada durante toda la estadía

---

## Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | La gestión de consumos puede desarrollarse independientemente del check-out; son módulos distintos. |
| **N — Negotiable** | ✅ | No impone la interfaz de carga ni el catálogo de consumos; esos son definidos en otra historia. |
| **V — Valuable** | ✅ | Asegura la integridad de la cuenta del huésped y protege al hotel de errores de facturación. |
| **E — Estimable** | ✅ | La restricción de estado "Ocupada", el motivo obligatorio de anulación y el registro de auditoría están definidos. |
| **S — Small** | ✅ | Cubre solo el ciclo de carga y anulación de consumos; la facturación final es una historia separada. |
| **T — Testable** | ✅ | El estado de la habitación, la exigencia de motivo y el registro de auditoría son verificables. |

---

## Criterios de aceptación (Gherkin)

```gherkin
# language: es
Característica: Registro y anulación de consumos durante la estadía

  Antecedentes:
    Dado que el recepcionista tiene sesión activa en la sede correspondiente

  Escenario: Registro exitoso de un consumo en habitación ocupada
    Dado que la habitación "205" tiene estado "Ocupada"
    Y el ítem "Agua mineral 500ml" está activo en el catálogo de consumos
    Cuando el recepcionista selecciona la habitación "205" y registra el consumo del ítem con su precio
    Entonces el sistema agrega el consumo a la cuenta de la habitación "205"
    Y el total de la cuenta de la habitación se actualiza con el valor del nuevo consumo

  Escenario: Intento de registrar consumo en habitación no ocupada
    Dado que la habitación "101" tiene estado distinto a "Ocupada"
    Cuando el recepcionista intenta registrar un consumo en esa habitación
    Entonces el sistema muestra el mensaje "Solo se pueden registrar consumos en habitaciones con estado Ocupada"
    Y no agrega ningún consumo

  Escenario: Anulación de consumo con motivo obligatorio
    Dado que la habitación "205" tiene registrado el consumo "Almuerzo ejecutivo" por valor de $35.000
    Cuando el recepcionista solicita la anulación de ese consumo
    Y el recepcionista ingresa el motivo "Consumo registrado en habitación incorrecta"
    Entonces el sistema anula el consumo y descuenta su valor de la cuenta de la habitación
    Y genera un registro de auditoría con: el consumo anulado, el motivo, el recepcionista responsable y el timestamp del servidor

  Escenario: Intento de anulación de consumo sin ingresar motivo
    Dado que la habitación "205" tiene un consumo registrado
    Cuando el recepcionista intenta anular el consumo sin ingresar ningún motivo
    Entonces el sistema muestra el mensaje "Debe ingresar un motivo para anular el consumo"
    Y no realiza la anulación

  Escenario: Registro de consumo con ítem inactivo del catálogo
    Dado que el ítem "Botella de vino tinto" está marcado como inactivo en el catálogo
    Cuando el recepcionista intenta registrar ese ítem como consumo
    Entonces el sistema no muestra el ítem en la lista de consumos disponibles
    Y no permite su registro
```

---

## Supuestos / preguntas abiertas

- Se asume que el catálogo de consumos disponibles para el recepcionista está filtrado por sede (items activos de la sede activa).
- No se especificó si el recepcionista puede editar un consumo ya registrado o si únicamente puede anularlo y registrar uno nuevo; se asume que solo puede anular.
- Se asume que el registro de auditoría de anulaciones está disponible para consulta del administrador pero no es modificable por el recepcionista.
