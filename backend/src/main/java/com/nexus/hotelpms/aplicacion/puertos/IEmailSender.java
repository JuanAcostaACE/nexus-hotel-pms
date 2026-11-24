package com.nexus.hotelpms.aplicacion.puertos;

public interface IEmailSender {
    void enviar(String destino, String asunto, String cuerpo, String nombreAdjunto, byte[] adjunto);
}
