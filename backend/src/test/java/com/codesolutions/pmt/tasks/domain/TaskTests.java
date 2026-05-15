package com.codesolutions.pmt.tasks.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.codesolutions.pmt.projects.domain.Project;
import com.codesolutions.pmt.shared.domain.TaskPriority;
import com.codesolutions.pmt.shared.domain.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TaskTests {
  @Test
  void marksEndDateWhenTaskIsDone() {
    Task task = task();

    task.changeStatus(TaskStatus.DONE, Instant.parse("2026-05-15T10:00:00Z"));

    assertThat(task.status()).isEqualTo(TaskStatus.DONE);
    assertThat(task.endDate()).isNotNull();
  }

  @Test
  void keepsEndDateEmptyForNonDoneStatus() {
    Task task = task();

    task.changeStatus(TaskStatus.DOING, Instant.parse("2026-05-15T10:00:00Z"));

    assertThat(task.status()).isEqualTo(TaskStatus.DOING);
    assertThat(task.endDate()).isNull();
  }

  private static Task task() {
    return Task.create(
        Project.create(
            "Projet",
            "Description",
            LocalDate.parse("2026-05-15"),
            Instant.parse("2026-05-15T10:00:00Z")),
        "Tache",
        "Description",
        LocalDate.parse("2026-05-20"),
        TaskPriority.MEDIUM,
        null,
        Instant.parse("2026-05-15T10:00:00Z"));
  }
}
