package com.nexus.hotelpms.infraestructura.seguridad;

import com.nexus.hotelpms.aplicacion.puertos.ITokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ITokenService tokens) throws Exception {
        http.csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                // Flujo público del cliente (HU-01 a HU-05)
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/sedes", "/api/disponibilidad", "/api/sedes/*/disponibilidad").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/reservas").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/reservas/*/pago", "/api/reservas/*/cancelar").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/reservas/*/comprobante").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/webhooks/pse").permitAll() // protegido por cabecera X-PSE-Secret
                .requestMatchers("/error").permitAll()
                // Todo lo demás exige sesión (recepción / administración)
                .anyRequest().authenticated())
            .exceptionHandling(e -> e
                .authenticationEntryPoint(entryPoint())
                .accessDeniedHandler(accessDenied()))
            .addFilterBefore(new JwtAuthFilter(tokens), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private AuthenticationEntryPoint entryPoint() {
        return (req, res, ex) -> {
            boolean expirada = Boolean.TRUE.equals(req.getAttribute(JwtAuthFilter.ATTR_EXPIRADA));
            escribir(res, 401, "No autenticado",
                    expirada ? "Su sesión ha expirado por inactividad" : "Debe iniciar sesión para acceder");
        };
    }

    private AccessDeniedHandler accessDenied() {
        return (req, res, ex) -> escribir(res, 403, "Acceso denegado", "No tiene permisos para acceder a esta sección");
    }

    private void escribir(jakarta.servlet.http.HttpServletResponse res, int estado, String error, String mensaje)
            throws java.io.IOException {
        res.setStatus(estado);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getWriter().write("{\"estado\":" + estado + ",\"error\":\"" + error + "\",\"mensaje\":\"" + mensaje + "\"}");
    }
}
