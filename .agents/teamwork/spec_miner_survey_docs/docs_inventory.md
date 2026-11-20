# JavaWeb 项目全量文档审计与规范发现清单（Documentation Inventory & Audit Report）

**报告生成时间**：2026-11-05  
**执行角色**：teamwork_spec_miner (Specification Miner)  
**审计范围**：`D:\JavaWeb\README.md`、`D:\JavaWeb\docs\` 全目录及根目录与部署相关辅助文档  
**对标源码**：`backend/` (Spring Boot 3.3.4), `frontend/` (Vue 3 + Vite), `deploy/`, 启动/停止脚本及 SQL 脚本  

---

## 一、 文档资产全景汇总表（Master Inventory Table）

| # | 文档路径 | 文档标题 / 核心主题 | 文档定位与目标受众 | 核心技术断言与覆盖范围 | 现状诊断 / 一致性健康度 |
|---|---|---|---|---|---|
| 1 | `README.md` | 🛒 JavaWeb Shop 现代电商平台（全栈参考实现） | 统一入口与总览指南；面向评测教师、学生与开发者 | 技术栈规范（Java 21/Spring Boot 3.3.4/Vue 3/Vite）、架构亮点、一键启停脚本、MySQL/Redis 依赖、测试套件（44 后端 + 15 前端）、答辩 Top 10 索引、目录树 | **部分滞后**：前端 Store 命名标注为 `(auth, cart)` 实为 `user.ts/cart.ts`；答辩索引仅列 10 题但讲义 07 实际已扩展至 12 题；目录树缺失 `docs/agents/` 与根目录辅助文档 |
| 2 | `CONTEXT.md` | Shop Platform Context | 领域通用语言（Ubiquitous Language）规范；面向团队开发者与建模者 | 统一实体与行为命名：`Customer`（非 User）、`Administrator`、`Brand`、`Category`、`Product`、`Cart Item`、`Order`、`Stock` | **健康**：与后端 DDD 统一通用语言（如 `@TableField("user_id") customerId`）高度一致，但未被 `README.md` 索引链接 |
| 3 | `todo.md` | 项目原始需求备忘 | 初始项目任务备忘；面向初期开发阶段 | 记录了课程作业初始 3 条原则（技术先进、功能完备、原理讲透） | **历史归档**：纯备忘性质历史碎片文件，无需正式维护 |
| 4 | `deploy/nginx/ssl/README.md` | SSL 证书放置指南 | 生产部署 Nginx SSL 证书操作指南；面向运维人员 | `cert.pem` 与 `key.pem` 放置规则；Cloudflare 15 年 Origin CA 申请步骤；本地自签名 OpenSSL 测试命令 | **健康**：步骤准确，配置与 `deploy.sh` 及 `shop.conf` 保持同步 |
| 5 | `docs/adr/0001-core-tech-stack-and-architecture.md` | Core Tech Stack and Architecture Selection | 架构决策记录；面向技术评审与架构维护 | 选型决议：Spring Boot 3.3+、Java 21、Sa-Token JWT、MyBatis-Plus 3.5+、Vue 3 + Vite、大仓 Monorepo | **健康**：与实际工程结构完全一致 |
| 6 | `docs/adr/0002-domain-boundaries-and-commerce-flow.md` | Domain Boundaries and Commerce Flow | 架构决策记录；面向业务边界与流程设计 | 选型决议：双端统一视窗（前台橱窗 + 后台控制台）、独立商品模型、端到端履约闭环、Redis Cache-Aside | **健康**：与业务实现高度一致 |
| 7 | `docs/adr/0003-infrastructure-and-educational-artifacts.md` | Infrastructure and Educational Artifacts | 架构决策记录；面向基础设施与答辩资料设计 | 选型决议：独立 `shop_db`、本地图片上传映射、OpenAPI 3 / Swagger、`docs/principles/` 原理讲义配套 | **小幅漂移**：文中记载配套“6 core topics”，实际后续已扩充演进至 9 篇全景技术讲义（新增答辩 QA、安全纵深、生产部署指南） |
| 8 | `docs/agents/issue-tracker.md` | Issue Tracker Configuration | Agent 任务跟踪配置；面向 AI 协作与工单管理 | 配置本地 markdown 跟踪目录 `.scratch/shop-platform/issues` 与 `spec` | **健康**：指向的 `.scratch` 工单存在，但未纳入 `README.md` 目录索引 |
| 9 | `docs/sql/schema.sql` | Shop Platform Database Schema (MySQL 8.4+) | 数据库 DDL 表结构定义；面向 DBA 与后端开发 | 7 张核心表：`sys_user`, `shop_brand`, `shop_category`, `shop_product`, `shop_cart_item`, `shop_order`, `shop_order_item`；UTF8MB4、自增主键、逻辑删除、索引与外键 | **100% 吻合**：与所有 7 个 Java Entity 实体及字段完全对齐（含 `origin`、快照字段等） |
| 10 | `docs/sql/data.sql` | Shop Platform Initial Seed Data | 种子数据脚本；面向初次部署初始化 | 预设数据：2 个用户（admin / customer，BCrypt 密文）、6 个国际/国产品牌（含产地）、9 个多级分类、7 个代表性商品 | **100% 吻合**：数据逻辑与业务系统完全贯通 |
| 11 | `docs/principles/01-springboot3-internals-and-architecture.md` | 01 - Spring Boot 3 核心底层原理与深模块架构设计 | 核心原理讲义第一篇；面向课程答辩与底层原理考查 | 自动装配（`AutoConfiguration.imports`）、Bean 生命周期、构造器注入优势、深模块架构、DDD 坏味道治理（值对象、批处理助手、充血计算） | **存在死链**：第 199 行超链接指向非存在路径 `model/DeliveryAddress.java`（实为 `dto/DeliveryAddress.java`） |
| 12 | `docs/principles/02-auth-and-satoken.md` | 02 - 无状态认证与 Sa-Token 权限鉴权原理 | 核心原理讲义第二篇；面向认证安全答辩 | Token vs Session、JWT 结构与 HMAC-SHA256 签名、Sa-Token Simple 模式、ThreadLocal 上下文与内存泄露防范、RBAC 动态角色、BCrypt 原理 | **轻微漂移**：第 109 行引用的 `jwt-secret-key` 与 `application.yml` 实际值不符；第 257-258 行描述测试提取 `useAuthStore`，实际 Store 文件为 `user.ts` 且测试直接读取 `localStorage` |
| 13 | `docs/principles/03-orm-and-mybatis-plus.md` | 03 - ORM 演进、AST 语法树重写与 MySQL 索引事务原理 | 核心原理讲义第三篇；面向持久层与数据库原理答辩 | JDBC 到 MP 演进、JSqlParser AST 语法树重写分页原理、自动审计填充、DDD 字段解耦映射、InnoDB B+ 树、ACID 底层 Undo/Redo/MVCC | **存在死链**：第 149 行超链接指向非存在路径 `model/DeliveryAddress.java`（实为 `dto/DeliveryAddress.java`） |
| 14 | `docs/principles/04-redis-caching-patterns.md` | 04 - Redis 缓存设计模式与经典高并发攻防 | 核心原理讲义第四篇；面向缓存与高性能答辩 | Cache-Aside 模式深模块内聚封装、分区 Key 路由策略、淘汰优于更新原理、Spring 事务 `afterCommit` 二次淘汰、穿透/击穿/雪崩防御、epoll 多路复用 | **健康**：源码引用与设计逻辑与代码高度契合 |
| 15 | `docs/principles/05-concurrency-and-inventory-safety.md` | 05 - 高并发库存防超卖原理与极简原子化设计 | 核心原理讲义第五篇；面向高并发与并发安全答辩 | 4 种库存扣减方案对比、单 SQL 条件原子更新、affectedRows 信号量与事务回滚、`afterCommit` 缓存双重淘汰、CountDownLatch 10 线程压测用例剖析 | **健康**：与 `ShopProductMapper`、`OrderCheckoutServiceImpl` 及 `OrderConcurrencyTest` 100% 对齐 |
| 16 | `docs/principles/06-vue3-and-vite-modern-frontend.md` | 06 - Vue 3 响应式原理、Vite ESM 极速构建与现代前端架构 | 核心原理讲义第六篇；面向前端现代化技术答辩 | Vue 3 Proxy vs Vue 2 defineProperty、WeakMap 依赖追踪、Vite 原生 ESM 毫秒冷启动、Pinia `useCartStore` 客户端深模块、Vitest + happy-dom 测试 | **轻微漂移**：第 191 行将用户状态 Pinia Store 描述为 `useAuthStore`，实际工程文件为 `useUserStore`（`src/stores/user.ts`） |
| 17 | `docs/principles/07-course-defense-qa.md` | 07 - 课程答辩高频核心考问与满分应答秘籍（Top 10 Q&A Cheat Sheet） | 答辩决胜指南；面向答辩应试与源码快速溯源 | 涵盖 Q1~Q12（共 12 大核心考问及标准满分话术、源码类名与代码行号引用） | **行号与题数漂移**：文档实际包含 12 题但标题标注为“Top 10”；第 267 行标注路由守卫位于 `router/index.ts` 第 45-75 行，实际路由守卫位于第 103-136 行；第 258 行叙述守卫配置了 `role: 'ROLE_ADMIN'`，实为 `requiresAdmin: true` |
| 18 | `docs/principles/08-defense-knowledge.md` | 08 - 项目安全防护体系与网络传输防御（答辩知识全景指南） | 安全专项答辩讲义；面向全栈安全与网络考问 | 8 维纵深防御、BCrypt/IDOR 防御、单 SQL 防超卖、文件上传沙箱、HTTP 本地权衡 vs HTTPS 生产必要性、Nginx SSL 卸载标准架构、答辩安全考问 | **健康**：逻辑严密，代码与配置引用精准 |
| 19 | `docs/principles/09-scheme1-production-deployment-guide.md` | 09 - 生产环境安全部署方案对比与 Cloudflare 边缘加固实战指南 | 生产部署指南与答辩；面向运维实操与上线安全 | 三大生产部署方案对比、方案一（单机+Cloudflare+Nginx+Docker）落地细节、端口收敛（80/443/2222）、15 年免费 Origin CA、真实 IP 还原、限流防爆破、一键部署脚本 | **健康**：与 `deploy/` 下所有配置文件及自动化部署脚本 100% 吻合 |

---

## 二、 详细文件逐项审计与对标校验

### 1. `README.md`
- **文件定位**：项目第一门面，面向代码审查者与答辩评审老师。
- **验证项分析**：
  - **命令与端口**：`start.bat` / `start.ps1`、`stop.bat` / `stop.ps1` 正确对应端口 8080（后端）、5173（前端）、6379（Redis）、3306（MySQL）。Swagger UI 地址 `http://localhost:8080/swagger-ui/index.html` 正确。
  - **测试用例统计**：
    - 文档宣称：“全套后端测试（44 个测试用例全部通过）”与“前端测试套件（15 个测试用例全部秒级通过）”。
    - **实测验证**：后端共有 13 个测试类，精确包含 44 个 `@Test` 方法；前端 3 个测试文件精确包含 15 个测试断言用例。数字真实可信。
  - **待同步项**：
    1. 项目目录全景树（第 223 行）写为 `stores/ # Pinia 响应式状态管理深模块 (auth, cart)`。实际前端代码中的文件是 `user.ts`、`cart.ts`、`index.ts`，导出的是 `useUserStore` 与 `useCartStore`，并无名为 `auth` 的 Store 文件。
    2. 原理讲义总览表与答辩索引标注为“Top 10 答辩高频核心考问”，而讲义 `07-course-defense-qa.md` 实际已系统性扩充至 12 个核心考问（新增了 Q11 统一领域语言 customerId 与 Q12 前端路由防越权自动化测试验证）。
    3. 项目目录全景树缺少 `docs/agents/` 目录以及根目录的 `CONTEXT.md` 和 `uploads/` 说明。

### 2. `docs/adr/` (架构决策记录)
- **涵盖文件**：`0001-core-tech-stack-and-architecture.md`, `0002-domain-boundaries-and-commerce-flow.md`, `0003-infrastructure-and-educational-artifacts.md`
- **验证项分析**：
  - ADR 0001 记录了 Monorepo、Spring Boot 3.3+、Java 21、Sa-Token JWT、MyBatis-Plus、Vue 3 + Vite 的决策，与实际代码完全吻合。
  - ADR 0002 记录了双端统一视窗、单体大仓路由分流、DDD 购物车订单流转、Redis 旁路缓存，与实现完全契合。
  - ADR 0003 记录了 `shop_db`、本地图片上传映射、OpenAPI 3 / Swagger、以及原理讲义。
  - **待同步项**：ADR 0003 第 9 行写明配套“6 core topics”，实际后续原则讲义已从 6 篇自然拓展为 9 篇全景技术矩阵，应在后效记录（Consequences）中予以更新标注。

### 3. `docs/agents/issue-tracker.md`
- **文件定位**：开发协作与任务工单配置元数据。
- **验证项分析**：
  - 指向 `.scratch/shop-platform/issues` 与 `.scratch/shop-platform/spec`。
  - 经扫描，该目录下包含 0001 至 0006 号历史工单及初始规格文档，记录了项目从脚手架到原理讲义的演进过程。
  - **待同步项**：此文件目前作为单点孤立在 `docs/agents/` 中，未在 `README.md` 中进行说明。

### 4. `docs/sql/schema.sql` 与 `docs/sql/data.sql`
- **验证项分析**：
  - `schema.sql` 准确包含 7 个表：`sys_user`, `shop_brand`, `shop_category`, `shop_product`, `shop_cart_item`, `shop_order`, `shop_order_item`。
  - 核心字段严格映射：
    - `shop_brand` 含有 `origin VARCHAR(64)` 产地字段，与 `ShopBrand.java` 完全吻合。
    - `shop_order_item` 包含不可变快照字段（`product_name`, `product_image`, `unit_price`, `quantity`, `totalPrice`），与 `ShopOrderItem.java` 完全吻合。
    - `shop_cart_item` 与 `shop_order` 的外键为 `user_id`，与 Java 实体中通过 `@TableField("user_id")` 映射至 `customerId` 完美对接。
  - `data.sql` 包含 BCrypt 格式的密文密码、 Unsplash 真实高精度图片，与前台展示及后台管理完全吻合。

### 5. `docs/principles/` (9 篇底层原理深度讲义)
- **01 讲（Spring Boot 3 底层原理与深模块）**：
  - 深入讲解 `AutoConfiguration.imports`、构造器注入、深模块与删除测试。
  - **发现死链**：第 199 行链接 `[DeliveryAddress](file:///D:/JavaWeb/backend/src/main/java/com/shop/modules/commerce/model/DeliveryAddress.java)` 物理路径失效！实际类文件位于 `backend/src/main/java/com/shop/modules/commerce/dto/DeliveryAddress.java`（包路径为 `com.shop.modules.commerce.dto`）。
- **02 讲（无状态认证与 Sa-Token）**：
  - 深入剖析 JWT 三段式结构、`StpLogicJwtForSimple` 与 BCrypt 随机盐机制。
  - **发现差异**：
    - 第 109 行引用的配置 `jwt-secret-key: shop-secret-key-shop-secret-key-123456`，在 `backend/src/main/resources/application.yml` 实际配置为 `shop-platform-jwt-secret-key-32chars-min-abcdef123456`。
    - 第 257-258 行叙述 Axios 请求拦截器从 `useAuthStore` 提取 Token，并在 401 时调用 `authStore.logout()`；而实际工程中 Store 是 `useUserStore`，且 Axios 拦截器直接通过 `localStorage.getItem('token')` 与 `localStorage.removeItem('token')` 完成操作。
- **03 讲（ORM 演进、AST 重写与 MySQL 索引事务）**：
  - 剖析 `PaginationInnerInterceptor` 借助 JSqlParser 动态改写 AST 语法树生成 `COUNT(*)` 和 `LIMIT`；分析聚簇索引与二级索引回表。
  - **发现死链**：第 149 行同样存在对 `model/DeliveryAddress.java` 的死链引用（实际应为 `dto/DeliveryAddress.java`）。
- **04 讲（Redis 缓存设计模式与高并发攻防）**：
  - 深入讲解 Cache-Aside、精细化分区缓存 Key（`catalog:brand:all` 与 `catalog:brand:list:...`）、`TransactionSynchronizationManager.afterCommit` 事务提交后二次淘汰缓存。
  - 状态：内容高度一致，代码引用精准。
- **05 讲（高并发库存防超卖与极简原子化设计）**：
  - 横向评测四大扣减方案，重点解析单条 SQL 原子条件更新 `WHERE id = ? AND stock >= ?` 与 InnoDB 行级锁；详细剖析 `OrderConcurrencyTest` 10 线程 CountDownLatch 压测实现。
  - 状态：内容与代码实现 100% 对齐。
- **06 讲（Vue 3 响应式原理与现代前端架构）**：
  - 解析 ES6 Proxy vs defineProperty、Vite ESM 毫秒冷启动、Pinia `useCartStore` 客户端深模块、Vitest + happy-dom 测试套件。
  - **发现差异**：第 191-192 行同样描述了 `useAuthStore` 与 `authStore.logout()`，存在命名漂移（实为 `useUserStore` 与 `localStorage`）。
- **07 讲（课程答辩高频核心考问与满分应答秘籍）**：
  - **发现差异与行号偏移**：
    - 题目总数：标题称“Top 10”，但正文实际提供了 12 个经典考问（Q1 至 Q12）。
    - 源码行号引用偏差：
      - 第 267 行标注前端路由守卫为 `frontend/src/router/index.ts（第 45-75 行）`，但第 45-75 行实为路由声明（`AdminLayout` 子路由定义）；全局前置导航守卫 `router.beforeEach` 真实物理行数为**第 103 至 136 行**。
      - 第 258 行叙述后台路由元信息声明了 `role: 'ROLE_ADMIN'`，而在源码中实际声明的是 `meta: { requiresAdmin: true }`，由守卫调用 `userStore.isAdmin` 完成权限鉴别。
- **08 讲（项目安全防护体系与网络传输防御）**：
  - 建立 8 维纵深防御拓扑，系统阐述 IDOR 越权防护、上传 4 重沙箱、HTTP 本地开发优势与生产 Nginx SSL 卸载实践。
  - 状态：完全一致，无死链。
- **09 讲（生产部署安全方案与 Cloudflare 边缘加固）**：
  - 生产方案全景对比；配置 Nginx 还原真实 IP（`CF-Connecting-IP`）、防爆破限流、Docker Bridge 隔离网络、自动化运维脚本。
  - 状态：与 `deploy/` 下所有配置文件及自动化部署脚本 100% 吻合。

---

## 三、 文档健康度与差异发现矩阵（Discrepancy Matrix）

| # | 差异类型 | 涉及文档与行数 | 当前文档表述 / 链接 | 真实代码 / 物理环境状态 | 修复与对齐规范要求 |
|---|---|---|---|---|---|
| D1 | **物理文件死链 (Broken Link)** | `docs/principles/01-springboot3-internals-and-architecture.md:199` | `[DeliveryAddress](file:///D:/JavaWeb/backend/src/main/java/com/shop/modules/commerce/model/DeliveryAddress.java)` | 物理文件不存在。实际位于 `backend/src/main/java/com/shop/modules/commerce/dto/DeliveryAddress.java` | 修正链接为 `.../commerce/dto/DeliveryAddress.java` |
| D2 | **物理文件死链 (Broken Link)** | `docs/principles/03-orm-and-mybatis-plus.md:149` | `[DeliveryAddress](file:///D:/JavaWeb/backend/src/main/java/com/shop/modules/commerce/model/DeliveryAddress.java)` | 物理文件不存在。实际位于 `backend/src/main/java/com/shop/modules/commerce/dto/DeliveryAddress.java` | 修正链接为 `.../commerce/dto/DeliveryAddress.java` |
| D3 | **代码行号引用偏移 (Line Drift)** | `docs/principles/07-course-defense-qa.md:267` | `前端路由守卫：frontend/src/router/index.ts（第 45-75 行）` | 第 45-75 行是前台订单与后台子路由定义，真正的 `router.beforeEach` 全局守卫位于 **第 103-136 行** | 修正行号引用为 `第 103-136 行` |
| D4 | **路由元数据属性不符 (Meta Key Drift)** | `docs/principles/07-course-defense-qa.md:258` | 文档称“所有 `/admin` 路由元信息均声明了 `requiresAuth: true` 与 `role: 'ROLE_ADMIN'`” | `frontend/src/router/index.ts` 实际声明为 `meta: { requiresAdmin: true }`，由守卫判定 `userStore.isAdmin` | 修正为 `meta: { requiresAdmin: true }` 并补充 `userStore.isAdmin` 判定逻辑 |
| D5 | **答辩考问数量不一致 (Count Drift)** | `README.md:176`, `docs/principles/07-course-defense-qa.md:1` | 标题称“答辩 Top 10 核心问题快速索引（Cheat Sheet）”，README 仅列 10 题 | 讲义 07 实际包含 12 题（补充了 Q11 统一领域语言 customerId 与 Q12 前端路由防越权自动化测试验证） | 在 README.md 答辩速查中补齐 Q11 与 Q12 快速索引；讲义 07 标题优化为“答辩高频核心考问（Top 12 Q&A Cheat Sheet）” |
| D6 | **Pinia Store 命名偏差 (Store Drift)** | `README.md:223`, `docs/principles/02-...:257`, `docs/principles/06-...:191` | 文档多次提到 `stores/ (auth, cart)` 以及 `useAuthStore`、`authStore.logout()` | 前端实际 Store 文件为 `src/stores/user.ts` 与 `src/stores/cart.ts`，导出的是 `useUserStore`；Axios 拦截器直接操作 `localStorage` 中的 token | 将文档中的 `useAuthStore` / `auth` 修正为 `useUserStore` / `user`，并说明 Axios 拦截器的直接持久化交互 |
| D7 | **JWT 私钥配置不符 (Config Drift)** | `docs/principles/02-auth-and-satoken.md:109` | `jwt-secret-key: shop-secret-key-shop-secret-key-123456` | `backend/src/main/resources/application.yml` 实际为 `shop-platform-jwt-secret-key-32chars-min-abcdef123456` | 将讲义 02 中的 YAML 示例私钥与配置文件保持同步一致 |
| D8 | **讲义篇数记录滞后 (Artifact Drift)** | `docs/adr/0003-infrastructure-and-educational-artifacts.md:9` | 决议记录称配套“covering 6 core topics” | 实际已演进并完备交付 9 篇全景技术深度讲义（01~09） | 在 ADR-0003 后效中补充记录从 6 篇扩展为 9 篇全景讲义的设计决策 |
| D9 | **目录树结构不完整 (Layout Drift)** | `README.md:204-238` | 项目目录结构未包含 `docs/agents/`、`deploy/` 详细子结构、根目录 `CONTEXT.md` 及 `uploads/` | 实际项目包含上述重要辅助与资源目录 | 完善 README.md 中的目录树全景，补齐缺失节点 |

---

## 四、 规范发现表（Features Discovered Table）

| # | Category | Feature | Description | Inputs | Outputs | Error Behavior | Discovered Via |
|---|---|---|---|---|---|---|---|
| F1 | Quickstart | 一键启停脚本套件 | Windows 环境下自动检测 Redis、MySQL，自动化启动 Spring Boot 与 Vite 前端并打开浏览器 | 双击 `start.bat` 或在终端运行 `.\start.ps1`；停止运行 `stop.bat` / `.\stop.ps1` | 拉起 8080 与 5173 端口，弹出浏览器访问 `http://localhost:5173` | 端口未监听输出告警；停止脚本强制 kill 占用端口进程 | `start.bat`, `start.ps1`, `stop.bat`, `stop.ps1`, `README.md` |
| F2 | Persistence | 单 SQL 状态机原子防超卖 | 基于 MySQL InnoDB 行级排他锁，单条带状态条件的 SQL 扣减库存，拦截并发穿透 | `productId`, `quantity` | 返回受影响行数 `affectedRows`（1 成功，0 失败） | `affectedRows == 0` 时抛出 `BusinessException` 触发 `@Transactional` 回滚 | `ShopProductMapper.decrementStock`, `OrderCheckoutServiceImpl` |
| F3 | Caching | Spring 事务提交后双重缓存淘汰 | 下单成功后即刻淘汰商品详情与首页橱窗缓存，并通过 `TransactionSynchronization.afterCommit` 钩子在 MySQL 事务提交后二次淘汰 | 商品 ID 列表及事务上下文 | Redis `DEL` 缓存键删除 | 记录 Warn 日志，不阻塞主业务 | `OrderCheckoutServiceImpl.evictProductDetailCache` |
| F4 | Caching | 品牌精细化分区缓存键策略 | 首页及全量查询路由至 `catalog:brand:all`，带参或分页查询路由至 `catalog:brand:list:{page}:{size}:{kw}:{status}` | 分页与筛选参数 | 命中返回反序列化 JSON，未命中查 DB 并回写 Redis（TTL 6h） | Redis 宕机捕获异常并降级直查 MySQL | `BrandServiceImpl.java`, `04-redis-caching-patterns.md` |
| F5 | Commerce | 不可变商品快照与值对象 | 下单瞬间冻结商品当时的标题、单价、主图深拷贝至 `shop_order_item`，并将收货信息封装为 `DeliveryAddress` | 购物车勾选条目与收货地址命令对象 | 持久化快照行项与结构化收件人信息 | 传入非法地址参数通过 `isValid()` 校验拦截 | `ShopOrderItem`, `DeliveryAddress`, `schema.sql` |
| F6 | Domain Modeling | 统一领域通用语言（customerId） | 商业上下文中全面以 `customerId` 指代交易主体，数据库物理列名 `user_id` 通过 MyBatis-Plus `@TableField("user_id")` 解耦防腐 | 数据库 `user_id` 列 | 实体与 Service 层暴露严谨无二义性的 `customerId` | 无，持久层透明转换 | `ShopCartItem.java`, `ShopOrder.java`, `CONTEXT.md` |
| F7 | Testing | 并发防超卖集成压测套件 | 利用 10 线程 CountDownLatch 门闩在同一纳秒争抢仅 1 件库存，严格断言 1 成功 9 失败且库存精确归零 | `mvn test -Dtest=OrderConcurrencyTest` | 控制台输出断言成功报告 | 超卖或负库存导致 JUnit 5 断言失败 | `backend/src/test/.../OrderConcurrencyTest.java` |
| F8 | Testing | 现代前端工程测试套件 | Vitest + happy-dom 毫秒级测试路由守卫、Axios 拦截器与 Pinia Cart Store 状态流转 | `cd frontend && npm test` | 15 个测试断言用例全部绿色通过 | 拦截异常或断言不符退出码非零 | `frontend/test/` (3 个 spec 文件) |
| F9 | Security | 生产级边缘防护与 IP 真实还原 | Cloudflare 小黄云隐藏源站，Nginx 解析 `CF-Connecting-IP` 还原客户端真实 IP 并对登录接口施行限流（5r/s） | 公网 HTTPS 请求到达 Nginx | 正常放行代理至后端；超额返回 HTTP 429 Too Many Requests | 超频触发 `limit_req` 阻断，记录限流日志 | `deploy/nginx/conf.d/shop.conf`, `09-...guide.md` |
| F10 | Infrastructure | 15 年免签 SSL 端到端链路 | Cloudflare Origin CA 证书与 Nginx SSL 卸载结合，公网端到端 Full(Strict) 强加密 | `cert.pem`, `key.pem` 挂载至 Nginx 443 端口 | 浏览器绿锁信任通信，Nginx 局域网转发后端 8080 | 缺少证书时 `deploy.sh` 自动生成自签名证书降级容灾 | `deploy/nginx/ssl/README.md`, `deploy.sh` |

---

## 五、 边界与异常工况发现表（Edge Cases Table）

| # | Feature | Input / 边界工况 | Observed Behavior / 实际系统表现 |
|---|---|---|---|
| E1 | 单 SQL 库存扣减 | 物理库存为 1，10 个并发线程同时发起扣减 1 件 | MySQL InnoDB 施加行排他锁串行执行；第 1 个线程扣减成功（`affectedRows=1`），其余 9 个线程判定 `stock >= 1` 不成立（`affectedRows=0`），立即触发业务异常并回滚事务，物理库存精准归 0，绝不产生负库存 |
| E2 | 事务提交后缓存二次淘汰 | 读线程在写事务提交前读取了 MySQL 可重复读（RR）旧版本快照并回写 Redis | 写事务完成后触发 `TransactionSynchronization.afterCommit`，执行二次 `DEL` 操作，瞬时粉碎刚刚回写的陈旧缓存，实现最终一致性 |
| E3 | Redis 宕机或网络中断 | Redis 服务突然关停，客户端发起品牌列表或首页推荐查询 | `BrandServiceImpl` 内部 `try-catch` 捕获异常，打印 Warn 日志，自动平滑降级直查 MySQL，前端接口依然秒级响应 200，绝不报错 500 |
| E4 | 购物车商品批量校验 | 客户端勾选的购物车商品中包含已被商家软删除（`is_deleted=1`）或已下架的商品 | `ProductBatchLookupHelper` 执行批量检索并校验状态，若发现失效商品立即抛出 `BusinessException`，中断结算链路并给出具体商品名提示 |
| E5 | 未登录访客访问保护页面 | 未登录浏览器直接输入 `http://localhost:5173/cart` 或 `/admin` | Vue Router 全局前置守卫拦截，弹出“该功能需要登录”提示，重定向至 `/login?redirect=...`，保留原跳转路径以备登录后无缝跳回 |
| E6 | 普通顾客垂直越权访问后台 | 登录普通顾客账号（`ROLE_CUSTOMER`），在浏览器手动输入 `http://localhost:5173/admin` | 前端路由守卫判定 `userStore.isAdmin` 为 false，弹出权限不足提示并强制跳转回首页 `/`；即便绕过前端直接调用后台 API，后端 Sa-Token 路由拦截器抛出 `NotRoleException`，全局异常处理器返回 `403 Forbidden` |
| E7 | 水平越权订单窥探 (IDOR) | 用户 A 登录后，尝试查看或操作用户 B 的订单详情（如传入订单 ID 为用户 B 的订单） | 后端 Controller 统一从 `StpUtil.getLoginIdAsLong()` 获取真实顾客 ID，Service 层严密校验 `!customerId.equals(order.getCustomerId())`，立即抛出 403 异常拦截窥探 |
| E8 | 文件上传路径穿越攻击 | 攻击者上传文件名包含 `../../etc/passwd` 等恶意穿越字符 | `LocalStorageService` 使用 `StringUtils.cleanPath` 规范化路径，并显式判断 `storedFilename.contains("..")` 以及目标文件父目录必须完全等同于根存储目录绝对路径，违规直接抛出异常拦截 |
| E9 | 生产高频登录撞库暴力破解 | 黑客脚本以 50 次/秒的高频向 `/api/auth/login` 发起并发密码尝试 | Nginx `limit_req zone=login_limit burst=5 nodelay` 内存限流池立即熔断，超出阈值的请求在最外层直接返回 HTTP 429，后方 Spring Boot 与 MySQL 零负载 |

---

## 六、 达到 100% 同步所需的明确更新规格（Synchronization Specification）

为彻底解决上述文档审计发现的所有死链、漂移与不一致，建议按以下规格清单进行同步修正：

1. **修正 2 处物理死链**：
   - 将 `docs/principles/01-springboot3-internals-and-architecture.md` 第 199 行中的 `.../commerce/model/DeliveryAddress.java` 修改为 `.../commerce/dto/DeliveryAddress.java`。
   - 将 `docs/principles/03-orm-and-mybatis-plus.md` 第 149 行中的 `.../commerce/model/DeliveryAddress.java` 修改为 `.../commerce/dto/DeliveryAddress.java`。
2. **修正 1 处路由守卫行号引用与元数据描述**：
   - 将 `docs/principles/07-course-defense-qa.md` 第 267 行引用的 `frontend/src/router/index.ts（第 45-75 行）` 修正为 `第 103-136 行`。
   - 将第 258 行对后台路由元信息的描述修正为 `meta: { requiresAdmin: true }`，并精确描述守卫判定 `userStore.isAdmin` 的工作机制。
3. **补齐答辩速查考问索引**：
   - 在 `README.md` 答辩速查目录中补齐 Q11（统一通用语言 customerId 与 ORM 防腐）与 Q12（前端路由防越权与自动化测试验证）。
   - 将讲义 07 标题优化对齐为“答辩高频核心考问（Top 12 Q&A Cheat Sheet）”。
4. **统一前端 Store 与 Token 提取描述**：
   - 在 `README.md` 第 223 行、讲义 02 第 257 行、讲义 06 第 191 行中，将 `useAuthStore` / `auth` 修正为 `useUserStore` / `user`，并准确叙述 Axios 请求拦截器与 `localStorage` 的交互机制。
5. **校准 Sa-Token 示例配置**：
   - 将讲义 02 第 109 行的 `jwt-secret-key` 示例修正为与 `application.yml` 保持一致的 `shop-platform-jwt-secret-key-32chars-min-abcdef123456`。
6. **完善项目目录全景树**：
   - 在 `README.md` 第 204-238 行目录树中补充 `docs/agents/`、`deploy/` 关键子项、`CONTEXT.md` 及 `uploads/` 存储目录说明。
7. **更新 ADR-0003 记录**：
   - 在 `docs/adr/0003-infrastructure-and-educational-artifacts.md` 的后效中补充说明讲义已从最初规划的 6 篇扩充至 9 篇全景技术矩阵。
