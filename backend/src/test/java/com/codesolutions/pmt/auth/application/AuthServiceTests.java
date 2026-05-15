package com.codesolutions.pmt.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codesolutions.pmt.auth.domain.RefreshToken;
import com.codesolutions.pmt.auth.domain.RefreshTokenRepository;
import com.codesolutions.pmt.shared.domain.BusinessException;
import com.codesolutions.pmt.shared.security.JwtService;
import com.codesolutions.pmt.users.domain.UserAccount;
import com.codesolutions.pmt.users.domain.UserRepository;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTests {
  private final UserRepository users = mock(UserRepository.class);
  private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
  private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
  private final JwtService jwtService = mock(JwtService.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-05-15T10:00:00Z"), ZoneOffset.UTC);
  private AuthService service;

  @BeforeEach
  void setUp() {
    service = new AuthService(users, refreshTokens, passwordEncoder, jwtService, clock, 14);
    when(passwordEncoder.encode(anyString())).thenReturn("encoded");
    when(jwtService.createAccessToken(any())).thenReturn("access-token");
  }

  @Test
  void registersAValidUser() throws Exception {
    UserAccount saved = user("alice", "alice@pmt.local");
    when(users.save(any(UserAccount.class))).thenReturn(saved);

    AuthService.AuthTokens tokens = service.register("alice", "alice@pmt.local", "Password123!");

    assertThat(tokens.accessToken()).isEqualTo("access-token");
    assertThat(tokens.user().email()).isEqualTo("alice@pmt.local");
    verify(refreshTokens).save(any(RefreshToken.class));
  }

  @Test
  void rejectsWeakPasswordsAndDuplicateAccounts() {
    assertThatThrownBy(() -> service.register("alice", "alice@pmt.local", "short"))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> service.register("alice", "alice@pmt.local", "password123!"))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> service.register("alice", "alice@pmt.local", "PASSWORD123!"))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> service.register("alice", "alice@pmt.local", "Password!!!"))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> service.register("alice", "alice@pmt.local", "Password123"))
        .isInstanceOf(BusinessException.class);

    when(users.existsByEmailIgnoreCase("alice@pmt.local")).thenReturn(true);
    assertThatThrownBy(() -> service.register("alice", "alice@pmt.local", "Password123!"))
        .isInstanceOf(BusinessException.class);

    when(users.existsByEmailIgnoreCase("alice@pmt.local")).thenReturn(false);
    when(users.existsByUsernameIgnoreCase("alice")).thenReturn(true);
    assertThatThrownBy(() -> service.register("alice", "alice@pmt.local", "Password123!"))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void logsInWithValidCredentialsAndRejectsInvalidOnes() throws Exception {
    UserAccount user = user("alice", "alice@pmt.local");
    when(users.findByEmailIgnoreCase("alice@pmt.local")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("Password123!", user.passwordHash())).thenReturn(true);

    assertThat(service.login("alice@pmt.local", "Password123!").accessToken())
        .isEqualTo("access-token");

    when(passwordEncoder.matches("bad", user.passwordHash())).thenReturn(false);
    assertThatThrownBy(() -> service.login("alice@pmt.local", "bad"))
        .isInstanceOf(BusinessException.class);

    when(users.findByEmailIgnoreCase("missing@pmt.local")).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.login("missing@pmt.local", "Password123!"))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void refreshesOnlyUsableTokensAndIgnoresBlankLogout() throws Exception {
    UserAccount user = user("alice", "alice@pmt.local");
    RefreshToken usable =
        RefreshToken.create(user, "hash", clock.instant().plusSeconds(60), clock.instant());
    RefreshToken expired =
        RefreshToken.create(user, "hash2", clock.instant().minusSeconds(60), clock.instant());
    when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.of(usable));

    assertThat(service.refresh("raw-token").accessToken()).isEqualTo("access-token");

    when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.of(expired));
    assertThatThrownBy(() -> service.refresh("raw-token")).isInstanceOf(BusinessException.class);

    when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.refresh("raw-token")).isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> service.refresh("")).isInstanceOf(BusinessException.class);

    service.logout("");

    verify(refreshTokens, never()).findByTokenHash("");
  }

  private UserAccount user(String username, String email) throws Exception {
    UserAccount user = UserAccount.register(username, email, "hash", clock.instant());
    Field field = UserAccount.class.getDeclaredField("id");
    field.setAccessible(true);
    field.set(user, UUID.randomUUID());
    return user;
  }
}
