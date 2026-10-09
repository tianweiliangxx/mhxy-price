package com.mhxy.price;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Base64;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@EnableConfigurationProperties(MimoProperties.class)
public class MimoRecognizer {
  static final String PROMPT = """
      你在看一张梦幻西游摊位画面。找出格子里的物品名和单价。
      只返回 JSON 数组，不要其他文字。每一项包含 name 和 price。
      price 是金币整数，不要带单位。看不清的格子不要写。
      示例：[{"name":"土属性吸收","price":970000}]
      """;

  private final MimoProperties properties;
  private final RestClient client;
  private final ObjectMapper mapper;

  public MimoRecognizer(MimoProperties properties, ObjectMapper mapper) {
    this.properties = properties;
    this.mapper = mapper;
    this.client = RestClient.create();
  }

  public List<RecognizedItem> recognize(byte[] image) {
    String apiKey = properties.apiKey() == null ? "" : properties.apiKey().trim();
    if (apiKey.isEmpty()) {
      throw new RecognizeException(HttpStatus.SERVICE_UNAVAILABLE, "未配置 MIMO_API_KEY");
    }
    String baseUrl = properties.baseUrl().replaceAll("/$", "");
    ObjectNode body = requestBody(image);
    JsonNode response;
    try {
      response = client.post()
          .uri(baseUrl + "/chat/completions")
          .header("Authorization", "Bearer " + apiKey)
          .header("api-key", apiKey)
          .contentType(MediaType.APPLICATION_JSON)
          .body(body)
          .retrieve()
          .body(JsonNode.class);
    } catch (RestClientException error) {
      throw new RecognizeException(HttpStatus.BAD_GATEWAY, "MiMo 拒绝了这次识别");
    }
    if (response == null) {
      throw new RecognizeException(HttpStatus.BAD_GATEWAY, "MiMo 拒绝了这次识别");
    }
    String content = response.path("choices").path(0).path("message").path("content").asText("");
    List<RecognizedItem> items = ItemJson.parse(content);
    if (items.isEmpty()) {
      throw new RecognizeException(HttpStatus.UNPROCESSABLE_ENTITY, "没有认出物品");
    }
    return items;
  }

  private ObjectNode requestBody(byte[] image) {
    String dataUrl = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(image);
    ObjectNode imageUrl = mapper.createObjectNode();
    imageUrl.put("url", dataUrl);
    ObjectNode imagePart = mapper.createObjectNode();
    imagePart.put("type", "image_url");
    imagePart.set("image_url", imageUrl);
    ObjectNode textPart = mapper.createObjectNode();
    textPart.put("type", "text");
    textPart.put("text", PROMPT);
    ArrayNode content = mapper.createArrayNode().add(imagePart).add(textPart);
    ObjectNode message = mapper.createObjectNode();
    message.put("role", "user");
    message.set("content", content);
    ObjectNode body = mapper.createObjectNode();
    body.put("model", properties.model());
    body.set("messages", mapper.createArrayNode().add(message));
    body.put("temperature", 0.2);
    return body;
  }
}
