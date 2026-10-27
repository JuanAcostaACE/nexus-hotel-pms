# Nexus Hotel PMS - Multi-Tenant Hotel Management System

![Estado](https://img.shields.io/badge/Estado-En%20Diseño-yellow)
![Especificación](https://img.shields.io/badge/Especificación-INVEST%20%2B%20Gherkin-blue)
![Arquitectura](https://img.shields.io/badge/Arquitectura-Multi--sede%20(Multitenancy)-green)
![Licencia](https://img.shields.io/badge/Licencia-MIT-lightgrey)

> Sistema de gestión hotelera integral para operación multi-sede (multitenant) que resuelve problemas críticos de operación hotelera: prevención de condiciones de carrera en reservas, pagos asíncronos con PSE, trazabilidad en mostrador, auditoría estricta y aislamiento lógico de datos por sucursal.

## Descripción Ejecutiva

Nexus Hotel PMS es una plataforma web diseñada para la gestión completa de reservas hoteleras, operación de recepción (front-desk) y control administrativo (backoffice) en entornos multi-sede.

El sistema aborda desafíos operativos reales:

- Bloqueo de concurrencia (15 min) - Evita doble reserva mientras el cliente paga (HU-02)
- Pagos asíncronos PSE - Manejo robusto de estados Aprobado/Rechazado/Pendiente con webhooks (HU-03)
- Trazabilidad de mostrador - Check-in con QR, control de No-Shows, transición automática a Housekeeping, recargos automáticos por Late Check-out (HU-08, HU-10, HU-11)
- Auditoría inmutable - Anulación de consumos con motivo obligatorio + trazabilidad; facturación final en PDF inalterable (HU-09, HU-12)
- Multitenancy lógico - Aislamiento estricto por sede_id; RBAC por rol (Admin global / Recepcionista por sede) (HU-13, HU-14)

## Características Arquitectónicas Clave

| Característica | Descripción | HU Relacionadas |
|----------------|-------------|-----------------|
| Concurrencia optimista + Timer 15 min | Bloqueo temporal de habitación al registrar reserva; liberación automática si expira | HU-02, HU-03 |
| Integración PSE asíncrona | Flujo completo: inicio -> webhook (Aprobado/Rechazado/Pendiente) -> actualización estado | HU-03 |
| Estados inmutables (timestamps servidor) | Check-in/out con timestamp del servidor, no editables; QR en comprobante valida reserva | HU-05, HU-08, HU-10 |
| Multitenancy por sede_id | Una instancia, múltiples sedes; vistas filtradas estricto; recepcionista no cambia de sede | HU-07, HU-13, HU-14, HU-17 |
| RBAC (Administrador / Recepcionista) | Login con bloqueo 3 intentos, sesión 15 min, acceso por rol y sede | HU-06, HU-14 |
| Ciclo de vida habitación | Disponible -> Bloqueada -> Reservada -> Ocupada -> En Limpieza -> Disponible (+ Mantenimiento) | HU-01, HU-02, HU-08, HU-10, HU-11, HU-15 |
| Auditoría de anulaciones | Motivo obligatorio + registro (quién, cuándo, por qué); histórico de facturas inalterable | HU-09, HU-12, HU-16 |
| Reportes operativos | Ocupación e ingresos por sede + rango de fechas; desglose estadía/consumos/impuestos | HU-17 |

## Actores del Sistema

| Actor | Tipo | Alcance | Permisos Clave |
|-------|------|---------|----------------|
| Cliente (Huésped) | Externo / Público | Una reserva a la vez | Consultar disponibilidad, reservar, pagar (PSE), cancelar, recibir comprobante QR |
| Recepcionista | Interno / Operativo | Una sola sede (asignada) | Login, buscar reservas (cédula/ID), check-in (manual/QR), marcar No-Show, registrar/anular consumos, check-out + late fee, habilitar habitación post-limpieza |
| Administrador | Interno / Global | Todas las sedes (cambio activo) | Login, gestión sedes, usuarios+roles+asignación sede, inventario habitaciones (CRUD+estados), catálogo consumos (CRUD+categorías), reportes ocupación/ingresos |
| Sistema (Automático) | Proceso | Transversal | Generar comprobante QR (post-pago), transición auto a En Limpieza (post-check-out), generar factura final PDF (post-check-out), timers liberación, webhooks PSE |

## Mapa del Product Backlog

### Modulo Cliente / Front-Office
| HU | Título | Enlace |
|----|--------|--------|
| HU-01 | Consulta de disponibilidad de habitaciones | [docs/backlog/HU-01-consulta-disponibilidad-habitaciones.md](docs/backlog/HU-01-consulta-disponibilidad-habitaciones.md) |
| HU-02 | Registro de reserva con bloqueo de concurrencia | [docs/backlog/HU-02-registro-reserva-bloqueo-concurrencia.md](docs/backlog/HU-02-registro-reserva-bloqueo-concurrencia.md) |
| HU-03 | Procesamiento de pago seguro mediante PSE | [docs/backlog/HU-03-procesamiento-pago-pse.md](docs/backlog/HU-03-procesamiento-pago-pse.md) |
| HU-04 | Cancelación de reserva y política de reembolso | [docs/backlog/HU-04-cancelacion-reserva-politica-reembolso.md](docs/backlog/HU-04-cancelacion-reserva-politica-reembolso.md) |
| HU-05 | Emisión automática de comprobante con código QR | [docs/backlog/HU-05-emision-comprobante-reserva-qr.md](docs/backlog/HU-05-emision-comprobante-reserva-qr.md) |

### Modulo Recepcionista / Operación Hotelera
| HU | Título | Enlace |
|----|--------|--------|
| HU-06 | Autenticación y control de acceso por rol | [docs/backlog/HU-06-autenticacion-control-accesos.md](docs/backlog/HU-06-autenticacion-control-accesos.md) |
| HU-07 | Búsqueda y localización de reservas en mostrador | [docs/backlog/HU-07-busqueda-localizacion-reservas.md](docs/backlog/HU-07-busqueda-localizacion-reservas.md) |
| HU-08 | Gestión de check-in y registro de no-shows | [docs/backlog/HU-08-gestion-checkin-no-shows.md](docs/backlog/HU-08-gestion-checkin-no-shows.md) |
| HU-09 | Registro y anulación de consumos durante la estadía | [docs/backlog/HU-09-registro-anulacion-consumos.md](docs/backlog/HU-09-registro-anulacion-consumos.md) |
| HU-10 | Gestión de check-out con penalidad por salida tardía | [docs/backlog/HU-10-gestion-checkout-penalidad-tardio.md](docs/backlog/HU-10-gestion-checkout-penalidad-tardio.md) |
| HU-11 | Transición de habitación a estado de limpieza | [docs/backlog/HU-11-transicion-estado-limpieza.md](docs/backlog/HU-11-transicion-estado-limpieza.md) |

### Modulo Administrador / Backoffice
| HU | Título | Enlace |
|----|--------|--------|
| HU-12 | Facturación final y cierre documental de la reserva | [docs/backlog/HU-12-facturacion-final-cierre-documental.md](docs/backlog/HU-12-facturacion-final-cierre-documental.md) |
| HU-13 | Configuración y aislamiento multi-sede (Multitenancy) | [docs/backlog/HU-13-configuracion-aislamiento-multisede.md](docs/backlog/HU-13-configuracion-aislamiento-multisede.md) |
| HU-14 | Gestión de usuarios y roles con asignación por sede | [docs/backlog/HU-14-gestion-usuarios-roles.md](docs/backlog/HU-14-gestion-usuarios-roles.md) |
| HU-15 | Gestión del inventario de habitaciones (CRUD) | [docs/backlog/HU-15-gestion-inventario-habitaciones.md](docs/backlog/HU-15-gestion-inventario-habitaciones.md) |
| HU-16 | Configuración del catálogo de consumos | [docs/backlog/HU-16-configuracion-catalogo-consumos.md](docs/backlog/HU-16-configuracion-catalogo-consumos.md) |
| HU-17 | Generación de reportes operativos de ocupación e ingresos | [docs/backlog/HU-17-generacion-reportes-operativos.md](docs/backlog/HU-17-generacion-reportes-operativos.md) |

> Ver también: [Índice completo del Backlog](docs/backlog/README.md) | [Consolidado (17 HU en 1 archivo)](docs/backlog/todas-las-historias-usuario.md)

## Estructura del Repositorio

nexus-hotel-pms/
├── README.md
├── .gitignore
└── docs/
    ├── backlog/
    │   ├── README.md
    │   ├── HU-01-consulta-disponibilidad-habitaciones.md
    │   ├── HU-02-registro-reserva-bloqueo-concurrencia.md
    │   ├── HU-03-procesamiento-pago-pse.md
    │   ├── HU-04-cancelacion-reserva-politica-reembolso.md
    │   ├── HU-05-emision-comprobante-reserva-qr.md
    │   ├── HU-06-autenticacion-control-accesos.md
    │   ├── HU-07-busqueda-localizacion-reservas.md
    │   ├── HU-08-gestion-checkin-no-shows.md
    │   ├── HU-09-registro-anulacion-consumos.md
    │   ├── HU-10-gestion-checkout-penalidad-tardio.md
    │   ├── HU-11-transicion-estado-limpieza.md
    │   ├── HU-12-facturacion-final-cierre-documental.md
    │   ├── HU-13-configuracion-aislamiento-multisede.md
    │   ├── HU-14-gestion-usuarios-roles.md
    │   ├── HU-15-gestion-inventario-habitaciones.md
    │   ├── HU-16-configuracion-catalogo-consumos.md
    │   ├── HU-17-generacion-reportes-operativos.md
    │   └── todas-las-historias-usuario.md
    └── arquitectura/
        ├── vista-contexto.md
        ├── vista-funcional.md
        └── vista-logica.md

## Primeros Pasos (para desarrollo)

bash
# Clonar
git clone https://github.com/JuanAcostaACE/nexus-hotel-pms.git
cd nexus-hotel-pms

# Explorar documentación
cat docs/backlog/README.md
cat docs/arquitectura/vista-contexto.md
cat docs/arquitectura/vista-funcional.md
cat docs/arquitectura/vista-logica.md

## Autor

**Juan David Acosta Urbano**  
Ingeniería de Software — Universidad Cooperativa de Colombia

## Licencia

Este proyecto está bajo licencia **MIT**. Ver LICENSE para más detalles.
