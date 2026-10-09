package com.mhxy.price;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PriceStore {
  private final JdbcTemplate jdbc;
  private final CatalogService catalog;

  public PriceStore(JdbcTemplate jdbc, CatalogService catalog) {
    this.jdbc = jdbc;
    this.catalog = catalog;
  }

  public Map<String, Object> catalog() {
    Map<Long, Map<String, Object>> items = new LinkedHashMap<>();
    jdbc.query("""
        SELECT id, name, category, side, price, source, updated_at
        FROM price_item
        ORDER BY category, name
        """, (row) -> {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("name", row.getString("name"));
      item.put("category", row.getString("category"));
      item.put("side", row.getString("side"));
      item.put("price", row.getLong("price"));
      item.put("updatedAt", row.getDate("updated_at").toLocalDate().toString());
      item.put("source", row.getString("source"));
      item.put("history", new ArrayList<Map<String, Object>>());
      items.put(row.getLong("id"), item);
    });
    if (!items.isEmpty()) {
      jdbc.query("""
          SELECT item_id, price, source, updated_at
          FROM price_history
          ORDER BY id
          """, (row) -> {
        Map<String, Object> item = items.get(row.getLong("item_id"));
        if (item == null) {
          return;
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> history = (List<Map<String, Object>>) item.get("history");
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("price", row.getLong("price"));
        record.put("updatedAt", row.getDate("updated_at").toLocalDate().toString());
        record.put("source", row.getString("source"));
        history.add(record);
      });
    }
    return Map.of("currency", "金币", "items", List.copyOf(items.values()));
  }

  @Transactional
  public void save(List<PriceRow> rows) {
    for (PriceRow row : rows) {
      saveOne(classified(row));
    }
  }

  private PriceRow classified(PriceRow row) {
    if (!"摆摊".equals(row.side()) && !"收购".equals(row.side())) {
      throw new RecognizeException(HttpStatus.BAD_REQUEST, "「" + row.name() + "」没有标明摆摊或收购");
    }
    CatalogService.Resolved resolved = catalog.resolve(row.name());
    if (resolved.matched()) {
      return row.withCategory(resolved.path());
    }
    if (row.categoryId() == null) {
      throw new RecognizeException(HttpStatus.BAD_REQUEST, "「" + row.name() + "」还没有分类");
    }
    return row.withCategory(catalog.assign(row.name(), row.categoryId()));
  }

  private void saveOne(PriceRow row) {
    List<Map<String, Object>> existing = jdbc.queryForList("""
        SELECT id, price, source, updated_at
        FROM price_item
        WHERE name = ? AND side = ?
        """, row.name(), row.side());
    if (existing.isEmpty()) {
      jdbc.update("""
          INSERT INTO price_item (name, category, side, price, source, updated_at)
          VALUES (?, ?, ?, ?, ?, ?)
          """, row.name(), row.category(), row.side(), row.price(), row.source(), date(row.updatedAt()));
      return;
    }
    Map<String, Object> current = existing.get(0);
    long price = ((Number) current.get("price")).longValue();
    if (price == row.price()) {
      jdbc.update("UPDATE price_item SET category = ? WHERE id = ?", row.category(), current.get("id"));
      return;
    }
    jdbc.update("""
        INSERT INTO price_history (item_id, price, source, updated_at)
        VALUES (?, ?, ?, ?)
        """, current.get("id"), price, current.get("source"), current.get("updated_at"));
    jdbc.update("""
        UPDATE price_item
        SET category = ?, price = ?, source = ?, updated_at = ?
        WHERE id = ?
        """, row.category(), row.price(), row.source(), date(row.updatedAt()), current.get("id"));
  }

  private static LocalDate date(String value) {
    if (value == null || value.isBlank()) {
      return LocalDate.now();
    }
    return LocalDate.parse(value);
  }
}
