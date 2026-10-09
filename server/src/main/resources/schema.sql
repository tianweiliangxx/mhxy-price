CREATE TABLE IF NOT EXISTS item_category (
  id BIGINT NOT NULL AUTO_INCREMENT,
  parent_id BIGINT NULL,
  name VARCHAR(128) NOT NULL,
  path VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_item_category_path (path),
  KEY idx_item_category_parent (parent_id)
);

CREATE TABLE IF NOT EXISTS catalog_item (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  category_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_catalog_item_name (name),
  KEY idx_catalog_item_category (category_id),
  CONSTRAINT fk_catalog_item_category FOREIGN KEY (category_id) REFERENCES item_category (id)
);

CREATE TABLE IF NOT EXISTS price_item (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  category VARCHAR(255) NOT NULL,
  side VARCHAR(16) NOT NULL,
  price BIGINT NOT NULL,
  source VARCHAR(64) NULL,
  updated_at DATE NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_price_item_name_side (name, side)
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
