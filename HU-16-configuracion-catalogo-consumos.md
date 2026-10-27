# HU-16: Configuración del catálogo de consumos

**Como** administrador  
**Quiero** crear y gestionar los ítems del catálogo de productos y servicios cobrables con su precio y categoría  
**Para** que los recepcionistas puedan registrar consumos correctamente y el historial de facturas quede íntegro

---

## Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | La gestión del catálogo es independiente del registro de consumos; puede desarrollarse antes que HU-09. |
| **N — Negotiable** | ✅ | No impone las categorías adicionales ni el diseño de la interfaz de gestión del catálogo. |
| **V — Valuable** | ✅ | Permite estandarizar los ítems cobrables y protege la integridad del historial de facturas al inactivar ítems. |
| **E — Estimable** | ✅ | Las reglas de creación, las categorías y el comportamiento al inactivar un ítem están claramente definidas. |
| **S — Small** | ✅ | Solo cubre el CRUD del catálogo; el registro de consumos en estadía es una historia separada (HU-09). |
| **T — Testable** | ✅ | La visibilidad del ítem activo/inactivo y la integridad del historial son verificables. |

---

## Criterios de aceptación (Gherkin)

```gherkin
# language: es
Característica: Configuración del catálogo de consumos

  Antecedentes:
    Dado que el administrador tiene sesión activa con la sede correspondiente activa

  Escenario: Creación exitosa de un ítem en el catálogo
    Dado que el administrador ingresa el nombre "Agua mineral 500ml", el precio $3.500 y la categoría "Minibar"
    Cuando confirma la creación del ítem
    Entonces el sistema registra el ítem como activo en el catálogo de la sede activa
    Y el ítem queda disponible para que los recepcionistas lo registren como consumo

  Escenario: Edición de precio o nombre de un ítem existente
    Dado que el ítem "Almuerzo ejecutivo" existe en el catálogo con precio $35.000
    Cuando el administrador actualiza el precio a $38.000 y confirma los cambios
    Entonces el sistema actualiza el precio del ítem en el catálogo
    Y los nuevos consumos registrados usarán el precio actualizado
    Y los consumos ya registrados en facturas anteriores conservan el precio original

  Escenario: Inactivación de un ítem del catálogo
    Dado que el ítem "Botella de vino tinto" está activo en el catálogo
    Cuando el administrador inactiva el ítem
    Entonces el sistema marca el ítem como inactivo
    Y el ítem deja de aparecer en la lista de consumos disponibles para los recepcionistas
    Y no se puede registrar ese ítem en nuevas cuentas de habitación

  Escenario: El historial de facturas no se ve afectado al inactivar un ítem
    Dado que el ítem "Botella de vino tinto" fue registrado como consumo en la reserva "RSV-20261001-001"
    Y la reserva ya tiene estado "Cerrada" con su factura generada
    Cuando el administrador inactiva el ítem "Botella de vino tinto"
    Entonces la factura de la reserva "RSV-20261001-001" conserva el ítem con su precio y descripción originales
    Y el historial de facturación no se modifica

  Escenario: Reactivación de un ítem previamente inactivo
    Dado que el ítem "Botella de vino tinto" está inactivo en el catálogo
    Cuando el administrador reactiva el ítem
    Entonces el ítem vuelve a aparecer en la lista de consumos disponibles para los recepcionistas
    Y puede registrarse en nuevas cuentas de habitación

  Escenario: Creación de ítem con datos obligatorios incompletos
    Dado que el administrador no ha ingresado el precio o la categoría del nuevo ítem
    Cuando intenta confirmar la creación
    Entonces el sistema muestra el mensaje indicando los campos obligatorios faltantes
    Y no crea el ítem

  Esquema del escenario: Categorías válidas para ítems del catálogo
    Dado que el administrador intenta crear un ítem con la categoría "<categoría>"
    Cuando confirma la creación
    Entonces el sistema "<resultado>"

    Ejemplos:
      | categoría    | resultado                              |
      | Restaurante  | acepta y crea el ítem exitosamente     |
      | Minibar      | acepta y crea el ítem exitosamente     |
      | Otro         | rechaza indicando categoría no válida  |
```

---

## Supuestos / preguntas abiertas

- Las categorías definidas en el requerimiento son "Restaurante" y "Minibar". El Esquema del escenario asume que son las únicas categorías válidas; si hay más, deben ser confirmadas.
- Se asume que el catálogo es por sede; ítems de la "Sede Norte" no están disponibles en la "Sede Sur". No fue especificado explícitamente.
- No se especificó si se puede eliminar definitivamente un ítem o solo inactivarlo; se asume que solo se puede inactivar para preservar la trazabilidad histórica.
