package com.nexus.hotelpms.infraestructura.correo;

import com.nexus.hotelpms.aplicacion.puertos.IEmailSender;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Salida SMTP del correo. Si hay un servidor configurado ({@code spring.mail.host}, variable SMTP_HOST) envía de
 * verdad; si no, escribe en el log lo que enviaría (modo demostración).
 */
@Component
public class SmtpEmailSender implements IEmailSender {
    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final ObjectProvider<JavaMailSender> servidor;
    private final String remitente;

    public SmtpEmailSender(ObjectProvider<JavaMailSender> servidor,
                           @Value("${hotel.correo-remitente:no-reply@nexus-hotel.local}") String remitente) {
        this.servidor = servidor;
        this.remitente = remitente;
    }

    public boolean smtpConfigurado() { return servidor.getIfAvailable() != null; }

    @Override
    public void enviar(String destino, String asunto, String cuerpo, String nombreAdjunto, byte[] adjunto) {
        JavaMailSender smtp = servidor.getIfAvailable();
        if (smtp == null) {
            log.info("[CORREO SIMULADO] para={} asunto='{}' adjunto={} ({} bytes)\n{}", destino, asunto,
                    nombreAdjunto, adjunto == null ? 0 : adjunto.length, cuerpo);
            return;
        }
        try {
            MimeMessage mensaje = smtp.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(mensaje, adjunto != null, "UTF-8");
            h.setFrom(remitente);
            h.setTo(destino);
            h.setSubject(asunto);
            h.setText(cuerpo);
            if (adjunto != null) {
                h.addAttachment(nombreAdjunto == null ? "documento.pdf" : nombreAdjunto, new ByteArrayResource(adjunto));
            }
            smtp.send(mensaje);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo enviar el correo: " + e.getMessage(), e);
        }
    }
}
