package com.codesolutions.pmt.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesolutions.pmt.shared.domain.BusinessException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityContextReaderTests {
  private final SecurityContextReader reader = new SecurityContextReader();

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void returnsAuthenticatedUserFromSecurityContext() {
    AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), "alice", "alice@pmt.local");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

    assertThat(reader.currentUser()).isEqualTo(user);
  }

  @Test
  void rejectsMissingOrUnexpectedAuthentication() {
    assertThatThrownBy(reader::currentUser).isInstanceOf(BusinessException.class);

    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("anonymous", null));

    assertThatThrownBy(reader::currentUser).isInstanceOf(BusinessException.class);
  }
}
