package com.codesolutions.pmt.auth.application;

import com.codesolutions.pmt.auth.domain.RefreshToken;
import com.codesolutions.pmt.auth.domain.RefreshTokenRepository;
import com.codesolutions.pmt.shared.domain.BusinessException;
import com.codesolutions.pmt.shared.security.JwtService;
import com.codesolutions.pmt.users.domain.UserAccount;
import com.codesolutions.pmt.users.domain.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final UserRepository users;
  private final RefreshTokenRepository refreshTokens;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final Clock clock;
  private final SecureRandom secureRandom = new SecureRandom();
  private final Duration refreshTokenLifetime;

  public AuthService(
      UserRepository users,
      RefreshTokenRepository refreshTokens,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      Clock clock,
      @Value("${pmt.security.refresh-token-days}") long refreshTokenDays) {
    this.users = users;
    this.refreshTokens = refreshTokens;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.clock = clock;
    this.refreshTokenLifetime = Duration.ofDays(refreshTokenDays);
  }

  @Transactional
  public AuthTokens register(String username, String email, String password) {
    validatePassword(password);
    if (users.existsByEmailIgnoreCase(email)) {
      throw BusinessException.conflict("Cette adresse email est deja utilisee.");
    }
    if (users.existsByUsernameIgnoreCase(username)) {
      throw BusinessException.conflict("Ce nom d'utilisateur est deja utilise.");
    }
    UserAccount user =
        users.save(
            UserAccount.register(
                username, email, passwordEncoder.encode(password), clock.instant()));
    return issueTokens(user);
  }

  @Transactional
  public AuthTokens login(String email, String password) {
    UserAccount user =
        users
            .findByEmailIgnoreCase(email)
            .orElseThrow(() -> BusinessException.badRequest("Identifiants invalides."));
    if (!passwordEncoder.matches(password, user.passwordHash())) {
      throw BusinessException.badRequest("Identifiants invalides.");
    }
    return issueTokens(user);
  }

  @Transactional
  public AuthTokens refresh(String rawRefreshToken) {
    if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
      throw BusinessException.badRequest("Refresh token manquant.");
    }
    RefreshToken existing =
        refreshTokens
            .findByTokenHash(hash(rawRefreshToken))
            .orElseThrow(() -> BusinessException.badRequest("Refresh token invalide."));
    if (!existing.isUsable(clock.instant())) {
      throw BusinessException.badRequest("Refresh token invalide.");
    }
    existing.revoke(clock.instant());
    return issueTokens(existing.user());
  }

  @Transactional
  public void logout(String rawRefreshToken) {
    if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
      return;
    }
    refreshTokens
        .findByTokenHash(hash(rawRefreshToken))
        .ifPresent(token -> token.revoke(clock.instant()));
  }

  private AuthTokens issueTokens(UserAccount user) {
    String rawRefreshToken = randomToken();
    Instant now = clock.instant();
    refreshTokens.save(
        RefreshToken.create(user, hash(rawRefreshToken), now.plus(refreshTokenLifetime), now));
    return new AuthTokens(
        jwtService.createAccessToken(user),
        rawRefreshToken,
        refreshTokenLifetime,
        new UserProfile(user.id().toString(), user.username(), user.email()));
  }

  private void validatePassword(String password) {
    if (password == null
        || password.length() < 8
        || !password.matches(".*[A-Z].*")
        || !password.matches(".*[a-z].*")
        || !password.matches(".*\\d.*")
        || !password.matches(".*[^A-Za-z0-9].*")) {
      throw BusinessException.badRequest(
          "Le mot de passe doit contenir au moins 8 caracteres, une majuscule, une minuscule, un chiffre et un caractere special.");
    }
  }

  private String randomToken() {
    byte[] bytes = new byte[48];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static String hash(String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder builder = new StringBuilder();
      for (byte b : digest) {
        builder.append(String.format("%02x", b));
      }
      return builder.toString();
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to hash refresh token", exception);
    }
  }

  public record AuthTokens(
      String accessToken, String refreshToken, Duration refreshTokenLifetime, UserProfile user) {}

  public record UserProfile(String id, String username, String email) {}
}
