package com.mhxy.price;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ItemJson {
  private static final Pattern ARRAY = Pattern.compile("\\[.*]", Pattern.DOTALL);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private ItemJson() {}

  public static List<RecognizedItem> parse(String content) {
    if (content == null || content.isBlank()) {
      return List.of();
    }
    Matcher matcher = ARRAY.matcher(content);
    if (!matcher.find()) {
      return List.of();
    }
    try {
      JsonNode array = MAPPER.readTree(matcher.group());
      if (!array.isArray()) {
        return List.of();
      }
      List<RecognizedItem> items = new ArrayList<>();
      for (JsonNode node : array) {
        String name = text(node.get("name")).trim();
        Long price = price(node.get("price"));
        if (name.isEmpty() || price == null || price <= 0) {
          continue;
        }
        items.add(new RecognizedItem(name, price));
      }
      return items;
    } catch (Exception error) {
      return List.of();
    }
  }

  private static String text(JsonNode node) {
    if (node == null || node.isNull()) {
      return "";
    }
    return node.asText("");
  }

  private static Long price(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    if (node.isNumber()) {
      return node.longValue();
    }
    String raw = node.asText("").replaceAll("[^0-9]", "");
    if (raw.isEmpty()) {
      return null;
    }
    try {
      return Long.parseLong(raw);
    } catch (NumberFormatException error) {
      return null;
    }
  }
}
