package com.nexus.hotelpms.dominio.modelo;

/** Ciclo de vida de una reserva (HU-02, 03, 04, 08, 10, 12). */
public enum EstadoReserva {
    Pendiente, Bloqueada, PendienteConfirmacion, Pagada, EnCurso, PendienteDeCierre, Cerrada,
    Cancelada, CanceladaConPenalidad, CanceladaPorTiempo, NoShow
}
