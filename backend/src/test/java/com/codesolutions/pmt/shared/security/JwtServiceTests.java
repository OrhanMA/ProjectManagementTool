package com.codesolutions.pmt.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.codesolutions.pmt.users.domain.UserAccount;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTests {
  private final Clock clock = Clock.fixed(Instant.parse("2026-05-15T10:00:00Z"), ZoneOffset.UTC);

  @Test
  void createsAndParsesAValidToken() throws Exception {
    JwtService service =
        new JwtService(new ObjectMapper(), clock, "secret-secret-secret-secret", 15);
    UserAccount user = UserAccount.register("alice", "alice@pmt.local", "hash", clock.instant());
    setId(user, UUID.fromString("11111111-1111-1111-1111-111111111111"));

    AuthenticatedUser parsed = service.parse(service.createAccessToken(user)).orElseThrow();

    assertThat(parsed.id()).isEqualTo(user.id());
    assertThat(parsed.email()).isEqualTo("alice@pmt.local");
  }

  @Test
  void rejectsMalformedTamperedAndExpiredTokens() throws Exception {
    JwtService validService =
        new JwtService(new ObjectMapper(), clock, "secret-secret-secret-secret", 15);
    JwtService expiredService =
        new JwtService(new ObjectMapper(), clock, "secret-secret-secret-secret", -1);
    UserAccount user = UserAccount.register("alice", "alice@pmt.local", "hash", clock.instant());
    setId(user, UUID.fromString("11111111-1111-1111-1111-111111111111"));
    String validToken = validService.createAccessToken(user);

    assertThat(validService.parse("bad-token")).isEmpty();
    assertThat(validService.parse(validToken + "tampered")).isEmpty();
    assertThat(validService.parse(expiredService.createAccessToken(user))).isEmpty();
  }

  private static void setId(UserAccount user, UUID id) throws Exception {
    Field field = UserAccount.class.getDeclaredField("id");
    field.setAccessible(true);
    field.set(user, id);
  }
}
