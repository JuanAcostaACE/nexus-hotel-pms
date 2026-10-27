# HU-17: Generación de reportes operativos de ocupación e ingresos

**Como** administrador  
**Quiero** generar reportes de ocupación e ingresos totales filtrados por sede y rango de fechas  
**Para** analizar el rendimiento financiero y operativo de cada sucursal del hotel

---

## Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | Los reportes son un módulo de consulta independiente; no modifica datos operativos. |
| **N — Negotiable** | ✅ | No impone el formato de salida del reporte (PDF, Excel, pantalla); el equipo decide. |
| **V — Valuable** | ✅ | Provee información de toma de decisiones para la gerencia del hotel. |
| **E — Estimable** | ✅ | Los filtros requeridos (sede, rango de fechas) y los tipos de reporte (ocupación, ingresos) están definidos. |
| **S — Small** | ✅ | Solo cubre la generación de reportes; el CRUD de habitaciones y reservas son historias separadas. |
| **T — Testable** | ✅ | El filtrado por sede y fechas, y los datos incluidos en cada tipo de reporte son verificables. |

---

## Criterios de aceptación (Gherkin)

```gherkin
# language: es
Característica: Generación de reportes operativos de ocupación e ingresos

  Antecedentes:
    Dado que el administrador tiene sesión activa
    Y existen datos de reservas y consumos en el sistema para el período consultado

  Escenario: Generación de reporte de ocupación filtrado por sede y rango de fechas
    Dado que el administrador selecciona la "Sede Norte" y el rango de fechas "01/09/2026 - 30/09/2026"
    Cuando solicita el reporte de ocupación
    Entonces el sistema genera un reporte que muestra el porcentaje de ocupación de la "Sede Norte" en ese período
    Y el reporte incluye únicamente datos de reservas de la "Sede Norte"
    Y no incluye datos de ninguna otra sede

  Escenario: Generación de reporte de ingresos totales filtrado por sede y rango de fechas
    Dado que el administrador selecciona la "Sede Sur" y el rango de fechas "01/08/2026 - 31/08/2026"
    Cuando solicita el reporte de ingresos
    Entonces el sistema genera un reporte con los ingresos totales de la "Sede Sur" en ese período
    Y el reporte detalla ingresos por estadía, ingresos por consumos y total general
    Y no incluye datos de ninguna otra sede

  Escenario: Intento de generar reporte sin seleccionar sede
    Dado que el administrador no ha seleccionado ninguna sede
    Cuando intenta generar el reporte
    Entonces el sistema muestra el mensaje "Debe seleccionar una sede para generar el reporte"
    Y no ejecuta la consulta

  Escenario: Intento de generar reporte con rango de fechas inválido
    Dado que el administrador ingresa como fecha de inicio "30/09/2026" y como fecha de fin "01/09/2026"
    Cuando intenta generar el reporte
    Entonces el sistema muestra el mensaje "La fecha de inicio debe ser anterior a la fecha de fin"
    Y no ejecuta la consulta

  Escenario: Reporte generado sin datos en el período seleccionado
    Dado que el administrador selecciona la "Sede Norte" y un rango de fechas sin reservas registradas
    Cuando solicita el reporte
    Entonces el sistema genera el reporte indicando que no hay datos para el período seleccionado
    Y muestra los valores de ocupación e ingresos en cero

  Escenario: El administrador global puede generar reportes de cualquier sede
    Dado que el administrador global cambia la sede activa a "Sede Sur"
    Y selecciona un rango de fechas válido
    Cuando solicita el reporte de ocupación o ingresos
    Entonces el sistema genera el reporte con datos exclusivos de la "Sede Sur"
```

---

## Supuestos / preguntas abiertas

- No se especificó el formato de salida del reporte (pantalla, PDF exportable, Excel/CSV); se asume visualización en pantalla con opción de exportar. El formato exacto queda por confirmar.
- No se especificó el cálculo exacto de "porcentaje de ocupación" (noches ocupadas / noches totales disponibles, reservas activas, etc.); se asume la métrica estándar hotelera pero debe confirmarse.
- No se especificó si el reporte de ingresos incluye reservas canceladas con penalidad; se asume que sí, ya que generaron ingresos parciales.
- No se especificó si se requieren más tipos de reportes adicionales (p. ej. consumos por categoría, no-shows, reembolsos); los enunciados en el requerimiento son ocupación e ingresos totales.
