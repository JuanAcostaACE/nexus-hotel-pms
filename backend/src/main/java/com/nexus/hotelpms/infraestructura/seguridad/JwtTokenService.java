package com.nexus.hotelpms.infraestructura.seguridad;

import com.nexus.hotelpms.aplicacion.puertos.ITokenService;
import com.nexus.hotelpms.aplicacion.sesion.AuthPrincipal;
import com.nexus.hotelpms.infraestructura.config.HotelProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

/** Implementación JWT (HS256). La expiración equivale al límite de inactividad de 15 min (HU-06). */
@Component
public class JwtTokenService implements ITokenService {
    private final SecretKey clave;
    private final HotelProperties props;
    private final Clock clock;

    public JwtTokenService(HotelProperties props, Clock clock) {
        this.props = props;
        this.clock = clock;
        this.clave = Keys.hmacShaKeyFor(props.jwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generar(AuthPrincipal p) {
        Instant ahora = clock.instant();
        var builder = Jwts.builder()
                .subject(p.userId())
                .claim("nombre", p.nombre())
                .claim("rol", p.rol())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusSeconds(props.sesionMinutos() * 60L)));
        if (p.sedeId() != null) {
            builder.claim("sedeId", p.sedeId());
        }
        return builder.signWith(clave).compact();
    }

    @Override
    public AuthPrincipal validar(String token) {
        Claims c = Jwts.parser().verifyWith(clave).clock(() -> Date.from(clock.instant()))
                .build().parseSignedClaims(token).getPayload();
        return new AuthPrincipal(c.getSubject(), c.get("nombre", String.class), c.get("rol", String.class),
                c.get("sedeId", String.class));
    }
}
