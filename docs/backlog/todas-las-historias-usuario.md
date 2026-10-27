# Todas las Historias de Usuario (HU-01 a HU-17)

Este archivo consolida las 17 historias de usuario del backlog en un único documento Markdown.

---

## HU-01: Consulta de disponibilidad de habitaciones

**Como** cliente  
**Quiero** consultar las habitaciones disponibles en una sede para un rango de fechas y número de huéspedes  
**Para** verificar qué opciones existen antes de realizar una reserva

---

### Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | No depende de ninguna otra historia para entregarse; el cliente puede consultar sin necesidad de iniciar sesión ni reservar. |
| **N — Negotiable** | ✅ | No impone tecnología de búsqueda ni interfaz específica; el equipo puede decidir el mecanismo de filtrado. |
| **V — Valuable** | ✅ | Permite al cliente tomar decisiones informadas antes de comprometerse a una compra. |
| **E — Estimable** | ✅ | Las reglas de negocio (sede obligatoria, fechas lógicas, filtro por capacidad) están claramente definidas. |
| **S — Small** | ✅ | Se limita a la consulta; la reserva es una historia separada. |
| **T — Testable** | ✅ | Los filtros y resultados esperados son verificables objetivamente. |

---

### Criterios de aceptación (Gherkin)

```gherkin
# language: es
Característica: Consulta de disponibilidad de habitaciones

  Antecedentes:
    Dado que el sistema tiene registradas múltiples sedes con habitaciones en distintos estados

  Escenario: Consulta exitosa con resultados disponibles
    Dado que el cliente selecciona la sede "Sede Norte"
    Y ingresa la fecha de entrada "2026-10-01" y la fecha de salida "2026-10-05"
    Y especifica 2 huéspedes
    Cuando solicita la consulta de disponibilidad
    Entonces el sistema muestra únicamente las habitaciones con estado "Disponible" de la "Sede Norte"
    Y excluye habitaciones cuya capacidad máxima sea menor a 2 huéspedes
    Y muestra el tipo, la capacidad y el precio base de cada habitación disponible

  Escenario: Sin resultados por falta de capacidad suficiente
    Dado que el cliente selecciona la sede "Sede Sur"
    Y ingresa fechas válidas
    Y especifica 3 huéspedes
    Cuando solicita la consulta de disponibilidad
    Entonces el sistema no muestra habitaciones de tipo "Simple" ni otras con capacidad menor a 3
    Y si no hay habitaciones con capacidad suficiente, muestra el mensaje "No hay habitaciones disponibles para los criterios seleccionados"

  Escenario: Intento de consulta sin seleccionar sede
    Dado que el cliente no ha seleccionado ninguna sede
    Cuando intenta solicitar la consulta de disponibilidad
    Entonces el sistema muestra el mensaje de error "Debe seleccionar una sede para continuar"
    Y no ejecuta la búsqueda

  Escenario: Fecha de salida anterior o igual a la fecha de entrada
    Dado que el cliente selecciona la sede "Sede Norte"
    Y ingresa la fecha de entrada "2026-10-05" y la fecha de salida "2026-10-03"
    Cuando intenta solicitar la consulta de disponibilidad
    Entonces el sistema muestra el mensaje de error "La fecha de salida debe ser posterior a la fecha de entrada"
    Y no ejecuta la búsqueda

  Escenario: Fecha de entrada en el pasado
    Dado que el cliente ingresa la fecha de entrada "2024-01-01"
    Cuando intenta solicitar la consulta de disponibilidad
    Entonces el sistema muestra el mensaje de error "La fecha de entrada no puede ser anterior a la fecha actual"
    Y no ejecuta la búsqueda

  Escenario: Habitaciones en estado distinto a Disponible son excluidas
    Dado que la sede "Sede Norte" tiene habitaciones en estado "Mantenimiento", "En Limpieza" y "Ocupada"
    Cuando el cliente consulta disponibilidad para esa sede con fechas y capacidad válidas
    Entonces el sistema solo muestra habitaciones con estado "Disponible"
    Y excluye las habitaciones en estado "Mantenimiento", "En Limpieza" y "Ocupada"
```

---

### Supuestos / preguntas abiertas

- Se asume que la consulta no requiere autenticación del cliente.
- Se asume que el número de huéspedes mínimo es 1 y que el sistema valida que sea un valor positivo entero; el límite máximo no fue especificado.
- No se especificó si las habitaciones bloqueadas temporalmente (en proceso de reserva) deben o no mostrarse; se asume que se excluyen de la consulta.

---

## HU-02: Registro de reserva con bloqueo de concurrencia

**Como** cliente  
**Quiero** registrar una reserva capturando mis datos y los de los huéspedes para asegurar la habitación seleccionada  
**Para** garantizar que la habitación quede reservada a mi nombre mientras completo el pago

---

### Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | Puede desarrollarse independientemente de la historia de pago; el bloqueo temporal es parte del registro. |
| **N — Negotiable** | ✅ | No especifica la interfaz de captura de datos ni el mecanismo interno de bloqueo; el equipo decide la implementación. |
| **V — Valuable** | ✅ | Protege al cliente de perder la habitación mientras gestiona el pago y evita doble reserva. |
| **E — Estimable** | ✅ | Reglas de tiempo de bloqueo (15 min) y liberación automática están claramente definidas. |
| **S — Small** | ✅ | Se limita al registro y bloqueo; el procesamiento del pago es una historia separada. |
| **T — Testable** | ✅ | El ID generado, el bloqueo y la liberación automática son verificables objetivamente. |

---

### Criterios de aceptación (Gherkin)

```gherkin
# language: es
Característica: Registro de reserva con bloqueo de concurrencia

  Escenario: Registro exitoso de reserva y bloqueo de habitación
    Dado que el cliente ha seleccionado una habitación disponible de la "Sede Norte"
    Y ha ingresado todos los datos requeridos del huésped principal
    Cuando confirma el registro de la reserva
    Entonces el sistema genera un ID de reserva único
    Y cambia el estado de la habitación a "Bloqueada"
    Y el sistema inicia un temporizador de 15 minutos para completar el pago
    Y redirige al cliente al flujo de pago

  Escenario: Liberación automática por tiempo de pago expirado
    Dado que el cliente tiene una reserva con habitación bloqueada
    Y han transcurrido 15 minutos sin que se complete el pago
    Cuando el temporizador expira
    Entonces el sistema libera la habitación y cambia su estado a "Disponible"
    Y cambia el estado de la reserva a "Cancelada por tiempo expirado"
    Y notifica al cliente que la reserva fue liberada por inactividad

  Escenario: Intento de registrar una reserva sobre habitación ya bloqueada
    Dado que la habitación "101" de la "Sede Norte" tiene estado "Bloqueada"
    Cuando otro cliente intenta registrar una reserva sobre la misma habitación
    Entonces el sistema muestra el mensaje "La habitación ya no está disponible, por favor seleccione otra"
    Y no crea una nueva reserva

  Escenario: Intento de registro con datos obligatorios incompletos
    Dado que el cliente ha seleccionado una habitación disponible
    Y no ha ingresado todos los datos requeridos del huésped
    Cuando intenta confirmar el registro de la reserva
    Entonces el sistema muestra un mensaje indicando los campos obligatorios faltantes
    Y no genera ninguna reserva ni bloquea la habitación
```

---

### Supuestos / preguntas abiertas

- Se asume que los datos mínimos del huésped son: nombre completo, documento de identidad y correo electrónico. El conjunto exacto de campos no fue especificado.
- Se asume que el cliente recibe notificación (correo o pantalla) cuando la reserva es liberada por expiración.
- No se especificó si el cliente puede reintentar el registro tras la expiración; se asume que sí, eligiendo nuevamente la habitación.

---

## HU-03: Procesamiento de pago seguro mediante PSE

**Como** cliente  
**Quiero** pagar mi reserva a través de la pasarela de pagos PSE  
**Para** confirmar y asegurar mi reserva de forma segura sin manejar efectivo

---

### Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | Depende lógicamente de que exista una reserva, pero como historia es desarrollable independientemente con datos de prueba. |
| **N — Negotiable** | ✅ | No impone detalles técnicos de integración con PSE; el equipo decide el mecanismo de webhook. |
| **V — Valuable** | ✅ | Convierte la intención de compra en un pago real y cambia el estado de la reserva a pagada. |
| **E — Estimable** | ✅ | Los tres estados de respuesta de PSE y sus consecuencias están claramente definidos. |
| **S — Small** | ✅ | Se limita al ciclo de pago; el registro y el comprobante son historias separadas. |
| **T — Testable** | ✅ | Cada respuesta de PSE (Aprobado, Rechazado, Pendiente) tiene un resultado esperado verificable. |

---

### Criterios de aceptación (Gherkin)

```gherkin
# language: es
Característica: Procesamiento de pago seguro mediante PSE

  Escenario: Pago aprobado por PSE
    Dado que el cliente tiene una reserva en estado "Bloqueada" pendiente de pago
    Y inicia el proceso de pago a través de PSE
    Cuando PSE responde con el estado "Aprobado"
    Entonces el sistema actualiza el estado de la reserva a "Pagada"
    Y libera el bloqueo temporal convirtiendo la habitación a estado "Reservada"
    Y notifica al cliente que el pago fue procesado exitosamente

  Escenario: Pago rechazado por PSE
    Dado que el cliente tiene una reserva en estado "Bloqueada" pendiente de pago
    Y inicia el proceso de pago a través de PSE
    Cuando PSE responde con el estado "Rechazado"
    Entonces el sistema anula la reserva
    Y libera la habitación cambiando su estado a "Disponible"
    Y notifica al cliente que el pago fue rechazado y la reserva cancelada

  Escenario: Pago pendiente en espera de webhook de PSE
    Dado que el cliente tiene una reserva en estado "Bloqueada" pendiente de pago
    Y inicia el proceso de pago a través de PSE
    Cuando PSE responde con el estado "Pendiente"
    Entonces el sistema mantiene la habitación en estado "Bloqueada"
    Y mantiene la reserva en estado "Pendiente de confirmación"
    Y espera el webhook de confirmación de PSE para actualizar el estado final

  Escenario: Recepción del webhook de confirmación aprobado
    Dado que existe una reserva en estado "Pendiente de confirmación"
    Y la habitación permanece bloqueada
    Cuando el sistema recibe el webhook de PSE con resultado "Aprobado"
    Entonces el sistema actualiza el estado de la reserva a "Pagada"
    Y actualiza el estado de la habitación a "Reservada"
    Y notifica al cliente la confirmación del pago

  Escenario: Recepción del webhook de confirmación rechazado
    Dado que existe una reserva en estado "Pendiente de confirmación"
    Y la habitación permanece bloqueada
    Cuando el sistema recibe el webhook de PSE con resultado "Rechazado"
    Entonces el sistema anula la reserva
    Y libera la habitación cambiando su estado a "Disponible"
    Y notifica al cliente que el pago fue rechazado y la reserva cancelada
```

---

### Supuestos / preguntas abiertas

- Se asume que mientras el estado sea "Pendiente", el temporizador de 15 minutos del bloqueo se suspende o extiende, ya que la regla de negocio indica que el sistema mantiene la habitación bloqueada hasta recibir el webhook. Si el temporizador sigue activo en paralelo, deberá confirmarse el comportamiento.
- No se especificó el canal de notificación al cliente (correo, SMS, pantalla); se asume notificación en pantalla y correo electrónico.
- No se especificó un tiempo máximo de espera del webhook antes de liberar la habitación; queda como pregunta abierta.

---

## HU-04: Cancelación de reserva y aplicación de política de reembolso

**Como** cliente  
**Quiero** cancelar mi reserva antes del check-in  
**Para** recuperar mi dinero total o parcialmente según las políticas de cancelación vigentes

---

### Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | La lógica de cancelación y reembolso puede desarrollarse de forma autónoma. |
| **N — Negotiable** | ✅ | No impone el canal de reembolso ni los plazos bancarios; el equipo define la implementación. |
| **V — Valuable** | ✅ | Otorga transparencia y confianza al cliente sobre las condiciones de cancelación. |
| **E — Estimable** | ✅ | Las dos ramas de reembolso (>48 h y <48 h) y la liberación de habitación están completamente definidas. |
| **S — Small** | ✅ | Solo cubre el flujo de cancelación; el procesamiento del pago original es una historia separada. |
| **T — Testable** | ✅ | El umbral de 48 horas, la penalidad de una noche y la liberación inmediata son verificables. |

---

### Criterios de aceptación (Gherkin)

```gherkin
# language: es
Característica: Cancelación de reserva y política de reembolso

  Escenario: Cancelación con más de 48 horas de anticipación — reembolso completo
    Dado que el cliente tiene una reserva en estado "Pagada"
    Y faltan más de 48 horas para la fecha de check-in
    Cuando el cliente solicita la cancelación de la reserva
    Entonces el sistema procesa el reembolso del valor total pagado
    Y cambia el estado de la reserva a "Cancelada"
    Y libera la habitación cambiando su estado a "Disponible" de inmediato
    Y notifica al cliente que el reembolso completo fue procesado

  Escenario: Cancelación con menos de 48 horas de anticipación — penalidad de una noche
    Dado que el cliente tiene una reserva en estado "Pagada"
    Y faltan menos de 48 horas para la fecha de check-in
    Cuando el cliente solicita la cancelación de la reserva
    Entonces el sistema retiene el valor equivalente a una noche como penalidad
    Y procesa el reembolso del valor restante (total pagado menos una noche)
    Y cambia el estado de la reserva a "Cancelada con penalidad"
    Y libera la habitación cambiando su estado a "Disponible" de inmediato
    Y notifica al cliente el valor retenido y el valor a reembolsar

  Escenario: Cancelación exactamente a las 48 horas del check-in
    Dado que el cliente tiene una reserva en estado "Pagada"
    Y faltan exactamente 48 horas para la fecha de check-in
    Cuando el cliente solicita la cancelación de la reserva
    Entonces el sistema aplica la política de reembolso completo
    Y libera la habitación de inmediato

  Escenario: Intento de cancelación de una reserva no pagada
    Dado que el cliente tiene una reserva en estado distinto a "Pagada"
    Cuando intenta cancelarla
    Entonces el sistema muestra el mensaje "Solo se pueden cancelar reservas en estado Pagada"
    Y no realiza ningún cambio de estado ni reembolso

  Escenario: Liberación inmediata de la habitación tras cancelación
    Dado que el cliente cancela una reserva con habitación en estado "Reservada"
    Cuando el sistema procesa la cancelación (con o sin penalidad)
    Entonces la habitación cambia a estado "Disponible" en el mismo instante del procesamiento
    Y queda visible en consultas de disponibilidad a partir de ese momento
```

---

### Supuestos / preguntas abiertas

- Se asume que "valor de una noche" equivale al precio base de la habitación por una noche, sin incluir consumos adicionales.
- No se especificó el canal ni el plazo del reembolso bancario (ej. 3–5 días hábiles); se asume que el sistema registra la solicitud de reembolso y el procesamiento bancario ocurre en segundo plano.
- No se especificó si el cliente puede cancelar reservas que ya pasaron el check-in; se asume que esta historia solo aplica antes del check-in.

---

## HU-05: Emisión automática de comprobante de reserva con código QR

**Como** sistema  
**Quiero** generar y entregar automáticamente un comprobante en PDF con código QR tras un pago exitoso  
**Para** que el cliente tenga un soporte formal de su reserva que agilice el check-in en el mostrador

---

### Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | El proceso de generación del PDF puede desarrollarse independientemente como componente de salida. |
| **N — Negotiable** | ✅ | No impone la biblioteca de generación de PDF ni el diseño visual del comprobante. |
| **V — Valuable** | ✅ | Reduce el tiempo de check-in y provee al cliente un soporte formal descargable. |
| **E — Estimable** | ✅ | El disparador (pago exitoso) y el contenido obligatorio (QR + datos de reserva) están definidos. |
| **S — Small** | ✅ | Solo cubre la generación y entrega del PDF; el pago y el check-in son historias separadas. |
| **T — Testable** | ✅ | Se puede verificar que el PDF se genera, que contiene el QR y que se entrega al cliente. |

---

### Criterios de aceptación (Gherkin)

```gherkin
# language: es
Característica: Emisión automática de comprobante de reserva con código QR

  Escenario: Generación automática del PDF tras pago aprobado
    Dado que el sistema ha recibido confirmación de pago aprobado para la reserva "RSV-20261001-001"
    Cuando el sistema procesa el evento de pago exitoso
    Entonces el sistema genera automáticamente un PDF de comprobante de reserva
    Y el PDF incluye el código QR asociado al ID de reserva
    Y el PDF incluye los datos de la reserva: nombre del huésped, sede, habitación, fechas y valor total pagado
    Y el sistema pone el PDF a disposición del cliente para descarga o envío

  Escenario: El comprobante se entrega al cliente tras la generación
    Dado que el sistema ha generado el PDF de comprobante de la reserva
    Cuando el proceso de entrega se ejecuta
    Entonces el cliente recibe el comprobante por el canal configurado
    Y el comprobante es accesible de inmediato sin necesidad de solicitarlo manualmente

  Escenario: El código QR del comprobante es válido y escaneable en el mostrador
    Dado que el recepcionista en el mostrador escanea el código QR del comprobante del huésped
    Cuando el sistema decodifica el QR
    Entonces el sistema muestra los datos de la reserva correspondiente al ID codificado en el QR
    Y permite iniciar el proceso de check-in directamente

  Escenario: No se genera comprobante si el pago no fue aprobado
    Dado que el pago de una reserva fue rechazado o está pendiente
    Cuando el sistema procesa el resultado del pago
    Entonces el sistema no genera ningún PDF de comprobante
    Y no entrega ningún documento al cliente
```

---

### Supuestos / preguntas abiertas

- Se asume que el canal de entrega del comprobante es el correo electrónico registrado por el cliente al hacer la reserva; no se especificó si también se ofrece descarga directa desde la plataforma.
- No se definió el contenido exacto y la diagramación visual del PDF; se asume que incluye como mínimo: ID de reserva, nombre del huésped, sede, número de habitación, fechas, valor total y código QR.
- No se especificó si el QR debe expirar o si es válido mientras la reserva esté activa; se asume que es válido hasta el check-out.

---

## HU-06: Autenticación y control de acceso por rol

**Como** recepcionista o administrador  
**Quiero** ingresar al sistema con mis credenciales para acceder a las funciones asignadas a mi rol  
**Para** operar el sistema de manera segura con los permisos correspondientes a mi cargo

---

### Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | La autenticación es la puerta de entrada al sistema y puede desarrollarse independientemente. |
| **N — Negotiable** | ✅ | No impone tecnología de autenticación (JWT, sesiones, etc.); el equipo decide la implementación. |
| **V — Valuable** | ✅ | Garantiza la seguridad del sistema y evita accesos no autorizados. |
| **E — Estimable** | ✅ | Las reglas de bloqueo (3 intentos) y expiración de sesión (15 min) están claramente definidas. |
| **S — Small** | ✅ | Cubre solo autenticación y control de sesión; la gestión de usuarios es una historia separada. |
| **T — Testable** | ✅ | El bloqueo, la expiración y el redireccionamiento por rol son verificables objetivamente. |

---

### Criterios de aceptación (Gherkin)

```gherkin
# language: es
Característica: Autenticación y control de acceso por rol

  Escenario: Inicio de sesión exitoso como recepcionista
    Dado que el usuario "recepcionista@hotel.com" tiene rol "Recepcionista" y cuenta activa
    Cuando ingresa sus credenciales correctas en el formulario de login
    Entonces el sistema valida las credenciales
    Y redirige al usuario al panel del módulo de recepción correspondiente a su sede
    Y establece una sesión activa

  Escenario: Inicio de sesión exitoso como administrador
    Dado que el usuario "admin@hotel.com" tiene rol "Administrador" y cuenta activa
    Cuando ingresa sus credenciales correctas en el formulario de login
    Entonces el sistema valida las credenciales
    Y redirige al usuario al panel de administración con acceso a todas las sedes
    Y establece una sesión activa

  Escenario: Bloqueo temporal tras 3 intentos fallidos
    Dado que el usuario intenta iniciar sesión con credenciales incorrectas
    Cuando falla el intento de login por tercera vez consecutiva
    Entonces el sistema bloquea temporalmente la cuenta
    Y muestra el mensaje "Su cuenta ha sido bloqueada temporalmente por múltiples intentos fallidos"
    Y no permite nuevos intentos de login hasta que el bloqueo sea levantado

  Escenario: Intento de login con cuenta bloqueada
    Dado que la cuenta del usuario está bloqueada temporalmente
    Cuando intenta iniciar sesión con credenciales correctas
    Entonces el sistema muestra el mensaje "Su cuenta está bloqueada. Contacte al administrador"
    Y no permite el ingreso

  Escenario: Expiración automática de sesión por inactividad
    Dado que el usuario tiene una sesión activa
    Y han transcurrido 15 minutos sin ninguna interacción con el sistema
    Cuando el sistema detecta la inactividad
    Entonces el sistema cierra la sesión automáticamente
    Y redirige al usuario a la pantalla de login
    Y muestra el mensaje "Su sesión ha expirado por inactividad"

  Escenario: Acceso denegado a módulo fuera del rol asignado
    Dado que el usuario con rol "Recepcionista" tiene sesión activa
    Cuando intenta acceder a una funcionalidad exclusiva del rol "Administrador"
    Entonces el sistema muestra el mensaje "No tiene permisos para acceder a esta sección"
    Y no muestra el contenido restringido
```

---

### Supuestos / preguntas abiertas

- Se asume que el bloqueo temporal dura hasta que el administrador lo desbloquee manualmente; no se especificó un tiempo automático de desbloqueo.
- No se especificó si el contador de intentos se reinicia tras un login exitoso o tras cierto tiempo; se asume que se reinicia tras un inicio de sesión exitoso.
- No se especificó si existe un rol de "Housekeeping" u otros; se trabajó únicamente con los roles mencionados (Recepcionista, Administrador).

---

## HU-07: Búsqueda y localización de reservas en el mostrador

**Como** recepcionista  
**Quiero** buscar la reserva de un huésped utilizando su cédula o ID de reserva  
**Para** localizar rápidamente la información necesaria durante la atención en el mostrador

---

### Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | La búsqueda de reservas puede desarrollarse independientemente del check-in; son operaciones distintas. |
| **N — Negotiable** | ✅ | No impone el motor de búsqueda ni el diseño de la pantalla de resultados. |
| **V — Valuable** | ✅ | Reduce el tiempo de atención en el mostrador y mejora la experiencia del huésped. |
| **E — Estimable** | ✅ | Los dos criterios de búsqueda (cédula, ID reserva) y el filtro por sede están claramente definidos. |
| **S — Small** | ✅ | Solo cubre la búsqueda; el check-in y la gestión de la reserva son historias separadas. |
| **T — Testable** | ✅ | Los resultados esperados y el aislamiento por sede son verificables. |

---

### Criterios de aceptación (Gherkin)

```gherkin
# language: es
Característica: Búsqueda y localización de reservas por recepcionista

  Antecedentes:
    Dado que el recepcionista tiene sesión activa en la "Sede Norte"

  Escenario: Búsqueda exitosa por cédula del huésped
    Dado que existe una reserva asociada al huésped con cédula "1098765432" en la "Sede Norte"
    Cuando el recepcionista ingresa la cédula "1098765432" en el buscador
    Entonces el sistema muestra la(s) reserva(s) activa(s) del huésped en la "Sede Norte"
    Y presenta los datos de la reserva: ID, habitación, fechas, estado y nombre del huésped

  Escenario: Búsqueda exitosa por ID de reserva
    Dado que existe la reserva "RSV-20261001-001" en la "Sede Norte"
    Cuando el recepcionista ingresa el ID "RSV-20261001-001" en el buscador
    Entonces el sistema muestra la información completa de esa reserva
    Y presenta los datos del huésped, habitación, fechas y estado actual

  Escenario: Búsqueda sin resultados en la sede activa
    Dado que no existe ninguna reserva asociada a la cédula "9999999999" en la "Sede Norte"
    Cuando el recepcionista busca por esa cédula
    Entonces el sistema muestra el mensaje "No se encontraron reservas para los criterios ingresados en esta sede"

  Escenario: Aislamiento de datos por sede — no se muestran reservas de otras sedes
    Dado que existe una reserva del huésped con cédula "1098765432" en la "Sede Sur"
    Y el recepcionista tiene sesión activa en la "Sede Norte"
    Cuando busca por la cédula "1098765432"
    Entonces el sistema no muestra la reserva de la "Sede Sur"
    Y solo muestra reservas correspondientes a la "Sede Norte"

  Escenario: Intento de búsqueda con campo vacío
    Cuando el recepcionista intenta realizar la búsqueda sin ingresar ningún criterio
    Entonces el sistema muestra el mensaje "Ingrese una cédula o ID de reserva para realizar la búsqueda"
    Y no ejecuta ninguna consulta
```

---

### Supuestos / preguntas abiertas

- Se asume que la búsqueda puede retornar múltiples reservas si un huésped tiene más de una reserva activa en la misma sede.
- No se especificó si se pueden buscar reservas en estados históricos (canceladas, cerradas); se asume que la búsqueda incluye todos los estados para facilitar la consulta en mostrador.
- No se especificó si el recepcionista puede hacer búsqueda parcial (ej. los primeros dígitos de la cédula); se asume búsqueda exacta.

---

## HU-08: Gestión de check-in y registro de no-shows

**Como** recepcionista  
**Quiero** registrar el ingreso oficial de un huésped o marcarlo como no-show según su llegada  
**Para** controlar con exactitud la ocupación real del hotel y mantener las métricas íntegras

---

### Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | El check-in puede desarrollarse independientemente del check-out y del registro de consumos. |
| **N — Negotiable** | ✅ | No impone el mecanismo de captura de timestamp ni la interfaz de registro. |
| **V — Valuable** | ✅ | Formaliza el ingreso del huésped y protege las métricas de ocupación del hotel. |
| **E — Estimable** | ✅ | El timestamp inalterable del servidor y la condición de no-show (23:59) están definidos. |
| **S — Small** | ✅ | Cubre solo el check-in y el no-show; el check-out es una historia separada. |
| **T — Testable** | ✅ | El timestamp del servidor, el cambio de estado y el criterio de no-show (23:59) son verificables. |

---

### Criterios de aceptación (Gherkin)

```gherkin
# language: es
Característica: Gestión de check-in y registro de no-shows

  Antecedentes:
    Dado que el recepcionista tiene sesión activa en la sede correspondiente

  Escenario: Check-in exitoso del huésped
    Dado que existe la reserva "RSV-20261001-001" en estado "Pagada"
    Y el huésped se presenta en el mostrador en la fecha de entrada programada
    Cuando el recepcionista registra el check-in de la reserva
    Entonces el sistema captura el timestamp exacto del servidor como hora oficial de ingreso
    Y cambia el estado de la reserva a "En curso"
    Y cambia el estado de la habitación a "Ocupada"
    Y el timestamp registrado es inalterable y no puede ser modificado manualmente

  Escenario: Registro de no-show cuando el huésped no llega en el día de entrada
    Dado que existe la reserva "RSV-20261001-002" en estado "Pagada"
    Y han pasado las 23:59 del día de entrada sin que el huésped se haya presentado
    Cuando el recepcionista marca la reserva como "No-Show"
    Entonces el sistema cambia el estado de la reserva a "No-Show"
    Y la habitación queda disponible para su proceso de limpieza o reasignación
    Y el evento queda registrado en el sistema sin alterar las métricas de ocupación real

  Escenario: Intento de check-in de una reserva no pagada
    Dado que la reserva "RSV-20261001-003" tiene estado distinto a "Pagada"
    Cuando el recepcionista intenta registrar el check-in
    Entonces el sistema muestra el mensaje "No se puede realizar el check-in: la reserva no está en estado Pagada"
    Y no realiza ningún cambio de estado

  Escenario: Intento de marcar como no-show antes de las 23:59 del día de entrada
    Dado que existe una reserva en estado "Pagada" para el día de hoy
    Y aún no son las 23:59
    Cuando el recepcionista intenta marcar la reserva como "No-Show"
    Entonces el sistema muestra el mensaje "No es posible marcar como No-Show antes de las 23:59 del día de entrada"
    Y no cambia el estado de la reserva

  Escenario: Check-in mediante escaneo del código QR del comprobante
    Dado que el huésped presenta el código QR de su comprobante de reserva
    Y la reserva asociada está en estado "Pagada"
    Cuando el recepcionista escanea el QR
    Entonces el sistema carga automáticamente los datos de la reserva correspondiente
    Y el recepcionista puede proceder a registrar el check-in desde esa vista
```

---

### Supuestos / preguntas abiertas

- Se asume que "inalterable" significa que el campo de timestamp de check-in no tiene ningún control de edición en la interfaz ni en el backend una vez guardado.
- No se especificó si el no-show aplica políticas de reembolso o retención; se asume que la gestión económica del no-show está cubierta por la historia de cancelación (HU-04) o es una política operativa fuera del alcance de esta historia.
- No se especificó si el sistema envía una alerta automática al recepcionista a las 23:59 para recordar marcar los no-shows; se registra como supuesto menor a confirmar.

---

## HU-09: Registro y anulación de consumos durante la estadía

**Como** recepcionista  
**Quiero** registrar productos y servicios consumidos por el huésped y anular cargos incorrectos con trazabilidad  
**Para** mantener la cuenta de la habitación precisa y auditada durante toda la estadía

---

### Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | La gestión de consumos puede desarrollarse independientemente del check-out; son módulos distintos. |
| **N — Negotiable** | ✅ | No impone la interfaz de carga ni el catálogo de consumos; esos son definidos en otra historia. |
| **V — Valuable** | ✅ | Asegura la integridad de la cuenta del huésped y protege al hotel de errores de facturación. |
| **E — Estimable** | ✅ | La restricción de estado "Ocupada", el motivo obligatorio de anulación y el registro de auditoría están definidos. |
| **S — Small** | ✅ | Cubre solo el ciclo de carga y anulación de consumos; la facturación final es una historia separada. |
| **T — Testable** | ✅ | El estado de la habitación, la exigencia de motivo y el registro de auditoría son verificables. |

---

### Criterios de aceptación (Gherkin)

```gherkin
# language: es
Característica: Registro y anulación de consumos durante la estadía

  Antecedentes:
    Dado que el recepcionista tiene sesión activa en la sede correspondiente

  Escenario: Registro exitoso de un consumo en habitación ocupada
    Dado que la habitación "205" tiene estado "Ocupada"
    Y el ítem "Agua mineral 500ml" está activo en el catálogo de consumos
    Cuando el recepcionista selecciona la habitación "205" y registra el consumo del ítem con su precio
    Entonces el sistema agrega el consumo a la cuenta de la habitación "205"
    Y el total de la cuenta de la habitación se actualiza con el valor del nuevo consumo

  Escenario: Intento de registrar consumo en habitación no ocupada
    Dado que la habitación "101" tiene estado distinto a "Ocupada"
    Cuando el recepcionista intenta registrar un consumo en esa habitación
    Entonces el sistema muestra el mensaje "Solo se pueden registrar consumos en habitaciones con estado Ocupada"
    Y no agrega ningún consumo

  Escenario: Anulación de consumo con motivo obligatorio
    Dado que la habitación "205" tiene registrado el consumo "Almuerzo ejecutivo" por valor de $35.000
    Cuando el recepcionista solicita la anulación de ese consumo
    Y el recepcionista ingresa el motivo "Consumo registrado en habitación incorrecta"
    Entonces el sistema anula el consumo y descuenta su valor de la cuenta de la habitación
    Y genera un registro de auditoría con: el consumo anulado, el motivo, el recepcionista responsable y el timestamp del servidor

  Escenario: Intento de anulación de consumo sin ingresar motivo
    Dado que la habitación "205" tiene un consumo registrado
    Cuando el recepcionista intenta anular el consumo sin ingresar ningún motivo
    Entonces el sistema muestra el mensaje "Debe ingresar un motivo para anular el consumo"
    Y no realiza la anulación

  Escenario: Registro de consumo con ítem inactivo del catálogo
    Dado que el ítem "Botella de vino tinto" está marcado como inactivo en el catálogo
    Cuando el recepcionista intenta registrar ese ítem como consumo
    Entonces el sistema no muestra el ítem en la lista de consumos disponibles
    Y no permite su registro
```

---

### Supuestos / preguntas abiertas

- Se asume que el catálogo de consumos disponibles para el recepcionista está filtrado por sede (items activos de la sede activa).
- No se especificó si el recepcionista puede editar un consumo ya registrado o si únicamente puede anularlo y registrar uno nuevo; se asume que solo puede anular.
- Se asume que el registro de auditoría de anulaciones está disponible para consulta del administrador pero no es modificable por el recepcionista.

---

## HU-10: Gestión de check-out con penalidad por salida tardía

**Como** recepcionista  
**Quiero** registrar el check-out de un huésped y cerrar su cuenta con el total de estadía más consumos  
**Para** formalizar la salida y cobrar los cargos correctos incluyendo recargos por salida tardía

---

### Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | El check-out puede desarrollarse una vez que check-in y consumos existan como datos; es independiente como historia. |
| **N — Negotiable** | ✅ | No impone el mecanismo de cálculo del recargo ni el método de cobro final. |
| **V — Valuable** | ✅ | Cierra la cuenta del huésped correctamente y protege los ingresos del hotel por salidas tardías. |
| **E — Estimable** | ✅ | La fórmula (estadía + consumos + recargo tardío) y el umbral de hora límite (13:00) están definidos. |
| **S — Small** | ✅ | Solo cubre el check-out; la transición a limpieza y la facturación son historias separadas. |
| **T — Testable** | ✅ | El cálculo del recargo, el umbral de hora y el cierre de cuenta son verificables objetivamente. |

---

### Criterios de aceptación (Gherkin)

```gherkin
# language: es
Característica: Gestión de check-out con penalidad por salida tardía

  Antecedentes:
    Dado que el recepcionista tiene sesión activa en la sede correspondiente

  Escenario: Check-out exitoso antes de la hora límite sin recargo
    Dado que la habitación "205" tiene estado "Ocupada" con la reserva "RSV-20261001-001"
    Y el check-out se realiza antes de las 13:00 del día de salida
    Cuando el recepcionista registra el check-out de la habitación "205"
    Entonces el sistema calcula el total final sumando el costo de estadía más los consumos registrados
    Y no aplica ningún recargo por salida tardía
    Y muestra el resumen de cuenta al recepcionista para confirmación

  Escenario: Check-out con recargo automático por salida tardía
    Dado que la habitación "312" tiene estado "Ocupada" con la reserva "RSV-20261001-002"
    Y el check-out se realiza después de las 13:00 del día de salida
    Cuando el recepcionista registra el check-out de la habitación "312"
    Entonces el sistema detecta automáticamente que la salida supera la hora límite de las 13:00
    Y calcula y agrega el recargo por Late Check-out al total de la cuenta
    Y muestra el desglose: costo de estadía + consumos + recargo por salida tardía
    Y el recargo queda registrado en la cuenta para su inclusión en la factura final

  Escenario: Intento de check-out en habitación que no está ocupada
    Dado que la habitación "101" tiene estado distinto a "Ocupada"
    Cuando el recepcionista intenta registrar el check-out de esa habitación
    Entonces el sistema muestra el mensaje "No se puede realizar check-out: la habitación no está en estado Ocupada"
    Y no realiza ningún cambio de estado

  Escenario: Confirmación del total antes de finalizar el check-out
    Dado que el recepcionista ha revisado el resumen de cuenta del huésped
    Cuando confirma el check-out
    Entonces el sistema registra el timestamp de salida tomado del servidor
    Y cambia el estado de la reserva a "Pendiente de cierre"
    Y activa el flujo de transición de la habitación al estado "En Limpieza"
```

---

### Supuestos / preguntas abiertas

- No se especificó el valor exacto del recargo por Late Check-out (monto fijo, porcentaje de la noche, etc.); se asume que es un valor configurable por el administrador. Esta información es necesaria para completar los escenarios con datos de ejemplo reales.
- Se asume que la "hora límite" de las 13:00 puede ser configurable por sede en el módulo de administración.
- No se especificó si el check-out genera automáticamente la factura final o si eso requiere un paso adicional; se asumió que la facturación es un paso separado (HU-12).

---

## HU-11: Transición de habitación a estado de limpieza tras check-out

**Como** recepcionista  
**Quiero** que tras el check-out la habitación pase automáticamente a estado "En Limpieza" y poder habilitarla manualmente una vez el aseo esté confirmado  
**Para** controlar el ciclo físico de preparación de la habitación antes de ofrecerla nuevamente

---

### Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | La gestión del ciclo de limpieza es independiente del check-out como transacción financiera. |
| **N — Negotiable** | ✅ | No impone cómo se comunica el equipo de aseo; el equipo puede decidir si hay módulo de housekeeping. |
| **V — Valuable** | ✅ | Evita que una habitación sucia aparezca como disponible, protegiendo la reputación del hotel. |
| **E — Estimable** | ✅ | La transición automática al check-out y la habilitación manual están claramente definidas. |
| **S — Small** | ✅ | Solo cubre el ciclo de estados post check-out; el check-out financiero es una historia separada. |
| **T — Testable** | ✅ | Los cambios de estado (Ocupada → En Limpieza → Disponible) son verificables objetivamente. |

---

### Criterios de aceptación (Gherkin)

```gherkin
# language: es
Característica: Transición de habitación a estado de limpieza tras check-out

  Antecedentes:
    Dado que el recepcionista tiene sesión activa en la sede correspondiente

  Escenario: Transición automática a "En Limpieza" tras confirmar el check-out
    Dado que la habitación "205" tiene estado "Ocupada"
    Cuando el recepcionista confirma el check-out de la habitación "205"
    Entonces el sistema cambia automáticamente el estado de la habitación a "En Limpieza"
    Y la habitación deja de aparecer como "Disponible" en las consultas de disponibilidad
    Y la habitación no puede ser asignada a nuevas reservas mientras esté "En Limpieza"

  Escenario: Habitación "En Limpieza" no visible en consultas de disponibilidad
    Dado que la habitación "205" está en estado "En Limpieza"
    Cuando un cliente consulta disponibilidad en la sede para las fechas correspondientes
    Entonces la habitación "205" no aparece en los resultados de disponibilidad

  Escenario: Habilitación manual de habitación tras confirmación de limpieza
    Dado que la habitación "205" está en estado "En Limpieza"
    Y el personal de aseo ha confirmado que la habitación está lista
    Cuando el recepcionista habilita manualmente la habitación "205"
    Entonces el sistema cambia el estado de la habitación a "Disponible"
    Y la habitación vuelve a aparecer en las consultas de disponibilidad

  Escenario: Intento de habilitar habitación sin pasar por estado "En Limpieza"
    Dado que la habitación "101" tiene estado "Ocupada"
    Cuando el recepcionista intenta cambiar directamente su estado a "Disponible" sin registrar check-out
    Entonces el sistema no permite el cambio directo de "Ocupada" a "Disponible"
    Y muestra el mensaje "Debe completar el check-out antes de habilitar la habitación"

  Escenario: Vista de habitaciones en limpieza para gestión del recepcionista
    Dado que existen varias habitaciones en estado "En Limpieza" en la sede activa
    Cuando el recepcionista accede al panel de gestión de habitaciones
    Entonces el sistema muestra las habitaciones en estado "En Limpieza" diferenciadas visualmente
    Y permite seleccionar cada una para habilitarla individualmente
```

---

### Supuestos / preguntas abiertas

- Se asume que el personal de aseo no tiene acceso propio al sistema para confirmar la limpieza; la confirmación la realiza el recepcionista de forma manual. Si existe un rol o módulo de housekeeping, este alcance podría ampliarse.
- No se especificó si hay un tiempo máximo en estado "En Limpieza" antes de generar una alerta; se registra como pregunta abierta.
- Se asume que el administrador también puede habilitar habitaciones desde el módulo de administración.

---

## HU-12: Facturación final y cierre documental de la reserva

**Como** sistema  
**Quiero** generar el PDF de factura final consolidada y bloquear la reserva en estado "Cerrada" al momento del check-out  
**Para** proveer al huésped un soporte financiero oficial y evitar modificaciones posteriores a la cuenta

---

### Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | La generación de la factura final puede desarrollarse como componente independiente de salida documental. |
| **N — Negotiable** | ✅ | No impone la plantilla fiscal ni el software de generación de PDF; el equipo lo decide. |
| **V — Valuable** | ✅ | Provee soporte legal y fiscal al huésped y cierra formalmente la transacción del hotel. |
| **E — Estimable** | ✅ | El contenido de la factura (pagos, impuestos, estado "Cerrada") y el bloqueo de modificaciones están definidos. |
| **S — Small** | ✅ | Solo cubre la generación del documento de cierre; el check-out y los consumos son historias separadas. |
| **T — Testable** | ✅ | El PDF, el detalle de impuestos, el estado "Cerrada" y el bloqueo de modificaciones son verificables. |

---

### Criterios de aceptación (Gherkin)

```gherkin
# language: es
Característica: Facturación final y cierre documental de la reserva

  Escenario: Generación automática de factura al confirmar el check-out
    Dado que la reserva "RSV-20261001-001" ha completado el proceso de check-out
    Y la cuenta incluye costo de estadía, consumos registrados e impuestos locales aplicables
    Cuando el sistema procesa el cierre de la reserva
    Entonces el sistema genera automáticamente un PDF de factura final
    Y el PDF consolida: detalle de pagos, consumos, impuestos locales desglosados y total a pagar
    Y el sistema cambia el estado de la reserva a "Cerrada"
    Y entrega el PDF al huésped por el canal configurado

  Escenario: La reserva cerrada no permite modificaciones
    Dado que la reserva "RSV-20261001-001" está en estado "Cerrada"
    Cuando el recepcionista o el sistema intenta agregar, modificar o anular un consumo en esa reserva
    Entonces el sistema muestra el mensaje "No se pueden realizar modificaciones en una reserva cerrada"
    Y rechaza cualquier cambio en la cuenta

  Escenario: La factura incluye el desglose de impuestos locales
    Dado que la reserva "RSV-20261001-002" tiene estado "Cerrada"
    Y el PDF de factura ha sido generado
    Cuando se revisa el contenido del PDF
    Entonces el documento incluye una línea separada por cada impuesto local aplicable con su nombre, tasa y valor
    Y muestra el total antes de impuestos y el total con impuestos claramente diferenciados

  Escenario: La factura no se genera si la reserva no ha completado el check-out
    Dado que la reserva "RSV-20261001-003" tiene estado "En curso" o "Pagada"
    Cuando el sistema evalúa si debe generar la factura final
    Entonces el sistema no genera ningún PDF de factura
    Y no cambia el estado de la reserva a "Cerrada"

  Escenario: El PDF de factura es accesible para el administrador
    Dado que la reserva "RSV-20261001-001" está en estado "Cerrada"
    Y su factura PDF fue generada
    Cuando el administrador accede al historial de reservas de la sede
    Entonces puede visualizar y descargar el PDF de factura de esa reserva
```

---

### Supuestos / preguntas abiertas

- No se especificaron los impuestos locales aplicables (IVA, ICA, impuesto al consumo, etc.) ni sus tasas. Se asume que son configurables por el administrador y se detalla solo la lógica de inclusión en el documento.
- Se asume que el canal de entrega del PDF es el correo electrónico registrado del huésped; si se requiere entrega en mostrador (impresión), es una extensión de alcance a confirmar.
- No se especificó si la factura puede ser reenvíada al huésped tras su generación; se deja como pregunta abierta.

---

## HU-13: Configuración y aislamiento multi-sede (Multitenancy)

**Como** administrador  
**Quiero** cambiar entre sedes y que todas las vistas, reservas y consumos se filtren exclusivamente por la sede activa  
**Para** gestionar distintas sucursales de manera segura e independiente sin mezclar información entre ellas

---

### Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | El mecanismo de aislamiento por sede puede desarrollarse como capa transversal independiente. |
| **N — Negotiable** | ✅ | No impone el modelo de datos de multi-tenancy; el equipo decide la arquitectura de separación. |
| **V — Valuable** | ✅ | Permite al administrador operar varias sedes sin riesgo de cruzar información ni comprometer la privacidad de cada sucursal. |
| **E — Estimable** | ✅ | La regla de filtrado estricto por ID de sede y el cambio de sede están claramente definidos. |
| **S — Small** | ✅ | Cubre solo la lógica de selección y aislamiento de sede; la gestión de usuarios y habitaciones son historias separadas. |
| **T — Testable** | ✅ | El filtrado por sede es verificable: ningún dato de otra sede debe aparecer en la sesión activa. |

---

### Criterios de aceptación (Gherkin)

```gherkin
# language: es
Característica: Configuración y aislamiento multi-sede

  Antecedentes:
    Dado que el administrador global tiene sesión activa
    Y existen al menos dos sedes configuradas: "Sede Norte" y "Sede Sur"

  Escenario: Cambio de sede activa por el administrador global
    Dado que el administrador tiene seleccionada la "Sede Norte" como sede activa
    Cuando el administrador cambia la sede activa a "Sede Sur"
    Entonces el sistema actualiza todas las vistas con la información correspondiente a la "Sede Sur"
    Y no muestra datos de la "Sede Norte" en ninguna vista del panel

  Escenario: Todas las vistas se filtran estrictamente por la sede activa
    Dado que el administrador tiene seleccionada la "Sede Norte" como sede activa
    Cuando accede a las vistas de reservas, habitaciones, consumos y usuarios
    Entonces cada vista muestra exclusivamente los datos asociados al ID de la "Sede Norte"
    Y no aparece ningún registro cuyo ID de sede sea diferente al de la sede activa

  Escenario: El recepcionista no puede cambiar de sede
    Dado que el usuario con rol "Recepcionista" está asignado a la "Sede Norte"
    Y tiene sesión activa
    Cuando accede al sistema
    Entonces el sistema no muestra ninguna opción para cambiar de sede
    Y todas sus operaciones están restringidas a los datos de la "Sede Norte"

  Escenario: Aislamiento de reportes por sede activa
    Dado que el administrador tiene seleccionada la "Sede Sur" como sede activa
    Cuando genera un reporte de ocupación o ingresos
    Entonces el reporte incluye únicamente datos de la "Sede Sur"
    Y no incluye ningún dato de la "Sede Norte" ni de otras sedes

  Escenario: Intento de acceso directo a datos de otra sede mediante URL o API
    Dado que el administrador tiene seleccionada la "Sede Norte" como sede activa
    Cuando intenta acceder directamente a recursos de la "Sede Sur" mediante una URL o llamada de API
    Entonces el sistema rechaza la solicitud
    Y muestra el mensaje "No tiene acceso a los recursos de la sede solicitada"
```

---

### Supuestos / preguntas abiertas

- Se asume que "administrador global" puede ver y gestionar todas las sedes, mientras que los administradores locales (si existen) estarían limitados a una sede específica. Este nivel de jerarquía no fue especificado y se registra como pregunta abierta.
- No se especificó si existe una "vista consolidada" que muestre datos de todas las sedes simultáneamente para el administrador global; se asume que solo se ve una sede a la vez.

---

## HU-14: Gestión de usuarios y roles con asignación por sede

**Como** administrador  
**Quiero** crear, editar y gestionar las cuentas de los empleados asignando su rol y sede de forma estricta  
**Para** garantizar que cada empleado acceda únicamente a la información y funciones de su sede asignada

---

### Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | La gestión de usuarios es independiente de las otras historias del módulo administrador. |
| **N — Negotiable** | ✅ | No impone el flujo de onboarding ni la interfaz de gestión de cuentas. |
| **V — Valuable** | ✅ | Asegura el control de acceso y protege la información de cada sede de accesos no autorizados. |
| **E — Estimable** | ✅ | La asignación estricta de sede y la restricción de acceso cruzado están claramente definidas. |
| **S — Small** | ✅ | Cubre solo el CRUD de usuarios y roles; la autenticación es una historia separada (HU-06). |
| **T — Testable** | ✅ | La restricción de acceso por sede es verificable: un usuario de Sede A no debe ver datos de Sede B. |

---

### Criterios de aceptación (Gherkin)

```gherkin
# language: es
Característica: Gestión de usuarios y roles con asignación por sede

  Antecedentes:
    Dado que el administrador global tiene sesión activa
    Y existen las sedes "Sede A" y "Sede B" configuradas en el sistema

  Escenario: Creación exitosa de un recepcionista asignado a una sede
    Dado que el administrador ingresa los datos del nuevo empleado: nombre, correo, contraseña y rol "Recepcionista"
    Y selecciona la "Sede A" como sede de asignación
    Cuando confirma la creación del usuario
    Entonces el sistema crea la cuenta con el rol "Recepcionista" asociado a la "Sede A"
    Y el nuevo recepcionista solo puede iniciar sesión y operar en la "Sede A"

  Escenario: El recepcionista de Sede A no tiene acceso a datos de Sede B
    Dado que el recepcionista "juan@hotel.com" está asignado a la "Sede A"
    Y tiene sesión activa
    Cuando intenta acceder a reservas, habitaciones o consumos de la "Sede B"
    Entonces el sistema deniega el acceso
    Y muestra el mensaje "No tiene permisos para acceder a los datos de esta sede"

  Escenario: Edición de datos de un usuario existente
    Dado que existe el usuario "pedro@hotel.com" con rol "Recepcionista" en la "Sede A"
    Cuando el administrador edita el nombre o correo del usuario y confirma los cambios
    Entonces el sistema actualiza los datos del usuario
    Y el rol y la sede de asignación no cambian a menos que el administrador los modifique explícitamente

  Escenario: Cambio de sede de un usuario existente
    Dado que existe el usuario "pedro@hotel.com" asignado a la "Sede A"
    Cuando el administrador cambia su sede de asignación a la "Sede B"
    Entonces el sistema actualiza la sede del usuario a "Sede B"
    Y el usuario pierde el acceso a los datos de la "Sede A"
    Y obtiene acceso únicamente a los datos de la "Sede B"

  Escenario: Desactivación de la cuenta de un usuario
    Dado que existe el usuario "carlos@hotel.com" con cuenta activa
    Cuando el administrador desactiva la cuenta
    Entonces el sistema impide que ese usuario inicie sesión
    Y muestra el mensaje "Su cuenta está inactiva. Contacte al administrador" si intenta ingresar

  Escenario: Intento de crear usuario sin asignar sede
    Dado que el administrador ingresa los datos del nuevo empleado pero no selecciona ninguna sede
    Cuando intenta confirmar la creación
    Entonces el sistema muestra el mensaje "Debe asignar una sede al usuario para continuar"
    Y no crea la cuenta
```

---

### Supuestos / preguntas abiertas

- Se asume que los campos mínimos para crear un usuario son: nombre completo, correo electrónico, contraseña inicial, rol y sede. El conjunto exacto de campos no fue especificado.
- No se especificó si el administrador puede asignar un usuario a múltiples sedes; se asume asignación a una sola sede por usuario basado en la regla "Sede A no tiene acceso a Sede B".
- No se especificó si existe un flujo de recuperación de contraseña para empleados; se deja como pregunta abierta.

---

## HU-15: Gestión del inventario de habitaciones (CRUD)

**Como** administrador  
**Quiero** crear, editar y cambiar el estado de las habitaciones del hotel  
**Para** mantener el inventario actualizado y controlar qué habitaciones están disponibles para reservas

---

### Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | La gestión del inventario de habitaciones puede desarrollarse independientemente de reservas y consumos. |
| **N — Negotiable** | ✅ | No impone la interfaz de gestión ni los campos adicionales opcionales de una habitación. |
| **V — Valuable** | ✅ | Permite mantener el catálogo de habitaciones exacto, incluyendo las temporalmente fuera de servicio. |
| **E — Estimable** | ✅ | Las operaciones CRUD y el efecto del estado "Mantenimiento" en la disponibilidad están claramente definidas. |
| **S — Small** | ✅ | Cubre solo el CRUD de habitaciones; la consulta de disponibilidad y las reservas son historias separadas. |
| **T — Testable** | ✅ | La ocultación de habitaciones en mantenimiento y la configuración de capacidad y precio son verificables. |

---

### Criterios de aceptación (Gherkin)

```gherkin
# language: es
Característica: Gestión del inventario de habitaciones

  Antecedentes:
    Dado que el administrador tiene sesión activa con la sede "Sede Norte" activa

  Escenario: Creación exitosa de una nueva habitación
    Dado que el administrador ingresa los datos de la nueva habitación: número, tipo, capacidad máxima y precio base
    Cuando confirma la creación de la habitación
    Entonces el sistema registra la habitación en el inventario de la "Sede Norte"
    Y la habitación queda en estado "Disponible" por defecto
    Y aparece en las consultas de disponibilidad para los filtros correspondientes

  Escenario: Edición de capacidad y precio base de una habitación existente
    Dado que la habitación "305" de la "Sede Norte" tiene capacidad 2 y precio base $150.000
    Cuando el administrador cambia la capacidad a 3 y el precio base a $180.000 y confirma los cambios
    Entonces el sistema actualiza los datos de la habitación "305"
    Y las consultas de disponibilidad futuras reflejan la nueva capacidad y precio

  Escenario: Cambio de estado de habitación a "Mantenimiento"
    Dado que la habitación "102" de la "Sede Norte" está en estado "Disponible"
    Cuando el administrador cambia su estado a "Mantenimiento"
    Entonces el sistema actualiza el estado de la habitación a "Mantenimiento"
    Y la habitación queda oculta de las consultas de disponibilidad para clientes
    Y no puede ser seleccionada para nuevas reservas mientras permanezca en ese estado

  Escenario: Restauración de habitación desde estado "Mantenimiento"
    Dado que la habitación "102" de la "Sede Norte" está en estado "Mantenimiento"
    Cuando el administrador cambia su estado a "Disponible"
    Entonces el sistema actualiza el estado a "Disponible"
    Y la habitación vuelve a aparecer en las consultas de disponibilidad

  Escenario: Intento de crear habitación con datos obligatorios incompletos
    Dado que el administrador no ha ingresado el precio base o la capacidad máxima
    Cuando intenta confirmar la creación de la habitación
    Entonces el sistema muestra el mensaje indicando los campos obligatorios faltantes
    Y no crea la habitación

  Escenario: Habitación en "Mantenimiento" no aparece en consultas de disponibilidad de clientes
    Dado que la habitación "102" está en estado "Mantenimiento"
    Cuando un cliente consulta disponibilidad en la "Sede Norte" para cualquier fecha
    Entonces la habitación "102" no aparece en los resultados de disponibilidad
```

---

### Supuestos / preguntas abiertas

- No se especificó si una habitación con reservas futuras puede pasar a "Mantenimiento"; se asume que el sistema debe advertir al administrador y requerir confirmación si hay reservas futuras afectadas.
- No se especificó el listado completo de campos de una habitación (ej. descripción, fotografías, amenidades); se trabajó con los campos mencionados: número, tipo, capacidad y precio base.
- Se asume que cada habitación pertenece a una única sede y no puede moverse entre sedes.

---

## HU-16: Configuración del catálogo de consumos

**Como** administrador  
**Quiero** crear y gestionar los ítems del catálogo de productos y servicios cobrables con su precio y categoría  
**Para** que los recepcionistas puedan registrar consumos correctamente y el historial de facturas quede íntegro

---

### Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | La gestión del catálogo es independiente del registro de consumos; puede desarrollarse antes que HU-09. |
| **N — Negotiable** | ✅ | No impone las categorías adicionales ni el diseño de la interfaz de gestión del catálogo. |
| **V — Valuable** | ✅ | Permite estandarizar los ítems cobrables y protege la integridad del historial de facturas al inactivar ítems. |
| **E — Estimable** | ✅ | Las reglas de creación, las categorías y el comportamiento al inactivar un ítem están claramente definidas. |
| **S — Small** | ✅ | Solo cubre el CRUD del catálogo; el registro de consumos en estadía es una historia separada (HU-09). |
| **T — Testable** | ✅ | La visibilidad del ítem activo/inactivo y la integridad del historial son verificables. |

---

### Criterios de aceptación (Gherkin)

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

### Supuestos / preguntas abiertas

- Las categorías definidas en el requerimiento son "Restaurante" y "Minibar". El Esquema del escenario asume que son las únicas categorías válidas; si hay más, deben ser confirmadas.
- Se asume que el catálogo es por sede; ítems de la "Sede Norte" no están disponibles en la "Sede Sur". No fue especificado explícitamente.
- No se especificó si se puede eliminar definitivamente un ítem o solo inactivarlo; se asume que solo se puede inactivar para preservar la trazabilidad histórica.

---

## HU-17: Generación de reportes operativos de ocupación e ingresos

**Como** administrador  
**Quiero** generar reportes de ocupación e ingresos totales filtrados por sede y rango de fechas  
**Para** analizar el rendimiento financiero y operativo de cada sucursal del hotel

---

### Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | Los reportes son un módulo de consulta independiente; no modifica datos operativos. |
| **N — Negotiable** | ✅ | No impone el formato de salida del reporte (PDF, Excel, pantalla); el equipo decide. |
| **V — Valuable** | ✅ | Provee información de toma de decisiones para la gerencia del hotel. |
| **E — Estimable** | ✅ | Los filtros requeridos (sede, rango de fechas) y los tipos de reporte (ocupación, ingresos) están definidos. |
| **S — Small** | ✅ | Solo cubre la generación de reportes; el CRUD de habitaciones y reservas son historias separadas. |
| **T — Testable** | ✅ | El filtrado por sede y fechas, y los datos incluidos en cada tipo de reporte son verificables. |

---

### Criterios de aceptación (Gherkin)

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

### Supuestos / preguntas abiertas

- No se especificó el formato de salida del reporte (pantalla, PDF exportable, Excel/CSV); se asume visualización en pantalla con opción de exportar. El formato exacto queda por confirmar.
- No se especificó el cálculo exacto de "porcentaje de ocupación" (noches ocupadas / noches totales disponibles, reservas activas, etc.); se asume la métrica estándar hotelera pero debe confirmarse.
- No se especificó si el reporte de ingresos incluye reservas canceladas con penalidad; se asume que sí, ya que generaron ingresos parciales.
- No se especificó si se requieren más tipos de reportes adicionales (p. ej. consumos por categoría, no-shows, reembolsos); los enunciados en el requerimiento son ocupación e ingresos totales.
