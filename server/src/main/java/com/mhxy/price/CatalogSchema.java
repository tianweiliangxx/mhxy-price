package com.mhxy.price;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

@Component
public class CatalogSchema implements ApplicationRunner {
  private final JdbcTemplate jdbc;
  private final ObjectMapper mapper;

  public CatalogSchema(JdbcTemplate jdbc, ObjectMapper mapper) {
    this.jdbc = jdbc;
    this.mapper = mapper;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    widenPrices();
    JsonNode seed = mapper.readTree(new ClassPathResource("catalog-seed.json").getInputStream());
    Map<String, Long> ids = new HashMap<>();
    jdbc.query("SELECT id, path FROM item_category", (row) -> {
      ids.put(row.getString("path"), row.getLong("id"));
    });
    Set<String> names = new HashSet<>(jdbc.query(
        "SELECT name FROM catalog_item",
        (row, index) -> row.getString("name")));
    for (JsonNode category : seed.get("categories")) {
      String path = category.get("path").asText();
      if (ids.containsKey(path)) {
        continue;
      }
      String parentPath = category.get("parentPath").isNull() ? null : category.get("parentPath").asText();
      Long parentId = parentPath == null ? null : ids.get(parentPath);
      KeyHolder keys = new GeneratedKeyHolder();
      jdbc.update(connection -> {
        var statement = connection.prepareStatement(
            "INSERT INTO item_category (parent_id, name, path) VALUES (?, ?, ?)",
            new String[] {"id"});
        if (parentId == null) {
          statement.setObject(1, null);
        } else {
          statement.setLong(1, parentId);
        }
        statement.setString(2, category.get("name").asText());
        statement.setString(3, path);
        return statement;
      }, keys);
      ids.put(path, keys.getKey().longValue());
    }
    for (JsonNode item : seed.get("items")) {
      String name = item.get("name").asText();
      if (names.contains(name)) {
        continue;
      }
      Long categoryId = ids.get(item.get("path").asText());
      if (categoryId == null) {
        continue;
      }
      jdbc.update("INSERT INTO catalog_item (name, category_id) VALUES (?, ?)", name, categoryId);
      names.add(name);
    }
  }

  private void widenPrices() {
    if (countColumn("price_item", "side") == 0) {
      jdbc.execute("ALTER TABLE price_item ADD COLUMN side VARCHAR(16) NOT NULL DEFAULT '收购'");
    }
    jdbc.execute("ALTER TABLE price_item MODIFY category VARCHAR(255) NOT NULL");
    if (countIndex("price_item", "uk_price_item_name_category") > 0) {
      jdbc.execute("ALTER TABLE price_item DROP INDEX uk_price_item_name_category");
    }
    if (countIndex("price_item", "uk_price_item_name_side") == 0) {
      jdbc.execute("ALTER TABLE price_item ADD UNIQUE KEY uk_price_item_name_side (name, side)");
    }
  }

  private int countColumn(String table, String column) {
    return jdbc.queryForObject("""
        SELECT COUNT(*) FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?
        """, Integer.class, table, column);
  }

  private int countIndex(String table, String index) {
    return jdbc.queryForObject("""
        SELECT COUNT(*) FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?
        """, Integer.class, table, index);
  }
}
