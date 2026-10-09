package com.mhxy.price;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PromptModeTest {
  @Test
  void eachModeUsesItsOwnPrompt() {
    String sell = MimoRecognizer.promptFor(MimoRecognizer.SELL_STALL);
    String buy = MimoRecognizer.promptFor(MimoRecognizer.BUY_STALL);
    String chat = MimoRecognizer.promptFor(MimoRecognizer.BUY_CHAT);
    assertTrue(sell.contains("摆摊出售"));
    assertTrue(buy.contains("收购摊位"));
    assertTrue(chat.contains("聊天框"));
    assertNotEquals(sell, buy);
    assertNotEquals(buy, chat);
    assertNotEquals(sell, chat);
  }

  @Test
  void unknownModeIsRejected() {
    assertThrows(RecognizeException.class, () -> MimoRecognizer.promptFor("stall"));
  }
}
