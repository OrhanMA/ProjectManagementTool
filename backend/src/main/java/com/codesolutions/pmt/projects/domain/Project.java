package com.codesolutions.pmt.projects.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "projects")
public class Project {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 36)
  private UUID id;

  @Column(nullable = false, length = 140)
  private String name;

  @Column(nullable = false, length = 1000)
  private String description;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected Project() {}

  private Project(String name, String description, LocalDate startDate, Instant createdAt) {
    this.name = name.trim();
    this.description = description.trim();
    this.startDate = startDate;
    this.createdAt = createdAt;
  }

  public static Project create(
      String name, String description, LocalDate startDate, Instant createdAt) {
    return new Project(name, description, startDate, createdAt);
  }

  public UUID id() {
    return id;
  }

  public String name() {
    return name;
  }

  public String description() {
    return description;
  }

  public LocalDate startDate() {
    return startDate;
  }

  public Instant createdAt() {
    return createdAt;
  }
}
