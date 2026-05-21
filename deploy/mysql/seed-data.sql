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
(1,  '服饰鞋包', 0, 1, '/images/categories/shirt.svg', NOW(), NOW()),
(2,  '数码家电', 0, 2, '/images/categories/laptop.svg', NOW(), NOW()),
(3,  '家居生活', 0, 3, '/images/categories/home.svg', NOW(), NOW()),
(4,  '美妆个护', 0, 4, '/images/categories/sparkles.svg', NOW(), NOW()),
(5,  '运动户外', 0, 5, '/images/categories/activity.svg', NOW(), NOW()),

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

(200,  '食品饮料', 0, 6, '/images/categories/food.svg', NOW(), NOW()),
(201,  '母婴用品', 0, 7, '/images/categories/baby.svg', NOW(), NOW()),
(202,  '图书文具', 0, 8, '/images/categories/books.svg', NOW(), NOW()),
(203,  '宠物生活', 0, 9, '/images/categories/pets.svg', NOW(), NOW()),
(204, '汽车用品', 0, 10, '/images/categories/auto.svg', NOW(), NOW()),
(205, '医药健康', 0, 11, '/images/categories/health.svg', NOW(), NOW()),
(206, '珠宝配饰', 0, 12, '/images/categories/jewelry.svg', NOW(), NOW()),

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

-- Products (categories 1~5) - 补充商品确保每专区至少8件
INSERT INTO product (id, name, description, price, original_price, tag, image, category_id, stock, sales, status, is_recommended, created_time, updated_time) VALUES
(49, '纯棉珠地POLO衫',     '珠地棉面料·挺括有型·商务休闲两穿',                    159.00,  229.00,  '新品',  'https://picsum.photos/seed/p49/400/400', 10, 400, 780,  0, 0, NOW(), NOW()),
(50, '法式碎花雪纺衫',     '浪漫碎花·轻盈雪纺·方领设计·荷叶袖',                   239.00,  329.00,  '热卖',  'https://picsum.photos/seed/p50/400/400', 11, 350, 1560, 0, 1, NOW(), NOW()),
(51, '厚底板鞋小白鞋',     '复古板鞋·拼色设计·百搭款·情侣款',                      269.00,  359.00,  '爆款',  'https://picsum.photos/seed/p51/400/400', 12, 500, 3200, 0, 1, NOW(), NOW()),
(52, '头层牛皮斜挎包',     '头层牛皮·质感五金·大容量·通勤约会',                    459.00,  659.00,  '新品',  'https://picsum.photos/seed/p52/400/400', 13, 200, 560,  0, 0, NOW(), NOW()),
(53, 'AI影像折叠屏手机',   '柔性折叠屏·AI影像芯片·IPX8防水·66W快充',              6999.00, 7999.00, '新品',  'https://picsum.photos/seed/p53/400/400', 20, 300, 890,  0, 1, NOW(), NOW()),
(54, '高性能游戏笔记本',   'RTX4060独显·2.5K 165Hz屏·32G内存·1T固态',             7499.00, 8999.00, '热卖',  'https://picsum.photos/seed/p54/400/400', 21, 200, 1450, 0, 1, NOW(), NOW()),
(55, '头戴式降噪耳机',     '自适应降噪·Hi-Res认证·40h续航·蛋白皮耳罩',             899.00,  1199.00, '爆款',  'https://picsum.photos/seed/p55/400/400', 22, 600, 3800, 0, 1, NOW(), NOW()),
(56, '麦饭石不粘锅三件套', '麦饭石涂层·无油烟·少油烹饪·含煎锅汤锅炒锅',            299.00,  399.00,  '热卖',  'https://picsum.photos/seed/p56/400/400', 30, 350, 2100, 0, 1, NOW(), NOW()),
(57, '加厚全遮光窗帘',     '物理遮光·隔热保温·高精密面料·多色可选',                 189.00,  269.00,  '新品',  'https://picsum.photos/seed/p57/400/400', 31, 400, 1200, 0, 0, NOW(), NOW()),
(58, '胶原蛋白紧致面霜',   '重组胶原蛋白·紧致提拉·抗皱淡纹·50g',                    299.00,  399.00,  '新品',  'https://picsum.photos/seed/p58/400/400', 40, 500, 1800, 0, 1, NOW(), NOW()),
(59, '持妆控油粉底液',     '24h持妆·自然哑光·控油不卡粉·遮瑕',                     169.00,  229.00,  '热卖',  'https://picsum.photos/seed/p59/400/400', 41, 700, 4300, 0, 1, NOW(), NOW()),
(60, '高弹力瑜伽服套装',   '四面弹力·高腰设计·裸感亲肤·运动健身',                   299.00,  399.00,  '新品',  'https://picsum.photos/seed/p60/400/400', 50, 450, 2300, 0, 1, NOW(), NOW()),
(61, '双层防暴风自动帐篷', '速开液压·防水防紫外线·3-4人·双层设计',                 399.00,  599.00,  '热卖',  'https://picsum.photos/seed/p61/400/400', 51, 200, 890,  0, 0, NOW(), NOW()),
(62, '海苔肉松蛋卷酥',     '鲜香酥脆·独立包装·海苔肉松夹心',                        29.90,   39.90,   '新品',  'https://picsum.photos/seed/p62/400/400', 210, 1200, 5600, 0, 1, NOW(), NOW()),
(63, '蜂蜜柚子酱冲饮',     '韩国蜂蜜·清香柚子·冷热皆宜·1kg大瓶',                   49.00,   69.00,   '热卖',  'https://picsum.photos/seed/p63/400/400', 211, 800, 3400, 0, 0, NOW(), NOW()),
(64, '有机高铁米粉礼盒',   '有机认证·强化铁锌钙·易消化·不添加糖',                   69.00,   99.00,   '热卖',  'https://picsum.photos/seed/p64/400/400', 220, 600, 4200, 0, 1, NOW(), NOW()),
(65, '婴儿手口湿巾80抽×12','EDI纯水·无酒精无香精·加厚珍珠纹·不连抽',               69.00,   99.00,   '爆款',  'https://picsum.photos/seed/p65/400/400', 221, 2000, 12000, 0, 1, NOW(), NOW()),
(66, '人间值得散文精选集', '治愈系散文·精装典藏·送书签',                            49.00,   69.00,   '热卖',  'https://picsum.photos/seed/p66/400/400', 230, 500, 2800, 0, 0, NOW(), NOW()),
(67, '商务活页皮面笔记本', 'A5活页·仿皮封面·可替换内芯·带笔插',                   35.00,   55.00,   '新品',  'https://picsum.photos/seed/p67/400/400', 231, 900, 3900, 0, 1, NOW(), NOW()),
(68, '冻干生骨肉猫粮',     '98%含肉量·冻干生骨肉·无谷配方·1.5kg',                  159.00,  219.00,  '热卖',  'https://picsum.photos/seed/p68/400/400', 240, 500, 4500, 0, 1, NOW(), NOW()),
(69, '狗狗磨牙洁齿棒',     '牛皮卷·洁齿护齿·耐咬磨牙·中大型犬',                   29.00,   49.00,   '新品',  'https://picsum.photos/seed/p69/400/400', 241, 1500, 6700, 0, 0, NOW(), NOW()),
(70, '4K超清行车记录仪',   '前后双录·超清夜视·停车监控·循环录制',                  399.00,  599.00,  '热卖',  'https://picsum.photos/seed/p70/400/400', 250, 400, 3400, 0, 1, NOW(), NOW()),
(71, '车载香薰精油套装',   '植物萃取·古龙香型·出风口夹·可调浓度',                  69.00,   99.00,   '新品',  'https://picsum.photos/seed/p71/400/400', 251, 800, 2100, 0, 0, NOW(), NOW()),
(72, '智能语音血糖仪',     '免调码·5秒出值·语音播报·100组记忆',                   199.00,  299.00,  '热卖',  'https://picsum.photos/seed/p72/400/400', 260, 600, 3800, 0, 1, NOW(), NOW()),
(73, '乳清蛋白粉增肌粉',   '进口乳清·高蛋白低脂·香草味·2磅装',                    259.00,  359.00,  '新品',  'https://picsum.photos/seed/p73/400/400', 261, 400, 2900, 0, 0, NOW(), NOW()),
(74, '轻奢钢带防水手表',   '镂空表盘·进口机芯·精钢表带·50米防水',                  599.00,  899.00,  '新品',  'https://picsum.photos/seed/p74/400/400', 270, 250, 1200, 0, 0, NOW(), NOW()),
(75, 'S925纯银锁骨链',     '星月吊坠·S925银·精致礼盒·送女友',                      199.00,  299.00,  '热卖',  'https://picsum.photos/seed/p75/400/400', 271, 500, 3400, 0, 1, NOW(), NOW()),
(76, '智能感应垃圾桶',     '红外感应·静音开合·一键打包·15L大容量',                 129.00,  199.00, '新品',  'https://picsum.photos/seed/p76/400/400', 32, 600, 1800, 0, 1, NOW(), NOW()),
(77, '家用梯子折叠人字梯', '加厚铝合金·六步梯·防滑踏板·承重150kg',                 259.00,  359.00, '热卖',  'https://picsum.photos/seed/p77/400/400', 32, 300, 900,  0, 0, NOW(), NOW()),
(78, '护发精油免洗发膜',   '摩洛哥坚果油·修护毛躁·抚平分叉·不油腻',                79.00,   129.00, '热卖',  'https://picsum.photos/seed/p78/400/400', 42, 800, 4500, 0, 1, NOW(), NOW()),
(79, '眼唇卸妆液温和不刺激','水油分层·卸妆力强·不糊眼·敏感肌可用',                  59.00,   89.00,  '爆款',  'https://picsum.photos/seed/p79/400/400', 40, 1200, 8900, 0, 0, NOW(), NOW()),
(80, '专业跳绳计数跳绳',   '轴承静音·防缠绕·精准计数·男女通用',                    39.00,   69.00,  '热卖',  'https://picsum.photos/seed/p80/400/400', 52, 1000, 5600, 0, 1, NOW(), NOW()),
(81, '冰丝防晒袖套',       'UPF50+·冰丝凉感·透气不闷·骑行跑步',                    29.00,   49.00,  '新品',  'https://picsum.photos/seed/p81/400/400', 50, 1500, 3400, 0, 0, NOW(), NOW()),
(82, '即食燕窝冰糖燕窝',   '100%燕窝·冰糖配方·即开即食·礼盒装',                    299.00,  499.00, '新品',  'https://picsum.photos/seed/p82/400/400', 212, 400, 2100, 0, 1, NOW(), NOW()),
(83, '三只松鼠坚果大礼包', '10袋装·每日坚果·当季新货·送礼自用',                    99.00,   159.00, '爆款',  'https://picsum.photos/seed/p83/400/400', 210, 800, 6700, 0, 0, NOW(), NOW()),
(84, '婴儿奶瓶PPSU',       'PPSU材质·宽口径·防胀气·240ml',                         89.00,   129.00, '热卖',  'https://picsum.photos/seed/p84/400/400', 220, 700, 5600, 0, 1, NOW(), NOW()),
(85, '婴儿连体衣春秋款',   '纯棉A类·按扣开襟·柔软亲肤·透气',                       79.00,   119.00, '新品',  'https://picsum.photos/seed/p85/400/400', 220, 500, 3200, 0, 0, NOW(), NOW()),
(86, '儿童早教故事机',     '中英双语·益智早教·蓝牙连接·卡通造型',                   169.00,  239.00, '热卖',  'https://picsum.photos/seed/p86/400/400', 222, 400, 2800, 0, 1, NOW(), NOW()),
(87, '三体全集科幻小说',   '刘慈欣代表作·雨果奖获奖作品·精装典藏',                   89.00,   129.00, '爆款',  'https://picsum.photos/seed/p87/400/400', 230, 1000, 12000,0, 1, NOW(), NOW()),
(88, '英语四六级真题试卷', '2026最新版·含听力·答案解析·送词汇手册',                 49.00,   79.00,  '热卖',  'https://picsum.photos/seed/p88/400/400', 232, 1500, 8900, 0, 0, NOW(), NOW()),
(89, '彩色马克笔套装',     '48色·双头设计·三角笔杆·可水洗',                        59.00,   89.00,  '新品',  'https://picsum.photos/seed/p89/400/400', 232, 900, 4300, 0, 0, NOW(), NOW()),
(90, '猫抓板猫窝一体',     '瓦楞纸·耐磨耐抓·可躺可抓·加厚',                         39.00,   69.00,  '热卖',  'https://picsum.photos/seed/p90/400/400', 242, 600, 4500, 0, 1, NOW(), NOW()),
(91, '宠物除臭消毒喷雾',   '生物酶分解·安全无毒·去味除菌·猫狗通用',                  49.00,   79.00,  '新品',  'https://picsum.photos/seed/p91/400/400', 241, 800, 2300, 0, 0, NOW(), NOW()),
(92, '猫咪零食冻干桶',     '混合冻干·纯肉制作·无添加·500g大桶',                     69.00,   99.00,  '爆款',  'https://picsum.photos/seed/p92/400/400', 240, 700, 5600, 0, 1, NOW(), NOW()),
(93, '汽车遮阳挡前挡',     '加厚铝箔·折叠收纳·隔热防晒·SUV通用',                    39.00,   69.00,  '热卖',  'https://picsum.photos/seed/p93/400/400', 251, 1000, 3400, 0, 1, NOW(), NOW()),
(94, '车载充电器快充',     '双USB·PD快充·智能分流·合金外壳',                        49.00,   79.00,  '新品',  'https://picsum.photos/seed/p94/400/400', 250, 2000, 7800, 0, 0, NOW(), NOW()),
(95, '摩托车头盔电动车',   '3C认证·高强度ABS·双镜片·国标',                          119.00,  179.00, '热卖',  'https://picsum.photos/seed/p95/400/400', 252, 500, 2900, 0, 1, NOW(), NOW()),
(96, '医用外科口罩100只',  '三层防护·熔喷布·透气不闷·独立包装',                     29.00,   49.00,  '爆款',  'https://picsum.photos/seed/p96/400/400', 262, 5000, 20000,0, 1, NOW(), NOW()),
(97, '复合维生素B族片',    'B1B2B6B12·抗疲劳·增强代谢·100片',                      59.00,   89.00,  '热卖',  'https://picsum.photos/seed/p97/400/400', 261, 1200, 5600, 0, 0, NOW(), NOW()),
(98, '膝盖护具保暖护膝',   '自发热保暖·针织面料·运动防护·透气',                     49.00,   79.00,  '新品',  'https://picsum.photos/seed/p98/400/400', 260, 800, 2100, 0, 0, NOW(), NOW()),
(99, '古法传承金手镯',     '足金999·古法工艺·磨砂质感·送礼首选',                   2999.00, 3599.00,'爆款',  'https://picsum.photos/seed/p99/400/400', 271, 100, 890,  0, 1, NOW(), NOW()),
(100,'真丝丝巾女围巾',     '100%桑蚕丝·印花工艺·亲肤舒适·送礼',                    199.00,  299.00, '热卖',  'https://picsum.photos/seed/p100/400/400', 271, 400, 2300, 0, 0, NOW(), NOW()),
(101,'潮流墨镜防晒太阳镜', '偏光镜片·UV400防护·轻量设计·男女同款',                  99.00,   169.00, '新品',  'https://picsum.photos/seed/p101/400/400', 272, 600, 3400, 0, 1, NOW(), NOW());

-- Remove original_price from selected products for visual diversity
UPDATE product SET original_price = NULL WHERE id IN (1,4,6,10,11,15,17,21,22,24,25,28,29,63,33,85,34,36,67,38,69,91,41,71,94,43,73,97,48,74,100);

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
