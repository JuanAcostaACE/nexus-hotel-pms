package com.nexus.hotelpms.infraestructura.jobs;

import com.nexus.hotelpms.infraestructura.correo.CorreoOutbox;
import com.nexus.hotelpms.infraestructura.correo.CorreoOutboxRepository;
import com.nexus.hotelpms.infraestructura.correo.SmtpEmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

/** Publica los correos pendientes de la cola (outbox) por SMTP; reintenta hasta CorreoOutbox.MAX_INTENTOS veces. */
@Component
public class PublicadorOutbox {
    private static final Logger log = LoggerFactory.getLogger(PublicadorOutbox.class);
    private static final int LOTE = 20;

    private final CorreoOutboxRepository cola;
    private final SmtpEmailSender smtp;
    private final Clock clock;

    public PublicadorOutbox(CorreoOutboxRepository cola, SmtpEmailSender smtp, Clock clock) {
        this.cola = cola;
        this.smtp = smtp;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${hotel.job-outbox-ms:5000}")
    public void publicarPendientes() {
        for (CorreoOutbox c : cola.findByEstadoOrderByCreadoAtAsc(CorreoOutbox.Estado.Pendiente, PageRequest.of(0, LOTE))) {
            try {
                smtp.enviar(c.getDestino(), c.getAsunto(), c.getCuerpo(), c.getNombreAdjunto(), c.getAdjunto());
                c.marcarEnviado(clock.instant());
            } catch (RuntimeException e) {
                c.registrarFallo(e.getMessage());
                log.warn("Correo {} no enviado (intento {}): {}", c.getId(), c.getIntentos(), e.getMessage());
            }
            cola.save(c);
        }
    }
}
