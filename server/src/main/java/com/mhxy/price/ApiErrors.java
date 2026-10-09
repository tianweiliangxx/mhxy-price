package com.mhxy.price;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiErrors {
  @ExceptionHandler(RecognizeException.class)
  public ResponseEntity<Map<String, String>> onRecognize(RecognizeException error) {
    return ResponseEntity.status(error.status()).body(Map.of("error", error.getMessage()));
  }
}
