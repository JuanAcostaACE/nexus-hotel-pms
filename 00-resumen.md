# 00-resumen — Backlog de Historias de Usuario

Sistema de Gestión Hotelera Multi-sede  
Generado: 2026-09-08 | Total de historias: 17

---

## Tabla resumen del backlog

| ID | Título | Módulo | Actor Principal | Valor de Negocio | Archivo |
|---|---|---|---|---|---|
| HU-01 | Consulta de disponibilidad de habitaciones | Cliente (Front-office) | Cliente | Verificar opciones antes de reservar filtrando por sede, fechas y capacidad | [HU-01-consulta-disponibilidad-habitaciones.md](./HU-01-consulta-disponibilidad-habitaciones.md) |
| HU-02 | Registro de reserva con bloqueo de concurrencia | Cliente (Front-office) | Cliente | Asegurar la habitación mientras el cliente completa el pago (bloqueo de 15 min) | [HU-02-registro-reserva-bloqueo-concurrencia.md](./HU-02-registro-reserva-bloqueo-concurrencia.md) |
| HU-03 | Procesamiento de pago seguro mediante PSE | Cliente (Front-office) | Cliente / Sistema | Convertir la reserva en pagada o anularla según respuesta de la pasarela | [HU-03-procesamiento-pago-pse.md](./HU-03-procesamiento-pago-pse.md) |
| HU-04 | Cancelación de reserva y política de reembolso | Cliente (Front-office) | Cliente | Anular reserva con reembolso total (>48 h) o parcial (<48 h) y liberar habitación | [HU-04-cancelacion-reserva-politica-reembolso.md](./HU-04-cancelacion-reserva-politica-reembolso.md) |
| HU-05 | Emisión automática de comprobante con código QR | Cliente (Front-office) | Sistema | Entregar PDF oficial con QR para agilizar check-in tras pago exitoso | [HU-05-emision-comprobante-reserva-qr.md](./HU-05-emision-comprobante-reserva-qr.md) |
| HU-06 | Autenticación y control de acceso por rol | Recepcionista (Operación) | Recepcionista / Admin | Acceso seguro con bloqueo por intentos fallidos y expiración por inactividad | [HU-06-autenticacion-control-accesos.md](./HU-06-autenticacion-control-accesos.md) |
| HU-07 | Búsqueda y localización de reservas en mostrador | Recepcionista (Operación) | Recepcionista | Encontrar rápidamente reservas por cédula o ID dentro de la sede activa | [HU-07-busqueda-localizacion-reservas.md](./HU-07-busqueda-localizacion-reservas.md) |
| HU-08 | Gestión de check-in y registro de no-shows | Recepcionista (Operación) | Recepcionista | Formalizar ingreso con timestamp inalterable o marcar no-show preservando métricas | [HU-08-gestion-checkin-no-shows.md](./HU-08-gestion-checkin-no-shows.md) |
| HU-09 | Registro y anulación de consumos durante estadía | Recepcionista (Operación) | Recepcionista | Cargar servicios a la habitación y anular con auditoría completa | [HU-09-registro-anulacion-consumos.md](./HU-09-registro-anulacion-consumos.md) |
| HU-10 | Gestión de check-out con penalidad por salida tardía | Recepcionista (Operación) | Recepcionista | Cerrar cuenta con cálculo automático de recargo Late Check-out después de las 13:00 | [HU-10-gestion-checkout-penalidad-tardio.md](./HU-10-gestion-checkout-penalidad-tardio.md) |
| HU-11 | Transición de habitación a estado de limpieza | Recepcionista (Operación) | Recepcionista / Sistema | Controlar el ciclo Ocupada → En Limpieza → Disponible para proteger la calidad | [HU-11-transicion-estado-limpieza.md](./HU-11-transicion-estado-limpieza.md) |
| HU-12 | Facturación final y cierre documental de reserva | Recepcionista (Operación) | Sistema | PDF con impuestos desglosados y bloqueo de reserva en estado "Cerrada" | [HU-12-facturacion-final-cierre-documental.md](./HU-12-facturacion-final-cierre-documental.md) |
| HU-13 | Configuración y aislamiento multi-sede | Administrador (Backoffice) | Administrador | Gestionar sedes aisladas sin cruce de información entre sucursales | [HU-13-configuracion-aislamiento-multisede.md](./HU-13-configuracion-aislamiento-multisede.md) |
| HU-14 | Gestión de usuarios y roles con asignación por sede | Administrador (Backoffice) | Administrador | Crear y gestionar cuentas de empleados con acceso estrictamente delimitado por sede | [HU-14-gestion-usuarios-roles.md](./HU-14-gestion-usuarios-roles.md) |
| HU-15 | Gestión del inventario de habitaciones (CRUD) | Administrador (Backoffice) | Administrador | Configurar capacidad, precio y estado de habitaciones; ocultar las de Mantenimiento | [HU-15-gestion-inventario-habitaciones.md](./HU-15-gestion-inventario-habitaciones.md) |
| HU-16 | Configuración del catálogo de consumos | Administrador (Backoffice) | Administrador | Definir ítems cobrables con precio y categoría; inactivar sin afectar historial | [HU-16-configuracion-catalogo-consumos.md](./HU-16-configuracion-catalogo-consumos.md) |
| HU-17 | Generación de reportes operativos de ocupación e ingresos | Administrador (Backoffice) | Administrador | Analizar rendimiento financiero por sede y rango de fechas | [HU-17-generacion-reportes-operativos.md](./HU-17-generacion-reportes-operativos.md) |

---

## Dependencias relevantes entre historias

```
HU-01 ──► HU-02 ──► HU-03 ──► HU-05
                │
                └──► HU-04

HU-06 (prerequisito transversal para HU-07 a HU-12)

HU-07 ──► HU-08 ──► HU-09 ──► HU-10 ──► HU-11 ──► HU-12

HU-13 (prerequisito transversal para HU-14, HU-15, HU-16, HU-17)
HU-14 ──► HU-06 (usuarios creados en HU-14 se autentican en HU-06)
HU-15 ──► HU-01 (habitaciones del inventario alimentan la consulta)
HU-16 ──► HU-09 (catálogo de consumos alimenta el registro en estadía)
```

---

## Notas de implementación

- Todas las historias siguen el formato **INVEST** completo con validación explícita de los 6 criterios.
- Todos los criterios de aceptación están escritos en **Gherkin en español** con la directiva `# language: es`.
- Los supuestos menores están declarados en cada historia para facilitar su revisión sin releer todos los escenarios.
- Las historias están ordenadas según el flujo operativo natural del sistema hotelero.
