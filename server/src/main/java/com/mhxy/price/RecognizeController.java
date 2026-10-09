package com.mhxy.price;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class RecognizeController {
  private final MimoRecognizer recognizer;

  public RecognizeController(MimoRecognizer recognizer) {
    this.recognizer = recognizer;
  }

  @PostMapping("/api/recognize")
  public Map<String, List<RecognizedItem>> recognize(
      @RequestPart("image") MultipartFile image,
      @RequestParam("mode") String mode) throws IOException {
    if (image == null || image.isEmpty()) {
      throw new RecognizeException(HttpStatus.BAD_REQUEST, "没有收到画面");
    }
    return Map.of("items", recognizer.recognize(image.getBytes(), mode));
  }

  @ExceptionHandler(RecognizeException.class)
  public ResponseEntity<Map<String, String>> onRecognize(RecognizeException error) {
    return ResponseEntity.status(error.status()).body(Map.of("error", error.getMessage()));
  }
}
