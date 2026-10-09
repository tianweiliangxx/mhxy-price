CREATE TABLE IF NOT EXISTS price_item (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  category VARCHAR(64) NOT NULL,
  price BIGINT NOT NULL,
  source VARCHAR(64) NULL,
  updated_at DATE NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_price_item_name_category (name, category)
);

CREATE TABLE IF NOT EXISTS price_history (
  id BIGINT NOT NULL AUTO_INCREMENT,
  item_id BIGINT NOT NULL,
  price BIGINT NOT NULL,
  source VARCHAR(64) NULL,
  updated_at DATE NOT NULL,
  PRIMARY KEY (id),
  KEY idx_price_history_item (item_id),
  CONSTRAINT fk_price_history_item FOREIGN KEY (item_id) REFERENCES price_item (id)
);
