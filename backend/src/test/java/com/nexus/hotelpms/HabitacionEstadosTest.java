package com.nexus.hotelpms;

import com.nexus.hotelpms.dominio.excepcion.ReglaNegocioException;
import com.nexus.hotelpms.dominio.modelo.EstadoHabitacion;
import com.nexus.hotelpms.dominio.modelo.Habitacion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Cambios de estado que puede ordenar el administrador (HU-11 y HU-15). */
class HabitacionEstadosTest {

    private Habitacion en(EstadoHabitacion estado) {
        Habitacion h = new Habitacion("sede", "101", "Doble", 2, BigDecimal.TEN);
        h.cambiarEstado(estado);
        return h;
    }

    @Test
    void disponibleVaAMantenimientoYVuelve() {
        Habitacion h = en(EstadoHabitacion.Disponible);
        h.cambiarEstadoAdministrativo(EstadoHabitacion.Mantenimiento);
        assertThat(h.getEstado()).isEqualTo(EstadoHabitacion.Mantenimiento);
        h.cambiarEstadoAdministrativo(EstadoHabitacion.Disponible);
        assertThat(h.getEstado()).isEqualTo(EstadoHabitacion.Disponible);
    }

    @Test
    void enLimpiezaSePuedeHabilitar() {
        Habitacion h = en(EstadoHabitacion.EnLimpieza);
        h.cambiarEstadoAdministrativo(EstadoHabitacion.Disponible);
        assertThat(h.getEstado()).isEqualTo(EstadoHabitacion.Disponible);
    }

    @Test
    void ocupadaNoPasaADisponibleSinCheckOut() {
        Habitacion h = en(EstadoHabitacion.Ocupada);
        assertThatThrownBy(() -> h.cambiarEstadoAdministrativo(EstadoHabitacion.Disponible))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Debe completar el check-out antes de habilitar la habitación");
        assertThat(h.getEstado()).isEqualTo(EstadoHabitacion.Ocupada);
    }

    @Test
    void ocupadaNoPasaAMantenimiento() {
        Habitacion h = en(EstadoHabitacion.Ocupada);
        assertThatThrownBy(() -> h.cambiarEstadoAdministrativo(EstadoHabitacion.Mantenimiento))
                .hasMessage("No se puede enviar a mantenimiento una habitación ocupada");
    }

    @Test
    void bloqueadaPorUnPagoNoSePuedeForzar() {
        Habitacion h = en(EstadoHabitacion.Bloqueada);
        assertThatThrownBy(() -> h.cambiarEstadoAdministrativo(EstadoHabitacion.Disponible))
                .isInstanceOf(ReglaNegocioException.class);
        assertThat(h.getEstado()).isEqualTo(EstadoHabitacion.Bloqueada);
    }

    @Test
    void reservadaNoSeLiberaAMano() {
        Habitacion h = en(EstadoHabitacion.Reservada);
        assertThatThrownBy(() -> h.cambiarEstadoAdministrativo(EstadoHabitacion.Disponible))
                .hasMessageContaining("cancele la reserva");
    }

    @Test
    void reservadaSiPuedeIrAMantenimiento() {
        Habitacion h = en(EstadoHabitacion.Reservada);
        h.cambiarEstadoAdministrativo(EstadoHabitacion.Mantenimiento); // la confirmación se exige en el servicio
        assertThat(h.getEstado()).isEqualTo(EstadoHabitacion.Mantenimiento);
    }

    @Test
    void elAdministradorNoPuedeForzarEstadosQueGobiernaLaReserva() {
        for (EstadoHabitacion destino : List.of(EstadoHabitacion.Ocupada, EstadoHabitacion.Reservada,
                EstadoHabitacion.Bloqueada, EstadoHabitacion.EnLimpieza)) {
            Habitacion h = en(EstadoHabitacion.Disponible);
            assertThatThrownBy(() -> h.cambiarEstadoAdministrativo(destino))
                    .as("destino %s", destino)
                    .isInstanceOf(ReglaNegocioException.class);
            assertThat(h.getEstado()).isEqualTo(EstadoHabitacion.Disponible);
        }
    }
}
