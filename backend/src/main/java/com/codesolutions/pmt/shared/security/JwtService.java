package com.codesolutions.pmt.shared.security;

import com.codesolutions.pmt.users.domain.UserAccount;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private static final String HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final byte[] secret;
  private final long accessTokenMinutes;

  public JwtService(
      ObjectMapper objectMapper,
      Clock clock,
      @Value("${pmt.security.jwt-secret}") String jwtSecret,
      @Value("${pmt.security.access-token-minutes}") long accessTokenMinutes) {
    this.objectMapper = objectMapper;
    this.clock = clock;
    this.secret = jwtSecret.getBytes(StandardCharsets.UTF_8);
    this.accessTokenMinutes = accessTokenMinutes;
  }

  public String createAccessToken(UserAccount user) {
    Instant now = clock.instant();
    Map<String, Object> claims = new LinkedHashMap<>();
    claims.put("sub", user.id().toString());
    claims.put("email", user.email());
    claims.put("username", user.username());
    claims.put("iat", now.getEpochSecond());
    claims.put("exp", now.plusSeconds(accessTokenMinutes * 60).getEpochSecond());
    return sign(encode(HEADER.getBytes(StandardCharsets.UTF_8)) + "." + encode(toJson(claims)));
  }

  public Optional<AuthenticatedUser> parse(String token) {
    try {
      String[] parts = token.split("\\.");
      if (parts.length != 3) {
        return Optional.empty();
      }
      String unsigned = parts[0] + "." + parts[1];
      if (!constantTimeEquals(sign(unsigned), token)) {
        return Optional.empty();
      }
      Map<String, Object> claims =
          objectMapper.readValue(Base64.getUrlDecoder().decode(parts[1]), new TypeReference<>() {});
      long expiresAt = ((Number) claims.get("exp")).longValue();
      if (Instant.ofEpochSecond(expiresAt).isBefore(clock.instant())) {
        return Optional.empty();
      }
      return Optional.of(
          new AuthenticatedUser(
              UUID.fromString((String) claims.get("sub")),
              (String) claims.get("username"),
              (String) claims.get("email")));
    } catch (RuntimeException | java.io.IOException exception) {
      return Optional.empty();
    }
  }

  private String sign(String unsignedToken) {
    return unsignedToken + "." + encode(hmac(unsignedToken));
  }

  private byte[] hmac(String unsignedToken) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret, "HmacSHA256"));
      return mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8));
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to sign JWT", exception);
    }
  }

  private byte[] toJson(Map<String, Object> claims) {
    try {
      return objectMapper.writeValueAsBytes(claims);
    } catch (java.io.IOException exception) {
      throw new IllegalStateException("Unable to serialize JWT claims", exception);
    }
  }

  private static String encode(byte[] value) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
  }

  private static boolean constantTimeEquals(String left, String right) {
    return java.security.MessageDigest.isEqual(
        left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
  }
}
