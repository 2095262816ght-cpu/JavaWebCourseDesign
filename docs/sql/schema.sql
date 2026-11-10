-- ========================================================
-- Shop Platform Database Schema (MySQL 8.4+)
-- Charset: utf8mb4, Collation: utf8mb4_unicode_ci
-- ========================================================

CREATE DATABASE IF NOT EXISTS `shop_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `shop_db`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- --------------------------------------------------------
-- Table: sys_user
-- --------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `username` VARCHAR(64) NOT NULL COMMENT 'Username for login',
  `password` VARCHAR(128) NOT NULL COMMENT 'BCrypt encrypted password',
  `nickname` VARCHAR(64) DEFAULT NULL COMMENT 'Display name',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'Avatar image URL',
  `role` VARCHAR(32) NOT NULL DEFAULT 'ROLE_CUSTOMER' COMMENT 'ROLE_CUSTOMER or ROLE_ADMIN',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1: Active, 0: Disabled',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0: Normal, 1: Deleted',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User Account Table';

-- --------------------------------------------------------
-- Table: shop_brand
-- --------------------------------------------------------
DROP TABLE IF EXISTS `shop_brand`;
CREATE TABLE `shop_brand` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Brand ID',
  `name` VARCHAR(64) NOT NULL COMMENT 'Brand Name',
  `origin` VARCHAR(64) DEFAULT NULL COMMENT 'Brand Origin / Country',
  `logo` VARCHAR(255) DEFAULT NULL COMMENT 'Brand Logo URL',
  `description` VARCHAR(512) DEFAULT NULL COMMENT 'Brand Description',
  `sort` INT NOT NULL DEFAULT 0 COMMENT 'Display Order (Ascending)',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1: Active, 0: Inactive',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0: Normal, 1: Deleted',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
  PRIMARY KEY (`id`),
  KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Product Brand Table';

-- --------------------------------------------------------
-- Table: shop_category
-- --------------------------------------------------------
DROP TABLE IF EXISTS `shop_category`;
CREATE TABLE `shop_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Category ID',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT 'Parent ID (0 for root)',
  `name` VARCHAR(64) NOT NULL COMMENT 'Category Name',
  `icon` VARCHAR(255) DEFAULT NULL COMMENT 'Category Icon URL',
  `sort` INT NOT NULL DEFAULT 0 COMMENT 'Display Order',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1: Active, 0: Inactive',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0: Normal, 1: Deleted',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Product Category Hierarchy Table';

-- --------------------------------------------------------
-- Table: shop_product
-- --------------------------------------------------------
DROP TABLE IF EXISTS `shop_product`;
CREATE TABLE `shop_product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Product ID',
  `brand_id` BIGINT NOT NULL COMMENT 'Brand ID foreign key',
  `category_id` BIGINT NOT NULL COMMENT 'Category ID foreign key',
  `name` VARCHAR(128) NOT NULL COMMENT 'Product Title',
  `subtitle` VARCHAR(255) DEFAULT NULL COMMENT 'Short Tagline/Subtitle',
  `main_image` VARCHAR(255) NOT NULL COMMENT 'Primary Showcase Image URL',
  `sub_images` TEXT DEFAULT NULL COMMENT 'JSON array or comma-separated URLs',
  `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Current Selling Price',
  `original_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Market/MSRP Price',
  `stock` INT NOT NULL DEFAULT 0 COMMENT 'Available Inventory',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1: On Sale, 0: Off Shelf',
  `detail_html` MEDIUMTEXT DEFAULT NULL COMMENT 'Rich HTML Description',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0: Normal, 1: Deleted',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_brand` (`brand_id`),
  KEY `idx_price` (`price`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Product Detail and Catalog Table';

-- --------------------------------------------------------
-- Table: shop_cart_item
-- --------------------------------------------------------
DROP TABLE IF EXISTS `shop_cart_item`;
CREATE TABLE `shop_cart_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Cart Item ID',
  `user_id` BIGINT NOT NULL COMMENT 'Owner User ID',
  `product_id` BIGINT NOT NULL COMMENT 'Product ID',
  `quantity` INT NOT NULL DEFAULT 1 COMMENT 'Purchasing Quantity',
  `checked` TINYINT NOT NULL DEFAULT 1 COMMENT '1: Selected for checkout, 0: Unchecked',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Added Time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated Time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Customer Shopping Cart Table';

-- --------------------------------------------------------
-- Table: shop_order
-- --------------------------------------------------------
DROP TABLE IF EXISTS `shop_order`;
CREATE TABLE `shop_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Order ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT 'Unique Business Order Number',
  `user_id` BIGINT NOT NULL COMMENT 'Customer User ID',
  `total_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Total Order Amount',
  `receiver_name` VARCHAR(64) NOT NULL COMMENT 'Shipping Receiver Name',
  `receiver_phone` VARCHAR(32) NOT NULL COMMENT 'Shipping Contact Phone',
  `receiver_address` VARCHAR(255) NOT NULL COMMENT 'Full Shipping Address',
  `status` VARCHAR(32) NOT NULL DEFAULT 'UNPAID' COMMENT 'UNPAID, PAID, SHIPPED, COMPLETED, CANCELLED',
  `payment_time` DATETIME DEFAULT NULL COMMENT 'Timestamp when payment succeeded',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Order Creation Time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Order Update Time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_order` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Commercial Order Table';

-- --------------------------------------------------------
-- Table: shop_order_item
-- --------------------------------------------------------
DROP TABLE IF EXISTS `shop_order_item`;
CREATE TABLE `shop_order_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Order Item Snapshot ID',
  `order_id` BIGINT NOT NULL COMMENT 'Belongs to Order ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT 'Order Number',
  `product_id` BIGINT NOT NULL COMMENT 'Purchased Product ID',
  `product_name` VARCHAR(128) NOT NULL COMMENT 'Product Title Snapshot at Purchase Time',
  `product_image` VARCHAR(255) NOT NULL COMMENT 'Product Image Snapshot at Purchase Time',
  `unit_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Unit Price Snapshot',
  `quantity` INT NOT NULL DEFAULT 1 COMMENT 'Quantity Purchased',
  `total_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Subtotal (unit_price * quantity)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Time',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Order Product Snapshot Table';

SET FOREIGN_KEY_CHECKS = 1;
