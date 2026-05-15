package com.codesolutions.pmt.notifications.application;

import com.codesolutions.pmt.tasks.domain.Task;
import com.codesolutions.pmt.users.domain.UserAccount;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {
  private final JavaMailSender mailSender;

  public EmailNotificationService(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  public void sendTaskAssigned(UserAccount assignee, Task task) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom("notifications@pmt.local");
    message.setTo(assignee.email());
    message.setSubject("PMT - Nouvelle tache assignee");
    message.setText(
        "Bonjour "
            + assignee.username()
            + ",\n\nLa tache \""
            + task.name()
            + "\" vous a ete assignee.\n\nProjet PMT");
    mailSender.send(message);
  }
}
