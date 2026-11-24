package com.nexus.hotelpms.infraestructura.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;

/** Parámetros de negocio configurables en application.yml (prefijo {@code hotel}). */
@ConfigurationProperties(prefix = "hotel")
public record HotelProperties(
        @DefaultValue("15") int bloqueoMinutos,
        @DefaultValue("13") int horaLimiteCheckout,
        @DefaultValue("50000") BigDecimal recargoLateCheckout,
        @DefaultValue("0.19") BigDecimal ivaTasa,
        @DefaultValue("0.08") BigDecimal impuestoConsumoTasa,
        @DefaultValue("America/Bogota") String zona,
        @DefaultValue("nexus-pse-secreto") String pseSecret,
        @DefaultValue("15") int sesionMinutos,
        @DefaultValue("cambia-este-secreto-jwt-de-al-menos-32-caracteres") String jwtSecret,
        @DefaultValue("true") boolean seedHabilitado,
        @DefaultValue("http://localhost:*,http://127.0.0.1:*") String corsOrigenes) { }
