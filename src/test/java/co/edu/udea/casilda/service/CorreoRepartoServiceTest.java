package co.edu.udea.casilda.service;

import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CorreoRepartoServiceTest {
    private final JavaMailSender mailSender = mock(JavaMailSender.class);

    @Test
    void enviaUnMensajeSeparadoACadaDestinatario() throws Exception {
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> new JavaMailSenderImpl().createMimeMessage());
        CorreoRepartoService service = new CorreoRepartoService(
                mailSender, true, "usuario", "clave", "remitente@example.com", "uno@example.com, dos@example.com");

        assertEquals("ENVIADO", service.notificar(42L));

        ArgumentCaptor<MimeMessage> mensajes = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(2)).send(mensajes.capture());
        List<MimeMessage> enviados = mensajes.getAllValues();
        assertEquals("uno@example.com", enviados.get(0).getRecipients(Message.RecipientType.TO)[0].toString());
        assertEquals("dos@example.com", enviados.get(1).getRecipients(Message.RecipientType.TO)[0].toString());
        assertEquals("remitente@example.com", enviados.get(0).getFrom()[0].toString());
        enviados.get(0).saveChanges();
        Multipart contenido = (Multipart) enviados.get(0).getContent();
        Multipart relacionado = (Multipart) contenido.getBodyPart(0).getContent();
        Multipart alternativas = (Multipart) relacionado.getBodyPart(0).getContent();
        assertTrue(alternativas.getBodyPart(0).getContentType().contains("text/plain"));
        assertTrue(alternativas.getBodyPart(1).getContentType().contains("text/html"));
    }

    @Test
    void noEnviaSiEstaDesactivadoOLaConfiguracionEsIncompleta() {
        assertEquals("DESACTIVADO", new CorreoRepartoService(mailSender, false, "", "", "", "").notificar(1L));
        assertEquals("ERROR", new CorreoRepartoService(
                mailSender, true, "usuario", "clave", "remitente@example.com", "uno@example.com,").notificar(1L));
        verifyNoInteractions(mailSender);
    }

    @Test
    void informaErrorParcialYTrataDeEnviarAlOtroDestinatario() {
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> new JavaMailSenderImpl().createMimeMessage());
        doThrow(new MailSendException("error")).doNothing().when(mailSender).send(any(MimeMessage.class));
        CorreoRepartoService service = new CorreoRepartoService(
                mailSender, true, "usuario", "clave", "remitente@example.com", "uno@example.com,dos@example.com");

        assertEquals("ERROR", service.notificar(42L));
        verify(mailSender, times(2)).send(any(MimeMessage.class));
    }
}
