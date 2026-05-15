package com.codesolutions.pmt.auth.api;

import com.codesolutions.pmt.auth.application.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private static final String REFRESH_COOKIE = "pmt_refresh_token";

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
    AuthService.AuthTokens tokens =
        authService.register(request.username(), request.email(), request.password());
    addRefreshCookie(response, tokens);
    return AuthResponse.from(tokens);
  }

  @PostMapping("/login")
  AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
    AuthService.AuthTokens tokens = authService.login(request.email(), request.password());
    addRefreshCookie(response, tokens);
    return AuthResponse.from(tokens);
  }

  @PostMapping("/refresh")
  AuthResponse refresh(
      @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
      HttpServletResponse response) {
    AuthService.AuthTokens tokens = authService.refresh(refreshToken);
    addRefreshCookie(response, tokens);
    return AuthResponse.from(tokens);
  }

  @PostMapping("/logout")
  void logout(
      @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
      HttpServletResponse response) {
    authService.logout(refreshToken);
    ResponseCookie cookie =
        ResponseCookie.from(REFRESH_COOKIE, "")
            .httpOnly(true)
            .secure(false)
            .sameSite("Lax")
            .path("/api/v1/auth")
            .maxAge(0)
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  private static void addRefreshCookie(
      HttpServletResponse response, AuthService.AuthTokens tokens) {
    ResponseCookie cookie =
        ResponseCookie.from(REFRESH_COOKIE, tokens.refreshToken())
            .httpOnly(true)
            .secure(false)
            .sameSite("Lax")
            .path("/api/v1/auth")
            .maxAge(tokens.refreshTokenLifetime())
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  public record RegisterRequest(
      @NotBlank @Size(max = 80) String username,
      @NotBlank @Email @Size(max = 180) String email,
      @NotBlank String password) {}

  public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

  public record AuthResponse(String accessToken, UserResponse user) {
    static AuthResponse from(AuthService.AuthTokens tokens) {
      return new AuthResponse(
          tokens.accessToken(),
          new UserResponse(tokens.user().id(), tokens.user().username(), tokens.user().email()));
    }
  }

  public record UserResponse(String id, String username, String email) {}
}
