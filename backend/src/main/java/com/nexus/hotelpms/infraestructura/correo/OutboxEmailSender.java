package com.nexus.hotelpms.infraestructura.correo;

import com.nexus.hotelpms.aplicacion.puertos.IEmailSender;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Implementación de {@link IEmailSender} que usan los servicios: no envía en la petición, solo deja el correo
 * en la cola (outbox). {@code PublicadorOutbox} lo entrega después por SMTP, así un fallo del correo
 * no afecta al pago ni al cierre de la reserva.
 */
@Primary
@Component
public class OutboxEmailSender implements IEmailSender {
    private final CorreoOutboxRepository cola;
    private final Clock clock;

    public OutboxEmailSender(CorreoOutboxRepository cola, Clock clock) {
        this.cola = cola;
        this.clock = clock;
    }

    @Override
    public void enviar(String destino, String asunto, String cuerpo, String nombreAdjunto, byte[] adjunto) {
        cola.save(new CorreoOutbox(destino, asunto, cuerpo, nombreAdjunto, adjunto, clock.instant()));
    }
}
