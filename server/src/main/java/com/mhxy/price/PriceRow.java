package com.mhxy.price;

public record PriceRow(
    String name,
    String category,
    long price,
    String source,
    String updatedAt,
    String side,
    Long categoryId) {

  PriceRow withCategory(String path) {
    return new PriceRow(name, path, price, source, updatedAt, side, categoryId);
  }
}
