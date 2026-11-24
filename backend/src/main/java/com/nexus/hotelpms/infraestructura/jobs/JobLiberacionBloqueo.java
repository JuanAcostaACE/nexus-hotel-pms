package com.nexus.hotelpms.infraestructura.jobs;

import com.nexus.hotelpms.aplicacion.servicios.ReservaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** HU-02: libera cada ~30 s las habitaciones cuyo bloqueo de 15 minutos venció sin pago. */
@Component
public class JobLiberacionBloqueo {
    private static final Logger log = LoggerFactory.getLogger(JobLiberacionBloqueo.class);
    private final ReservaService reservaService;

    public JobLiberacionBloqueo(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @Scheduled(fixedDelayString = "${hotel.job-liberacion-ms:30000}")
    public void liberarExpirados() {
        int n = reservaService.liberarBloqueosExpirados();
        if (n > 0) {
            log.info("Bloqueos expirados liberados: {}", n);
        }
    }
}
