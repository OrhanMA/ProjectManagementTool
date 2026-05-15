package com.codesolutions.pmt.shared.domain;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {
  private final HttpStatus status;

  public BusinessException(HttpStatus status, String message) {
    super(message);
    this.status = status;
  }

  public HttpStatus status() {
    return status;
  }

  public static BusinessException notFound(String message) {
    return new BusinessException(HttpStatus.NOT_FOUND, message);
  }

  public static BusinessException badRequest(String message) {
    return new BusinessException(HttpStatus.BAD_REQUEST, message);
  }

  public static BusinessException forbidden(String message) {
    return new BusinessException(HttpStatus.FORBIDDEN, message);
  }

  public static BusinessException conflict(String message) {
    return new BusinessException(HttpStatus.CONFLICT, message);
  }
}
