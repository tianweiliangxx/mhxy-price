package com.mhxy.price;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PriceController {
  private final PriceStore store;

  public PriceController(PriceStore store) {
    this.store = store;
  }

  @GetMapping("/api/items")
  public Map<String, Object> items() {
    return store.catalog();
  }

  @PostMapping("/api/items")
  public Map<String, Object> save(@RequestBody JsonNode body) {
    List<PriceRow> rows = rowsOf(body);
    if (rows.isEmpty()) {
      throw new RecognizeException(HttpStatus.BAD_REQUEST, "没有要写入的物价");
    }
    store.save(rows);
    return store.catalog();
  }

  private static List<PriceRow> rowsOf(JsonNode body) {
    JsonNode array = body;
    if (body != null && body.isObject() && body.has("items")) {
      array = body.get("items");
    }
    if (body != null && body.isObject() && body.has("name")) {
      return List.of(itemOf(body));
    }
    if (array == null || !array.isArray()) {
      return List.of();
    }
    List<PriceRow> rows = new ArrayList<>();
    for (JsonNode node : array) {
      PriceRow item = itemOf(node);
      if (!item.name().isBlank() && item.price() > 0 && !item.category().isBlank()) {
        rows.add(item);
      }
    }
    return rows;
  }

  private static PriceRow itemOf(JsonNode node) {
    String name = node.path("name").asText("").trim();
    String category = node.path("category").asText("").trim();
    long price = node.path("price").isNumber()
        ? node.path("price").asLong()
        : digits(node.path("price").asText(""));
    String source = node.path("source").asText("").trim();
    String updatedAt = node.path("updatedAt").asText("").trim();
    if (updatedAt.isEmpty()) {
      updatedAt = LocalDate.now().toString();
    }
    return new PriceRow(name, category, price, source.isEmpty() ? null : source, updatedAt);
  }

  private static long digits(String raw) {
    String numbers = raw.replaceAll("[^0-9]", "");
    if (numbers.isEmpty()) {
      return 0;
    }
    return Long.parseLong(numbers);
  }
}
