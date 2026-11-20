# Shop Platform 数据库架构与全量数据字典（Database Schema & Data Dictionary）

> **数据库版本**：MySQL 8.4+  
> **默认字符集**：`utf8mb4`  
> **默认排序规则**：`utf8mb4_unicode_ci`  
> **存储引擎**：InnoDB（支持行级锁、ACID 事务与外键机制）  
> **目标数据库名**：`shop_db`  
> **DDL 脚本**：[`schema.sql`](file:///D:/JavaWeb/docs/sql/schema.sql)  
> **种子数据脚本**：[`data.sql`](file:///D:/JavaWeb/docs/sql/data.sql)

---

## 目录（Table of Contents）

1. [数据库架构与设计规范](#1-数据库架构与设计规范)
2. [数据表全景概览](#2-数据表全景概览)
3. [核心数据字典（7 张核心数据表详解）](#3-核心数据字典7-张核心数据表详解)
   - [3.1 系统用户表 (`sys_user`)](#31-系统用户表-sys_user)
   - [3.2 商品品牌表 (`shop_brand`)](#32-商品品牌表-shop_brand)
   - [3.3 商品分类表 (`shop_category`)](#33-商品分类表-shop_category)
   - [3.4 商品信息表 (`shop_product`)](#34-商品信息表-shop_product)
   - [3.5 购物车行项表 (`shop_cart_item`)](#35-购物车行项表-shop_cart_item)
   - [3.6 交易主订单表 (`shop_order`)](#36-交易主订单表-shop_order)
   - [3.7 订单快照明细表 (`shop_order_item`)](#37-订单快照明细表-shop_order_item)
4. [领域驱动设计（DDD）映射与防腐设计](#4-领域驱动设计ddd映射与防腐设计)
   - [4.1 统一通用语言解耦：`customerId` 与 `user_id` 映射](#41-统一通用语言解耦customerid-与-user_id-映射)
   - [4.2 消除数据泥团：`DeliveryAddress` 值对象封装](#42-消除数据泥团deliveryaddress-值对象封装)
   - [4.3 充血实体计算行为：小计内聚消除特性依恋](#43-充血实体计算行为小计内聚消除特性依恋)
5. [数据库初始化与种子数据注入机制](#5-数据库初始化与种子数据注入机制)
   - [5.1 本地开发环境手工/脚本初始化](#51-本地开发环境手工脚本初始化)
   - [5.2 容器化自动化初始化 (`docker-entrypoint-initdb.d`)](#52-容器化自动化初始化-docker-entrypoint-initdbd)

---

## 1. 数据库架构与设计规范

本项目持久层遵循现代企业级与领域驱动设计（DDD）规范：

- **主键规范**：全表统一采用 `BIGINT NOT NULL AUTO_INCREMENT` 作为物理自增主键，保障 B+ 树叶子节点顺序插入，避免聚簇索引页频繁分裂。
- **软删除规范（Soft Deletion）**：用户（`sys_user`）、品牌（`shop_brand`）、分类（`shop_category`）、商品（`shop_product`）4 张基础主数据表均包含 `is_deleted TINYINT NOT NULL DEFAULT 0` 列，对接 MyBatis-Plus 全局 `@TableLogic` 机制，查询自动拼接 `WHERE is_deleted = 0`，删除自动降级为更新；而其余 3 张表**刻意不设软删除列**：`shop_cart_item` 属于高频增删暂存表且建有 `(user_id, product_id)` 唯一联合索引（必须物理 `DELETE` 避免移除后再次加购触发唯一键冲突），`shop_order` 与 `shop_order_item` 属于不可抹除的商业财务凭证（取消交易仅允许状态流转为 `CANCELLED`，严禁任何删除操作，且 `shop_order_item` 不设 `updated_at` 以宣示只读快照属性）。
- **审计规范（Audit Columns）**：业务表统一预留 `created_at`（创建时间）与 `updated_at`（修改时间），结合 MySQL 原生 `DEFAULT CURRENT_TIMESTAMP` 和 `ON UPDATE CURRENT_TIMESTAMP` 以及 MyBatis-Plus 的 `FieldFill.INSERT` / `FieldFill.INSERT_UPDATE` 实现数据库与应用层双重自适应填充。
- **金额精度规范**：所有商品标价、销售价、小计、订单总额均采用高精度 `DECIMAL(10, 2)` 存储，严禁使用浮点数 `FLOAT` 或 `DOUBLE`，Java 实体对应 `java.math.BigDecimal`。
- **不可变快照隔离**：订单项（`shop_order_item`）在支付下单瞬间固化商品的标题、图片、单价快照，解耦商品目录后续修改对历史交易证据的影响。

---

## 2. 数据表全景概览

| 表名 | 实体类全限定名 | 逻辑功能描述 | 记录量级/设计特点 |
|---|---|---|---|
| `sys_user` | `com.shop.modules.system.entity.SysUser` | 系统用户账号、密码密文与基础角色 | 支撑 RBAC 权限与 BCrypt 安全认证 |
| `shop_brand` | `com.shop.modules.catalog.entity.ShopBrand` | 商品品牌、产地与品牌 Logo | 包含扩展字段 `origin`（产地）与排序索引 |
| `shop_category` | `com.shop.modules.catalog.entity.ShopCategory` | 商品多级分类树形结构 | 父子层级自关联，索引加速 `parent_id` 检索 |
| `shop_product` | `com.shop.modules.catalog.entity.ShopProduct` | 商品主表（标题、价格、库存、富文本） | 关联品牌与分类，单 SQL 条件防超卖核心实体 |
| `shop_cart_item` | `com.shop.modules.commerce.entity.ShopCartItem` | 顾客在线购物车行项 | 顾客唯一联合键 (`uk_user_product`)，充血小计 |
| `shop_order` | `com.shop.modules.commerce.entity.ShopOrder` | 交易主订单（单号、金额、配送地址、状态） | 状态机驱动，封装 `DeliveryAddress` 值对象 |
| `shop_order_item` | `com.shop.modules.commerce.entity.ShopOrderItem` | 订单商品行项不可变历史快照 | 冗余快照字段，永久冻结下单时刻商品属性 |

---

## 3. 核心数据字典（7 张核心数据表详解）

### 3.1 系统用户表 (`sys_user`)

- **表名**：`sys_user`
- **说明**：系统账户主表，存储登录凭证、加密密码、展示资料与系统角色。
- **映射实体**：[`SysUser.java`](file:///D:/JavaWeb/backend/src/main/java/com/shop/modules/system/entity/SysUser.java)

| 列名 | 数据类型 | 可空 | 默认值 | 约束 / 索引 | 业务含义与说明 |
|---|---|:---:|---|---|---|
| `id` | `BIGINT` | 否 | `AUTO_INCREMENT` | `PRIMARY KEY` | 用户唯一自增主键 |
| `username` | `VARCHAR(64)` | 否 | 无 | `UNIQUE KEY uk_username` | 登录用户名（唯一） |
| `password` | `VARCHAR(128)` | 否 | 无 | 无 | BCrypt 动态加盐哈希密文（形如 `$2a$10$...`） |
| `nickname` | `VARCHAR(64)` | 是 | `NULL` | 无 | 昵称 / 真实姓名 |
| `avatar` | `VARCHAR(255)` | 是 | `NULL` | 无 | 用户头像图片 URL |
| `role` | `VARCHAR(32)` | 否 | `'ROLE_CUSTOMER'` | 无 | 角色标识：`ROLE_CUSTOMER`（顾客）或 `ROLE_ADMIN`（管理员） |
| `status` | `TINYINT` | 否 | `1` | 无 | 账号状态：`1` 正常激活，`0` 禁用冻结 |
| `is_deleted` | `TINYINT` | 否 | `0` | 无 | 软删除标记：`0` 正常，`1` 已注销删除 |
| `created_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | 无 | 账号注册创建时间 |
| `updated_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | 无 | 资料最近更新时间 |

---

### 3.2 商品品牌表 (`shop_brand`)

- **表名**：`shop_brand`
- **说明**：商品所属品牌目录，记录品牌名称、产地国家或地区、排序权重与视觉 Logo。
- **映射实体**：[`ShopBrand.java`](file:///D:/JavaWeb/backend/src/main/java/com/shop/modules/catalog/entity/ShopBrand.java)

| 列名 | 数据类型 | 可空 | 默认值 | 约束 / 索引 | 业务含义与说明 |
|---|---|:---:|---|---|---|
| `id` | `BIGINT` | 否 | `AUTO_INCREMENT` | `PRIMARY KEY` | 品牌唯一自增主键 |
| `name` | `VARCHAR(64)` | 否 | 无 | 无 | 品牌名称（如 Apple, Huawei） |
| `origin` | `VARCHAR(64)` | 是 | `NULL` | 无 | **品牌产地/发源国家或地区**（如“美国加利福尼亚”、“中国深圳”） |
| `logo` | `VARCHAR(255)` | 是 | `NULL` | 无 | 品牌 Logo 图标 URL |
| `description` | `VARCHAR(512)` | 是 | `NULL` | 无 | 品牌故事/详细简介 |
| `sort` | `INT` | 否 | `0` | `KEY idx_sort` | 排序权重（升序排序，数值越小越靠前） |
| `status` | `TINYINT` | 否 | `1` | 无 | 状态：`1` 正常显示，`0` 停用隐藏 |
| `is_deleted` | `TINYINT` | 否 | `0` | 无 | 软删除标记：`0` 正常，`1` 已删除 |
| `created_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | 无 | 品牌创建时间 |
| `updated_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | 无 | 品牌资料更新时间 |

> **字段特别说明**：`origin VARCHAR(64)` 为本项目核心扩展字段，实体类对应 `private String origin;`，在前台首页品牌墙、商品详情页与后台品牌管理对话框（含关键字检索）均有直接数据交互，并通过 Redis 分区键（`catalog:brand:featured:*`、`catalog:brand:portal:all`、`catalog:brand:all`、`catalog:brand:list:*`）提供高性能缓存支撑。

---

### 3.3 商品分类表 (`shop_category`)

- **表名**：`shop_category`
- **说明**：商品分类目录体系，通过 `parent_id` 实现自关联多级无限树形层级（如 一级“手机数码” -> 二级“智能手机”）。
- **映射实体**：[`ShopCategory.java`](file:///D:/JavaWeb/backend/src/main/java/com/shop/modules/catalog/entity/ShopCategory.java)

| 列名 | 数据类型 | 可空 | 默认值 | 约束 / 索引 | 业务含义与说明 |
|---|---|:---:|---|---|---|
| `id` | `BIGINT` | 否 | `AUTO_INCREMENT` | `PRIMARY KEY` | 分类唯一自增主键 |
| `parent_id` | `BIGINT` | 否 | `0` | `KEY idx_parent_id` | 父分类 ID（`0` 代表顶级根分类） |
| `name` | `VARCHAR(64)` | 否 | 无 | 无 | 分类名称（如“电脑办公”、“轻薄笔记本”） |
| `icon` | `VARCHAR(255)` | 是 | `NULL` | 无 | 分类图标类名或图片 URL |
| `sort` | `INT` | 否 | `0` | 无 | 排序权重（升序排列） |
| `status` | `TINYINT` | 否 | `1` | 无 | 状态：`1` 启用，`0` 禁用 |
| `is_deleted` | `TINYINT` | 否 | `0` | 无 | 软删除标记：`0` 正常，`1` 已删除 |
| `created_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | 无 | 分类创建时间 |
| `updated_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | 无 | 分类更新时间 |

---

### 3.4 商品信息表 (`shop_product`)

- **表名**：`shop_product`
- **说明**：商城核心商品目录表，记录商品标价、销售价、物理库存、多图及富文本介绍。
- **映射实体**：[`ShopProduct.java`](file:///D:/JavaWeb/backend/src/main/java/com/shop/modules/catalog/entity/ShopProduct.java)

| 列名 | 数据类型 | 可空 | 默认值 | 约束 / 索引 | 业务含义与说明 |
|---|---|:---:|---|---|---|
| `id` | `BIGINT` | 否 | `AUTO_INCREMENT` | `PRIMARY KEY` | 商品唯一自增主键 |
| `brand_id` | `BIGINT` | 否 | 无 | `KEY idx_brand` | 关联所属品牌 ID（对应 `shop_brand.id`） |
| `category_id` | `BIGINT` | 否 | 无 | `KEY idx_category` | 关联所属分类 ID（对应 `shop_category.id`） |
| `name` | `VARCHAR(128)` | 否 | 无 | 无 | 商品全称标题 |
| `subtitle` | `VARCHAR(255)` | 是 | `NULL` | 无 | 商品副标题 / 卖点一句话推荐 |
| `main_image` | `VARCHAR(255)` | 否 | 无 | 无 | 商品主图封面 URL（列表展示与默认图） |
| `sub_images` | `TEXT` | 是 | `NULL` | 无 | 轮播画廊副图 URL 集合（JSON 或逗号分隔） |
| `price` | `DECIMAL(10,2)` | 否 | `0.00` | `KEY idx_price` | 当前实际销售单价 |
| `original_price`| `DECIMAL(10,2)` | 否 | `0.00` | 无 | 市场划线标价 / 厂商建议零售价（MSRP） |
| `stock` | `INT` | 否 | `0` | 无 | 可用物理库存数量（并发扣减核心资源） |
| `status` | `TINYINT` | 否 | `1` | 无 | 上架状态：`1` 在售上架，`0` 仓库下架 |
| `detail_html` | `MEDIUMTEXT` | 是 | `NULL` | 无 | 商品详情富文本 HTML 代码 |
| `is_deleted` | `TINYINT` | 否 | `0` | 无 | 软删除标记：`0` 正常，`1` 已下架回收 |
| `created_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | 无 | 商品创建时间 |
| `updated_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | 无 | 商品最近编辑修改时间 |

> **高并发防超卖核心机制**：库存扣减通过 `ShopProductMapper.decrementStock(productId, quantity)` 执行单条原子 SQL：
> ```sql
> UPDATE shop_product SET stock = stock - #{quantity} 
> WHERE id = #{productId} AND stock >= #{quantity} AND is_deleted = 0;
> ```
> 借助 InnoDB 聚簇索引行级排他锁保证原子判定，彻底避免高并发秒杀场景下的超卖问题。

---

### 3.5 购物车行项表 (`shop_cart_item`)

- **表名**：`shop_cart_item`
- **说明**：顾客购物车商品暂存行项，支持独立勾选状态与购买件数调整。
- **映射实体**：[`ShopCartItem.java`](file:///D:/JavaWeb/backend/src/main/java/com/shop/modules/commerce/entity/ShopCartItem.java)

| 列名 | 数据类型 | 可空 | 默认值 | 约束 / 索引 | 业务含义与说明 |
|---|---|:---:|---|---|---|
| `id` | `BIGINT` | 否 | `AUTO_INCREMENT` | `PRIMARY KEY` | 购物车条目唯一主键 |
| `user_id` | `BIGINT` | 否 | 无 | `UNIQUE KEY uk_user_product` (联合 1), `KEY idx_user_id` | 拥有该购物车的用户 ID（DDD 映射为 `customerId`） |
| `product_id` | `BIGINT` | 否 | 无 | `UNIQUE KEY uk_user_product` (联合 2) | 加入购物车的商品 ID |
| `quantity` | `INT` | 否 | `1` | 无 | 计划购买数量 |
| `checked` | `TINYINT` | 否 | `1` | 无 | 结算选中标记：`1` 已勾选，`0` 未勾选 |
| `created_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | 无 | 首次加入购物车时间 |
| `updated_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | 无 | 修改数量或勾选状态时间 |

> **索引说明**：`UNIQUE KEY uk_user_product (user_id, product_id)` 确保同一顾客对同一商品在购物车中仅有一条物理记录；重复加购时在 Service 层执行原子累加而非新增记录。

---

### 3.6 交易主订单表 (`shop_order`)

- **表名**：`shop_order`
- **说明**：商城商业交易主表，存储唯一业务订单流水号、支付金额、收货人地址信息与订单生命周期状态。
- **映射实体**：[`ShopOrder.java`](file:///D:/JavaWeb/backend/src/main/java/com/shop/modules/commerce/entity/ShopOrder.java)

| 列名 | 数据类型 | 可空 | 默认值 | 约束 / 索引 | 业务含义与说明 |
|---|---|:---:|---|---|---|
| `id` | `BIGINT` | 否 | `AUTO_INCREMENT` | `PRIMARY KEY` | 订单唯一自增主键 |
| `order_no` | `VARCHAR(64)` | 否 | 无 | `UNIQUE KEY uk_order_no` | 全局唯一业务订单编号（毫秒时间序列 + 4 位随机码生成） |
| `user_id` | `BIGINT` | 否 | 无 | `KEY idx_user_order` (复合 1) | 下单顾客 ID（DDD 映射为 `customerId`） |
| `total_amount` | `DECIMAL(10,2)` | 否 | `0.00` | 无 | 订单结算实付总金额 |
| `receiver_name` | `VARCHAR(64)` | 否 | 无 | 无 | 配送收货人姓名 |
| `receiver_phone`| `VARCHAR(32)` | 否 | 无 | 无 | 配送联系电话 |
| `receiver_address`| `VARCHAR(255)`| 否 | 无 | 无 | 详细收货物理地址 |
| `status` | `VARCHAR(32)` | 否 | `'UNPAID'` | 无 | 订单生命周期状态（`UNPAID`, `PAID`, `SHIPPED`, `COMPLETED`, `CANCELLED`） |
| `payment_time` | `DATETIME` | 是 | `NULL` | 无 | 支付成功回调时间戳 |
| `created_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | `KEY idx_user_order` (复合 2) | 下单生成时间 |
| `updated_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | 无 | 订单状态最后流转时间 |

> **复合索引说明**：`KEY idx_user_order (user_id, created_at)` 专门针对顾客“我的订单”历史列表按照下单时间倒序分页进行深度性能优化，完全避免 `filesort`。

---

### 3.7 订单快照明细表 (`shop_order_item`)

- **表名**：`shop_order_item`
- **说明**：订单项历史快照表，存储每个订单所包含的具体商品快照数据（标题、主图、购买单价、数量、小计）。
- **映射实体**：[`ShopOrderItem.java`](file:///D:/JavaWeb/backend/src/main/java/com/shop/modules/commerce/entity/ShopOrderItem.java)

| 列名 | 数据类型 | 可空 | 默认值 | 约束 / 索引 | 业务含义与说明 |
|---|---|:---:|---|---|---|
| `id` | `BIGINT` | 否 | `AUTO_INCREMENT` | `PRIMARY KEY` | 明细项唯一自增主键 |
| `order_id` | `BIGINT` | 否 | 无 | `KEY idx_order_id` | 所属主订单物理 ID（关联 `shop_order.id`） |
| `order_no` | `VARCHAR(64)` | 否 | 无 | 无 | 冗余所属主订单业务编号（加速单据对账与检索） |
| `product_id` | `BIGINT` | 否 | 无 | 无 | 所购商品物理 ID（关联 `shop_product.id`） |
| `product_name` | `VARCHAR(128)` | 否 | 无 | 无 | **下单瞬间商品名称不可变快照** |
| `product_image`| `VARCHAR(255)` | 否 | 无 | 无 | **下单瞬间商品主图不可变快照** |
| `unit_price` | `DECIMAL(10,2)` | 否 | `0.00` | 无 | **下单瞬间成交单价不可变快照** |
| `quantity` | `INT` | 否 | `1` | 无 | 购买件数 |
| `total_price` | `DECIMAL(10,2)` | 否 | `0.00` | 无 | 行项小计金额（`unit_price * quantity`） |
| `created_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | 无 | 快照创建生成时间 |

> **交易历史不可篡改性**：即便商家在后台修改了对应商品的标题、价格、封面，或者执行了商品软删除，用户查看历史订单时仍然看到当时成交的真实商品图文与价格。

---

## 4. 领域驱动设计（DDD）映射与防腐设计

物理数据库 Schema 侧重于关系型存储的范式化与检索效率，而业务领域模型则侧重于通用语言的严谨性与业务行为的内聚。本项目通过 MyBatis-Plus 实现了经典 DDD 防腐层映射。

### 4.1 统一通用语言解耦：`customerId` 与 `user_id` 映射

在通用语言（Ubiquitous Language）中，电商商业上下文的操作主体是“顾客（`Customer`）”，而非通用的系统登录账号“用户（`User`）”。为了防止系统用户概念污染电商领域模型，同时保持底层数据表的命名习惯：

- **数据库物理列名**：`shop_cart_item` 与 `shop_order` 中的列名为 `user_id`；
- **Java 实体属性定义**：在 `ShopCartItem.java` 和 `ShopOrder.java` 中声明：
  ```java
  @TableField("user_id")
  private Long customerId;
  ```
- **业务收益**：
  1. 商业领域的 Service 契约（如 `OrderCheckoutService`、`CartService`）入参统一为 `Long customerId`，消除命名二义性；
  2. 实体内部提供 `getUserId()` / `setUserId()` 兼容方法，实现与第三方框架无缝对接；
  3. MyBatis-Plus 在动态生成 SQL 时自动将 `customerId` 映射为 `user_id`，底层对业务完全透明。

### 4.2 消除数据泥团：`DeliveryAddress` 值对象封装

在 `shop_order` 数据表中，收货人信息被拍平存储为 3 个物理列：
- `receiver_name VARCHAR(64)`
- `receiver_phone VARCHAR(32)`
- `receiver_address VARCHAR(255)`

如果在 Java 实体与 Service 层继续将这三者分散传递，就会形成 Martin Fowler 在《重构》中指出的经典坏味道——**数据泥团（Data Clumps）**。本项目通过不可变值对象（Value Object）进行了优雅重构：

- **值对象类**：[`com.shop.modules.commerce.dto.DeliveryAddress`](file:///D:/JavaWeb/backend/src/main/java/com/shop/modules/commerce/dto/DeliveryAddress.java)
- **订单实体内聚**：在 `ShopOrder.java` 中：
  ```java
  @TableField("receiver_name")
  private String receiverName;

  @TableField("receiver_phone")
  private String receiverPhone;

  @TableField("receiver_address")
  private String receiverAddress;

  @TableField(exist = false)
  private DeliveryAddress deliveryAddress;

  public DeliveryAddress getDeliveryAddress() {
      if (deliveryAddress != null) {
          return deliveryAddress;
      }
      if (receiverName != null || receiverPhone != null || receiverAddress != null) {
          this.deliveryAddress = new DeliveryAddress(receiverName, receiverPhone, receiverAddress);
          return deliveryAddress;
      }
      return null;
  }

  public void setDeliveryAddress(DeliveryAddress deliveryAddress) {
      this.deliveryAddress = deliveryAddress;
      if (deliveryAddress != null) {
          this.receiverName = deliveryAddress.getReceiverName();
          this.receiverPhone = deliveryAddress.getReceiverPhone();
          this.receiverAddress = deliveryAddress.getReceiverAddress();
      }
  }
  ```
- **业务收益**：
  1. 概念完整：收货信息作为一个原子业务概念在订单结算、审核、流转中传递；
  2. 防御性校验：`DeliveryAddress` 内置的三字段均带 `@NotBlank` / `@Size` Bean Validation 约束，配合构造器与 Setter 的统一 `trim()` 净化，在控制器层完成收货信息的非空与长度合法性校验；
  3. 非破坏性保护：实体 Setter 内部处理映射，传入 `null` 时不破坏既有数据。

> 注：`DeliveryAddress` 当前为**可变** DTO（提供 3 个 Setter 以配合 JSON 反序列化与实体映射），其值对象语义由"构造器统一净化 + `equals`/`hashCode` 按值比较 + 三字段高内聚传递"共同体现；若需严格不可变，可后续移除 Setter 并改用 `@JsonCreator` 反序列化。

### 4.3 充血实体计算行为：小计内聚消除特性依恋

传统“贫血模型（Anemic Domain Model）”中，实体仅包含 Getter/Setter，所有计算逻辑被散落到 Service 层的循环遍历中，产生“特性依恋（Feature Envy）”。本项目将计算行为回归实体本身：

- **购物车小计**：`ShopCartItem.calculateSubtotal(unitPrice)`
  ```java
  public BigDecimal calculateSubtotal(BigDecimal unitPrice) {
      if (unitPrice == null || this.quantity == null) {
          return BigDecimal.ZERO;
      }
      return unitPrice.multiply(BigDecimal.valueOf(this.quantity));
  }
  ```
- **订单项小计**：`ShopOrderItem.calculateSubtotal()`
  ```java
  public BigDecimal calculateSubtotal() {
      if (this.unitPrice == null || this.quantity == null) {
          return BigDecimal.ZERO;
      }
      return this.unitPrice.multiply(BigDecimal.valueOf(this.quantity));
  }
  ```

---

## 5. 数据库初始化与种子数据注入机制

本项目支持本地开发与生产容器编排两种无缝初始化机制，保证数据库在任何干净环境下一键就绪。

### 5.1 本地开发环境手工/脚本初始化

在本地 Windows / macOS / Linux 开发环境下，已安装 MySQL 8.4 服务的开发者可直接运行如下指令完成库表创建与种子数据导入：

```bash
# 1. 登录 MySQL 并执行建表 DDL (自动创建 shop_db 库并构建 7 张表结构)
mysql -u root -p < docs/sql/schema.sql

# 2. 执行种子数据 DML (插入初始管理员、演示顾客、6大品牌、9大多级分类、7大商品)
mysql -u root -p < docs/sql/data.sql
```

或者使用主流数据库管理工具（Navicat、DBeaver、DataGrip、IntelliJ IDEA Database 工具窗口）：
1. 打开 `docs/sql/schema.sql`，选中并“作为 SQL 运行”；
2. 打开 `docs/sql/data.sql`，执行导入预设演示数据。

### 5.2 容器化自动化初始化 (`docker-entrypoint-initdb.d`)

生产编排 [`deploy/docker-compose.prod.yml`](file:///D:/JavaWeb/deploy/docker-compose.prod.yml) 与试用编排 [`deploy/trial/docker-compose.yml`](file:///D:/JavaWeb/deploy/trial/docker-compose.yml) 均利用官方 MySQL 镜像的声明式初始化特性，且在安全加固后**统一只挂载建表脚本 `schema.sql`，不再自动挂载 `data.sql`**：

- **生产编排**：只读挂载仓库内的 `../docs/sql/schema.sql`；
- **试用编排**：只读挂载部署包同级的 `./sql/schema.sql`（打包时从 `docs/sql/` 复制而来），**刻意不挂载 `data.sql`**，杜绝重装或初始化时将公开的默认管理员口令自动写入公网环境。

> 容器化首次部署后数据库仅有 7 张空表结构，需按主 [README](file:///D:/JavaWeb/README.md) 的「管理员账号初始化」创建管理员账号；仅本地离线演示若确需导入演示商品与测试账号，才手动执行 `data.sql`。

```yaml
services:
  shop-mysql:
    image: mysql:8.4
    container_name: shop-mysql
    restart: always
    mem_limit: 1400m
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD:-Shop_Trial_2026!}
      MYSQL_DATABASE: ${DB_NAME:-shop_db}
      TZ: Asia/Shanghai
    volumes:
      - ./data/mysql:/var/lib/mysql
      - ./sql/schema.sql:/docker-entrypoint-initdb.d/01_schema.sql:ro
      # 刻意不挂载 data.sql，避免默认口令随容器初始化自动落库
```

**工作执行原理**：
1. 容器首次启动时，挂载的数据目录为空（生产为 `mysql_data` 卷，试用为 `./data/mysql` 目录）；
2. 容器启动脚本 `entrypoint` 自动检测到空库，读取挂载在 `/docker-entrypoint-initdb.d/01_schema.sql` 下的 DDL 脚本，自动构建完整的 `shop_db` 7 张表体系与索引；
3. 一旦数据目录初始化完成，后续容器重启不再重复执行初始化。试用环境如需清空重置库表，可执行 `docker compose down && rm -rf ./data/mysql && docker compose up -d`。
