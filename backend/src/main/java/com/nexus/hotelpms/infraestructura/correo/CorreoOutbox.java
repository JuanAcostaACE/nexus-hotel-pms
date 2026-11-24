package com.nexus.hotelpms.infraestructura.correo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** Correo en cola (patrón Outbox): se guarda al generarse y un job lo publica después. */
@Document("outbox_correos")
public class CorreoOutbox {
    public enum Estado { Pendiente, Enviado, Fallido }

    public static final int MAX_INTENTOS = 5;

    @Id private String id;
    private String destino;
    private String asunto;
    private String cuerpo;
    private String nombreAdjunto;
    private byte[] adjunto;
    @Indexed private Estado estado = Estado.Pendiente;
    private int intentos;
    private String ultimoError;
    private Instant creadoAt;
    private Instant enviadoAt;

    protected CorreoOutbox() { }

    public CorreoOutbox(String destino, String asunto, String cuerpo, String nombreAdjunto, byte[] adjunto, Instant ahora) {
        this.destino = destino;
        this.asunto = asunto;
        this.cuerpo = cuerpo;
        this.nombreAdjunto = nombreAdjunto;
        this.adjunto = adjunto;
        this.creadoAt = ahora;
    }

    public void marcarEnviado(Instant ahora) {
        this.estado = Estado.Enviado;
        this.enviadoAt = ahora;
        this.adjunto = null; // el PDF ya salió: no se conserva en la cola
        this.ultimoError = null;
    }

    public void registrarFallo(String error) {
        this.intentos++;
        this.ultimoError = error;
        if (intentos >= MAX_INTENTOS) {
            this.estado = Estado.Fallido;
        }
    }

    public String getId() { return id; }
    public String getDestino() { return destino; }
    public String getAsunto() { return asunto; }
    public String getCuerpo() { return cuerpo; }
    public String getNombreAdjunto() { return nombreAdjunto; }
    public byte[] getAdjunto() { return adjunto; }
    public Estado getEstado() { return estado; }
    public int getIntentos() { return intentos; }
    public String getUltimoError() { return ultimoError; }
    public Instant getCreadoAt() { return creadoAt; }
    public Instant getEnviadoAt() { return enviadoAt; }
}
