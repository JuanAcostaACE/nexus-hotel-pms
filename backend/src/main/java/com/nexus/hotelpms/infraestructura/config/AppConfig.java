package com.nexus.hotelpms.infraestructura.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(HotelProperties.class)
public class AppConfig {

    /** Reloj del servidor: única fuente de timestamps oficiales (check-in, check-out, auditoría). */
    @Bean
    public Clock clock(HotelProperties props) {
        return Clock.system(ZoneId.of(props.zona()));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
