package com.mhxy.price;

import org.springframework.http.HttpStatus;

public class RecognizeException extends RuntimeException {
  private final HttpStatus status;

  public RecognizeException(HttpStatus status, String message) {
    super(message);
    this.status = status;
  }

  public HttpStatus status() {
    return status;
  }
}
