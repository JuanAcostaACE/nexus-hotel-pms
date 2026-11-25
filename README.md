# Nexus Hotel PMS

Sistema de gestión hotelera para operación **multi-sede**. Cubre la reserva en línea con bloqueo de concurrencia, el pago asíncrono con PSE, la operación de mostrador (check-in, consumos, check-out, limpieza y facturación) y la administración de sedes, usuarios, inventario, catálogo y reportes.

- **Backend:** API REST en Spring Boot 3.2 (Java 17) con MongoDB Atlas y autenticación JWT. Código en [`backend/`](backend/).
- **Frontend:** SPA en JavaScript puro, en el repositorio [NexusFrontend](https://github.com/JuanAcostaACE/NexusFrontend).
- **Historias de usuario:** 17 (HU-01 a HU-17) en [`docs/backlog/`](docs/backlog/).

## Contenido

1. [Diagramas de arquitectura](#diagramas-de-arquitectura)
2. [Patrones de diseño](#patrones-de-diseño)
3. [Roles y permisos](#roles-y-permisos)
4. [Historias de usuario](#historias-de-usuario)
5. [Estructura del repositorio](#estructura-del-repositorio)
6. [Cómo ejecutarlo](#cómo-ejecutarlo)
7. [Pruebas](#pruebas)
8. [Autor](#autor)

## Diagramas de arquitectura

Los diagramas están en formato de imagen. La versión textual de cada vista está en [`docs/arquitectura/`](docs/arquitectura/).

| Vista | Qué muestra | Detalle |
|---|---|---|
| **Contexto** | El sistema, sus tres actores (Cliente, Recepcionista, Administrador) y los sistemas externos (PSE y correo). | [vista-contexto.md](docs/arquitectura/vista-contexto.md) |
| **Funcional** | Los componentes del sistema y las interfaces entre ellos: reservas, pagos, comprobantes QR, facturación, inventario, operación, sedes y autenticación. | [vista-funcional.md](docs/arquitectura/vista-funcional.md) |
| **Lógica (por capas)** | Las clases de cada capa (presentación, aplicación, dominio e infraestructura) y sus dependencias. | [vista-logica.md](docs/arquitectura/vista-logica.md) |
| **Despliegue** | Dónde corre cada parte: navegador, servidor web, API, base de datos, servidor de trabajos, correo y PSE. | — |

### Vista de contexto
<img width="888" height="659" alt="Diagrama vista de contexto" src="https://github.com/user-attachments/assets/7eca8614-7886-4c24-8ac6-2dc2dbe65af4" />

### Vista funcional
<img width="1457" height="835" alt="diagrama vista funcional (6)" src="https://github.com/user-attachments/assets/41c19ea1-ce8d-43d9-be63-9063b17e9a65" />


### Vista logica
<img width="1124" height="1084" alt="diagrama vista logica (7)" src="https://github.com/user-attachments/assets/737c2550-963f-40a2-84e9-cb528aee2a19" />


### Vista de desarrollo (por capas)
<img width="1209" height="961" alt="diagrama vista de desarrollo(por capas) (8)" src="https://github.com/user-attachments/assets/b4349d45-f65d-4450-a725-9d335a41eee5" />

### Vista de despliegue
<img width="1565" height="667" alt="Diagrama vista de despliegue" src="https://github.com/user-attachments/assets/5f02dcbc-721e-4543-9b7d-b891d06a9c4c" />

## Patrones de diseño

| Patrón | Problema que resuelve | Dónde está en el código |
|---|---|---|
| **Strategy** | La política de reembolso cambia según las horas antes del check-in (HU-04). | `aplicacion/reembolso/`: `PoliticaReembolso`, `ReembolsoTotal`, `ReembolsoParcial`, `SelectorPoliticaReembolso` |
| **Adapter** | La pasarela PSE tiene su propio formato. El sistema solo conoce una interfaz (HU-03). | `aplicacion/puertos/IPagoGateway` y `infraestructura/pse/PseGatewayAdapter` |

## Roles y permisos

| Rol | Acceso | Qué puede hacer |
|---|---|---|
| **Cliente** | Público (sin login). Para operar una reserva se pide el código y el documento. | Consultar disponibilidad, reservar, pagar, cancelar y descargar el comprobante (HU-01 a HU-05). |
| **Recepcionista** | Sesión. Solo ve los datos de su sede. | Buscar reservas, check-in y no-show, consumos, check-out, habilitar habitaciones y facturar (HU-06 a HU-12). |
| **Administrador** | Sesión. Acceso a todas las sedes. | Lo mismo que el recepcionista, más sedes, usuarios, inventario, catálogo y reportes (HU-13 a HU-17). |

## Historias de usuario

| HU | Título | Módulo |
|---|---|---|
| [HU-01](docs/backlog/HU-01-consulta-disponibilidad-habitaciones.md) | Consulta de disponibilidad de habitaciones | Cliente |
| [HU-02](docs/backlog/HU-02-registro-reserva-bloqueo-concurrencia.md) | Registro de reserva con bloqueo de concurrencia | Cliente |
| [HU-03](docs/backlog/HU-03-procesamiento-pago-pse.md) | Procesamiento de pago seguro mediante PSE | Cliente |
| [HU-04](docs/backlog/HU-04-cancelacion-reserva-politica-reembolso.md) | Cancelación de reserva y política de reembolso | Cliente |
| [HU-05](docs/backlog/HU-05-emision-comprobante-reserva-qr.md) | Emisión de comprobante con código QR | Sistema |
| [HU-06](docs/backlog/HU-06-autenticacion-control-accesos.md) | Autenticación y control de acceso por rol | Seguridad |
| [HU-07](docs/backlog/HU-07-busqueda-localizacion-reservas.md) | Búsqueda y localización de reservas en mostrador | Recepcionista |
| [HU-08](docs/backlog/HU-08-gestion-checkin-no-shows.md) | Gestión de check-in y registro de no-shows | Recepcionista |
| [HU-09](docs/backlog/HU-09-registro-anulacion-consumos.md) | Registro y anulación de consumos | Recepcionista |
| [HU-10](docs/backlog/HU-10-gestion-checkout-penalidad-tardio.md) | Check-out con penalidad por salida tardía | Recepcionista |
| [HU-11](docs/backlog/HU-11-transicion-estado-limpieza.md) | Transición de habitación a estado de limpieza | Recepcionista |
| [HU-12](docs/backlog/HU-12-facturacion-final-cierre-documental.md) | Facturación final y cierre documental | Sistema |
| [HU-13](docs/backlog/HU-13-configuracion-aislamiento-multisede.md) | Configuración y aislamiento multi-sede | Administrador |
| [HU-14](docs/backlog/HU-14-gestion-usuarios-roles.md) | Gestión de usuarios y roles por sede | Administrador |
| [HU-15](docs/backlog/HU-15-gestion-inventario-habitaciones.md) | Gestión del inventario de habitaciones | Administrador |
| [HU-16](docs/backlog/HU-16-configuracion-catalogo-consumos.md) | Configuración del catálogo de consumos | Administrador |
| [HU-17](docs/backlog/HU-17-generacion-reportes-operativos.md) | Reportes de ocupación e ingresos | Administrador |

El resumen con dependencias entre historias está en [`00-resumen.md`](00-resumen.md). Todas las historias en un solo archivo: [`todas-las-historias-usuario.md`](todas-las-historias-usuario.md).

## Estructura del repositorio

```
.
├── backend/                 API REST (Spring Boot + MongoDB) — ver backend/README.md
│   ├── src/                 Código fuente por capas
│   ├── postman/             Colección de Postman (58 requests por HU)
│   └── scripts/             Cliente de terminal y demostración
├── docs/
│   ├── arquitectura/        Vistas de contexto, funcional y lógica (texto)
│   └── backlog/             Historias de usuario HU-01 a HU-17
├── 00-resumen.md            Resumen del backlog y dependencias
├── todas-las-historias-usuario.md
└── HU-*.md                  Historias de usuario (copia en la raíz)
```

## Cómo ejecutarlo

La guía completa (variables de entorno, IntelliJ, PowerShell, datos de prueba y despliegue) está en [`backend/README.md`](backend/README.md).

Resumen rápido:

```powershell
cd backend
$env:MONGODB_URI = "mongodb+srv://USUARIO:CLAVE@CLUSTER.mongodb.net/"
$env:PORT = "8081"
mvn spring-boot:run
```

## Pruebas

- **Pruebas unitarias:** `mvn test` (42 pruebas: estados de habitación, reembolsos, reservas, seguridad y alineación con los diagramas).
- **Postman:** colección en `backend/postman/`, con una carpeta por historia de usuario.
- **Terminal:** `backend/scripts/demo.ps1` recorre las HU de punta a punta, y `backend/scripts/nexus.ps1` ofrece menús de cliente, recepción y administración.

## Autor

**Juan David Acosta Urbano**
Ingeniería de Software — Universidad Cooperativa de Colombia
