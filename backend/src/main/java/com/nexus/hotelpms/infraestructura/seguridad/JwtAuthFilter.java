package com.nexus.hotelpms.infraestructura.seguridad;

import com.nexus.hotelpms.aplicacion.puertos.ITokenService;
import com.nexus.hotelpms.aplicacion.puertos.IUsuarioRepository;
import com.nexus.hotelpms.aplicacion.sesion.AuthPrincipal;
import com.nexus.hotelpms.dominio.modelo.Recepcionista;
import com.nexus.hotelpms.dominio.modelo.Usuario;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lee "Authorization: Bearer ...". En cada petición válida devuelve un token renovado en la cabecera
 * {@code X-Token-Renovado}: así la sesión solo expira tras 15 minutos SIN actividad (HU-06).
 *
 * <p>Además consulta al usuario en la base en cada petición: si la cuenta fue desactivada la sesión deja de servir,
 * y el rol y la sede salen del registro actual (no del token), de modo que un cambio de sede hecho por el
 * administrador (HU-14) surte efecto de inmediato.</p>
 */
public class JwtAuthFilter extends OncePerRequestFilter {
    public static final String ATTR_EXPIRADA = "sesionExpirada";
    public static final String ATTR_INACTIVA = "cuentaInactiva";
    private final ITokenService tokens;
    private final IUsuarioRepository usuarios;

    public JwtAuthFilter(ITokenService tokens, IUsuarioRepository usuarios) {
        this.tokens = tokens;
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                AuthPrincipal delToken = tokens.validar(header.substring(7));
                Usuario u = usuarios.findById(delToken.userId()).orElse(null);
                if (u == null || !u.isActivo()) {
                    req.setAttribute(ATTR_INACTIVA, true);
                } else {
                    String sedeId = (u instanceof Recepcionista r) ? r.getSedeId() : null;
                    AuthPrincipal p = new AuthPrincipal(u.getId(), u.getNombre(), u.getRol(), sedeId);
                    var auth = new UsernamePasswordAuthenticationToken(p, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + p.rol())));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    res.setHeader("X-Token-Renovado", tokens.generar(p));
                }
            } catch (ExpiredJwtException e) {
                req.setAttribute(ATTR_EXPIRADA, true);
            } catch (JwtException | IllegalArgumentException e) {
                // token inválido: la petición sigue sin autenticar y el entry point responde 401
            }
        }
        chain.doFilter(req, res);
    }
}
