package com.codesolutions.pmt.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.codesolutions.pmt.users.domain.UserAccount;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RefreshTokenTests {
  @Test
  void isUsableOnlyWhenNotRevokedAndNotExpired() {
    Instant now = Instant.parse("2026-05-15T10:00:00Z");
    UserAccount user = UserAccount.register("alice", "alice@pmt.local", "hash", now);
    RefreshToken usable = RefreshToken.create(user, "hash", now.plusSeconds(60), now);
    RefreshToken expired = RefreshToken.create(user, "hash2", now.minusSeconds(60), now);

    assertThat(usable.isUsable(now)).isTrue();
    assertThat(expired.isUsable(now)).isFalse();

    usable.revoke(now);

    assertThat(usable.isUsable(now)).isFalse();
  }
}
