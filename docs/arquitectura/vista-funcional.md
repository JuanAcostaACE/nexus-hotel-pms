# Vista Funcional — Nexus Hotel PMS

> **Propósito:** Describir las capacidades funcionales del sistema organizadas por módulos y casos de uso, trazables a las 17 HU.

---

## Mapa de Capacidades por Módulo

### 🟦 Módulo Cliente / Front-Office (Público)

| Capacidad | HU | Descripción Breve |
|-----------|----|-------------------|
| Consultar disponibilidad | HU-01 | Filtros: sede, fechas, huéspedes; solo habitaciones *Disponibles* con capacidad suficiente |
| Registrar reserva + bloqueo 15 min | HU-02 | Captura datos huésped, genera ID, bloquea habitación, timer 15 min, redirige a pago |
| Pagar con PSE (async + webhook) | HU-03 | Estados: Aprobado → *Pagada/Reservada*; Rechazado → *Cancelada/Disponible*; Pendiente → espera webhook |
| Cancelar con política reembolso | HU-04 | >48h: 100%; <48h: penalidad 1 noche; liberación inmediata habitación |
| Recibir comprobante PDF + QR | HU-05 | Generación automática tras pago aprobado; QR codifica `reserva_id`; válido hasta check-out |

### 🟨 Módulo Recepcionista / Operación Hotelera (Privado, por sede)

| Capacidad | HU | Descripción Breve |
|-----------|----|-------------------|
| Autenticación + RBAC + sesión 15 min | HU-06 | Login por rol (Recepcionista/Admin), bloqueo 3 intentos, expiración inactividad, acceso solo a su sede |
| Buscar reserva (cédula / ID) | HU-07 | Aislado por sede activa; sin resultados → mensaje claro |
| Check-in (manual + QR) + No-Show | HU-08 | Timestamp servidor inalterable; *Pagada* → *En curso/Ocupada*; No-Show solo tras 23:59 |
| Registrar/anular consumos (auditoría) | HU-09 | Solo habitaciones *Ocupada*; anulación requiere motivo + auditoría (quién, cuándo, por qué) |
| Check-out + Late Check-out fee | HU-10 | Cálculo: estadía + consumos + recargo si >13:00; timestamp salida servidor; transición a limpieza |
| Habilitar habitación post-limpieza | HU-11 | Auto *Ocupada* → *En Limpieza* en check-out; recepcionista habilita manual a *Disponible* |

### 🟩 Módulo Administrador / Backoffice (Global, multi-sede)

| Capacidad | HU | Descripción Breve |
|-----------|----|-------------------|
| Facturación final + cierre inmutable | HU-12 | PDF consolidado (pagos, consumos, impuestos desglosados); estado *Cerrada* bloquea modificaciones |
| Configuración multi-sede + aislamiento | HU-13 | Admin global cambia sede activa; vistas filtradas estricto por `sede_id`; recepcionista no cambia sede |
| Gestión usuarios + roles + sede | HU-14 | CRUD usuarios; asignación obligatoria sede; recepcionista solo ve su sede; desactivación cuentas |
| Inventario habitaciones (CRUD + estados) | HU-15 | Número, tipo, capacidad, precio base; estado *Mantenimiento* oculta de disponibilidad |
| Catálogo consumos (CRUD + categorías) | HU-16 | Nombre, precio, categoría (Restaurante/Minibar); inactivar preserva histórico facturas |
| Reportes ocupación + ingresos | HU-17 | Filtros: sede + rango fechas; % ocupación; ingresos estadía + consumos + total; admin global ve cualquier sede |

---

## Diagrama de Casos de Uso (Mermaid)

```mermaid
useCaseDiagram
    package "Cliente" {
        usecase("UC01 Consultar disponibilidad") as UC01
        usecase("UC02 Registrar reserva + bloqueo") as UC02
        usecase("UC03 Pagar con PSE") as UC03
        usecase("UC04 Cancelar + reembolso") as UC04
        usecase("UC05 Recibir comprobante QR") as UC05
        actor("Cliente") as Client
        Client --> UC01
        Client --> UC02
        Client --> UC03
        Client --> UC04
        Client --> UC05
    }
    
    package "Recepcionista" {
        usecase("UC06 Login + RBAC") as UC06
        usecase("UC07 Buscar reserva") as UC07
        usecase("UC08 Check-in/No-Show") as UC08
        usecase("UC09 Consumos + anulación") as UC09
        usecase("UC10 Check-out + late fee") as UC10
        usecase("UC11 Habilitar limpieza") as UC11
        actor("Recepcionista") as Recep
        Recep --> UC06
        Recep --> UC07
        Recep --> UC08
        Recep --> UC09
        Recep --> UC10
        Recep --> UC11
    }
    
    package "Administrador" {
        usecase("UC12 Factura final + cierre") as UC12
        usecase("UC13 Multi-sede + aislamiento") as UC13
        usecase("UC14 Usuarios + roles") as UC14
        usecase("UC15 Inventario habitaciones") as UC15
        usecase("UC16 Catálogo consumos") as UC16
        usecase("UC17 Reportes") as UC17
        actor("Administrador") as Admin
        Admin --> UC06
        Admin --> UC12
        Admin --> UC13
        Admin --> UC14
        Admin --> UC15
        Admin --> UC16
        Admin --> UC17
    }
    
    package "Sistema" {
        usecase("UC05 Generar comprobante") as UC05s
        usecase("UC12 Generar factura") as UC12s
        usecase("UC11 Auto limpieza") as UC11s
        actor("Sistema") as Sys
        Sys --> UC05s
        Sys --> UC12s
        Sys --> UC11s
    }
    
    package "PSE" {
        actor("Pasarela PSE") as PSE
        PSE --> UC03
    }
```

---

## Reglas de Negocio Transversales

| Regla | HU Afectadas | Descripción |
|-------|--------------|-------------|
| **Aislamiento por sede** | HU-07, HU-13, HU-14, HU-17 | Ningún dato cruza `sede_id` en sesión activa |
| **Bloqueo concurrencia 15 min** | HU-02, HU-03 | Timer inicia en registro; expiración libera habitación |
| **Estados inmutables (timestamps)** | HU-08, HU-10 | Check-in/out timestamps del servidor, no editables |
| **Auditoría anulaciones** | HU-09, HU-12 | Motivo obligatorio; registro: usuario, timestamp, detalle |
| **Inmutabilidad factura cerrada** | HU-12 | Estado *Cerrada* rechaza cualquier modificación en cuenta |
| **Categorías consumos fijas** | HU-16 | Solo *Restaurante* y *Minibar* válidas (extensible por config) |
| **QR = reserva_id** | HU-05, HU-08 | Comprobante y check-in comparten mismo identificador |

---

## Pendientes de Definición (Open Questions)

- Valor exacto recargo Late Check-out (HU-10) — configurable por admin/sede
- Tiempo máximo espera webhook PSE antes de liberar (HU-03)
- Cálculo exacto % ocupación (HU-17) — noches ocupadas / disponibles vs reservas activas
- Impuestos locales y tasas (HU-12) — configurables por admin
- Jerarquía administradores (global vs local por sede) — HU-13
- Formato exportación reportes (HU-17) — PDF/Excel/CSV/pantalla
- Recuperación contraseña empleados (HU-14)
