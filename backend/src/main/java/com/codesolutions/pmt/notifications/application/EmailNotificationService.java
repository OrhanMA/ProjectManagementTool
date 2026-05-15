package com.codesolutions.pmt.notifications.application;

import com.codesolutions.pmt.tasks.domain.Task;
import com.codesolutions.pmt.users.domain.UserAccount;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {
  private final JavaMailSender mailSender;

  public EmailNotificationService(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  public void sendTaskAssigned(UserAccount assignee, Task task) {
    MimeMessage message = mailSender.createMimeMessage();
    try {
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom("notifications@pmt.local");
      helper.setTo(assignee.email());
      helper.setSubject("PMT - Nouvelle tâche assignée");
      helper.setText(plainText(assignee, task), html(assignee, task));
      message.saveChanges();
      mailSender.send(message);
    } catch (MessagingException exception) {
      throw new IllegalStateException("Impossible de préparer l'email d'assignation.", exception);
    }
  }

  private String plainText(UserAccount assignee, Task task) {
    return "Bonjour "
        + assignee.username()
        + ",\n\nLa tâche \""
        + task.name()
        + "\" vous a été assignée.\n\n"
        + "Projet: "
        + task.project().name()
        + "\nPriorité: "
        + task.priority()
        + "\nStatut: "
        + task.status()
        + "\nÉchéance: "
        + task.dueDate();
  }

  private String html(UserAccount assignee, Task task) {
    return """
        <!doctype html>
        <html lang="fr">
          <body style="margin:0;background-color:#f4f5f7;font-family:Arial,Helvetica,sans-serif;color:#172b4d;">
            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background-color:#f4f5f7;">
              <tr>
                <td align="center" style="padding-top:28px;padding-right:16px;padding-bottom:28px;padding-left:16px;">
                  <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="width:600px;max-width:100%%;background-color:#ffffff;border:1px solid #dfe1e6;border-radius:8px;">
                    <tr>
                      <td style="padding-top:22px;padding-right:24px;padding-bottom:18px;padding-left:24px;background-color:#0747a6;border-radius:8px 8px 0 0;">
                        <p style="margin:0;color:#deebff;font-size:12px;line-height:18px;font-weight:700;">PMT</p>
                        <h1 style="margin:4px 0 0;color:#ffffff;font-size:22px;line-height:30px;">Nouvelle tâche assignée</h1>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding-top:24px;padding-right:24px;padding-bottom:8px;padding-left:24px;">
                        <p style="margin:0 0 14px;color:#172b4d;font-size:15px;line-height:24px;">Bonjour %s,</p>
                        <p style="margin:0;color:#172b4d;font-size:15px;line-height:24px;">La tâche suivante vous a été assignée.</p>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding-top:12px;padding-right:24px;padding-bottom:12px;padding-left:24px;">
                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background-color:#f7f8fa;border:1px solid #dfe1e6;border-radius:8px;">
                          <tr>
                            <td style="padding-top:16px;padding-right:18px;padding-bottom:16px;padding-left:18px;">
                              <p style="margin:0;color:#6b778c;font-size:12px;line-height:18px;font-weight:700;">%s</p>
                              <h2 style="margin:4px 0 10px;color:#172b4d;font-size:18px;line-height:26px;">%s</h2>
                              <p style="margin:0;color:#44546f;font-size:14px;line-height:22px;">%s</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding-top:8px;padding-right:24px;padding-bottom:24px;padding-left:24px;">
                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0">
                          <tr>
                            <td style="padding-top:8px;padding-right:8px;padding-bottom:8px;padding-left:0;color:#44546f;font-size:13px;line-height:20px;"><strong>Priorité</strong><br>%s</td>
                            <td style="padding-top:8px;padding-right:8px;padding-bottom:8px;padding-left:8px;color:#44546f;font-size:13px;line-height:20px;"><strong>Statut</strong><br>%s</td>
                            <td style="padding-top:8px;padding-right:0;padding-bottom:8px;padding-left:8px;color:#44546f;font-size:13px;line-height:20px;"><strong>Échéance</strong><br>%s</td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
            </table>
          </body>
        </html>
        """
        .formatted(
            escape(assignee.username()),
            escape(task.project().name()),
            escape(task.name()),
            escape(task.description()),
            escape(String.valueOf(task.priority())),
            escape(String.valueOf(task.status())),
            escape(String.valueOf(task.dueDate())));
  }

  private String escape(String value) {
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }
}
