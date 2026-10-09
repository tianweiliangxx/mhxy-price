package com.mhxy.price;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class RecognizeController {
  private final MimoRecognizer recognizer;
  private final ExecutorService workers = Executors.newCachedThreadPool();

  public RecognizeController(MimoRecognizer recognizer) {
    this.recognizer = recognizer;
  }

  @PostMapping(value = "/api/recognize", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter recognize(
      @RequestPart("image") MultipartFile image,
      @RequestParam("mode") String mode) throws IOException {
    if (image == null || image.isEmpty()) {
      throw new RecognizeException(HttpStatus.BAD_REQUEST, "没有收到画面");
    }
    MimoRecognizer.promptFor(mode);
    byte[] bytes = image.getBytes();
    SseEmitter emitter = new SseEmitter(180_000L);
    workers.submit(() -> stream(emitter, bytes, mode));
    return emitter;
  }

  private void stream(SseEmitter emitter, byte[] bytes, String mode) {
    try {
      RecognizeOutcome outcome = recognizer.recognize(bytes, mode, (chunk) -> send(emitter, Map.of(
          "type", "reasoning",
          "text", chunk
      )));
      send(emitter, Map.of(
          "type", "done",
          "items", outcome.items(),
          "reasoning", outcome.reasoning()
      ));
      emitter.complete();
    } catch (RecognizeException error) {
      send(emitter, Map.of("type", "error", "error", error.getMessage()));
      emitter.complete();
    } catch (Exception error) {
      send(emitter, Map.of("type", "error", "error", "识别失败"));
      emitter.complete();
    }
  }

  private void send(SseEmitter emitter, Map<String, ?> event) {
    try {
      emitter.send(SseEmitter.event().data(event));
    } catch (IOException error) {
      emitter.completeWithError(error);
    }
  }

  @ExceptionHandler(RecognizeException.class)
  public ResponseEntity<Map<String, String>> onRecognize(RecognizeException error) {
    return ResponseEntity.status(error.status()).body(Map.of("error", error.getMessage()));
  }
}
