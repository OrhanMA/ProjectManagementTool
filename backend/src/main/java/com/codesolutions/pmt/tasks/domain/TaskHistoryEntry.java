package com.codesolutions.pmt.tasks.domain;

import com.codesolutions.pmt.users.domain.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "task_history_entries")
public class TaskHistoryEntry {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 36)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "task_id")
  private Task task;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "actor_id")
  private UserAccount actor;

  @Column(name = "field_name", nullable = false, length = 80)
  private String fieldName;

  @Column(name = "old_value", length = 2000)
  private String oldValue;

  @Column(name = "new_value", length = 2000)
  private String newValue;

  @Column(name = "changed_at", nullable = false)
  private Instant changedAt;

  protected TaskHistoryEntry() {}

  private TaskHistoryEntry(
      Task task,
      UserAccount actor,
      String fieldName,
      String oldValue,
      String newValue,
      Instant changedAt) {
    this.task = task;
    this.actor = actor;
    this.fieldName = fieldName;
    this.oldValue = oldValue;
    this.newValue = newValue;
    this.changedAt = changedAt;
  }

  public static TaskHistoryEntry record(
      Task task,
      UserAccount actor,
      String fieldName,
      String oldValue,
      String newValue,
      Instant changedAt) {
    return new TaskHistoryEntry(task, actor, fieldName, oldValue, newValue, changedAt);
  }

  public String fieldName() {
    return fieldName;
  }

  public String oldValue() {
    return oldValue;
  }

  public String newValue() {
    return newValue;
  }

  public Instant changedAt() {
    return changedAt;
  }

  public UserAccount actor() {
    return actor;
  }
}
