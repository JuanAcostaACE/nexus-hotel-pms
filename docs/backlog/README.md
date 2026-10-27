# Índice del Backlog — Nexus Hotel PMS

Este directorio contiene las **17 Historias de Usuario (HU-01 a HU-17)** que conforman el *Product Backlog* del sistema, cada una documentada bajo el estándar **INVEST** con criterios de aceptación en **Gherkin (BDD)**.

## Metodología

| Aspecto | Detalle |
|---------|---------|
| **Formato de Historia** | INVEST (Independent, Negotiable, Valuable, Estimable, Small, Testable) |
| **Criterios de Aceptación** | Gherkin (Given/When/Then) en español (\# language: es\) |
| **Trazabilidad** | Cada HU incluye: validación INVEST, escenarios Gherkin, supuestos y preguntas abiertas |
| **Consolidado** | \	odas-las-historias-usuario.md\ — archivo único con las 17 HU para lectura continua

## Lista de Historias de Usuario

| Código | Título | Módulo | Archivo |
|--------|--------|--------|---------|
| **HU-01** | Consulta de disponibilidad de habitaciones | Cliente / Front-office | [HU-01-consulta-disponibilidad-habitaciones.md](HU-01-consulta-disponibilidad-habitaciones.md) |
| **HU-02** | Registro de reserva con bloqueo de concurrencia | Cliente / Front-office | [HU-02-registro-reserva-bloqueo-concurrencia.md](HU-02-registro-reserva-bloqueo-concurrencia.md) |
| **HU-03** | Procesamiento de pago seguro mediante PSE | Cliente / Front-office | [HU-03-procesamiento-pago-pse.md](HU-03-procesamiento-pago-pse.md) |
| **HU-04** | Cancelación de reserva y política de reembolso | Cliente / Front-office | [HU-04-cancelacion-reserva-politica-reembolso.md](HU-04-cancelacion-reserva-politica-reembolso.md) |
| **HU-05** | Emisión automática de comprobante con código QR | Sistema / Transversal | [HU-05-emision-comprobante-reserva-qr.md](HU-05-emision-comprobante-reserva-qr.md) |
| **HU-06** | Autenticación y control de acceso por rol | Transversal / Seguridad | [HU-06-autenticacion-control-accesos.md](HU-06-autenticacion-control-accesos.md) |
| **HU-07** | Búsqueda y localización de reservas en mostrador | Recepcionista / Operación | [HU-07-busqueda-localizacion-reservas.md](HU-07-busqueda-localizacion-reservas.md) |
| **HU-08** | Gestión de check-in y registro de no-shows | Recepcionista / Operación | [HU-08-gestion-checkin-no-shows.md](HU-08-gestion-checkin-no-shows.md) |
| **HU-09** | Registro y anulación de consumos durante la estadía | Recepcionista / Operación | [HU-09-registro-anulacion-consumos.md](HU-09-registro-anulacion-consumos.md) |
| **HU-10** | Gestión de check-out con penalidad por salida tardía | Recepcionista / Operación | [HU-10-gestion-checkout-penalidad-tardio.md](HU-10-gestion-checkout-penalidad-tardio.md) |
| **HU-11** | Transición de habitación a estado de limpieza | Recepcionista / Operación | [HU-11-transicion-estado-limpieza.md](HU-11-transicion-estado-limpieza.md) |
| **HU-12** | Facturación final y cierre documental de la reserva | Sistema / Transversal | [HU-12-facturacion-final-cierre-documental.md](HU-12-facturacion-final-cierre-documental.md) |
| **HU-13** | Configuración y aislamiento multi-sede (Multitenancy) | Administrador / Backoffice | [HU-13-configuracion-aislamiento-multisede.md](HU-13-configuracion-aislamiento-multisede.md) |
| **HU-14** | Gestión de usuarios y roles con asignación por sede | Administrador / Backoffice | [HU-14-gestion-usuarios-roles.md](HU-14-gestion-usuarios-roles.md) |
| **HU-15** | Gestión del inventario de habitaciones (CRUD) | Administrador / Backoffice | [HU-15-gestion-inventario-habitaciones.md](HU-15-gestion-inventario-habitaciones.md) |
| **HU-16** | Configuración del catálogo de consumos | Administrador / Backoffice | [HU-16-configuracion-catalogo-consumos.md](HU-16-configuracion-catalogo-consumos.md) |
| **HU-17** | Generación de reportes operativos de ocupación e ingresos | Administrador / Backoffice | [HU-17-generacion-reportes-operativos.md](HU-17-generacion-reportes-operativos.md) |

## Máquinas de Estado Identificadas

### EstadoHabitacion
\\\
Disponible ? Bloqueada ? Reservada ? Ocupada ? En Limpieza ? Disponible
                    ?
              Mantenimiento (desde Disponible/Ocupada)
\\\

**Transiciones clave:**
- \Disponible ? Bloqueada\ — HU-02 (registro reserva, 15 min)
- \Bloqueada ? Disponible\ — HU-02 (expiración) / HU-03 (pago rechazado)
- \Bloqueada ? Reservada\ — HU-03 (pago aprobado)
- \Reservada ? Ocupada\ — HU-08 (check-in)
- \Ocupada ? En Limpieza\ — HU-10/HU-11 (check-out automático)
- \En Limpieza ? Disponible\ — HU-11 (habilitación manual)
- \Disponible/Ocupada ? Mantenimiento\ — HU-15 (admin)

### EstadoReserva
\\\
Pendiente ? Bloqueada ? Pagada ? En curso ? Pendiente de cierre ? Cerrada
    ?            ?          ?
Cancelada   Cancelada  Cancelada con penalidad
    ?            ?
No-Show    (desde Pagada tras 23:59 sin check-in)
\\\

**Transiciones clave:**
- \Pendiente ? Bloqueada\ — HU-02 (registro)
- \Bloqueada ? Pagada\ — HU-03 (pago aprobado/webhook)
- \Bloqueada ? Cancelada\ — HU-02 (expiración 15 min) / HU-03 (pago rechazado)
- \Pagada ? Cancelada / Cancelada con penalidad\ — HU-04 (>48h / <48h)
- \Pagada ? No-Show\ — HU-08 (23:59 sin llegada)
- \Pagada ? En curso\ — HU-08 (check-in exitoso)
- \En curso ? Pendiente de cierre\ — HU-10 (check-out confirmado)
- \Pendiente de cierre ? Cerrada\ — HU-12 (factura generada)

## Navegación

- [Volver al README principal](../../README.md)
- [Ver archivo consolidado](todas-las-historias-usuario.md)
- [Vistas de arquitectura](../arquitectura/)
