# Vista Lógica — Nexus Hotel PMS

> **Propósito:** Definir la estructura estática del dominio (entidades, relaciones, agregados) y las máquinas de estado clave, base para el modelo de datos y diseño de APIs.

---

## Modelo de Dominio (Entidades Principales)

### Entidades Core

| Entidad | Atributos Clave | Relaciones |
|---------|-----------------|------------|
| **Sede** | id, nombre, direccion, activa | 1:N Habitación, Usuario, Reserva, Consumo, CatálogoItem |
| **Usuario** | id, nombre, email, password_hash, rol (Admin/Recepcionista), sede_id, activo | N:1 Sede; 1:N Reserva (creadas), Consumo (registrados) |
| **Habitacion** | id, numero, tipo, capacidad_max, precio_base, estado, sede_id | N:1 Sede; 1:N Reserva; 1:N Consumo (cuenta) |
| **Reserva** | id, codigo, cliente_nombre, cliente_documento, cliente_email, fecha_entrada, fecha_salida, num_huespedes, estado, sede_id, habitacion_id, valor_total, created_at, checkin_at, checkout_at | N:1 Sede, Habitacion, Usuario (creador); 1:N Consumo, Pago, Factura |
| **Consumo** | id, reserva_id, habitacion_id, catalogo_item_id, cantidad, precio_unitario, subtotal, registrado_por, registrado_at, anulado, anulado_por, anulado_at, motivo_anulacion | N:1 Reserva, Habitacion, CatalogoItem, Usuario |
| **CatalogoItem** | id, nombre, precio, categoria (Restaurante/Minibar), activo, sede_id | N:1 Sede; 1:N Consumo |
| **Pago** | id, reserva_id, monto, metodo (PSE), estado (Pendiente/Aprobado/Rechazado), transaccion_id, webhook_recibido_at, created_at | N:1 Reserva |
| **Factura** | id, reserva_id, numero_factura, pdf_url, subtotal_estadia, subtotal_consumos, impuestos_detalle (JSON), total, generada_at | 1:1 Reserva

---

## Value Objects / Enums

`	ypescript
// Estados de habitación (HU-01, HU-02, HU-08, HU-10, HU-11, HU-15)
enum EstadoHabitacion {
  Disponible,
  Bloqueada,      // 15 min tras registro reserva (HU-02)
  Reservada,      // Pago aprobado, pre check-in (HU-03)
  Ocupada,        // Check-in realizado (HU-08)
  EnLimpieza,     // Post check-out automático (HU-11)
  Mantenimiento   // Admin (HU-15)
}

// Estados de reserva (HU-02, HU-03, HU-04, HU-08, HU-10, HU-12)
enum EstadoReserva {
  Pendiente,              // Iniciada, sin bloqueo aún
  Bloqueada,              // 15 min timer activo (HU-02)
  Pagada,                 // PSE aprobado (HU-03)
  EnCurso,                // Check-in done (HU-08)
  PendienteDeCierre,      // Check-out done, factura pendiente (HU-10)
  Cerrada,                // Factura generada, inmutable (HU-12)
  Cancelada,              // Cliente cancela >48h (HU-04)
  CanceladaConPenalidad,  // Cliente cancela <48h (HU-04)
  CanceladaPorTiempo,     // Expiración 15 min (HU-02)
  NoShow                  // No llegó 23:59 día entrada (HU-08)
}

// Categorías catálogo (HU-16)
enum CategoriaConsumo {
  Restaurante,
  Minibar
}

// Roles usuario (HU-06, HU-14)
enum RolUsuario {
  Administrador,   // Acceso global, multi-sede
  Recepcionista    // Acceso solo a su sede asignada
}
`

---

## Agregados y Consistencia

| Agregado Raíz | Entidades Internas | Invariantes de Consistencia |
|---------------|-------------------|----------------------------|
| **Reserva** | Consumo, Pago, Factura | - Solo habitaciones *Disponibles* pueden reservarse<br>- Bloqueo 15 min expira → *CanceladaPorTiempo*<br>- Check-in solo si *Pagada*<br>- Consumos solo si habitación *Ocupada*<br>- Factura solo si *PendienteDeCierre* → *Cerrada* inmutable |
| **Habitacion** | — | - Transiciones de estado válidas únicamente (ver máquina de estado)<br>- *Mantenimiento* excluye de disponibilidad |
| **Sede** | Usuario, Habitacion, CatalogoItem | - Aislamiento estricto: queries filtran por sede_id<br>- Recepcionista vinculado a 1 sede |
| **CatalogoItem** | — | - Inactivar no borra histórico (Consumo conserva precio original) |

---

## Máquinas de Estado Detalladas

### EstadoHabitacion (Diagrama de Transiciones)

`mermaid
stateDiagram-v2
    [*] --> Disponible
    Disponible --> Bloqueada: HU-02 Registrar reserva
    Disponible --> Mantenimiento: HU-15 Admin
    Bloqueada --> Disponible: HU-02 Expiración 15 min\nHU-03 Pago rechazado
    Bloqueada --> Reservada: HU-03 Pago aprobado (webhook)
    Reservada --> Ocupada: HU-08 Check-in
    Reservada --> Disponible: HU-04 Cancelación (>48h o <48h)
    Ocupada --> EnLimpieza: HU-10/HU-11 Check-out confirmado
    EnLimpieza --> Disponible: HU-11 Recepcionista habilita
    Ocupada --> Mantenimiento: HU-15 Admin (con advertencia reservas futuras)
    Mantenimiento --> Disponible: HU-15 Admin restaura
`

### EstadoReserva (Diagrama de Transiciones)

`mermaid
stateDiagram-v2
    [*] --> Pendiente
    Pendiente --> Bloqueada: HU-02 Registrar reserva (datos completos)
    Bloqueada --> CanceladaPorTiempo: HU-02 Timer 15 min expira
    Bloqueada --> Pagada: HU-03 PSE Aprobado (sync o webhook)
    Bloqueada --> Cancelada: HU-03 PSE Rechazado
    Bloqueada --> PendienteConfirmacion: HU-03 PSE Pendiente (espera webhook)
    PendienteConfirmacion --> Pagada: HU-03 Webhook Aprobado
    PendienteConfirmacion --> Cancelada: HU-03 Webhook Rechazado
    Pagada --> Cancelada: HU-04 Cliente cancela >48h
    Pagada --> CanceladaConPenalidad: HU-04 Cliente cancela <48h
    Pagada --> NoShow: HU-08 23:59 sin check-in
    Pagada --> EnCurso: HU-08 Check-in exitoso (timestamp servidor)
    EnCurso --> PendienteDeCierre: HU-10 Check-out confirmado (timestamp servidor)
    PendienteDeCierre --> Cerrada: HU-12 Factura generada + PDF
    Cerrada --> [*]: Inmutable, fin de ciclo
`

---

## Reglas de Navegación y Acceso (Multitenancy)

| Contexto | Filtro Obligatorio |
|----------|-------------------|
| Consulta disponibilidad (HU-01) | habitacion.sede_id = sede_seleccionada AND estado = Disponible |
| Búsqueda reserva recepcionista (HU-07) | eserva.sede_id = usuario.sede_id |
| Listado habitaciones admin (HU-15) | habitacion.sede_id = sede_activa_admin |
| Catálogo consumos (HU-09, HU-16) | item.sede_id = sede_activa AND activo = true |
| Reportes (HU-17) | eserva.sede_id = sede_seleccionada AND fecha IN rango |
| API directa / URL (HU-13) | Validar esource.sede_id == session.sede_id o admin global |
---

## APIs Sugeridas (Endpoints REST)

| Módulo | Endpoint | Método | Descripción | HU |
|--------|----------|--------|-------------|-----|
| Disponibilidad | /api/sedes/{sedeId}/disponibilidad | GET | Query: fecha_entrada, fecha_salida, huespedes | HU-01 |
| Reservas | /api/reservas | POST | Registrar reserva + bloqueo | HU-02 |
| Reservas | /api/reservas/{id} | GET | Detalle reserva | HU-07 |
| Reservas | /api/reservas/buscar | GET | Query: cedula OR codigo, filtrado por sede sesión | HU-07 |
| Pagos | /api/reservas/{id}/pago | POST | Inicia pago PSE | HU-03 |
| Pagos | /api/webhooks/pse | POST | Webhook PSE (async) | HU-03 |
| Cancelación | /api/reservas/{id}/cancelar | POST | Body: motivo (opcional) | HU-04 |
| Comprobante | /api/reservas/{id}/comprobante | GET | Descarga PDF + QR | HU-05 |
| Auth | /api/auth/login | POST | Login + JWT/sesión | HU-06 |
| Check-in | /api/reservas/{id}/checkin | POST | Body: qr_code (opcional) | HU-08 |
| No-Show | /api/reservas/{id}/no-show | POST | Solo tras 23:59 | HU-08 |
| Consumos | /api/habitaciones/{id}/consumos | POST/GET | Registrar/listar | HU-09 |
| Consumos | /api/consumos/{id}/anular | POST | Body: motivo obligatorio | HU-09 |
| Check-out | /api/habitaciones/{id}/checkout | POST | Calcula late fee auto | HU-10 |
| Limpieza | /api/habitaciones/{id}/habilitar | POST | EnLimpieza → Disponible | HU-11 |
| Factura | /api/reservas/{id}/factura | GET | Descarga PDF final | HU-12 |
| Sedes | /api/sedes | GET/POST/PUT | CRUD sedes (admin global) | HU-13 |
| Sedes | /api/sedes/{id}/activar | POST | Cambia sede activa (admin global) | HU-13 |
| Usuarios | /api/usuarios | GET/POST/PUT/DELETE | CRUD + asignación sede | HU-14 |
| Habitaciones | /api/habitaciones | GET/POST/PUT | CRUD + cambio estado | HU-15 |
| Catálogo | /api/catalogo | GET/POST/PUT | CRUD + activar/inactivar | HU-16 |
| Reportes | /api/reportes/ocupacion | GET | Query: sede_id, fecha_inicio, fecha_fin | HU-17 |
| Reportes | /api/reportes/ingresos | GET | Query: sede_id, fecha_inicio, fecha_fin | HU-17 |

---

## Eventos de Dominio (para Event Sourcing / Outbox Pattern)

| Evento | Disparador | Payload Clave | Consumidores |
|--------|------------|---------------|--------------|
| ReservaRegistrada | HU-02 | reserva_id, habitacion_id, expires_at | Notificaciones, Timer liberación |
| PagoProcesado | HU-03 (sync/webhook) | reserva_id, estado, transaccion_id | Reserva, Comprobante, Habitación |
| ComprobanteGenerado | HU-05 | reserva_id, pdf_url, qr_code | Email service |
| CheckInRealizado | HU-08 | reserva_id, habitacion_id, timestamp | Habitación (→ Ocupada), Métricas |
| NoShowRegistrado | HU-08 | reserva_id, habitacion_id | Habitación (→ Disponible/EnLimpieza), Métricas |
| ConsumoRegistrado | HU-09 | consumo_id, reserva_id, subtotal | Cuenta habitación, Factura futura |
| ConsumoAnulado | HU-09 | consumo_id, motivo, usuario | Auditoría, Cuenta habitación |
| CheckOutRealizado | HU-10 | reserva_id, habitacion_id, late_fee, timestamp | Habitación (→ EnLimpieza), Factura |
| HabitacionHabilitada | HU-11 | habitacion_id, usuario | Disponibilidad |
| FacturaGenerada | HU-12 | factura_id, reserva_id, pdf_url, total | Email service, Contabilidad |
| ReporteSolicitado | HU-17 | tipo, sede_id, rango, usuario | Async report worker |

---

## Pendientes de Diseño

- Estrategia de particionado de tablas por sede_id (row-level security vs schema por sede)
- Manejo de concurrencia optimista (version / updated_at) en Reserva y Habitacion
- Índices compuestos para consultas de disponibilidad por rango de fechas
- Outbox table para garantía de entrega de eventos a notificaciones/webhooks
- Migración de estados legacy si hay datos previos
