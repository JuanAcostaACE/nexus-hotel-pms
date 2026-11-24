package com.nexus.hotelpms.aplicacion.servicios;

import com.nexus.hotelpms.aplicacion.puertos.ITokenService;
import com.nexus.hotelpms.aplicacion.puertos.IUsuarioRepository;
import com.nexus.hotelpms.aplicacion.sesion.AuthPrincipal;
import com.nexus.hotelpms.dominio.excepcion.CredencialesInvalidasException;
import com.nexus.hotelpms.dominio.excepcion.RecursoNoEncontradoException;
import com.nexus.hotelpms.dominio.modelo.Recepcionista;
import com.nexus.hotelpms.dominio.modelo.Usuario;
import com.nexus.hotelpms.infraestructura.config.HotelProperties;
import com.nexus.hotelpms.presentacion.dto.Dtos.LoginResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** HU-06: autenticación, bloqueo por intentos fallidos y emisión del token de sesión. */
@Service
public class AuthService {
    private final IUsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final ITokenService tokens;
    private final HotelProperties props;

    public AuthService(IUsuarioRepository usuarios, PasswordEncoder encoder, ITokenService tokens,
                       HotelProperties props) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.tokens = tokens;
        this.props = props;
    }

    public LoginResponse login(String email, String password) {
        Usuario u = usuarios.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new CredencialesInvalidasException("Credenciales incorrectas"));
        boolean ok = encoder.matches(password, u.getPasswordHash());
        try {
            u.autenticar(ok); // incrementa/reinicia el contador y puede bloquear la cuenta
        } finally {
            usuarios.save(u); // el contador de intentos debe persistirse aunque falle el login
        }
        String sedeId = (u instanceof Recepcionista r) ? r.getSedeId() : null;
        AuthPrincipal principal = new AuthPrincipal(u.getId(), u.getNombre(), u.getRol(), sedeId);
        return new LoginResponse(tokens.generar(principal), "Bearer", u.getRol(), sedeId, u.getNombre(),
                props.sesionMinutos());
    }

    public java.util.List<Usuario> listarUsuarios() {
        return usuarios.findAll();
    }

    /** Solo administradores (control en el controlador): levanta el bloqueo de una cuenta. */
    public void desbloquear(String usuarioId) {
        Usuario u = usuarios.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        u.desbloquear();
        usuarios.save(u);
    }
}
