-- ============================================================
-- SKU 数据迁移脚本
-- 从旧的 product_spec 表迁移数据到新的 product_spec_group,
-- product_spec_value, product_sku 表
-- ============================================================

-- Step 1: 创建规格分组 (按 product_id + spec_name 去重)
INSERT INTO product_spec_group (product_id, name, sort)
SELECT ps.product_id, ps.spec_name, 0
FROM product_spec ps
GROUP BY ps.product_id, ps.spec_name;

-- Step 2: 创建规格值 (关联到对应的分组)
INSERT INTO product_spec_value (group_id, value, sort)
SELECT psg.id, ps.spec_value, ps.sort
FROM product_spec ps
JOIN product_spec_group psg ON psg.product_id = ps.product_id AND psg.name = ps.spec_name;

-- Step 3: 单维度商品 SKU (直接取 extra_price 和 stock)
INSERT INTO product_sku (product_id, spec_value_ids, price, stock, image, enabled, sort)
SELECT
    ps.product_id,
    JSON_ARRAY(psv.id),
    ps.extra_price,
    ps.stock,
    ps.image,
    1,
    ps.sort
FROM product_spec ps
JOIN product_spec_group psg ON psg.product_id = ps.product_id AND psg.name = ps.spec_name
JOIN product_spec_value psv ON psv.group_id = psg.id AND psv.value = ps.spec_value
WHERE (SELECT COUNT(DISTINCT spec_name) FROM product_spec WHERE product_id = ps.product_id) = 1;

-- Step 4: 多维度商品 SKU (笛卡尔积, price=null, stock=product.stock)
-- 当前数据中最多只有 2 个维度, 使用 cross join 生成组合
INSERT INTO product_sku (product_id, spec_value_ids, price, stock, image, enabled, sort)
SELECT
    g1.product_id,
    JSON_ARRAY(v1.id, v2.id),
    NULL,
    p.stock,
    NULL,
    1,
    0
FROM product_spec_group g1
JOIN product_spec_group g2 ON g2.product_id = g1.product_id AND g2.id > g1.id
JOIN product_spec_value v1 ON v1.group_id = g1.id
JOIN product_spec_value v2 ON v2.group_id = g2.id
JOIN product p ON p.id = g1.product_id
WHERE (SELECT COUNT(DISTINCT spec_name) FROM product_spec WHERE product_id = g1.product_id) > 1;
