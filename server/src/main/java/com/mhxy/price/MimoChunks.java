package com.mhxy.price;

import com.fasterxml.jackson.databind.JsonNode;

final class MimoChunks {
  private MimoChunks() {}

  static void append(JsonNode chunk, StringBuilder reasoning, StringBuilder answer) {
    JsonNode choice = chunk.path("choices").path(0);
    appendNode(choice.path("delta"), reasoning, answer);
    appendNode(choice.path("message"), reasoning, answer);
  }

  private static void appendNode(JsonNode node, StringBuilder reasoning, StringBuilder answer) {
    if (node.isMissingNode() || node.isNull()) {
      return;
    }
    appendField(node, "reasoning_content", reasoning);
    appendField(node, "content", answer);
  }

  private static void appendField(JsonNode node, String field, StringBuilder target) {
    JsonNode value = node.get(field);
    if (value == null || value.isNull()) {
      return;
    }
    target.append(value.asText(""));
  }
}
