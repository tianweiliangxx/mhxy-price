package com.mhxy.price;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ItemJsonTest {
  @Test
  void keepsNamedPositivePrices() {
    String content = """
        识别结果：
        ```json
        [{"name":"土属性吸收","price":970000},{"name":"","price":1},{"name":"空价","price":0},{"name":"冥想","price":"488888"}]
        ```
        """;
    List<RecognizedItem> items = ItemJson.parse(content);
    assertEquals(2, items.size());
    assertEquals("土属性吸收", items.get(0).name());
    assertEquals(970000, items.get(0).price());
    assertEquals("冥想", items.get(1).name());
    assertEquals(488888, items.get(1).price());
  }
}
