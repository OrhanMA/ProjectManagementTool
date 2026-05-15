package com.codesolutions.pmt.users.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "users")
public class UserAccount {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 36)
  private UUID id;

  @Column(nullable = false, unique = true, length = 80)
  private String username;

  @Column(nullable = false, unique = true, length = 180)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 120)
  private String passwordHash;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected UserAccount() {}

  private UserAccount(String username, String email, String passwordHash, Instant createdAt) {
    this.username = username;
    this.email = email.toLowerCase();
    this.passwordHash = passwordHash;
    this.createdAt = createdAt;
  }

  public static UserAccount register(
      String username, String email, String passwordHash, Instant createdAt) {
    return new UserAccount(username.trim(), email.trim(), passwordHash, createdAt);
  }

  public UUID id() {
    return id;
  }

  public String username() {
    return username;
  }

  public String email() {
    return email;
  }

  public String passwordHash() {
    return passwordHash;
  }

  public Instant createdAt() {
    return createdAt;
  }
}
