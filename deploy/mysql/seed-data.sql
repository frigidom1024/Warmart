-- ============================================================
-- Seed Data: Chinese categories, products
-- Run AFTER init.sql (schema creation)
-- ============================================================

-- Add tag column if not exists (migration for older schemas)
SET @dbname = 'mall_product';
SET @exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'product' AND COLUMN_NAME = 'tag');
SET @sql = IF(@exists = 0, 'ALTER TABLE mall_product.product ADD COLUMN tag VARCHAR(50) DEFAULT NULL AFTER original_price', 'SELECT "tag column already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- mall_product cleanup (delete existing test data)
-- ============================================================
USE mall_product;

DELETE FROM product_comment WHERE id > 0;
DELETE FROM product_image WHERE id > 0;
DELETE FROM product_spec WHERE id > 0;
DELETE FROM product WHERE id > 0;
DELETE FROM category WHERE id > 0;

-- ============================================================
-- Categories
-- ============================================================
INSERT INTO category (id, name, parent_id, sort, icon, created_time, updated_time) VALUES
(1,  '服饰鞋包', 0, 1, NULL, NOW(), NOW()),
(2,  '数码家电', 0, 2, NULL, NOW(), NOW()),
(3,  '家居生活', 0, 3, NULL, NOW(), NOW()),
(4,  '美妆个护', 0, 4, NULL, NOW(), NOW()),
(5,  '运动户外', 0, 5, NULL, NOW(), NOW()),

(10, '男装',    1, 1, NULL, NOW(), NOW()),
(11, '女装',    1, 2, NULL, NOW(), NOW()),
(12, '鞋靴',    1, 3, NULL, NOW(), NOW()),
(13, '箱包',    1, 4, NULL, NOW(), NOW()),

(20, '智能手机', 2, 1, NULL, NOW(), NOW()),
(21, '笔记本电脑', 2, 2, NULL, NOW(), NOW()),
(22, '耳机配件', 2, 3, NULL, NOW(), NOW()),

(30, '厨房用品', 3, 1, NULL, NOW(), NOW()),
(31, '家居家纺', 3, 2, NULL, NOW(), NOW()),
(32, '生活日用', 3, 3, NULL, NOW(), NOW()),

(40, '面部护肤', 4, 1, NULL, NOW(), NOW()),
(41, '彩妆',    4, 2, NULL, NOW(), NOW()),
(42, '洗发护发', 4, 3, NULL, NOW(), NOW()),

(50, '运动鞋服', 5, 1, NULL, NOW(), NOW()),
(51, '户外装备', 5, 2, NULL, NOW(), NOW()),
(52, '健身器材', 5, 3, NULL, NOW(), NOW()),

(200,  '食品饮料', 0, 6, NULL, NOW(), NOW()),
(201,  '母婴用品', 0, 7, NULL, NOW(), NOW()),
(202,  '图书文具', 0, 8, NULL, NOW(), NOW()),
(203,  '宠物生活', 0, 9, NULL, NOW(), NOW()),
(204, '汽车用品', 0, 10, NULL, NOW(), NOW()),
(205, '医药健康', 0, 11, NULL, NOW(), NOW()),
(206, '珠宝配饰', 0, 12, NULL, NOW(), NOW()),

(210, '休闲零食', 200, 1, NULL, NOW(), NOW()),
(211, '茶饮酒水', 200, 2, NULL, NOW(), NOW()),
(212, '营养保健', 200, 3, NULL, NOW(), NOW()),

(220, '奶粉辅食', 201, 1, NULL, NOW(), NOW()),
(221, '尿裤湿巾', 201, 2, NULL, NOW(), NOW()),
(222, '玩具益智', 201, 3, NULL, NOW(), NOW()),

(230, '文学小说', 202, 1, NULL, NOW(), NOW()),
(231, '办公文具', 202, 2, NULL, NOW(), NOW()),
(232, '学生用品', 202, 3, NULL, NOW(), NOW()),

(240, '猫粮猫砂', 203, 1, NULL, NOW(), NOW()),
(241, '狗粮用品', 203, 2, NULL, NOW(), NOW()),
(242, '宠物玩具', 203, 3, NULL, NOW(), NOW()),

(250, '车载电器', 204, 1, NULL, NOW(), NOW()),
(251, '内饰清洁', 204, 2, NULL, NOW(), NOW()),
(252, '骑行装备', 204, 3, NULL, NOW(), NOW()),

(260, '医疗器械', 205, 1, NULL, NOW(), NOW()),
(261, '保健养生', 205, 2, NULL, NOW(), NOW()),
(262, '家庭药箱', 205, 3, NULL, NOW(), NOW()),

(270, '时尚手表', 206, 1, NULL, NOW(), NOW()),
(271, '首饰配饰', 206, 2, NULL, NOW(), NOW()),
(272, '眼镜',    206, 3, NULL, NOW(), NOW());

-- ============================================================
-- Products (服饰鞋包 — category 1)
-- ============================================================
INSERT INTO product (id, name, description, price, original_price, tag, image, category_id, stock, sales, status, is_recommended, created_time, updated_time) VALUES
(1,  '经典条纹短袖T恤',     '纯棉面料，舒适透气，经典条纹设计，百搭单品',          199.00,  299.00,  '热卖',  '/images/product_01.png', 10, 500, 1280, 0, 1, NOW(), NOW()),
(2,  '韩版宽松连衣裙',     '温柔碎花印花，松紧腰设计，飘逸显瘦',                  359.00,  459.00,  '新品',  '/images/product_02.png', 11, 300, 560,  0, 1, NOW(), NOW()),
(3,  '商务休闲皮鞋',       '头层牛皮，舒适耐穿，商务通勤必备',                    499.00,  699.00,  '爆款',  '/images/product_03.png', 12, 200, 2340, 0, 1, NOW(), NOW()),
(4,  '潮流帆布双肩包',     '防水面料，大容量多隔层，通勤旅行两用',                259.00,  329.00,  '热卖',  '/images/product_04.png', 13, 400, 890,  0, 0, NOW(), NOW()),
(5,  '简约休闲西装外套',   '轻奢羊毛混纺，立体剪裁，干练有型',                    699.00,  899.00,  '新品',  '/images/product_05.png', 10, 180, 340,  0, 1, NOW(), NOW()),
(6,  '法式方领泡泡袖上衣', '复古方领设计，泡泡袖修饰手臂，法式浪漫',              269.00,  359.00,  '热卖',  '/images/product_06.png', 11, 350, 1670, 0, 0, NOW(), NOW());

-- Products (数码家电 — category 2)
INSERT INTO product (id, name, description, price, original_price, tag, image, category_id, stock, sales, status, is_recommended, created_time, updated_time) VALUES
(7,  '超清影像旗舰手机',   '1亿像素·120Hz高刷屏·5000mAh大电池·66W快充',         3999.00, 4699.00, '爆款',  '/images/product_07.png', 20, 1000, 5600, 0, 1, NOW(), NOW()),
(8,  '14英寸轻薄笔记本',   '2.5K全面屏·酷睿i7·16G内存·512G固态·1.2kg超轻',      5299.00, 5999.00, '热卖',  '/images/product_08.png', 21, 500, 3210, 0, 1, NOW(), NOW()),
(9,  '主动降噪蓝牙耳机',   'ANC自适应降噪·HiFi音质·30h续航·IPX5防水',            799.00,  999.00,  '新品',  '/images/product_09.png', 22, 800, 1870, 0, 1, NOW(), NOW()),
(10, '智能运动手表 Pro',   '1.43寸AMOLED·血氧心率监测·GPS定位·14天续航',        1299.00, 1599.00, '新品',  '/images/product_10.png', 20, 600, 980,  0, 0, NOW(), NOW()),
(11, '机械键盘 87键',      'Cherry青轴·RGB背光·PBT键帽·铝合金面板',             449.00,  599.00,  '热卖',  '/images/product_11.png', 22, 400, 2150, 0, 0, NOW(), NOW()),
(12, '便携蓝牙音箱',       'IPX7级防水·20h续航·TWS串联·低音增强',               299.00,  399.00,  '爆款',  '/images/product_12.png', 22, 700, 4320, 0, 1, NOW(), NOW());

-- Products (家居生活 — category 3)
INSERT INTO product (id, name, description, price, original_price, tag, image, category_id, stock, sales, status, is_recommended, created_time, updated_time) VALUES
(13, '日式陶瓷餐具套装',   '16头釉下彩工艺·微波炉适用·礼盒装',                   168.00,  228.00,  '热卖',  '/images/product_13.png', 30, 350, 980,  0, 1, NOW(), NOW()),
(14, '纯棉四件套',         '60支长绒棉·亲肤透气·简约北欧风·1.8m床',             399.00,  559.00,  '爆款',  '/images/product_14.png', 31, 250, 3100, 0, 1, NOW(), NOW()),
(15, '真空压缩袋套装',     '6件套·免抽气泵·PEVA材质·防潮防螨',                   79.00,   119.00,  '',     '/images/product_15.png', 32, 600, 1500, 0, 0, NOW(), NOW()),
(16, '北欧风格落地灯',     '三色温调节·金属灯杆·氛围感满满',                     259.00,  329.00,  '新品',  '/images/product_16.png', 31, 200, 430,  0, 0, NOW(), NOW()),
(17, '304不锈钢保温壶',    '2L大容量·24h保温·食品级304·防滑底座',               129.00,  179.00,  '热卖',  '/images/product_17.png', 30, 450, 2670, 0, 1, NOW(), NOW());

-- Products (美妆个护 — category 4)
INSERT INTO product (id, name, description, price, original_price, tag, image, category_id, stock, sales, status, is_recommended, created_time, updated_time) VALUES
(18, '玻尿酸精华液',       '三重玻尿酸·深层补水·淡化细纹·清爽不黏腻',           239.00,  299.00,  '热卖',  '/images/product_18.png', 40, 800, 4300, 0, 1, NOW(), NOW()),
(19, '哑光丝绒唇釉',       '丝绒质地·显色持久·不沾杯·多色可选',                 129.00,  169.00,  '新品',  '/images/product_19.png', 41, 600, 1890, 0, 1, NOW(), NOW()),
(20, '氨基酸洗发水',       '温和清洁·无硅油·修护受损·蓬松控油',                 89.00,   129.00,  '爆款',  '/images/product_20.png', 42, 900, 5600, 0, 1, NOW(), NOW()),
(21, '烟酰胺美白面膜',     '20片装·提亮肤色·补水保湿·温和不刺激',               109.00,  159.00,  '热卖',  '/images/product_21.png', 40, 700, 3400, 0, 0, NOW(), NOW()),
(22, '防晒霜SPF50+',       '清爽不油腻·PA++++·防水防汗·日常通勤必备',           99.00,   139.00,  '',     '/images/product_22.png', 40, 1000, 7800, 0, 0, NOW(), NOW());

-- Products (运动户外 — category 5)
INSERT INTO product (id, name, description, price, original_price, tag, image, category_id, stock, sales, status, is_recommended, created_time, updated_time) VALUES
(23, '轻便缓震跑鞋',       '全掌气垫·透气飞织面料·防滑大底·轻量化设计',         459.00,  599.00,  '热卖',  '/images/product_23.png', 50, 400, 2100, 0, 1, NOW(), NOW()),
(24, '加厚防滑瑜伽垫',     '10mm加厚·环保TPE材质·双面防滑·送收纳带',           169.00,  229.00,  '新品',  '/images/product_24.png', 52, 350, 870,  0, 0, NOW(), NOW()),
(25, '户外登山双肩包',     '50L大容量·防水面料·多仓设计·减压背负',             329.00,  429.00,  '热卖',  '/images/product_25.png', 51, 200, 650,  0, 0, NOW(), NOW()),
(26, '速干运动T恤',        'CoolMax面料·排汗速干·四面弹力·无缝工艺',           149.00,  199.00,  '爆款',  '/images/product_26.png', 50, 500, 3900, 0, 1, NOW(), NOW());

-- Products (食品饮料 — category 200)
INSERT INTO product (id, name, description, price, original_price, tag, image, category_id, stock, sales, status, is_recommended, created_time, updated_time) VALUES
(27, '每日坚果礼盒',       '7种坚果搭配，每日一包，锁鲜小包独立包装',                89.00,   129.00,  '热卖',  '/images/product_27.png', 210, 800, 4500, 0, 1, NOW(), NOW()),
(28, '精品阿拉比卡咖啡豆', '中度烘焙·醇厚回甘·可磨粉·500g装',                     79.00,   119.00,  '',     '/images/product_28.png', 211, 400, 1800, 0, 0, NOW(), NOW()),
(29, '明前龙井绿茶',       '2026年头采·核心产区·清香甘爽·250g铁罐装',             69.00,   99.00,   '新品',  '/images/product_29.png', 211, 300, 760,  0, 0, NOW(), NOW()),
(30, '进口黑巧克力礼盒',   '72%可可含量·丝滑浓郁·铁盒装·送女友礼物',              129.00,  179.00,  '爆款',  '/images/product_30.png', 210, 500, 3200, 0, 1, NOW(), NOW());

-- Products (母婴用品 — category 201)
INSERT INTO product (id, name, description, price, original_price, tag, image, category_id, stock, sales, status, is_recommended, created_time, updated_time) VALUES
(31, '婴儿纸尿裤 XL码',    '3D立体环绕·瞬吸干爽·整夜防漏·XL56片装',               99.00,   149.00,  '爆款',  '/images/product_31.png', 221, 1000, 8900, 0, 1, NOW(), NOW()),
(32, '儿童拼装积木桌',     '大颗粒积木·双面桌板·收纳一体·培养动手能力',            169.00,  239.00,  '热卖',  '/images/product_32.png', 222, 350, 2100, 0, 1, NOW(), NOW()),
(33, '婴儿润肤乳液',       '无香精·无酒精·温和保湿·舒缓干红',                      79.00,   109.00,  '',     '/images/product_33.png', 220, 600, 3400, 0, 0, NOW(), NOW());

-- Products (图书文具 — category 202)
INSERT INTO product (id, name, description, price, original_price, tag, image, category_id, stock, sales, status, is_recommended, created_time, updated_time) VALUES
(34, '畅销小说三册套装',   '年度热销文学小说集，精装典藏版',                          89.00,   129.00,  '',     '/images/product_34.png', 230, 500, 2300, 0, 0, NOW(), NOW()),
(35, '精装钢笔礼盒',       '德国笔尖·金属笔杆·商务送礼·配墨水',                     199.00,  299.00,  '新品',  '/images/product_35.png', 231, 300, 980,  0, 1, NOW(), NOW()),
(36, '手账笔记本套装',     'A5点阵本·皮面精装·绑带设计·送贴纸',                     39.00,   59.00,   '热卖',  '/images/product_36.png', 232, 800, 5600, 0, 0, NOW(), NOW());

-- Products (宠物生活 — category 203)
INSERT INTO product (id, name, description, price, original_price, tag, image, category_id, stock, sales, status, is_recommended, created_time, updated_time) VALUES
(37, '全价成猫猫粮',       '鸡肉配方·高蛋白低脂肪·10kg大包装',                      129.00,  179.00,  '爆款',  '/images/product_37.png', 240, 600, 6700, 0, 1, NOW(), NOW()),
(38, '宠物发声玩具套装',   '耐咬发声球·磨牙洁齿·狗狗猫咪通用',                      49.00,   79.00,   '',     '/images/product_38.png', 242, 900, 3800, 0, 0, NOW(), NOW()),
(39, '智能宠物饮水机',     '四重过滤·静音水泵·2.5L大容量·UV杀菌',                  189.00,  259.00,  '新品',  '/images/product_39.png', 240, 400, 1200, 0, 0, NOW(), NOW());

-- Products (汽车用品 — category 204)
INSERT INTO product (id, name, description, price, original_price, tag, image, category_id, stock, sales, status, is_recommended, created_time, updated_time) VALUES
(40, '车载手机支架',       '磁吸+夹扣双固定·360°旋转·单手操作',                     39.00,   69.00,   '热卖',  '/images/product_40.png', 250, 1200, 7800, 0, 1, NOW(), NOW()),
(41, '无线车载吸尘器',     '大吸力·Type-C充电·小巧便携·多功能吸嘴',                199.00,  299.00,  '',     '/images/product_41.png', 250, 350, 1500, 0, 0, NOW(), NOW()),
(42, '汽车记忆棉腰靠',     '人体工学设计·透气网布·四季通用·缓解疲劳',               159.00,  229.00,  '新品',  '/images/product_42.png', 251, 500, 2100, 0, 0, NOW(), NOW());

-- Products (医药健康 — category 205)
INSERT INTO product (id, name, description, price, original_price, tag, image, category_id, stock, sales, status, is_recommended, created_time, updated_time) VALUES
(43, '维生素C泡腾片',      '20支装·草莓味·增强免疫·易吸收',                         59.00,   89.00,   '',     '/images/product_43.png', 261, 1500, 9200, 0, 0, NOW(), NOW()),
(44, '颈椎按摩仪',         '脉冲理疗·热敷功能·语音播报·16档力度',                   299.00,  399.00,  '爆款',  '/images/product_44.png', 260, 400, 4300, 0, 1, NOW(), NOW()),
(45, '电子血压计',         '上臂式全自动·大屏显示·语音播报·双人记忆',               179.00,  259.00,  '热卖',  '/images/product_45.png', 260, 600, 5600, 0, 1, NOW(), NOW());

-- Products (珠宝配饰 — category 206)
INSERT INTO product (id, name, description, price, original_price, tag, image, category_id, stock, sales, status, is_recommended, created_time, updated_time) VALUES
(46, '简约石英手表',       '超薄表盘·米兰尼斯表带·30米防水·送女友',                 299.00,  499.00,  '热卖',  '/images/product_46.png', 270, 300, 3400, 0, 1, NOW(), NOW()),
(47, '淡水珍珠耳钉',       'S925银针·6-7mm珍珠·温润优雅·礼盒装',                   159.00,  259.00,  '新品',  '/images/product_47.png', 271, 400, 1800, 0, 0, NOW(), NOW()),
(48, '偏光太阳镜',         '飞行员款·UV400防护·偏光镜片·轻量设计',                  199.00,  329.00,  '',     '/images/product_48.png', 272, 500, 2800, 0, 0, NOW(), NOW());

-- ============================================================
-- Product specs
-- ============================================================
INSERT INTO product_spec (product_id, spec_name, spec_value, extra_price, stock, sort) VALUES
-- T恤
(1, '颜色', '白色',  0, 150, 1),
(1, '颜色', '黑色',  0, 150, 2),
(1, '颜色', '灰色',  0, 100, 3),
(1, '尺码', 'M',     0, 150, 1),
(1, '尺码', 'L',     0, 150, 2),
(1, '尺码', 'XL',    0, 100, 3),
-- 连衣裙
(2, '颜色', '碎花蓝', 0, 100, 1),
(2, '颜色', '碎花粉', 0, 100, 2),
(2, '尺码', 'S',      0, 80,  1),
(2, '尺码', 'M',      0, 100, 2),
(2, '尺码', 'L',      0, 80,  3),
-- 皮鞋
(3, '颜色', '黑色',  0, 100, 1),
(3, '颜色', '棕色',  0, 80,  2),
(3, '尺码', '39',    0, 40,  1),
(3, '尺码', '40',    0, 50,  2),
(3, '尺码', '41',    0, 50,  3),
(3, '尺码', '42',    0, 40,  4),
(3, '尺码', '43',    0, 30,  5),
-- 手机
(7, '颜色', '星钻黑', 0, 300, 1),
(7, '颜色', '陶瓷白', 0, 300, 2),
(7, '颜色', '渐变蓝', 0, 200, 3),
(7, '存储', '8+128G', 0, 400, 1),
(7, '存储', '8+256G', 300, 300, 2),
(7, '存储', '12+256G', 500, 200, 3),
-- 笔记本
(8, '颜色', '深空灰', 0, 200, 1),
(8, '颜色', '银色',   0, 200, 2),
(8, '内存', '16G+512G', 0, 250, 1),
(8, '内存', '32G+1T',  800, 150, 2),
-- 耳机
(9, '颜色', '黑色',  0, 300, 1),
(9, '颜色', '白色',  0, 300, 2),
(9, '颜色', '薄荷绿', 50, 200, 3),
-- 跑鞋
(23, '颜色', '黑白',  0, 120, 1),
(23, '颜色', '灰蓝',  0, 120, 2),
(23, '颜色', '荧光绿', 0, 80,  3),
(23, '尺码', '39',    0, 50,  1),
(23, '尺码', '40',    0, 60,  2),
(23, '尺码', '41',    0, 60,  3),
(23, '尺码', '42',    0, 50,  4),
(23, '尺码', '43',    0, 40,  5),
-- 坚果礼盒
(27, '规格', '经典款', 0, 300, 1),
(27, '规格', '尊享款', 30, 200, 2),
-- 纸尿裤
(31, '尺码', 'L码',  0, 400, 1),
(31, '尺码', 'XL码', 0, 400, 2),
(31, '尺码', 'XXL码', 20, 200, 3),
-- 钢笔礼盒
(35, '颜色', '黑色',   0, 150, 1),
(35, '颜色', '银色',   0, 100, 2),
(35, '笔尖', '0.38mm', 0, 120, 1),
(35, '笔尖', '0.5mm',  0, 130, 2),
-- 猫粮
(37, '口味', '鸡肉味', 0, 300, 1),
(37, '口味', '鱼肉味', 0, 300, 2),
-- 手表
(46, '颜色', '玫瑰金', 0, 120, 1),
(46, '颜色', '银色',   0, 120, 2),
(46, '颜色', '黑色',   0, 60,  3);

-- ============================================================
-- Product images (gallery)
-- ============================================================
INSERT INTO product_image (product_id, url, sort) VALUES
(1,  '/images/product_01a.png', 1),
(1,  '/images/product_01b.png', 2),
(2,  '/images/product_02a.png', 1),
(3,  '/images/product_03a.png', 1),
(3,  '/images/product_03b.png', 2),
(4,  '/images/product_04a.png', 1),
(7,  '/images/product_07a.png', 1),
(7,  '/images/product_07b.png', 2),
(7,  '/images/product_07c.png', 3),
(8,  '/images/product_08a.png', 1),
(8,  '/images/product_08b.png', 2),
(9,  '/images/product_09a.png', 1),
(13, '/images/product_13a.png', 1),
(14, '/images/product_14a.png', 1),
(18, '/images/product_18a.png', 1),
(19, '/images/product_19a.png', 1),
(23, '/images/product_23a.png', 1),
(26, '/images/product_26a.png', 1),
(27, '/images/product_27a.png', 1),
(30, '/images/product_30a.png', 1),
(31, '/images/product_31a.png', 1),
(32, '/images/product_32a.png', 1),
(35, '/images/product_35a.png', 1),
(37, '/images/product_37a.png', 1),
(40, '/images/product_40a.png', 1),
(44, '/images/product_44a.png', 1),
(45, '/images/product_45a.png', 1),
(46, '/images/product_46a.png', 1);

-- ============================================================
-- Sample product comments
-- ============================================================
INSERT INTO product_comment (product_id, user_id, content, rating, created_time) VALUES
(1,  2, '衣服质量很好，纯棉面料很舒服，尺码标准，推荐购买！', 5, NOW()),
(3,  2, '皮质很不错，穿着很舒适，上班通勤很合适', 4, NOW()),
(7,  2, '拍照效果特别好，电池也很耐用，一天一充完全够用', 5, NOW()),
(8,  2, '很轻薄，携带方便，运行速度流畅，外观也很漂亮', 5, NOW()),
(9,  2, '降噪效果不错，音质清晰，佩戴也舒适', 4, NOW()),
(13, 2, '餐具看起来很有质感，送礼也很体面', 5, NOW()),
(14, 2, '面料很舒服，洗了也不褪色，颜色很好看', 4, NOW()),
(18, 2, '保湿效果很好，不黏腻，用了一周皮肤状态好多了', 5, NOW()),
(20, 2, '味道很好闻，洗完头发很蓬松，控油效果也不错', 4, NOW()),
(23, 2, '鞋子很轻便，跑步感觉很好，透气性也不错', 5, NOW()),
(27, 1, '每天一包坚果很方便，日期新鲜，推荐购买', 5, NOW()),
(30, 1, '巧克力口感很好，包装精美，送礼很合适', 5, NOW()),
(31, 2, '纸尿裤吸水性很好，一整夜不用换，回购多次', 5, NOW()),
(37, 1, '猫很喜欢吃，颗粒大小适中，吃了便便正常', 4, NOW()),
(44, 2, '按摩很舒服，热敷功能冬天用太舒服了', 5, NOW()),
(46, 1, '手表很精致，戴着很显气质，赠送的也很好', 5, NOW()),
(35, 2, '钢笔写字顺滑，礼盒包装很有档次，送人合适', 4, NOW()),
(40, 1, '支架很稳，磁吸力强，开车用非常方便', 5, NOW()),
(45, 2, '操作简单，老人也能轻松使用，测量准确', 4, NOW());
