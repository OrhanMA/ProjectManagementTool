package com.codesolutions.pmt.tasks.domain;

import com.codesolutions.pmt.projects.domain.Project;
import com.codesolutions.pmt.shared.domain.TaskPriority;
import com.codesolutions.pmt.shared.domain.TaskStatus;
import com.codesolutions.pmt.users.domain.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tasks")
public class Task {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 36)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "project_id")
  private Project project;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assignee_id")
  private UserAccount assignee;

  @Column(nullable = false, length = 160)
  private String name;

  @Column(nullable = false, length = 2000)
  private String description;

  @Column(name = "due_date", nullable = false)
  private LocalDate dueDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TaskPriority priority;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TaskStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Task() {}

  private Task(
      Project project,
      String name,
      String description,
      LocalDate dueDate,
      TaskPriority priority,
      UserAccount assignee,
      Instant now) {
    this.project = project;
    this.name = name.trim();
    this.description = description.trim();
    this.dueDate = dueDate;
    this.priority = priority;
    this.assignee = assignee;
    this.status = TaskStatus.BACKLOG;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public static Task create(
      Project project,
      String name,
      String description,
      LocalDate dueDate,
      TaskPriority priority,
      UserAccount assignee,
      Instant now) {
    return new Task(project, name, description, dueDate, priority, assignee, now);
  }

  public UUID id() {
    return id;
  }

  public Project project() {
    return project;
  }

  public UserAccount assignee() {
    return assignee;
  }

  public String name() {
    return name;
  }

  public String description() {
    return description;
  }

  public LocalDate dueDate() {
    return dueDate;
  }

  public LocalDate endDate() {
    return endDate;
  }

  public TaskPriority priority() {
    return priority;
  }

  public TaskStatus status() {
    return status;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public void rename(String name, Instant now) {
    this.name = name.trim();
    this.updatedAt = now;
  }

  public void changeDescription(String description, Instant now) {
    this.description = description.trim();
    this.updatedAt = now;
  }

  public void changeDueDate(LocalDate dueDate, Instant now) {
    this.dueDate = dueDate;
    this.updatedAt = now;
  }

  public void changeEndDate(LocalDate endDate, Instant now) {
    this.endDate = endDate;
    this.updatedAt = now;
  }

  public void changePriority(TaskPriority priority, Instant now) {
    this.priority = priority;
    this.updatedAt = now;
  }

  public void changeStatus(TaskStatus status, Instant now) {
    this.status = status;
    this.updatedAt = now;
    if (status == TaskStatus.DONE && this.endDate == null) {
      this.endDate = LocalDate.now();
    }
  }

  public void assignTo(UserAccount assignee, Instant now) {
    this.assignee = assignee;
    this.updatedAt = now;
  }
}
