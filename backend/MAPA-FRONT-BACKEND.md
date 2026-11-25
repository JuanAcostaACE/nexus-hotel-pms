# Mapa: botón del frontend → endpoint → código del backend

Cada endpoint del controlador tiene un comentario `// Front: ...` que dice en qué pantalla y botón se usa.
Cada método de servicio tiene `// Llamado desde: ...` con el controlador que lo llama.

Para ir desde VS Code: **Ctrl + Shift + F** y busca el nombre del método (por ejemplo `crear(`).

## Cliente (sin login)

| Botón en la web | Endpoint | Controlador (método) | Servicio |
|---|---|---|---|
| Buscar (Habitaciones) | `GET /api/sedes/{id}/disponibilidad` | `ReservaController.disponibilidad` | `ReservaService.consultarDisponibilidad` |
| Reservar → Confirmar reserva | `POST /api/reservas` | `ReservaController.crear` | `ReservaService.crear` |
| Pagar con PSE | `POST /api/reservas/{id}/pago` | `PagoController.pagar` | `PagoService.iniciarPago` |
| Cancelar reserva | `POST /api/reservas/{id}/cancelar` | `ReservaController.cancelar` | `ReservaService.cancelar` |
| Descargar comprobante | `GET /api/reservas/{id}/comprobante` | `ReservaController.comprobante` | `ReservaService.comprobanteCliente` |

## Recepción

| Botón en la web | Endpoint | Controlador (método) | Servicio |
|---|---|---|---|
| Buscar reserva → Buscar | `GET /api/reservas/buscar` | `ReservaController.buscar` | `ReservaService.buscar` |
| Registrar check-in | `POST /api/reservas/{id}/checkin` | `ReservaController.checkIn` | `ReservaService.checkIn` |
| Marcar No-Show | `POST /api/reservas/{id}/no-show` | `ReservaController.noShow` | `ReservaService.registrarNoShow` |
| Registrar consumo | `POST /api/habitaciones/{id}/consumos` | `OperacionController.registrarConsumo` | `OperacionService.registrarConsumo` |
| Anular (consumo) | `POST /api/consumos/{id}/anular` | `OperacionController.anular` | `OperacionService.anularConsumo` |
| Ver resumen de cuenta / Confirmar check-out | `POST /api/habitaciones/{id}/checkout` | `OperacionController.checkout` | `OperacionService.checkout` |
| Marcar lista (limpieza) | `POST /api/habitaciones/{id}/habilitar` | `OperacionController.habilitar` | `OperacionService.habilitar` |
| Ver factura | `GET /api/reservas/{id}/factura` | `ReservaController.factura` | `FacturacionService.consultar` |
| Descargar PDF | `GET /api/reservas/{id}/factura/pdf` | `ReservaController.facturaPdf` | `FacturacionService.pdf` |

## Administrador

| Botón en la web | Endpoint | Controlador (método) | Servicio |
|---|---|---|---|
| Sedes → Crear sede | `POST /api/admin/sedes` | `AdminController.crearSede` | `AdministracionService.crearSede` |
| Sedes → Editar → Guardar | `PUT /api/admin/sedes/{id}` | `AdminController.actualizarSede` | `AdministracionService.actualizarSede` |
| Sedes → Inactivar / Activar | `POST /api/admin/sedes/{id}/estado` | `AdminController.activarSede` | `AdministracionService.cambiarActivaSede` |
| Selector Sede activa | `PUT /api/admin/sede-activa` | `AdminController.cambiarSedeActiva` | `AdministracionService.cambiarSedeActiva` |
| Usuarios → Crear recepcionista | `POST /api/admin/usuarios` | `AdminController.crearRecepcionista` | `AdministracionService.crearRecepcionista` |
| Usuarios → Desbloquear | `POST /api/admin/usuarios/{id}/desbloquear` | `AdminController.desbloquear` | `AuthService.desbloquear` |
| Usuarios → Desactivar | `DELETE /api/admin/usuarios/{id}` | `AdminController.inactivarUsuario` | `AdministracionService.inactivarUsuario` |
| Habitaciones → Crear habitación | `POST /api/admin/habitaciones` | `AdminController.crearHabitacion` | `AdministracionService.crearHabitacion` |
| Habitaciones → Cambiar a | `POST /api/admin/habitaciones/{id}/estado` | `AdminController.cambiarEstado` | `AdministracionService.cambiarEstadoHabitacion` |
| Catálogo → Crear ítem | `POST /api/admin/catalogo` | `AdminController.crearItem` | `AdministracionService.crearItem` |
| Reportes → Generar | `GET /api/admin/reportes/ocupacion` e `ingresos` | `AdminController.ocupacion` / `ingresos` | `ReporteService.ocupacion` / `ingresos` |

## Autenticación

| Botón en la web | Endpoint | Controlador (método) | Servicio |
|---|---|---|---|
| Acceso personal → Entrar | `POST /api/auth/login` | `AuthController.login` | `AuthService.login` |
| Contraseña → Guardar | `POST /api/auth/cambiar-password` | `AuthController.cambiarPassword` | `AuthService.cambiarPassword` |

## Reglas de negocio (dónde están)

| Regla | Clase |
|---|---|
| Bloqueo de habitación 15 min (findAndModify) | `ReservaService.crear` → `HabitacionRepository.bloquearSiDisponible` |
| Liberar bloqueos vencidos | `JobLiberacionBloqueo` → `ReservaService.liberarBloqueosExpirados` |
| Política de reembolso (48 h) | `aplicacion/reembolso/` (Strategy) |
| Pasarela PSE | `infraestructura/pse/PseGatewayAdapter` (Adapter) |
| Recargo por salida tardía (13:00) | `OperacionService.checkout` (hora límite en `HotelProperties`) |
| IVA 19 % y consumo 8 % | `FacturacionService.cerrarReserva` |
| Bloqueo de cuenta tras 3 intentos | `Usuario.autenticar` |
| Aislamiento por sede | `AuthPrincipal.puedeAccederASede`, usado en `OperacionService` |
