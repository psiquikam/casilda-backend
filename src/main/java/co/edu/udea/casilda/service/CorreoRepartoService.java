package co.edu.udea.casilda.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class CorreoRepartoService {
    private final JavaMailSender mailSender;
    private final boolean habilitado;
    private final String usuario;
    private final String clave;
    private final String remitente;
    private final String destinatarios;

    public CorreoRepartoService(JavaMailSender mailSender,
            @Value("${app.correo-reparto.habilitado:false}") boolean habilitado,
            @Value("${spring.mail.username:}") String usuario,
            @Value("${spring.mail.password:}") String clave,
            @Value("${app.correo-reparto.remitente:}") String remitente,
            @Value("${app.correo-reparto.destinatarios:}") String destinatarios) {
        this.mailSender = mailSender;
        this.habilitado = habilitado;
        this.usuario = usuario;
        this.clave = clave;
        this.remitente = remitente;
        this.destinatarios = destinatarios;
    }

    public String notificar(Long solicitudId) {
        if (!habilitado) {
            return "DESACTIVADO";
        }

        String[] correos = destinatarios.split(",", -1);
        if (usuario.isBlank() || clave.isBlank() || remitente.isBlank() || correos.length != 2) {
            log.error("Correo de reparto: configure el remitente, la clave y dos destinatarios");
            return "ERROR";
        }

        try {
            new InternetAddress(remitente.trim(), true).validate();
            for (int i = 0; i < correos.length; i++) {
                correos[i] = correos[i].trim();
                InternetAddress direccion = new InternetAddress(correos[i], true);
                direccion.validate();
            }
        } catch (AddressException ex) {
            log.error("Correo de reparto: destinatarios inválidos", ex);
            return "ERROR";
        }

        if (correos[0].equalsIgnoreCase(correos[1])) {
            log.error("Correo de reparto: los destinatarios deben ser diferentes");
            return "ERROR";
        }

        boolean error = false;
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String texto = """
                CASILDA | Equipo de Atención

                Confirmación de asignación

                Se ha realizado el reparto de una solicitud al equipo responsable.

                Estado: Asignada
                Fecha de notificación: %s
                Próximo paso: El equipo de atención puede consultar la asignación en CASILDA.

                Por confidencialidad, los datos personales y los detalles de la solicitud
                se encuentran disponibles únicamente en la plataforma.

                Equipo de Atención | CASILDA
                Mensaje automático, por favor no responder.
                """.formatted(fecha);
        String html = """
                <!DOCTYPE html>
                <html lang="es"><body style="margin:0;padding:24px;background:#f2f5f3;font-family:Arial,sans-serif;color:#26352e">
                  <div style="max-width:620px;margin:auto;background:#fff;border:1px solid #dfe9e2;border-radius:8px;overflow:hidden">
                    <div style="background:#175c3a;color:#fff;padding:24px 32px">
                      <div style="font-size:22px;font-weight:bold;letter-spacing:1px">CASILDA</div>
                      <div style="font-size:14px;margin-top:6px">Equipo de Atención</div>
                    </div>
                    <div style="padding:30px 32px">
                      <h1 style="font-size:22px;margin:0 0 16px;color:#175c3a">Confirmación de asignación</h1>
                      <p style="font-size:15px;line-height:1.6">Le informamos que se ha realizado el reparto de una solicitud al equipo responsable.</p>
                      <div style="background:#f1f7f3;border-left:4px solid #33865a;padding:16px 20px;margin:22px 0;font-size:14px;line-height:1.8">
                        <strong>Estado:</strong> Asignada<br>
                        <strong>Fecha de notificación:</strong> %s<br>
                        <strong>Próximo paso:</strong> El equipo de atención puede consultar la asignación en CASILDA.
                      </div>
                      <p style="font-size:14px;line-height:1.6">Por confidencialidad, los datos personales y los detalles de la solicitud se encuentran disponibles únicamente en la plataforma.</p>
                      <p style="font-size:14px;line-height:1.6;margin-bottom:0">Cordialmente,<br><strong>Equipo de Atención | CASILDA</strong></p>
                    </div>
                    <div style="background:#f6f8f6;padding:16px 32px;color:#65736b;font-size:12px">Mensaje automático, por favor no responder.</div>
                  </div>
                </body></html>
                """.formatted(fecha);
        for (String correo : correos) {
            try {
                MimeMessage mensaje = mailSender.createMimeMessage();
                MimeMessageHelper contenido = new MimeMessageHelper(mensaje, true, "UTF-8");
                contenido.setFrom(remitente.trim());
                contenido.setTo(correo);
                contenido.setSubject("CASILDA | Confirmación de asignación");
                contenido.setText(texto, html);
                mailSender.send(mensaje);
            } catch (MailException | MessagingException ex) {
                log.error("No fue posible enviar la notificación de reparto de SOL-{}", solicitudId, ex);
                error = true;
            }
        }
        return error ? "ERROR" : "ENVIADO";
    }
}
