# Nexus Hotel PMS — Backend (Spring Boot 3 + MongoDB)

API REST de un sistema de gestión hotelera **multi-sede**. Implementa las **17 historias de usuario (HU-01 a HU-17)** del
repositorio de referencia [nexus-hotel-pms](https://github.com/JuanAcostaACE/nexus-hotel-pms) y se prueba con Postman.

- **Fase 1 (HU-01 a HU-12):** disponibilidad, reserva con bloqueo, pago PSE, cancelación con reembolso, comprobante con QR,
  autenticación, búsqueda, check-in/no-show, consumos, check-out, limpieza y facturación.
- **Fase 2 (HU-13 a HU-17):** sedes, usuarios y roles, inventario de habitaciones, catálogo de consumos y reportes
  (todo exclusivo del Administrador).

## Stack
Java 17 · Spring Boot 3.2.5 · Maven · Spring Web · Spring Data MongoDB · Spring Security + JWT (jjwt 0.12.5) · Validation ·
ZXing (QR). Base de datos: **MongoDB Atlas**. Pago PSE y correo **simulados**; los PDF (comprobante y factura) son reales.

## Requisitos (Windows)
1. **JDK 17** o superior (`java -version`).
2. **Maven 3.9+** (`mvn -version`), o IntelliJ IDEA, que ya lo trae.
3. **MongoDB Atlas**: un cluster (el gratuito sirve) y su cadena de conexión. En Atlas → *Network Access* agrega tu IP.
4. **Postman** (para probar la API).

## Configuración
La cadena de conexión **no está en el código ni en este repositorio**: se lee de variables de entorno.

| Variable | Obligatoria | Valor por defecto | Descripción |
|---|---|---|---|
| `MONGODB_URI` | Sí | — | Cadena de conexión de Atlas (`mongodb+srv://usuario:clave@cluster.xxxx.mongodb.net/`) |
| `MONGODB_DATABASE` | No | `nexus_hotel_pms` | Nombre de la base de datos |
| `PORT` | No | `8080` | Puerto del servidor |
| `JWT_SECRET` | No | valor de desarrollo | Secreto para firmar los JWT (cámbialo fuera de pruebas) |
| `PSE_WEBHOOK_SECRET` | No | `nexus-pse-secreto` | Valor esperado en la cabecera `X-PSE-Secret` del webhook |
| `CORS_ORIGENES` | No | `http://localhost:*,http://127.0.0.1:*` | Orígenes del frontend web permitidos, separados por coma (admite comodines, p. ej. `https://*.vercel.app`) |

## Ejecutar

**IntelliJ IDEA:** *Run → Edit Configurations → + → Spring Boot*, clase principal
`com.nexus.hotelpms.NexusHotelPmsApplication`, y en *Environment variables* escribe, en una sola línea separada por `;`:
`MONGODB_URI=...;MONGODB_DATABASE=nexus_hotel_v2;PORT=8081`

**PowerShell:**
```powershell
$env:MONGODB_URI = "mongodb+srv://USUARIO:CLAVE@CLUSTER.mongodb.net/"
$env:MONGODB_DATABASE = "nexus_hotel_v2"
$env:PORT = "8081"
mvn spring-boot:run
```

Al primer arranque, si la base no tiene sedes, se crean **datos de prueba**:

| Qué | Valor |
|---|---|
| Administrador | `admin@hotel.com` / `Admin123*` |
| Recepcionista Sede Norte | `recepcionista@hotel.com` / `Recep123*` |
| Recepcionista Sede Sur | `recepcion.sur@hotel.com` / `Recep123*` |
| Habitaciones (en cada sede) | 101 Simple (1 pers.), 102 Doble (2), 201 Familiar (4), 205 Doble (2) |
| Catálogo (en cada sede) | Agua mineral 500ml, Almuerzo ejecutivo, Botella de vino (inactivo) |

Estas credenciales son solo para desarrollo y pruebas.

## Despliegue (Railway) y frontend web
El backend se publica tal cual en [Railway](https://railway.app) (Maven + Java 17, `system.properties`):
1. *New Project → Deploy from GitHub repo* y, en *Settings → Root Directory*, escribe `backend`.
2. En *Variables* define `MONGODB_URI`, `MONGODB_DATABASE`, `JWT_SECRET` (nuevo, mínimo 32 caracteres),
   `PSE_WEBHOOK_SECRET` y `CORS_ORIGENES` (la URL del frontend, p. ej. `https://tu-app.vercel.app`). Railway asigna `PORT`.
3. En Atlas → *Network Access* permite la IP de Railway (para una demo, `0.0.0.0/0`).
4. *Settings → Networking → Generate Domain* y prueba `https://TU-URL/api/sedes`.

El cliente de terminal funciona contra el backend publicado: `scripts
exus.ps1 -BaseUrl https://TU-URL`.

## Pruebas
- **Unitarias** (dominio y patrón Strategy, no necesitan Mongo): `mvn test`
- **Demostración en terminal** (recorre HU-01 a HU-17 con pausas, colores y verificación de cada código HTTP):
  `powershell -ExecutionPolicy Bypass -File scripts\demo.ps1` (opciones: `-BaseUrl`, `-Auto` para correr sin pausas,
  `-AbrirPdf` para abrir el comprobante y la factura al final). Necesita el backend en marcha y crea datos de prueba.
- **Regresión de la API** (con el backend en marcha): `B=http://localhost:8081 bash scripts/prueba-api.sh`
  (por defecto usa el puerto 8081). Recorre HU-02 a HU-16 y muestra el código HTTP de cada paso.
  Crea datos de prueba (sede, habitaciones, reservas) en la base configurada.

## Probar con Postman
1. *Import* → `postman/Nexus-Hotel-PMS-HU.postman_collection.json`.
2. Variable `baseUrl`: `http://localhost:8081` por defecto; cámbiala si usas otro puerto.
3. Ejecuta primero la carpeta **00** (guarda tokens e IDs en variables) y luego cada carpeta en orden con *Collection Runner*.
4. Cada request trae sus pruebas (`pm.test`): verde = la regla de la HU se cumple. Las fechas se calculan solas.
5. La colección tiene 50 requests (HU-01 a HU-17) y se puede ejecutar varias veces seguidas: cada corrida crea sus propios datos
   (sede, usuario, habitación e ítem con nombres únicos) y deja libres las habitaciones de prueba. Los datos creados quedan en la base.

El campo `simulacion` de `POST /api/reservas/{id}/pago` acepta `APROBADO`, `RECHAZADO` o `PENDIENTE`
(sin distinguir mayúsculas); si se omite, el pago se aprueba. Los correos se escriben en el log (`[CORREO SIMULADO]`).

## Endpoints

`{id}` de una reserva acepta el código (`RSV-20261001-001`) o el ID interno.
*Sesión* = cabecera `Authorization: Bearer <token>`. Cada respuesta autenticada trae `X-Token-Renovado`: la sesión caduca
tras **15 minutos sin actividad** (HU-06).

### Fase 1
| HU | Método y ruta | Acceso |
|---|---|---|
| 06 | `POST /api/auth/login` | público |
| 06 | `GET /api/admin/usuarios`, `POST /api/admin/usuarios/{id}/desbloquear` | Administrador |
| 01 | `GET /api/sedes`, `GET /api/sedes/{sedeId}/disponibilidad?fechaEntrada&fechaSalida&huespedes` | público |
| 02 | `POST /api/reservas` (bloquea 15 min) | público |
| 03 | `POST /api/reservas/{id}/pago`, `POST /api/webhooks/pse` (cabecera `X-PSE-Secret`) | público |
| 04 | `POST /api/reservas/{id}/cancelar` | público |
| 05 | `GET /api/reservas/{id}/comprobante` (PDF con QR) | público |
| 07 | `GET /api/reservas/buscar?cedula=` o `?id=`, `GET /api/reservas/{id}` | sesión |
| 08 | `POST /api/reservas/{id}/checkin`, `POST /api/reservas/{id}/no-show` | sesión |
| 09 | `GET /api/catalogo`, `POST/GET /api/habitaciones/{id}/consumos`, `POST /api/consumos/{id}/anular` | sesión |
| 10 | `POST /api/habitaciones/{id}/checkout` (`?confirmar=true` para finalizar) | sesión |
| 11 | `GET /api/habitaciones?estado=EnLimpieza`, `POST /api/habitaciones/{id}/habilitar` | sesión |
| 12 | `GET /api/reservas/{id}/factura`, `/factura/pdf`, `POST /api/reservas/{id}/cerrar` | sesión |

### Fase 2 (solo Administrador; un recepcionista recibe 403)
| HU | Método y ruta | Descripción |
|---|---|---|
| 13 | `GET /api/admin/sedes`, `POST /api/admin/sedes`, `PUT /api/admin/sedes/{id}`, `POST /api/admin/sedes/{id}/estado` | Listar todas (activas e inactivas), crear, editar y activar/inactivar sedes |
| 13 | `GET /api/admin/reservas?sedeId=` | Reservas de la sede elegida |
| 14 | `GET /api/admin/usuarios[?sedeId=]`, `POST /api/admin/usuarios`, `PUT /api/admin/usuarios/{id}`, `PUT /api/admin/usuarios/{id}/sede`, `DELETE /api/admin/usuarios/{id}` | Listar (todos o por sede), crear recepcionista, editar nombre/correo, reasignar sede, desactivar |
| 15 | `POST /api/admin/habitaciones`, `PUT /api/admin/habitaciones/{id}`, `POST /api/admin/habitaciones/{id}/estado` | Inventario y cambio de estado (p. ej. Mantenimiento) |
| 16 | `GET /api/admin/catalogo?sedeId=`, `POST /api/admin/catalogo`, `PUT /api/admin/catalogo/{id}`, `POST /api/admin/catalogo/{id}/estado` | Catálogo (activos e inactivos); inactivar no borra el histórico |
| 17 | `GET /api/admin/reportes/ocupacion`, `GET /api/admin/reportes/ingresos` (`?sedeId&fechaInicio&fechaFin`) | Ocupación; ingresos desglosados en estadía, recargo, consumos y total general |

## Arquitectura (paquete base `com.nexus.hotelpms`)
```
presentacion.api / dto / excepcion      Controllers REST, DTOs, manejo de errores
aplicacion.servicios                    ReservaService, PagoService, OperacionService, FacturacionService,
                                        AuthService, AdministracionService
aplicacion.puertos / reembolso / sesion Interfaces (IReservaRepository, IPagoGateway, IEmailSender, IPdfGenerator…) + Strategy
dominio.modelo / excepcion              Sede, Usuario→Administrador/Recepcionista, Habitacion, Reserva, Consumo, Pago, Factura…
infraestructura                         persistencia, pse (PseGatewayAdapter), correo (SmtpEmailSender), pdf (FacturaPdfGenerator),
                                        jobs (JobLiberacionBloqueo), seguridad (JWT), config (DataInitializer…)
```
Dependencias: presentación → aplicación → dominio; la infraestructura implementa los puertos de la aplicación.

**Aislamiento multi-sede:** el JWT lleva `sedeId`; un recepcionista solo ve y opera datos de su sede (si pide otra recibe 403 con "No tiene permisos para acceder a los datos de esta sede");
el administrador ve todas.

## Patrones de diseño
**Strategy — política de reembolso (HU-04).** Documentado con Javadoc en cada clase:
`PoliticaReembolso` (Strategy) · `ReembolsoTotal` y `ReembolsoParcial` (estrategias concretas) ·
`SelectorPoliticaReembolso` (elige según las horas al check-in; 48 h exactas = total) ·
`ReservaService#cancelar` (Context). Agregar una política nueva = una clase nueva, sin tocar el servicio.

**Adapter — pasarela PSE (HU-03).** `PseGatewayAdapter` adapta la API de PSE (`PseClienteSimulado`: centavos,
`OK/FAILED/PENDING`) al puerto `IPagoGateway` que usa la aplicación.

## Decisiones y supuestos (donde las HU no especifican)
- Bloqueo de habitación **atómico** en MongoDB (`findAndModify` Disponible→Bloqueada): dos clientes simultáneos → uno recibe 409.
- Con pago `Pendiente` la reserva pasa a `PendienteConfirmacion` y **se suspende** el temporizador de 15 min.
- Pago rechazado → reserva `Cancelada` y habitación `Disponible`.
- Penalidad de cancelación tardía = una noche (precio base). El límite de 48 h se mide contra las 00:00 de la fecha de entrada.
- **Check-in:** solo con la reserva `Pagada` y **no antes de la fecha de entrada** (422). El timestamp lo pone el servidor y es inalterable.
- **No-show:** solo después de las 23:59 del día de entrada.
- Recargo Late Check-out: valor fijo configurable (`hotel.recargo-late-checkout`, 50.000 COP) después de `hotel.hora-limite-checkout` (13:00).
- Impuestos de la factura (configurables): IVA 19 % sobre estadía + recargo; impuesto al consumo 8 % sobre consumos.
- Check-out: `confirmar=false` solo muestra el resumen; `confirmar=true` registra salida → `PendienteDeCierre` → habitación
  `EnLimpieza` → factura → `Cerrada`. Si la facturación fallara, la reserva queda `PendienteDeCierre` y se reintenta con
  `POST /api/reservas/{id}/cerrar`.
- La auditoría de anulaciones (consumo, motivo, responsable, timestamp del servidor) queda en el documento `consumos` y en el log.
- Bloqueo de cuenta: tras 3 fallos queda bloqueada hasta que un administrador la desbloquee; el contador se reinicia con un login exitoso.
- Baja de usuario (HU-14): lógica (`activo=false`); el usuario inactivo no puede iniciar sesión.
- Reportes (HU-17): ocupación = noches vendidas / (habitaciones × noches del rango); ingresos = reservas confirmadas
  (`Pagada`, `EnCurso`, `PendienteDeCierre`, `Cerrada`) cuya fecha de salida cae en el rango, desglosados en estadía, recargo
  por salida tardía y consumos (sin los anulados). Sin datos en el período, los valores salen en cero con un mensaje.
- La "sede activa" del administrador (HU-13) no se guarda en el servidor: el administrador indica la sede en cada consulta
  (`sedeId`), y el recepcionista siempre queda limitado a la suya.
- Zona horaria del servidor para todos los timestamps oficiales: `America/Bogota`.
