package com.nexus.hotelpms.dominio.modelo;

import com.nexus.hotelpms.dominio.excepcion.CredencialesInvalidasException;
import com.nexus.hotelpms.dominio.excepcion.CuentaBloqueadaException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Superclase de {@link Administrador} y {@link Recepcionista} (herencia del diagrama de clases).
 * Regla HU-06: 3 intentos fallidos consecutivos bloquean la cuenta hasta que un administrador la desbloquee.
 */
@Document("usuarios")
public abstract class Usuario {
    public static final int MAX_INTENTOS = 3;

    @Id private String id;
    private String nombre;
    @Indexed(unique = true) private String email;
    @JsonIgnore private String passwordHash;
    private boolean activo = true;
    private int intentosFallidos;
    private boolean bloqueado;

    protected Usuario() { }

    protected Usuario(String nombre, String email, String passwordHash) {
        this.nombre = nombre;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    /**
     * Aplica el resultado de validar credenciales.
     * @param credencialesValidas resultado de comparar la contraseña (lo hace la capa de aplicación)
     */
    public void autenticar(boolean credencialesValidas) {
        if (!activo) {
            throw new CredencialesInvalidasException("Su cuenta está inactiva. Contacte al administrador");
        }
        if (bloqueado) {
            throw new CuentaBloqueadaException("Su cuenta está bloqueada. Contacte al administrador");
        }
        if (credencialesValidas) {
            intentosFallidos = 0;
            return;
        }
        intentosFallidos++;
        if (intentosFallidos >= MAX_INTENTOS) {
            bloqueado = true;
            throw new CuentaBloqueadaException(
                    "Su cuenta ha sido bloqueada temporalmente por múltiples intentos fallidos");
        }
        throw new CredencialesInvalidasException("Credenciales incorrectas");
    }

    public void desbloquear() { this.bloqueado = false; this.intentosFallidos = 0; }
    public void inactivar() { this.activo = false; }

    /** Reemplaza el hash de la contraseña (el cifrado lo hace la capa de aplicación). */
    public void cambiarPasswordHash(String nuevoHash) { this.passwordHash = nuevoHash; }

    /** HU-14: edita nombre y correo sin tocar el rol ni la sede. */
    public void actualizarDatos(String nombre, String email) {
        this.nombre = nombre;
        this.email = email;
    }

    /** Rol expuesto en el JWT. */
    public abstract String getRol();

    /** ¿Puede operar sobre datos de esa sede? (aislamiento multi-sede, HU-07/HU-17). */
    public abstract boolean tienePermisoSobre(String sedeId);

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    @JsonIgnore public String getPasswordHash() { return passwordHash; }
    public boolean isActivo() { return activo; }
    public boolean isBloqueado() { return bloqueado; }
    public int getIntentosFallidos() { return intentosFallidos; }
}
