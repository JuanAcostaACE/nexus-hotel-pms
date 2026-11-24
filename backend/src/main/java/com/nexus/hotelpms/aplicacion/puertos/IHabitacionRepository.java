package com.nexus.hotelpms.aplicacion.puertos;

import com.nexus.hotelpms.dominio.modelo.EstadoHabitacion;
import com.nexus.hotelpms.dominio.modelo.Habitacion;

import java.util.List;
import java.util.Optional;

public interface IHabitacionRepository {
    Habitacion guardar(Habitacion habitacion);
    Optional<Habitacion> buscarPorId(String id);
    List<Habitacion> buscarPorSede(String sedeId);
    List<Habitacion> buscarPorSedeYEstado(String sedeId, EstadoHabitacion estado);
    /**
     * Bloqueo atómico (HU-02): cambia Disponible→Bloqueada en una sola operación de base de datos.
     * @return true si este llamado logró el bloqueo; false si otro cliente ya la tenía
     */
    boolean bloquearSiDisponible(String habitacionId);
}
