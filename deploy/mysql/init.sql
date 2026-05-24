CREATE DATABASE IF NOT EXISTS `mall_user` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `mall_product` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `mall_order` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `mall_auth` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ============================================================
-- mall_auth
-- ============================================================
USE `mall_auth`;

CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    email VARCHAR(100),
    phone VARCHAR(20),
    avatar VARCHAR(500),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    status INT DEFAULT 0,
    created_time DATETIME NOT NULL,
    updated_time DATETIME NOT NULL,
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `user` (id, username, password, nickname, email, role, status, created_time, updated_time) VALUES
(1, 'admin', '$2a$10$N.ZTmC7qCmU8LqE8v7l0XuRiQQGn0rJIKH6I5q5CJ0wYXBy5G6K2S', 'Admin', 'admin@mall.com', 'SUPER_ADMIN', 0, NOW(), NOW()),
(2, 'user', '$2a$10$N.ZTmC7qCmU8LqE8v7l0XuRiQQGn0rJIKH6I5q5CJ0wYXBy5G6K2S', 'TestUser', 'user@mall.com', 'USER', 0, NOW(), NOW());

-- ============================================================
-- mall_user
-- ============================================================
USE `mall_user`;

CREATE TABLE IF NOT EXISTS user_address (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    receiver_name VARCHAR(50) NOT NULL,
    receiver_phone VARCHAR(20) NOT NULL,
    province VARCHAR(50),
    city VARCHAR(50),
    district VARCHAR(50),
    detail_address VARCHAR(200) NOT NULL,
    is_default INT DEFAULT 0,
    created_time DATETIME NOT NULL,
    updated_time DATETIME NOT NULL,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    type VARCHAR(50),
    status INT DEFAULT 0,
    created_time DATETIME NOT NULL,
    updated_time DATETIME NOT NULL,
    INDEX idx_type (type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50),
    content TEXT NOT NULL,
    contact_info VARCHAR(100),
    status INT DEFAULT 0,
    reply_content TEXT,
    reply_time DATETIME,
    created_time DATETIME NOT NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS consultation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    reply_content TEXT,
    reply_time DATETIME,
    created_time DATETIME NOT NULL,
    INDEX idx_product_id (product_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO notice (title, content, type, status, created_time, updated_time) VALUES
('Welcome to our mall', 'Thank you for registering!', 'system', 0, NOW(), NOW()),
('Summer Promotion', 'Get up to 50% off on selected items', 'promotion', 0, NOW(), NOW());

-- ============================================================
-- mall_product
-- ============================================================
USE `mall_product`;

CREATE TABLE IF NOT EXISTS category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    sort INT DEFAULT 0,
    icon VARCHAR(500),
    created_time DATETIME NOT NULL,
    updated_time DATETIME NOT NULL,
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS banner (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200),
    subtitle VARCHAR(200),
    description TEXT,
    image_url VARCHAR(500) NOT NULL,
    link_url VARCHAR(500),
    btn_text VARCHAR(50),
    align VARCHAR(20) DEFAULT 'left',
    sort INT DEFAULT 0,
    status INT DEFAULT 0,
    created_time DATETIME NOT NULL,
    updated_time DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    original_price DECIMAL(10,2),
    image VARCHAR(500),
    images TEXT,
    category_id BIGINT,
    stock INT DEFAULT 0,
    sales INT DEFAULT 0,
    status INT DEFAULT 0,
    is_recommended INT DEFAULT 0,
    created_time DATETIME NOT NULL,
    updated_time DATETIME NOT NULL,
    INDEX idx_category_id (category_id),
    INDEX idx_status (status),
    INDEX idx_is_recommended (is_recommended),
    FULLTEXT INDEX ft_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS product_spec (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    spec_name VARCHAR(100) NOT NULL,
    spec_value VARCHAR(100) NOT NULL,
    extra_price DECIMAL(10,2) DEFAULT 0,
    stock INT DEFAULT 0,
    image VARCHAR(500),
    sort INT DEFAULT 0,
    INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS product_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    url VARCHAR(500) NOT NULL,
    sort INT DEFAULT 0,
    INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS product_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT,
    rating INT NOT NULL,
    image_urls TEXT,
    created_time DATETIME NOT NULL,
    INDEX idx_product_id (product_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_favorite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_time DATETIME NOT NULL,
    UNIQUE KEY uk_user_product (user_id, product_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO category (id, name, parent_id, sort, created_time, updated_time) VALUES
(1, 'Electronics', 0, 1, NOW(), NOW()),
(2, 'Clothing', 0, 2, NOW(), NOW()),
(3, 'Home & Garden', 0, 3, NOW(), NOW()),
(4, 'Smartphones', 1, 1, NOW(), NOW()),
(5, 'Laptops', 1, 2, NOW(), NOW()),
(6, 'Men''s Clothing', 2, 1, NOW(), NOW()),
(7, 'Women''s Clothing', 2, 2, NOW(), NOW());

INSERT INTO banner (title, subtitle, description, image_url, link_url, btn_text, align, sort, status, created_time, updated_time) VALUES
('Summer Sale', 'Up to 50% off', 'Summer promotion on all items', '/images/banner1.jpg', '/product/list', 'Shop Now', 'left', 1, 0, NOW(), NOW()),
('New Arrivals', 'Check out new products', 'Latest products just arrived', '/images/banner2.jpg', '/product/list', 'Explore', 'center', 2, 0, NOW(), NOW());

INSERT INTO product (id, name, description, price, image, category_id, stock, sales, status, is_recommended, created_time, updated_time) VALUES
(1, 'iPhone 15', 'Latest Apple smartphone', 6999.00, '/images/product1.jpg', 4, 100, 50, 0, 1, NOW(), NOW()),
(2, 'MacBook Air M3', 'Lightweight laptop', 8999.00, '/images/product2.jpg', 5, 50, 30, 0, 1, NOW(), NOW()),
(3, 'Cotton T-Shirt', 'Comfortable cotton t-shirt', 199.00, '/images/product3.jpg', 6, 200, 120, 0, 0, NOW(), NOW());

INSERT INTO product_spec (product_id, spec_name, spec_value, extra_price, stock, sort) VALUES
(1, 'Color', 'Black', 0, 50, 1),
(1, 'Color', 'White', 0, 50, 2),
(1, 'Storage', '128GB', 0, 30, 1),
(1, 'Storage', '256GB', 500, 20, 2),
(2, 'Color', 'Silver', 0, 25, 1),
(2, 'Color', 'Space Gray', 0, 25, 2),
(3, 'Size', 'M', 0, 50, 1),
(3, 'Size', 'L', 0, 50, 2),
(3, 'Color', 'Black', 0, 40, 1),
(3, 'Color', 'White', 0, 40, 2);

INSERT INTO product_image (product_id, url, sort) VALUES
(1, '/images/product1_1.jpg', 1),
(1, '/images/product1_2.jpg', 2),
(2, '/images/product2_1.jpg', 1),
(3, '/images/product3_1.jpg', 1);

INSERT INTO product_comment (product_id, user_id, content, rating, created_time) VALUES
(1, 2, 'Great phone!', 5, NOW()),
(2, 2, 'Very lightweight and fast', 4, NOW());

-- ============================================================
-- mall_order
-- ============================================================
USE `mall_order`;

CREATE TABLE IF NOT EXISTS cart (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT DEFAULT 1,
    checked INT DEFAULT 1,
    spec_info VARCHAR(500) DEFAULT NULL,
    created_time DATETIME NOT NULL,
    updated_time DATETIME NOT NULL,
    INDEX idx_user_id (user_id),
    KEY uk_user_product (user_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `order` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_no VARCHAR(32) NOT NULL UNIQUE,
    total_amount DECIMAL(10,2) NOT NULL,
    status INT DEFAULT 0,
    payment_method VARCHAR(50),
    payment_time DATETIME,
    delivery_time DATETIME,
    receive_time DATETIME,
    receiver_name VARCHAR(50) NOT NULL,
    receiver_phone VARCHAR(20) NOT NULL,
    receiver_address VARCHAR(200) NOT NULL,
    created_time DATETIME NOT NULL,
    updated_time DATETIME NOT NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_order_no (order_no),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT,
    product_name VARCHAR(200),
    product_image VARCHAR(500),
    spec_info VARCHAR(200),
    price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    order_no VARCHAR(32) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    method VARCHAR(50),
    status INT DEFAULT 0,
    pay_time DATETIME,
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
