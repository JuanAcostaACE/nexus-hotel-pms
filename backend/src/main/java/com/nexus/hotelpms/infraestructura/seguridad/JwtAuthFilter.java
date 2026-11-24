package com.nexus.hotelpms.infraestructura.seguridad;

import com.nexus.hotelpms.aplicacion.puertos.ITokenService;
import com.nexus.hotelpms.aplicacion.sesion.AuthPrincipal;
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
 */
public class JwtAuthFilter extends OncePerRequestFilter {
    public static final String ATTR_EXPIRADA = "sesionExpirada";
    private final ITokenService tokens;

    public JwtAuthFilter(ITokenService tokens) {
        this.tokens = tokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                AuthPrincipal p = tokens.validar(header.substring(7));
                var auth = new UsernamePasswordAuthenticationToken(p, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + p.rol())));
                SecurityContextHolder.getContext().setAuthentication(auth);
                res.setHeader("X-Token-Renovado", tokens.generar(p));
            } catch (ExpiredJwtException e) {
                req.setAttribute(ATTR_EXPIRADA, true);
            } catch (JwtException | IllegalArgumentException e) {
                // token inválido: la petición sigue sin autenticar y el entry point responde 401
            }
        }
        chain.doFilter(req, res);
    }
}
