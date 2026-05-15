package com.codesolutions.pmt.auth.domain;

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
@Table(name = "refresh_tokens")
public class RefreshToken {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 36)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private UserAccount user;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected RefreshToken() {}

  private RefreshToken(UserAccount user, String tokenHash, Instant expiresAt, Instant createdAt) {
    this.user = user;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
    this.createdAt = createdAt;
  }

  public static RefreshToken create(
      UserAccount user, String tokenHash, Instant expiresAt, Instant createdAt) {
    return new RefreshToken(user, tokenHash, expiresAt, createdAt);
  }

  public UserAccount user() {
    return user;
  }

  public boolean isUsable(Instant now) {
    return revokedAt == null && expiresAt.isAfter(now);
  }

  public void revoke(Instant now) {
    this.revokedAt = now;
  }
}
