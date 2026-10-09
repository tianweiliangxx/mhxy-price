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
  static final String SELL_STALL = "sell-stall";
  static final String BUY_STALL = "buy-stall";
  static final String BUY_CHAT = "buy-chat";

  static final String SELL_STALL_PROMPT = """
      你在看一张梦幻西游的摆摊出售窗口。请只识别窗口中间正在出售的物品格子。

      每个格子通常有：物品图标、白色的物品名，以及「单价」旁边的价格数字。这是卖家标出的出售价。

      单价按金币数额变色，用来核对位数：
      - 1 到 9999（1 到 4 位）：黑色
      - 10000 到 99999（5 位）：蓝色
      - 100000 到 999999（6 位）：绿色，例如 970000
      - 1000000 到 9999999（7 位）：红色
      - 10000000 到 99999999（8 位）：紫色
      - 100000000 及以上（9 位及以上）：品红色

      要做的事：
      - 按格子从上到下、从左到右，每个出售格子记一条。
      - name 按格子上的汉字原样抄写，不要改成近义词，也不要漏字。
      - price 只取该格子「单价」旁边的整数金币，去掉逗号和单位。读出的位数要和上面的颜色一致；对不上时以看到的数字为准，不要为了凑颜色改数字。
      - 不要把收购窗口、聊天喊话、窗口标题、按钮、数量、总价、现金和摊主编号写进来。
      - 看不清的格子整条跳过，不要猜测。

      只返回一个 JSON 数组，不要解释，不要用 markdown。
      格式示例：[{"name":"土属性吸收","price":970000}]
      没有出售格子时返回 []。
      """;

  static final String BUY_STALL_PROMPT = """
      你在看一张梦幻西游的收购摊位窗口。请只识别窗口中间、摊主正在收购的物品格子。

      每个格子通常有：物品图标、白色的物品名，以及「单价」旁边的价格数字。这是摊主愿意出的收购价。

      单价按金币数额变色，用来核对位数：
      - 1 到 9999（1 到 4 位）：黑色
      - 10000 到 99999（5 位）：蓝色
      - 100000 到 999999（6 位）：绿色，例如 970000
      - 1000000 到 9999999（7 位）：红色
      - 10000000 到 99999999（8 位）：紫色
      - 100000000 及以上（9 位及以上）：品红色

      要做的事：
      - 按格子从上到下、从左到右，每个收购格子记一条。
      - name 按格子上的汉字原样抄写，不要改成近义词，也不要漏字。
      - price 只取该格子「单价」旁边的整数金币，去掉逗号和单位。读出的位数要和上面的颜色一致；对不上时以看到的数字为准，不要为了凑颜色改数字。
      - 不要把摆摊出售格子、聊天喊话、窗口标题、按钮、数量、总价、现金和摊主编号写进来。
      - 看不清的格子整条跳过，不要猜测。

      只返回一个 JSON 数组，不要解释，不要用 markdown。
      格式示例：[{"name":"土属性吸收","price":970000}]
      没有收购格子时返回 []。
      """;

  static final String BUY_CHAT_PROMPT = """
      你在看一张梦幻西游的游戏画面。请只看聊天框，找出玩家用文字喊出来的收购。

      喊话里会出现「收」「收购」「高价收」，后面是物品名和出价，例如「收金柳露 10万」。

      要做的事：
      - 每一条喊话收购记一条。同一个人重复喊的同一物品只留一条。
      - name 是喊话里要收的物品名，按汉字原样抄写。
      - price 是这条喊话的出价，换成整数金币。写着「万」的乘以 10000，例如 10万 写成 100000。喊话里的数字如果按银两上色，颜色和摊位单价相同：1 到 4 位黑色，5 位蓝色，6 位绿色，7 位红色，8 位紫色，9 位及以上品红色。颜色用来核对位数；和文字对不上时以喊话里的数字或「万」为准。
      - 不要识别摆摊格子、收购摊位格子、窗口标题、系统提示和普通聊天。
      - 没有价格或看不清物品名的喊话整条跳过。

      只返回一个 JSON 数组，不要解释，不要用 markdown。
      格式示例：[{"name":"金柳露","price":100000}]
      没有喊话收购时返回 []。
      """;

  static String promptFor(String mode) {
    if (SELL_STALL.equals(mode)) return SELL_STALL_PROMPT;
    if (BUY_STALL.equals(mode)) return BUY_STALL_PROMPT;
    if (BUY_CHAT.equals(mode)) return BUY_CHAT_PROMPT;
    throw new RecognizeException(HttpStatus.BAD_REQUEST, "不认识的识别类型");
  }

  private final MimoProperties properties;
  private final RestClient client;
  private final ObjectMapper mapper;

  public MimoRecognizer(MimoProperties properties, ObjectMapper mapper) {
    this.properties = properties;
    this.mapper = mapper;
    this.client = RestClient.create();
  }

  public List<RecognizedItem> recognize(byte[] image, String mode) {
    String prompt = promptFor(mode);
    String apiKey = properties.apiKey() == null ? "" : properties.apiKey().trim();
    if (apiKey.isEmpty()) {
      throw new RecognizeException(HttpStatus.SERVICE_UNAVAILABLE, "未配置 MIMO_API_KEY");
    }
    String baseUrl = properties.baseUrl().replaceAll("/$", "");
    ObjectNode body = requestBody(image, prompt);
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

  private ObjectNode requestBody(byte[] image, String prompt) {
    String dataUrl = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(image);
    ObjectNode imageUrl = mapper.createObjectNode();
    imageUrl.put("url", dataUrl);
    ObjectNode imagePart = mapper.createObjectNode();
    imagePart.put("type", "image_url");
    imagePart.set("image_url", imageUrl);
    ObjectNode textPart = mapper.createObjectNode();
    textPart.put("type", "text");
    textPart.put("text", prompt);
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
