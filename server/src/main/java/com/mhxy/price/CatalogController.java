package com.mhxy.price;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CatalogController {
  private final CatalogService catalog;

  public CatalogController(CatalogService catalog) {
    this.catalog = catalog;
  }

  @GetMapping("/api/categories")
  public List<Map<String, Object>> categories() {
    return catalog.tree();
  }

  @PostMapping("/api/catalog/resolve")
  public Map<String, Object> resolve(@RequestBody JsonNode body) {
    JsonNode names = body == null ? null : body.get("names");
    List<String> requested = new ArrayList<>();
    if (names != null && names.isArray()) {
      for (JsonNode name : names) {
        requested.add(name.asText("").trim());
      }
    }
    return Map.of("items", catalog.resolveAll(requested));
  }
}
