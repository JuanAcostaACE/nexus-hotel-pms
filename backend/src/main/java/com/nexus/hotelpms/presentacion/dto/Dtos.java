package com.nexus.hotelpms.presentacion.dto;

import com.nexus.hotelpms.dominio.modelo.Consumo;
import com.nexus.hotelpms.dominio.modelo.Habitacion;
import com.nexus.hotelpms.dominio.modelo.Reserva;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Contratos de entrada/salida de la API REST. */
public final class Dtos {
    private Dtos() { }

    public record LoginRequest(@NotBlank(message = "El email es obligatorio") String email,
                               @NotBlank(message = "La contraseña es obligatoria") String password) { }

    public record LoginResponse(String token, String tipo, String rol, String sedeId, String nombre,
                                int expiraEnMinutos) { }

    public record CrearReservaRequest(
            @NotBlank(message = "La sede es obligatoria") String sedeId,
            @NotBlank(message = "La habitación es obligatoria") String habitacionId,
            @NotNull(message = "La fecha de entrada es obligatoria") LocalDate fechaEntrada,
            @NotNull(message = "La fecha de salida es obligatoria") LocalDate fechaSalida,
            @NotNull(message = "El número de huéspedes es obligatorio") @Min(value = 1, message = "El número de huéspedes debe ser positivo") Integer numHuespedes,
            @NotBlank(message = "El nombre completo del huésped es obligatorio") String clienteNombre,
            @NotBlank(message = "El documento de identidad es obligatorio") String clienteDocumento,
            @NotBlank(message = "El correo electrónico es obligatorio") @Email(message = "El correo electrónico no es válido") String clienteEmail) { }

    public record DisponibilidadResponse(List<Habitacion> habitaciones, String mensaje) { }

    /** El documento del huésped acompaña al código de la reserva: el código solo no basta para operar sobre ella. */
    public record PagoRequest(@NotBlank(message = "El banco es obligatorio") String banco,
                              String simulacion,
                              @NotBlank(message = "El documento del huésped es obligatorio") String documento) { }

    public record CancelarRequest(@NotBlank(message = "El documento del huésped es obligatorio") String documento) { }

    public record CambiarPasswordRequest(
            @NotBlank(message = "La contraseña actual es obligatoria") String passwordActual,
            @NotBlank(message = "La contraseña nueva es obligatoria")
            @Size(min = 8, message = "La contraseña nueva debe tener al menos 8 caracteres") String passwordNueva) { }

    public record PagoResponse(String transaccionId, String estadoPago, String estadoReserva, String mensaje) { }

    public record WebhookPseRequest(@NotBlank(message = "transaccionId es obligatorio") String transaccionId,
                                    @NotBlank(message = "resultado es obligatorio") String resultado) { }

    public record CancelacionResponse(String codigo, String estado, String politica, BigDecimal reembolso,
                                      BigDecimal retenido, String mensaje) { }

    public record ConsumoRequest(@NotBlank(message = "El ítem es obligatorio") String catalogoItemId,
                                 @NotNull(message = "La cantidad es obligatoria") @Min(value = 1, message = "La cantidad debe ser mayor a cero") Integer cantidad) { }

    public record AnularConsumoRequest(String motivo) { }

    public record CuentaHabitacion(String habitacionNumero, String reservaCodigo, List<Consumo> consumos,
                                   BigDecimal totalConsumos) { }

    public record ResumenCuenta(String reservaCodigo, String habitacionNumero, BigDecimal costoEstadia,
                                List<Consumo> consumos, BigDecimal totalConsumos, boolean salidaTardia,
                                BigDecimal recargoLateCheckout, BigDecimal total, boolean confirmado,
                                String estadoReserva, String mensaje) { }

    public record ReservaDetalle(Reserva reserva, String mensaje) { }

    public record BusquedaResponse(List<Reserva> reservas, String mensaje) { }
}
