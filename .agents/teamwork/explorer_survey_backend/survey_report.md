# 后端架构、数据库规范、配置与 API 全景调研报告
**Backend, Database Schemas, Configurations, and REST APIs Technical Survey Report**

> **调研执行代理**：Teamwork Explorer (`explorer_survey_backend`)  
> **调研基准目录**：`D:\JavaWeb`  
> **报告生成时间**：2026-11-05  
> **调研范围**：`backend/` 源码工程、`backend/src/main/resources/` 配置文件、`docs/sql/` 数据库脚本、`deploy/` 部署拓扑、`docs/` 技术文档与 `README.md`。

---

## 一、后端工程骨架与构建依赖调研 (Backend Architecture & Build)

### 1.1 构建与运行时环境
- **构建工具**：Apache Maven (3.9+)
- **项目组织坐标**：
  - `groupId`: `com.shop`
  - `artifactId`: `shop-backend`
  - `version`: `1.0.0-SNAPSHOT`
  - `name`: `shop-backend`
  - `description`: Modern Full-Stack Shop Platform Backend
- **JDK 规范**：
  - `<java.version>`: `21`
  - `<maven.compiler.source>`: `21`
  - `<maven.compiler.target>`: `21`
  - *运行环境实测*：在本地 OpenJDK 25.0.4 运行时环境下完全向下兼容，构建与测试零异常。
- **核心框架版本矩阵**：
  | 框架 / 组件 | 依赖定义与版本 | 核心作用与规范 |
  | :--- | :--- | :--- |
  | **Spring Boot** | `spring-boot-starter-parent: 3.3.4` | 基础微服务脚手架，全面基于 Jakarta EE 规范 |
  | **Spring Web** | `spring-boot-starter-web` (Spring 6.1.13) | RESTful API 路由分发、嵌入式 Tomcat 容器 |
  | **Bean Validation** | `spring-boot-starter-validation` (Hibernate Validator 8.0) | JSR-303/380 参数声明式校验注解 (`@Valid`, `@NotBlank` 等) |
  | **Spring Data Redis** | `spring-boot-starter-data-redis` (Lettuce 驱动) + `commons-pool2` | 旁路缓存读写、分布式 Session 支持、连接池优化 |
  | **MySQL 驱动** | `com.mysql:mysql-connector-j` (版本由父 POM 管理) | 现代 MySQL 8.x/8.4 JDBC 数据库驱动 |
  | **MyBatis-Plus** | `mybatis-plus-spring-boot3-starter: 3.5.7` | 单表零 SQL CRUD、Lambda 类型安全查询、AST 物理分页拦截 |
  | **Sa-Token 权限** | `sa-token-spring-boot3-starter: 1.39.0` | 极简声明式权限拦截、动态 RBAC、用户态上下文绑定 |
  | **Sa-Token Redis** | `sa-token-redis-jackson: 1.39.0` | Sa-Token 会话与状态持久化至 Redis (Jackson 序列化) |
  | **Sa-Token JWT** | `sa-token-jwt: 1.39.0` | JWT Simple 模式无状态 HMAC-SHA256 签名令牌生成 |
  | **SpringDoc OpenAPI** | `springdoc-openapi-starter-webmvc-ui: 2.6.0` | OpenAPI 3 交互式文档与 Swagger UI 界面渲染 |
  | **Spring Boot Test** | `spring-boot-starter-test` (JUnit 5 + Mockito + AssertJ) | 自动化单元测试与并发多线程集成测试 |

### 1.2 源码目录结构全景 (`backend/src/main/java/com/shop`)
```
com.shop
├── ShopApplication.java               # 引导启动类 (@SpringBootApplication + @MapperScan)
├── common/                             # 全局通用底座
│   ├── controller/
│   │   └── CommonUploadController.java # 单文件上传公共接口 (/api/common/upload)
│   ├── exception/
│   │   ├── BusinessException.java      # 统一业务受检异常 (携带业务状态码与提示)
│   │   └── GlobalExceptionHandler.java # @RestControllerAdvice 全局异常捕获兜底
│   ├── result/
│   │   ├── PageResult.java             # 通用分页数据包装 (current, size, total, records)
│   │   ├── Result.java                 # 通用统一响应包装 record (code, message, data, timestamp)
│   │   └── ResultCode.java             # 标准错误码与提示枚举 (200, 400, 401, 403, 404, 500, 1000)
│   └── storage/
│       ├── LocalStorageService.java    # 本地文件存储深模块实现 (防路径穿透、UUID 重命名、资源加载)
│       ├── StorageProperties.java      # 存储配置绑定类 (shop.storage.local-dir / url-prefix)
│       └── StorageService.java          # 存储抽象门面接口 (store, loadAsResource, delete)
├── config/                             # 基础设施与框架装配配置
│   ├── MybatisPlusConfig.java          # MyBatis-Plus 分页拦截器 (MySQL 方言) 与审计时间自动填充
│   ├── OpenApiConfig.java              # Swagger 3.0 元数据声明与 satoken 请求头鉴权组件注入
│   ├── SaTokenConfig.java              # 注册 StpLogicJwtForSimple 实现标准 JWT 令牌
│   ├── StpInterfaceImpl.java           # 实现 Sa-Token 角色/权限接口，动态查库装载 SysUser 角色
│   └── WebMvcConfig.java               # WebMvcConfigurer: /uploads/** 静态映射、CORS、SaInterceptor
└── modules/                            # 业务功能纵向深模块划分
    ├── auth/                           # 认证授权中心
    │   ├── controller/AuthController.java
    │   ├── dto/{AuthTokenVO, LoginCommand, RegisterCommand, UpdateProfileCommand, UserProfileVO}
    │   └── service/{AuthService, impl/AuthServiceImpl}
    ├── catalog/                        # 目录与商品中心
    │   ├── controller/{AdminBrandController, AdminCategoryController, AdminProductController, PortalCatalogController, PortalProductController}
    │   ├── dto/{AdminProductQuery, BrandCommand, BrandQuery, BrandVO, CarouselSlideVO, CategoryCommand, CategoryNodeVO, HomeShowcaseVO, ProductAdminVO, ProductCardVO, ProductCommand, ProductDetailVO, ProductQueryCommand, ProductStatusUpdateDTO}
    │   ├── entity/{ShopBrand, ShopCategory, ShopProduct}
    │   ├── mapper/{ShopBrandMapper, ShopCategoryMapper, ShopProductMapper}
    │   └── service/{BrandService, CategoryService, ProductAdminService, ProductCatalogService, util/CategoryTreeBuilder, util/ProductImageHelper}
    ├── commerce/                       # 商业闭环中心
    │   ├── controller/{AdminOrderController, CartController, OrderController}
    │   ├── dto/{AddCartCommand, AdminOrderQuery, CartItemVO, CartSummaryVO, CheckoutCommand, DeliveryAddress, OrderCheckoutResult, OrderDetailVO, OrderItemVO, OrderSummaryVO, UpdateCartItemCommand}
    │   ├── entity/{ShopCartItem, ShopOrder, ShopOrderItem}
    │   ├── mapper/{ShopCartItemMapper, ShopOrderItemMapper, ShopOrderMapper}
    │   └── service/{CartService, OrderCheckoutService, ProductBatchLookupHelper}
    └── system/                         # 系统管理模块
        ├── controller/AdminUserController.java
        ├── dto/UserStatusUpdateDTO.java
        ├── entity/SysUser.java
        ├── mapper/SysUserMapper.java
        └── service/{UserService, impl/UserServiceImpl}
```

---

## 二、配置文件与环境变量调研 (Configurations & Environment)

### 2.1 默认/开发配置 (`backend/src/main/resources/application.yml`)
- **服务端口**：`server.port = 8080`
- **应用名称**：`spring.application.name = shop-backend`
- **数据源配置 (HikariCP)**：
  - Driver: `com.mysql.cj.jdbc.Driver`
  - URL: `jdbc:mysql://localhost:3306/shop_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true`
  - 用户名/密码: `root` / `ghtnby666`
  - 连接池参数: `min-idle: 5`, `maximum-pool-size: 20`, `idle-timeout: 30000ms`, `connection-timeout: 30000ms`, `max-lifetime: 1800000ms`
- **Redis 缓存配置 (Lettuce)**：
  - 主机/端口: `localhost:6379`
  - 无密码鉴权
  - 超时时间: `3000ms`
  - 连接池参数: `max-active: 16`, `max-idle: 8`, `min-idle: 2`
- **文件上传限制**：
  - 单文件最大: `10MB`，总请求最大: `20MB`
- **MyBatis-Plus 全局设置**：
  - `map-underscore-to-camel-case: true` (开启下划线转驼峰映射)
  - `log-impl: org.apache.ibatis.logging.stdout.StdOutImpl` (控制台输出执行 SQL)
  - `logic-delete-field: isDeleted`, `logic-delete-value: 1`, `logic-not-delete-value: 0`
  - `mapper-locations: classpath*:/mapper/**/*.xml`
- **Sa-Token 鉴权设置**：
  - `token-name: satoken` (HTTP 请求头携带标识)
  - `timeout: 2592000` (有效期 30 天，单位秒)
  - `active-timeout: -1` (永不因闲置超时)
  - `is-concurrent: true`, `is-share: true` (支持多端并发登录与 Token 共享)
  - `token-style: jwt`
  - `jwt-secret-key: shop-platform-jwt-secret-key-32chars-min-abcdef123456`
- **自定义文件存储深模块 (`shop.storage`)**：
  - `local-dir: ./uploads` (本地持久化目录)
  - `url-prefix: /uploads/` (映射的 HTTP 访问路径前缀)
- **SpringDoc OpenAPI**：
  - Swagger UI 入口: `/swagger-ui/index.html`
  - API 元数据接口: `/v3/api-docs`

### 2.2 生产配置 (`backend/src/main/resources/application-prod.yml`)
- **激活条件**：`spring.config.activate.on-profile: prod`
- **容器与停机**：`server.port = 8080`, `server.shutdown = graceful` (优雅停机)
- **数据源生产解耦 (环境变量驱动)**：
  - URL: `jdbc:mysql://${DB_HOST:shop-mysql}:${DB_PORT:3306}/${DB_NAME:shop_db}?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true`
  - 用户名/密码: `${DB_USER:root}` / `${DB_PASSWORD}`
  - 连接池参数扩容: `minimum-idle: 10`, `maximum-pool-size: 30`
- **Redis 生产解耦 (强密码鉴权)**：
  - 主机/端口: `${REDIS_HOST:shop-redis}` / `${REDIS_PORT:6379}`
  - 鉴权密码: `${REDIS_PASSWORD}`
  - 连接池参数扩容: `max-active: 32`, `max-idle: 16`, `min-idle: 4`
- **生产日志与安全审计**：
  - MyBatis-Plus SQL 打印关闭: `log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl` (防止敏感数据泄露与高频刷盘)
  - 日志落地路径: `/app/logs/shop-backend.log`
  - 滚动归档策略: `max-file-size: 20MB`, `max-history: 30` (按天滚动保留30天)
- **生产 Token 配置差异**：
  - Token 有效期收敛为 7 天 (`timeout: 604800`)
  - 生产配置声明了 `token-style: uuid` (*注：详见第五节一致性分析*)
- **生产存储路径**：
  - `shop.storage.local-dir: /app/uploads`

### 2.3 部署编排与环境变量清单 (`deploy/`)
- **`deploy/.env.example` 生产模板变量**：
  - `DB_ROOT_PASSWORD`: MySQL root 强密码 (默认模板 `Shop_Prod_Root_Secret_2026!`)
  - `DB_PASSWORD`: 业务用户密码
  - `DB_NAME`: 数据库名 (`shop_db`)
  - `DB_USER`: 数据库用户 (`root`)
  - `REDIS_PASSWORD`: Redis 连接强密码 (`Shop_Prod_Redis_Token_2026!`)
  - `DOMAIN_NAME`: 生产主域名 (`shop.example.com`)
- **`deploy/docker-compose.prod.yml` 拓扑**：
  - `shop-mysql`: MySQL 8.4 (挂载 `../docs/sql/schema.sql` 和 `data.sql` 自动初始化)
  - `shop-redis`: Redis 7.2 Alpine (`--requirepass`, `--appendonly yes`)
  - `shop-backend`: Dockerfile 多阶段构建产物，运行于非 root 用户 `appuser` (UID 10001)，健康探针探查 `http://localhost:8080/v3/api-docs`
  - `shop-frontend`: Nginx 托管 Vue 3 静态产物
  - `shop-nginx`: 边缘反向代理网关，全系统**唯一对外暴露 80/443 端口**，后端与数据库在 `shop-internal` 内网全隔离。

---

## 三、数据库设计、实体映射与 SQL 校验 (Database & Entities)

### 3.1 数据库物理基础
- 数据库名称：`shop_db`
- 推荐版本：MySQL 8.0+ / 8.4+
- 字符集与排序规则：`DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`
- 表存储引擎：`ENGINE=InnoDB`

### 3.2 实体类 vs 物理表结构对齐矩阵 (全 7 张表)

| 物理表名 | 对应实体类 | 主键策略 | 业务唯一索引 | 普通外键/检索索引 | 逻辑删除字段 | 自动填充审计时间 | 对齐状态 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `sys_user` | `SysUser` | `id` (AUTO) | `uk_username` (`username`) | 无 | `is_deleted` (`@TableLogic`) | `created_at`, `updated_at` | **完全一致** |
| `shop_brand` | `ShopBrand` | `id` (AUTO) | 无 | `idx_sort` (`sort`) | `is_deleted` (`@TableLogic`) | `created_at`, `updated_at` | **完全一致** (含 `origin` 产地字段) |
| `shop_category` | `ShopCategory` | `id` (AUTO) | 无 | `idx_parent_id` (`parent_id`) | `is_deleted` (`@TableLogic`) | `created_at`, `updated_at` | **完全一致** |
| `shop_product` | `ShopProduct` | `id` (AUTO) | 无 | `idx_category`, `idx_brand`, `idx_price` | `is_deleted` (`@TableLogic`) | `created_at`, `updated_at` | **完全一致** |
| `shop_cart_item` | `ShopCartItem` | `id` (AUTO) | `uk_user_product` (`user_id`, `product_id`) | `idx_user_id` (`user_id`) | **无** (物理清除) | `created_at`, `updated_at` | **完全一致** (`user_id` 映射 `customerId`) |
| `shop_order` | `ShopOrder` | `id` (AUTO) | `uk_order_no` (`order_no`) | `idx_user_order` (`user_id`, `created_at`) | **无** (财务凭据不可篡改) | `created_at`, `updated_at` | **完全一致** (含 `DeliveryAddress` 值对象封装) |
| `shop_order_item` | `ShopOrderItem` | `id` (AUTO) | 无 | `idx_order_id` (`order_id`) | **无** (不可变订单快照) | 仅 `created_at` (无更新) | **完全一致** (不可变快照模式) |

### 3.3 核心数据设计亮点与验证
1. **统一领域主格名称 (`customerId` vs. `user_id`)**：
   - 表设计保留通用外键列名 `user_id`；
   - 商业模块（`ShopCartItem` / `ShopOrder`）实体类统一使用 `@TableField("user_id") private Long customerId;`；
   - 兼顾了 DBA 传统 SQL 习惯与 DDD 领域建模对“顾客（Customer）”概念的严格界定，消除多角色二义性。
2. **收件人数据泥团消除 (`DeliveryAddress` 值对象)**：
   - 物理表 `shop_order` 扁平存储 `receiver_name`, `receiver_phone`, `receiver_address`；
   - Java 实体类引入 `@TableField(exist = false) private DeliveryAddress deliveryAddress;` 并封装联动访问器，符合 Fowler 重构原则。
3. **高并发库存单 SQL 原子更新 (`ShopProductMapper`)**：
   - 包含自定义原子更新语句：
     ```sql
     UPDATE shop_product 
     SET stock = stock - #{quantity} 
     WHERE id = #{productId} 
       AND stock >= #{quantity} 
       AND is_deleted = 0;
     ```
   - 借助 InnoDB 行级排他排他锁串行化更新，零外部依赖消除并发超卖。
4. **初始化种子数据 (`docs/sql/data.sql`)**：
   - 管理员账号：`admin` / `admin123` (BCrypt: `$2a$10$7W6pZavnkFW5AgosezBYnOx3i.15aTEg8o3jNELhKL/OC1oEbI.jK`)
   - 演示顾客：`customer` / `123456` (BCrypt: `$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi`)
   - 预设 6 大知名品牌（Apple, Huawei, Xiaomi, Sony, Nike, Uniqlo，均具备真实产地与 Unsplash 优质图源）
   - 预设 9 个层级分类（4 个一级分类 + 5 个二级子分类）
   - 预设 7 款真实商品数据

---

## 四、REST API 接口规范与模块边界清单 (REST APIs & Boundaries)

系统共包含 **11 个控制器**，实现 **27 个端点**，严格划分为五大模块边界：

### 4.1 认证中心 (`com.shop.modules.auth.controller.AuthController`，基础路径: `/api/auth`)
| HTTP 方法 | 完整 URL 路径 | 接口摘要与业务说明 | 参数类型 / Body | 权限拦截规则 |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | 顾客账号注册 (自动 BCrypt 加盐散列) | `@RequestBody RegisterCommand` | 公开接口 |
| `POST` | `/api/auth/login` | 统一账号登录 (返回 Sa-Token 与用户信息) | `@RequestBody LoginCommand` | 公开接口 |
| `GET` | `/api/auth/profile` | 获取当前登录用户个人资料 | 无 | 需登录 (`checkLogin`) |
| `PUT` | `/api/auth/profile` | 更新当前登录用户资料与密码 | `@RequestBody UpdateProfileCommand` | 需登录 (`checkLogin`) |
| `POST` | `/api/auth/logout` | 注销当前用户会话 (主动失效 Token) | 无 | 需登录 (`checkLogin`) |

### 4.2 商城前台目录与商品展示 (`com.shop.modules.catalog.controller.*`，基础路径: `/api/portal`)
| HTTP 方法 | 完整 URL 路径 | 接口摘要与业务说明 | 控制器 / 参数 | 缓存与优化特征 |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/portal/categories/tree` | 获取全量分类层级树 | `PortalCatalogController` | Redis 缓存 (`catalog:category:tree`)，TTL 24小时 |
| `GET` | `/api/portal/brands/featured` | 获取热门推荐品牌列表 | `limit` (默认 6) | Redis 缓存 (`catalog:brand:featured:{limit}`)，TTL 6小时 |
| `GET` | `/api/portal/home` | 首页聚合橱窗数据 (轮播/品牌/分类/热卖/新品) | `PortalProductController` | Redis 缓存 (`catalog:showcase:home`)，单请求消除瀑布流 |
| `GET` | `/api/portal/products` | 多维动态条件检索商品 (分类/品牌/价格区间/排序) | `ProductQueryCommand` (分页 12) | 动态 SQL 分页，自动包含子分类商品 |
| `GET` | `/api/portal/products/{id}` | 获取前台商品全量详情与图片画廊 | 路径变量 `id` | Redis 缓存 (`catalog:product:detail:{id}`)，TTL 30分钟 |

### 4.3 顾客交易与购物车结算 (`com.shop.modules.commerce.controller.*`)
| HTTP 方法 | 完整 URL 路径 | 接口摘要与业务说明 | 控制器 / 参数 | 权限与事务保护 |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/cart` | 获取当前顾客购物车明细及金额合计 | `CartController` | 需登录，实时核算有效单价与库存 |
| `POST` | `/api/cart/items` | 添加商品至购物车 (累加并校验可用库存) | `@RequestBody AddCartCommand` | 需登录，`@Transactional` |
| `PUT` | `/api/cart/items/{id}` | 修改购物车项数量或勾选状态 | `@RequestBody UpdateCartItemCommand` | 需登录，水平越权校验 (`customerId`) |
| `DELETE` | `/api/cart/items/{id}` | 移除指定购物车行项 | 路径变量 `id` | 需登录，水平越权校验 |
| `DELETE` | `/api/cart` | 一键清空购物车已加购商品 | 无 | 需登录，物理批量清除 |
| `POST` | `/api/orders/checkout` | 提交已勾选购物车条目原子下单 | `OrderController` / `CheckoutCommand` | 需登录，`@Transactional`，单 SQL 原子防超卖 |
| `POST` | `/api/orders/{orderNo}/pay` | 模拟订单支付成功 (流转为 PAID) | 路径变量 `orderNo` | 需登录，幂等与水平越权校验 |
| `GET` | `/api/orders/my` | 分页查询当前顾客订单历史与快照 | `pageNum`, `pageSize` | 需登录，批量加载商品行项快照 |
| `GET` | `/api/orders/{id}` | 获取指定订单明细与快照详情 | 路径变量 `id` | 需登录，防 IDOR 越权拦截 |

### 4.4 管理后台一体化管控 (`com.shop.modules.*.controller.Admin*Controller`，基础路径: `/api/admin`)
*全局安全拦截*：受 `WebMvcConfig` 声明式路由统一保护：`SaRouter.match("/api/admin/**", r -> StpUtil.checkRole("ROLE_ADMIN"))`。
| HTTP 方法 | 完整 URL 路径 | 接口摘要与业务说明 | 控制器 / 参数 |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/admin/brands` | 分页条件检索品牌列表 | `AdminBrandController` (`BrandQuery`) |
| `GET` | `/api/admin/brands/{id}` | 获取品牌详细信息 | 路径变量 `id` |
| `POST` | `/api/admin/brands` | 新建商品品牌 (自动淘汰品牌缓存) | `@RequestBody BrandCommand` |
| `PUT` | `/api/admin/brands/{id}` | 更新商品品牌信息 (自动淘汰品牌缓存) | `@RequestBody BrandCommand` |
| `DELETE` | `/api/admin/brands/{id}` | 逻辑删除品牌 (自动淘汰品牌缓存) | 路径变量 `id` |
| `GET` | `/api/admin/categories` (`/tree`) | 获取树形表格分类列表 | `AdminCategoryController` |
| `POST` | `/api/admin/categories` | 新建商品分类 (支持无限级父子关联) | `@RequestBody CategoryCommand` |
| `PUT` | `/api/admin/categories/{id}` | 更新商品分类信息与状态 | `@RequestBody CategoryCommand` |
| `DELETE` | `/api/admin/categories/{id}` | 删除分类 (安全拦截：含子分类禁止删除) | 路径变量 `id` |
| `GET` | `/api/admin/products` | 多条件复合分页查询商品管理列表 | `AdminProductController` (`AdminProductQuery`) |
| `GET` | `/api/admin/products/{id}` | 获取后台商品详情 | 路径变量 `id` |
| `POST` | `/api/admin/products` | 新建商品 (核验品牌/分类合法性，返回新 ID) | `@RequestBody ProductCommand` |
| `PUT` | `/api/admin/products/{id}` | 更新商品 (自动失效商品详情与首页缓存) | `@RequestBody ProductCommand` |
| `PATCH` | `/api/admin/products/{id}/status` | 快速切换商品上下架状态 | `status` 参数或 Body |
| `DELETE` | `/api/admin/products/{id}` | 逻辑删除商品 (自动淘汰缓存) | 路径变量 `id` |
| `GET` | `/api/admin/orders` | 全平台订单分页检索 (订单号/状态/收件人手机号) | `AdminOrderController` (`AdminOrderQuery`) |
| `PATCH` | `/api/admin/orders/{id}/ship` | 标记订单为已发货 (`SHIPPED`) | 路径变量 `id` |
| `GET` | `/api/admin/users` | 分页查询系统用户列表 (支持关键字/角色/状态筛选) | `AdminUserController` |
| `PUT` | `/api/admin/users/{id}/status` | 修改用户启停状态 (禁用立即踢下线会话) | `@RequestBody UserStatusUpdateDTO` |
| `GET` | `/api/admin/users/{id}` | 获取用户详细资料 | 路径变量 `id` |

### 4.5 公共与基础服务 (`com.shop.common.controller.CommonUploadController`)
| HTTP 方法 | 完整 URL 路径 | 接口摘要与业务说明 | 说明 |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/common/upload` | 通用静态文件上传 (`multipart/form-data`) | 沙箱安全防护，返回公网访问路径 `/uploads/{uuid}.ext` |

---

## 五、代码与现有文档比对差异分析 (Discrepancy Analysis)

经系统性穿透比对当前后端实现、`docs/` 技术讲义与根目录 `README.md`，梳理出如下要点与差异：

### 5.1 完全对齐与验证通过项 (Verified Alignment)
1. **测试覆盖率与用例总数**：
   - `README.md` 记载：“运行全套后端测试（44 个测试用例全部通过）”。
   - 实测执行 `mvn test`：13 个测试类、**44 个测试用例全部通过（Failures: 0, Errors: 0, Skipped: 0）**，包含 `OrderConcurrencyTest` 10 线程并发压力测试断言。
2. **端口与服务契约**：
   - 后端端口 8080、前端端口 5173、MySQL 端口 3306、Redis 端口 6379 与脚本 `start.bat`/`start.ps1`/`stop.bat`/`stop.ps1` 完全对应。
3. **品牌产地（`origin`）全链路对齐**：
   - 实体类 `ShopBrand.origin`、数据库 `shop_brand.origin`、初始数据 `data.sql` 以及 `README.md` 核心亮点均已完全同步。
4. **DDD 值对象与业务语言**：
   - `DeliveryAddress` 消除数据泥团、`customerId` 指代交易主体、不可变订单快照模式在代码、实体、ADR 与 Principles 讲义中完全一致。
5. **缓存二次淘汰机制**：
   - `OrderCheckoutServiceImpl` 中使用 `TransactionSynchronizationManager.registerSynchronization` 在 `afterCommit` 执行二次缓存淘汰，与 `04-redis-caching-patterns.md` 及答辩 Q&A 描述完全一致。

### 5.2 发现的技术差异与改进建议 (Discrepancies & Recommendations)
1. **`application-prod.yml` 中的 `sa-token.token-style` 声明与架构设计不一致**：
   - **现状观察**：`application.yml` 中声明 `token-style: jwt` 并配置 `jwt-secret-key`；而 `application-prod.yml` 第 58 行声明了 `token-style: uuid`。
   - **代码实相**：在 `SaTokenConfig.java` 中，Spring 容器明确注册了 `@Bean public StpLogic getStpLogicJwt() { return new StpLogicJwtForSimple(); }`。Sa-Token 底层一旦注入了 `StpLogicJwtForSimple`，便强制生成 JWT 格式 Token，YAML 中的 `token-style: uuid` 实际上被覆盖忽略。
   - **改进建议**：应在后续文档或配置更新中将 `application-prod.yml` 的 `token-style` 修正为与开发环境一致的 `jwt`，并补全密钥配置说明，避免产生歧义。
2. **MyBatis-Plus 的 `mapper-locations` 冗余配置**：
   - **现状观察**：`application.yml` 与 `application-prod.yml` 均配置了 `mapper-locations: classpath*:/mapper/**/*.xml`。
   - **代码实相**：工程中所有 7 个 Mapper 均为纯 Java 接口（继承 `BaseMapper<T>`），定制 SQL 均采用注解方式（`@Update`），`src/main/resources` 下根本不存在 `mapper/` 目录或 XML 文件。
   - **文档建议**：在 `docs/principles/03-orm-and-mybatis-plus.md` 中补充说明本项目为单表零 XML、全注解极简风格。
3. **`docs/sql/` 缺少 Markdown 格式的数据字典文档**：
   - **现状观察**：`docs/sql/` 目录下仅存在 `schema.sql` 和 `data.sql` 两个物理执行脚本，缺乏对应的表结构设计说明文档（如 `README.md` 或 `dictionary.md`）。
   - **改进建议**：本次调研报告第三节已完整提炼 7 张核心表的字段类型、索引与业务设计，后续文档同步阶段可在 `docs/sql/README.md` 中固化该数据字典。

---

## 六、调研结论与后续行动建议 (Conclusion & Next Steps)

1. **后端完整性评定**：后端工程架构清晰、模块边界严格、测试覆盖完善（44/44 通过），深模块设计和单 SQL 原子防超卖等核心亮点在源码层面均有扎实佐证。
2. **数据与文档支撑**：数据表结构与 Java 实体完全对齐，相关设计思想在 `docs/principles/` 和 `docs/adr/` 中有充分的理论阐述。
3. **交付物归档**：
   - 本调研全景报告已写入：`D:\JavaWeb\.agents\teamwork\explorer_survey_backend\survey_report.md`
   - 标准 5 组件交接报告已写入：`D:\JavaWeb\.agents\teamwork\explorer_survey_backend\handoff.md`
