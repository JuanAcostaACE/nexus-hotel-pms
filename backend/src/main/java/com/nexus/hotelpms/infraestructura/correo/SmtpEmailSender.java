package com.nexus.hotelpms.infraestructura.correo;

import com.nexus.hotelpms.aplicacion.puertos.IEmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Envío de correo SIMULADO: escribe en el log lo que se enviaría. Reemplazable por JavaMailSender real. */
@Component
public class SmtpEmailSender implements IEmailSender {
    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    @Override
    public void enviar(String destino, String asunto, String cuerpo, String nombreAdjunto, byte[] adjunto) {
        log.info("[CORREO SIMULADO] para={} asunto='{}' adjunto={} ({} bytes)\n{}", destino, asunto,
                nombreAdjunto, adjunto == null ? 0 : adjunto.length, cuerpo);
    }
}
