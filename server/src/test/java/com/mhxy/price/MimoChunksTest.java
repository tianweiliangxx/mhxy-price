package com.mhxy.price;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class MimoChunksTest {
  @Test
  void collectsReasoningBeforeTheAnswer() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    StringBuilder reasoning = new StringBuilder();
    StringBuilder answer = new StringBuilder();
    MimoChunks.append(mapper.readTree("""
        {"choices":[{"delta":{"reasoning_content":"先看格子"}}]}
        """), reasoning, answer);
    MimoChunks.append(mapper.readTree("""
        {"choices":[{"delta":{"content":"[{\\"name\\":\\"防御\\",\\"price\\":970000}]"}}]}
        """), reasoning, answer);
    assertEquals("先看格子", reasoning.toString());
    assertEquals(1, ItemJson.parse(answer.toString()).size());
  }
}
