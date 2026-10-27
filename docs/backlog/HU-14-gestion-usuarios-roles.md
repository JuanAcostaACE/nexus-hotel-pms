# HU-14: Gestión de usuarios y roles con asignación por sede

**Como** administrador  
**Quiero** crear, editar y gestionar las cuentas de los empleados asignando su rol y sede de forma estricta  
**Para** garantizar que cada empleado acceda únicamente a la información y funciones de su sede asignada

---

## Validación INVEST

| Criterio | Estado | Justificación |
|---|---|---|
| **I — Independent** | ✅ | La gestión de usuarios es independiente de las otras historias del módulo administrador. |
| **N — Negotiable** | ✅ | No impone el flujo de onboarding ni la interfaz de gestión de cuentas. |
| **V — Valuable** | ✅ | Asegura el control de acceso y protege la información de cada sede de accesos no autorizados. |
| **E — Estimable** | ✅ | La asignación estricta de sede y la restricción de acceso cruzado están claramente definidas. |
| **S — Small** | ✅ | Cubre solo el CRUD de usuarios y roles; la autenticación es una historia separada (HU-06). |
| **T — Testable** | ✅ | La restricción de acceso por sede es verificable: un usuario de Sede A no debe ver datos de Sede B. |

---

## Criterios de aceptación (Gherkin)

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

## Supuestos / preguntas abiertas

- Se asume que los campos mínimos para crear un usuario son: nombre completo, correo electrónico, contraseña inicial, rol y sede. El conjunto exacto de campos no fue especificado.
- No se especificó si el administrador puede asignar un usuario a múltiples sedes; se asume asignación a una sola sede por usuario basado en la regla "Sede A no tiene acceso a Sede B".
- No se especificó si existe un flujo de recuperación de contraseña para empleados; se deja como pregunta abierta.
