package com.mhxy.price;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {
  private final JdbcTemplate jdbc;

  public CatalogService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> tree() {
    List<Map<String, Object>> rows = jdbc.query("""
        SELECT id, parent_id, name, path FROM item_category ORDER BY path
        """, (row, index) -> {
      Map<String, Object> node = new LinkedHashMap<>();
      node.put("id", row.getLong("id"));
      node.put("parentId", row.getObject("parent_id") == null ? null : row.getLong("parent_id"));
      node.put("name", row.getString("name"));
      node.put("path", row.getString("path"));
      node.put("children", new ArrayList<Map<String, Object>>());
      return node;
    });
    Map<Long, Map<String, Object>> byId = new LinkedHashMap<>();
    for (Map<String, Object> row : rows) {
      byId.put((Long) row.get("id"), row);
    }
    List<Map<String, Object>> roots = new ArrayList<>();
    for (Map<String, Object> row : rows) {
      Long parentId = (Long) row.get("parentId");
      if (parentId == null || !byId.containsKey(parentId)) {
        roots.add(row);
      } else {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> children = (List<Map<String, Object>>) byId.get(parentId).get("children");
        children.add(row);
      }
    }
    return roots;
  }

  public List<Map<String, Object>> resolveAll(List<String> names) {
    List<Map<String, Object>> result = new ArrayList<>();
    for (String name : names) {
      result.add(describe(name, resolve(name)));
    }
    return result;
  }

  public Resolved resolve(String name) {
    String text = name == null ? "" : name.trim();
    Resolved exact = one(find(text));
    if (exact != null) {
      return exact.matched() ? exact : Resolved.none();
    }
    Resolved withNeidan = one(find(text + "内丹"));
    if (withNeidan != null) {
      return withNeidan.matched() ? withNeidan : Resolved.none();
    }
    int mark = text.indexOf('·');
    if (mark > 0) {
      Resolved head = one(find(text.substring(0, mark)));
      if (head != null) {
        return head.matched() ? head : Resolved.none();
      }
    }
    return Resolved.none();
  }

  public String assign(String name, long categoryId) {
    List<String> existing = jdbc.query("""
        SELECT c.path
        FROM catalog_item i
        JOIN item_category c ON c.id = i.category_id
        WHERE i.name = ?
        """, (row, index) -> row.getString("path"), name);
    if (!existing.isEmpty()) {
      return existing.get(0);
    }
    List<String> paths = jdbc.query(
        "SELECT path FROM item_category WHERE id = ?",
        (row, index) -> row.getString("path"),
        categoryId);
    if (paths.isEmpty()) {
      throw new RecognizeException(HttpStatus.BAD_REQUEST, "没有这个分类");
    }
    jdbc.update("INSERT INTO catalog_item (name, category_id) VALUES (?, ?)", name, categoryId);
    return paths.get(0);
  }

  private Map<String, Object> describe(String name, Resolved resolved) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("name", name);
    row.put("matched", resolved.matched());
    row.put("categoryPath", resolved.path());
    row.put("categoryId", resolved.categoryId());
    return row;
  }

  private List<Resolved> find(String name) {
    return jdbc.query("""
        SELECT c.id, c.path
        FROM catalog_item i
        JOIN item_category c ON c.id = i.category_id
        WHERE i.name = ?
        """, (row, index) -> new Resolved(true, row.getLong("id"), row.getString("path")), name);
  }

  private Resolved one(List<Resolved> found) {
    if (found.isEmpty()) {
      return null;
    }
    if (found.size() > 1) {
      return Resolved.none();
    }
    return found.get(0);
  }

  public record Resolved(boolean matched, Long categoryId, String path) {
    static Resolved none() {
      return new Resolved(false, null, null);
    }
  }
}
