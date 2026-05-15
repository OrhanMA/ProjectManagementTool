package com.codesolutions.pmt.projects.domain;

import com.codesolutions.pmt.shared.domain.ProjectRole;
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
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "project_memberships")
public class ProjectMembership {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 36)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "project_id")
  private Project project;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private UserAccount user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private ProjectRole role;

  @Column(name = "joined_at", nullable = false)
  private Instant joinedAt;

  protected ProjectMembership() {}

  private ProjectMembership(Project project, UserAccount user, ProjectRole role, Instant joinedAt) {
    this.project = project;
    this.user = user;
    this.role = role;
    this.joinedAt = joinedAt;
  }

  public static ProjectMembership create(
      Project project, UserAccount user, ProjectRole role, Instant joinedAt) {
    return new ProjectMembership(project, user, role, joinedAt);
  }

  public UUID id() {
    return id;
  }

  public Project project() {
    return project;
  }

  public UserAccount user() {
    return user;
  }

  public ProjectRole role() {
    return role;
  }

  public void changeRole(ProjectRole role) {
    this.role = role;
  }
}
