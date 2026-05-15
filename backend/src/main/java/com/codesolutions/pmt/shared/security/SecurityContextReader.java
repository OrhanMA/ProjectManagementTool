package com.codesolutions.pmt.shared.security;

import com.codesolutions.pmt.shared.domain.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextReader {
  public AuthenticatedUser currentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
      throw new BusinessException(HttpStatus.UNAUTHORIZED, "Authentification requise.");
    }
    return user;
  }
}
