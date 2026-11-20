# Project: JavaWeb Shop 电商平台 — 当前交付状态

## Architecture
- **Monorepo Structure**:
  - `backend/`: Java 21, Spring Boot 3.3.4, MyBatis-Plus 3.5.7, Sa-Token 1.39.0（JWT Simple 模式 + Redis 会话）, SpringDoc 2.6.0。服务端口 `8080`，REST 路由统一以 `/api/` 开头。
  - `frontend/`: Vue 3.5.12, TypeScript 5.6.3, Vite 5.4.9, Pinia 2.2.4（`user`、`cart`）, Element Plus 2.8.5。开发端口 `5173`，Vite 代理 `/api` 与 `/uploads` 到 `8080`。
  - `deploy/`: `docker-compose.prod.yml`（生产 5 容器、仅暴露 80/443、Nginx SSL 卸载）；`deploy/trial/`（云端试用 5 容器、仅暴露 80、Cloudflare 边缘 HTTPS 终结 + 本地构建产物/官方镜像）。
  - `docs/`: 1 篇新手导读（`docs/principles/00`）+ 11 篇底层原理与运维讲义（`docs/principles/01-11`）、3 份 ADR（`docs/adr/0001-0003`）、数据库脚本与数据字典（`docs/sql/`）、AI 工单规范（`docs/agents/`）、团队分工与答辩指南。
  - 根目录脚本：`start.bat` / `start.ps1` / `stop.bat` / `stop.ps1`；根目录 `docker-compose.yml`、`shop.conf`、`frontend.conf`、`.env.example` 为试用部署包平铺副本。

## Delivered Feature Inventory
| # | 功能 | 当前实现 | 状态 |
|---|------|----------|------|
| 1 | 注册 / 登录 / 资料接口 | BCrypt 加盐哈希；Sa-Token JWT Simple 签发；Token 存 `localStorage`，请求头 `satoken`（后端已备 `GET/PUT /api/auth/profile` 接口） | DONE |
| 2 | 双端权限控制 | 前端 `requiresAuth` / `requiresAdmin` 路由守卫；后端 `/api/admin/**` 强制 `ROLE_ADMIN`；禁用用户即时踢下线 | DONE |
| 3 | 后台用户管理 | 分页查询、关键词/角色/状态筛选、详情、启用/禁用 | DONE |
| 4 | 品牌管理 | CRUD、产地 `origin`（前台展示 + 后台关键字检索）、Logo 上传、状态与排序；Redis 分区缓存 | DONE |
| 5 | 分类管理 | 多级分类树、增删改、父子层级校验；前台/后台两套树缓存 | DONE |
| 6 | 商品管理 | CRUD、上下架、库存、主图（单张必填）+ 副图（多张可选）、富文本详情 | DONE |
| 7 | 首页聚合橱窗 | `/api/portal/home` 单请求返回轮播、甄选品牌、分类树、热销、新品；`catalog:showcase:home` 缓存 | DONE |
| 8 | 商品检索 | 关键词、品牌、分类（含子分类）、价格区间、价格/时间排序、分页 | DONE |
| 9 | 购物车 | 加购累加、数量修改、勾选、小计/合计、本地缓存 `shop_cart_cache` 与登录态同步 | DONE |
| 10 | 下单结算 | 校验勾选商品、单 SQL 原子扣减库存、订单项不可变快照、清空已结算购物车 | DONE |
| 11 | 订单状态机 | `UNPAID → PAID → SHIPPED → COMPLETED`；`UNPAID`/`PAID` 可取消为 `CANCELLED` 并回补库存 | DONE |
| 12 | 后台订单管理 | 订单号/状态/收货手机号筛选、分页、标记发货、取消 | DONE |
| 13 | 缓存与一致性 | Cache-Aside + 写后主动淘汰 + `TransactionalCacheEvictor` 事务提交后二次淘汰；Redis 异常降级直查库 | DONE |
| 14 | 前端体验 | 双端布局、玻璃拟态样式 `glass.css`、`ProductCard` 复用、`v-reveal` 滚动入场 | DONE |
| 15 | 容器化部署 | 生产编排（Cloudflare + Nginx + HTTPS）；试用编排（阿里云 ECS 2C4G，公网仅 80，对外 HTTPS 由 Cloudflare 终结） | DONE |
| 16 | 自动化测试 | 后端 15 个测试类 / 67 个用例；前端 4 个 spec / 19 个用例 | DONE |

## Milestones
| # | Name | Scope | Status |
|---|------|-------|--------|
| M1 | 脚手架与认证 | Monorepo、统一响应/异常、Sa-Token、双端布局、注册登录与资料 | DONE |
| M2 | 目录与商品 | 品牌、分类、商品管理，首页聚合橱窗与前台检索 | DONE |
| M3 | 购物车与订单 | 购物车、结算、原子扣减、订单快照与全状态流转 | DONE |
| M4 | 教学与答辩资料 | 1 篇新手导读、11 篇原理与运维讲义、Top 12 答辩指南、ADR、数据字典与四人分工指南 | DONE |
| M5 | 生产部署方案 | 生产 Compose、Nginx SSL/限流、部署脚本与安全讲义 | DONE |
| M6 | 云端试用部署 | 阿里云 ECS 单机容器化上线，HTTPS 域名 + Cloudflare 代理，公网端口收敛为 80 | DONE |
| M7 | 文档同步 | README / PROJECT / CONTEXT / docs 与代码现状对齐 | DONE |

## Interface Contracts
- **Backend Base Path**: `/api/auth/**`、`/api/portal/**`、`/api/cart/**`、`/api/orders/**`、`/api/admin/**`（brands/categories/products/orders/users）、`/api/common/**`（上传）。
- **Authentication**: 请求头 `satoken` 携带 JWT；前端 `localStorage` 使用 `token`、`user` 与 `shop_cart_cache`；收到 401 时统一清理并通过 `auth:expired` 事件同步退出。
- **Upload**: `POST /api/common/upload`，文件落盘 `uploads/`，通过 `/uploads/**` 访问；试运行时由 Nginx 直接托管上传目录。
- **Database**: 7 张表（`sys_user`、`shop_brand`、`shop_category`、`shop_product`、`shop_cart_item`、`shop_order`、`shop_order_item`）；`shop_brand.origin VARCHAR(64)`；`shop_product.main_image` 必填、`sub_images` 可选；逻辑删除字段 `is_deleted`。
- **Redis Keys**: `catalog:showcase:home`、`catalog:product:detail:{id}`、`catalog:category:tree`、`catalog:category:admin:tree`、`catalog:brand:all`、`catalog:brand:portal:all`、`catalog:brand:list:{page}:{size}:{kw}:{status}`、`catalog:brand:featured:{limit}`。
- **Order Status**: `UNPAID`、`PAID`、`SHIPPED`、`COMPLETED`、`CANCELLED`。
- **Ports**: 开发 5173 / 8080 / 3306 / 6379；生产 80 / 443；试用仅 80（MySQL、Redis、后端不映射宿主机；8080/8443 已于 2026-11-16 下线）。

## Verification
```bash
# 后端测试（15 个测试类 / 67 个用例，含 10 线程防超卖压测）
cd backend && mvn test

# 前端测试（4 个 spec / 19 个用例）与生产构建
cd frontend && npm test && npm run build
```
- **云端试用环境**：2026-11-18 实测 `https://shopnest.dpdns.org`（Cloudflare 代理，证书校验通过）、`http://123.57.26.237` 与 `/api/portal/home`、`/api/portal/products` 均返回 HTTP 200（`code=200`）；`8080` / `8443` 及公网 `22` 已确认从公网不可达。
- **已知约束**：`deploy/trial/docker-compose.yml` 的 `shop-backend` 必须保留 `command: ["java", "-jar", "/app/app.jar"]`，否则官方 JRE 镜像会立即退出并反复重启。
- **镜像加速**：国内部署需在 `/etc/docker/daemon.json` 配置可用的公共镜像源，地址中不得包含反引号或全角符号。

## Code Layout
- `README.md`: 项目总览、部署指南、测试说明与答辩速查。
- `CONTEXT.md`: 领域统一语言与术语表。
- `backend/`: Spring Boot 源码（`common`、`config`、`modules/auth|catalog|commerce|system`）、资源、测试与 `Dockerfile`。
- `frontend/`: Vue 3 源码（`api`、`components`、`directives`、`layouts`、`router`、`stores`、`styles`、`utils`、`views`）、测试与 `Dockerfile`。
- `deploy/`: 生产编排与 Nginx/脚本；`deploy/trial/`: 试用环境编排与网关配置。
- `docs/adr/`: 架构决策记录（0001-0003）。
- `docs/principles/`: 1 篇新手导读（00）与 11 篇技术与运维讲义（01-11）。
- `docs/sql/`: `schema.sql`、`data.sql`、数据字典 `README.md`。
- `docs/agents/`: AI 协作与工单管理规范。
