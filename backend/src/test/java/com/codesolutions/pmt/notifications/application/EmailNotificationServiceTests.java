package com.codesolutions.pmt.notifications.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codesolutions.pmt.projects.domain.Project;
import com.codesolutions.pmt.shared.domain.TaskPriority;
import com.codesolutions.pmt.tasks.domain.Task;
import com.codesolutions.pmt.users.domain.UserAccount;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;

class EmailNotificationServiceTests {
  private final JavaMailSender mailSender = org.mockito.Mockito.mock(JavaMailSender.class);
  private final EmailNotificationService service = new EmailNotificationService(mailSender);

  @Test
  void sendsStyledHtmlNotificationWhenTaskIsAssigned() throws Exception {
    MimeMessage message = new MimeMessage(jakarta.mail.Session.getInstance(new Properties()));
    when(mailSender.createMimeMessage()).thenReturn(message);

    service.sendTaskAssigned(assignee(), task());

    ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).createMimeMessage();
    verify(mailSender).send(captor.capture());
    MimeMessage sent = captor.getValue();
    String body = body(sent);

    assertThat(sent.getSubject()).isEqualTo("PMT - Nouvelle tâche assignée");
    assertThat(sent.getAllRecipients()[0].toString()).isEqualTo("marc.member@pmt.local");
    assertThat(sent.getContentType()).contains("multipart");
    assertThat(body).contains("Nouvelle tâche assignée");
    assertThat(body).contains("background-color:#0747a6");
    assertThat(body).contains("Concevoir le modèle");
    assertThat(body).contains("Projet PMT");
  }

  private String body(MimeMessage message) throws Exception {
    return textFrom(message.getContent());
  }

  private String textFrom(Object content) throws Exception {
    if (content instanceof String text) {
      return text;
    }
    if (content instanceof Multipart multipart) {
      StringBuilder result = new StringBuilder();
      for (int index = 0; index < multipart.getCount(); index++) {
        BodyPart part = multipart.getBodyPart(index);
        result.append(textFrom(part.getContent()));
      }
      return result.toString();
    }
    return "";
  }

  private UserAccount assignee() {
    return UserAccount.register(
        "marc.member", "marc.member@pmt.local", "hash", Instant.parse("2026-05-15T10:00:00Z"));
  }

  private Task task() {
    return Task.create(
        Project.create(
            "Projet PMT",
            "Description",
            LocalDate.parse("2026-05-15"),
            Instant.parse("2026-05-15T10:00:00Z")),
        "Concevoir le modèle",
        "Préparer le schéma relationnel.",
        LocalDate.parse("2026-05-22"),
        TaskPriority.HIGH,
        assignee(),
        Instant.parse("2026-05-15T10:00:00Z"));
  }
}
