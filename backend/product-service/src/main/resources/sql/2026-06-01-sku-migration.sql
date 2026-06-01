-- ============================================================
-- SKU 规格组合系统升级 - 数据库迁移脚本
-- 日期: 2026-06-01
-- 说明: 创建 product_spec_group, product_spec_value, product_sku 三张新表
-- ============================================================

-- 1. 创建规格分组表
CREATE TABLE IF NOT EXISTS product_spec_group (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id  BIGINT NOT NULL,
    name        VARCHAR(100) NOT NULL,
    sort        INT DEFAULT 0,
    KEY idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 创建规格值表
CREATE TABLE IF NOT EXISTS product_spec_value (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id    BIGINT NOT NULL,
    value       VARCHAR(100) NOT NULL,
    sort        INT DEFAULT 0,
    KEY idx_group (group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 创建 SKU 表
CREATE TABLE IF NOT EXISTS product_sku (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id      BIGINT NOT NULL,
    spec_value_ids  JSON NOT NULL,
    price           DECIMAL(10,2) DEFAULT NULL,
    stock           INT DEFAULT 0,
    image           VARCHAR(500) DEFAULT NULL,
    enabled         TINYINT(1) DEFAULT 1,
    sort            INT DEFAULT 0,
    KEY idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
