# HU-06: Autenticación y control de acceso por rol

**Como** recepcionista o administrador  
**Quiero** ingresar al sistema con mis credenciales para acceder a las funciones asignadas a mi rol  
**Para** operar el sistema de manera segura con los permisos correspondientes a mi cargo

---

## Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | La autenticación es la puerta de entrada al sistema y puede desarrollarse independientemente. |
| **N — Negotiable** | ✅ | No impone tecnología de autenticación (JWT, sesiones, etc.); el equipo decide la implementación. |
| **V — Valuable** | ✅ | Garantiza la seguridad del sistema y evita accesos no autorizados. |
| **E — Estimable** | ✅ | Las reglas de bloqueo (3 intentos) y expiración de sesión (15 min) están claramente definidas. |
| **S — Small** | ✅ | Cubre solo autenticación y control de sesión; la gestión de usuarios es una historia separada. |
| **T — Testable** | ✅ | El bloqueo, la expiración y el redireccionamiento por rol son verificables objetivamente. |

---

## Criterios de aceptación (Gherkin)

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

## Supuestos / preguntas abiertas

- Se asume que el bloqueo temporal dura hasta que el administrador lo desbloquee manualmente; no se especificó un tiempo automático de desbloqueo.
- No se especificó si el contador de intentos se reinicia tras un login exitoso o tras cierto tiempo; se asume que se reinicia tras un inicio de sesión exitoso.
- No se especificó si existe un rol de "Housekeeping" u otros; se trabajó únicamente con los roles mencionados (Recepcionista, Administrador).
