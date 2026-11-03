-- ========================================================
-- Shop Platform Initial Seed Data
-- ========================================================

USE `shop_db`;

SET NAMES utf8mb4;

-- --------------------------------------------------------
-- Users (Password for admin: admin123, for testuser: 123456)
-- --------------------------------------------------------
DELETE FROM `sys_user`;
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `avatar`, `role`, `status`) VALUES
(1, 'admin', '$2a$10$7W6pZavnkFW5AgosezBYnOx3i.15aTEg8o3jNELhKL/OC1oEbI.jK', '平台超级管理员', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&h=200&fit=crop', 'ROLE_ADMIN', 1),
(2, 'customer', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '演示顾客', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&h=200&fit=crop', 'ROLE_CUSTOMER', 1);

-- --------------------------------------------------------
-- Brands
-- --------------------------------------------------------
DELETE FROM `shop_brand`;
INSERT INTO `shop_brand` (`id`, `name`, `logo`, `description`, `sort`, `status`) VALUES
(1, 'Apple', 'https://images.unsplash.com/photo-1611186871348-b1ce696e52c9?w=200&h=200&fit=crop', 'Apple 设计制造 iPhone、Mac、iPad 以及智能穿戴设备。', 1, 1),
(2, 'Huawei', 'https://images.unsplash.com/photo-1580910051074-3eb694886505?w=200&h=200&fit=crop', '华为是全球领先的 ICT 与智能终端提供商，聚焦科技创新。', 2, 1),
(3, 'Xiaomi', 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=200&h=200&fit=crop', '小米，让全球每个人都能享受科技带来的美好生活。', 3, 1),
(4, 'Sony', 'https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=200&h=200&fit=crop', '索尼视听娱乐与影像科技，带来极致声音与视觉享受。', 4, 1),
(5, 'Nike', 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=200&h=200&fit=crop', 'Nike，Just Do It。全球著名运动与跑步装备品牌。', 5, 1),
(6, 'Uniqlo', 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=200&h=200&fit=crop', '优衣库 LifeWear，服适人生，简约而高品质的日常穿着。', 6, 1);

-- --------------------------------------------------------
-- Categories
-- --------------------------------------------------------
DELETE FROM `shop_category`;
INSERT INTO `shop_category` (`id`, `parent_id`, `name`, `icon`, `sort`, `status`) VALUES
(1, 0, '手机数码', 'el-icon-phone', 1, 1),
(2, 0, '电脑办公', 'el-icon-monitor', 2, 1),
(3, 0, '运动户外', 'el-icon-football', 3, 1),
(4, 0, '服饰鞋包', 'el-icon-goods', 4, 1),
(5, 1, '智能手机', 'el-icon-mobile', 1, 1),
(6, 1, '无线耳机', 'el-icon-headset', 2, 1),
(7, 2, '轻薄笔记本', 'el-icon-laptop', 1, 1),
(8, 3, '专业跑鞋', 'el-icon-trophy', 1, 1),
(9, 4, '休闲服饰', 'el-icon-collection-tag', 1, 1);

-- --------------------------------------------------------
-- Products
-- --------------------------------------------------------
DELETE FROM `shop_product`;
INSERT INTO `shop_product` (`id`, `brand_id`, `category_id`, `name`, `subtitle`, `main_image`, `sub_images`, `price`, `original_price`, `stock`, `status`, `detail_html`) VALUES
(1, 1, 5, 'iPhone 16 Pro Max 256GB 原色钛金属', 'A18 Pro芯片，超长续航，4800万像素超广角摄像头', 'https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=800&h=800&fit=crop', 'https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=800&h=800&fit=crop,https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?w=800&h=800&fit=crop', 9999.00, 10999.00, 50, 1, '<p>iPhone 16 Pro Max 强力打造，采用坚固质轻的钛金属设计，配备 6.9 英寸超视网膜 XDR 显示屏与 ProMotion 自适应刷新率技术。</p>'),
(2, 2, 5, '华为 Mate 60 Pro 12GB+512GB 雅川青', '超可靠玄武架构，全焦段超清影像，双向北斗卫星消息', 'https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=800&h=800&fit=crop', 'https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=800&h=800&fit=crop', 6999.00, 7499.00, 30, 1, '<p>华为 Mate 60 Pro 体验遥遥领先的通信与影像实力，支持卫星通话，超耐摔第二代昆仑玻璃。</p>'),
(3, 1, 7, 'MacBook Air 15 英寸 M3 芯片 (16GB+512GB)', '轻薄动力之作，Liquid 视网膜显示屏，18小时超长续航', 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800&h=800&fit=crop', 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800&h=800&fit=crop', 11999.00, 12999.00, 25, 1, '<p>强大的 M3 芯片让日常工作和重度任务都能轻快处理，静音无风扇设计，优雅而强劲。</p>'),
(4, 4, 6, '索尼 WH-1000XM5 无线头戴降噪耳机 铂金银', '双芯驱动旗舰降噪，Hi-Res 高解析度音质，30小时续航', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&h=800&fit=crop', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&h=800&fit=crop', 2299.00, 2699.00, 80, 1, '<p>集成处理器 V1 与 HD 降噪处理器 QN1 双芯片协同，配合 8 个麦克风，带来殿堂级静谧体验。</p>'),
(5, 3, 5, '小米 14 Ultra 16GB+512GB 经典黑', '徕卡全焦段四摄，一英寸可变光圈主摄，双向卫星通信', 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800&h=800&fit=crop', 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800&h=800&fit=crop', 6499.00, 6999.00, 40, 1, '<p>移动影像旗舰力作，徕卡光学Summilux镜头，第三代骁龙8移动平台，打造非凡性能。</p>'),
(6, 5, 8, 'Nike Air Zoom Pegasus 40 缓震跑步鞋', '双重 Zoom Air 气垫，透气工程网眼鞋面，灵敏回弹', 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&h=800&fit=crop', 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&h=800&fit=crop', 699.00, 899.00, 100, 1, '<p>经典小飞马系列第40代升级，更加舒适的中足包裹贴合，为日常慢跑提供源源不断的推进力。</p>'),
(7, 6, 9, '优衣库 AIRism 宽松圆领大版型短袖 T 恤', '吸汗速干黑科技面料，棉质外观柔滑触感，男女同款', 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=800&h=800&fit=crop', 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=800&h=800&fit=crop', 99.00, 149.00, 200, 1, '<p>AIRism 棉混纺面料具有干爽凉感，落肩宽版廓形，潮流百搭，四季舒适。</p>');
