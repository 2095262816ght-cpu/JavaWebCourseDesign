# 基础设施与教学答辩资料体系决策 (ADR 0003: Infrastructure and Educational Artifacts)

我们决定使用专有 MySQL 数据库 `shop_db` 并配套初始化 DDL 脚本与本地开发种子数据脚本；采用基于 Spring MVC 静态资源映射的本地磁盘文件存储来存放商品与品牌图片；集成 SpringDoc OpenAPI 3 提供交互式 API 契约文档；并在 `docs/principles/` 目录下打造涵盖 11 篇核心底层原理、部署加固与答辩指南的系统化教学专栏，并配套 1 篇新手导读作为阅读入口。

## 备选方案评估与选型对比 (Considered Options)
- **Database Setup**：提供完整的初始化表结构脚本（`schema.sql`）与种子数据预设脚本（`data.sql`），而非要求用户手动漫长配置。
- **Image Storage**：采用深模块封装的本地磁盘存储 + Spring MVC 静态资源 URL 路径映射（`/uploads/**`），而非强制绑定需要云账号与凭据的公有云对象存储（Aliyun OSS / Tencent COS）或繁琐的 Docker MinIO 方案。
- **API Documentation**：集成 SpringDoc OpenAPI 3（Swagger UI）并配合全局统一 `Result<T>` 响应协议，而非依赖易失步的手工 Postman 文件或无注释裸 Controller。
- **Educational Artifacts**：在 `docs/principles/` 专栏下编写覆盖 11 大核心主题的深度技术讲义、配套 1 篇新手导读，并配以代码级透彻注释，而非仅交付空泛的项目源码。

## 决策影响与架构收益 (Consequences)
- **100% 离线自主可控**：系统完全无需依赖任何外部公有云账户、AccessKey 凭据或联网存储服务，即使在完全断网的环境下也能开箱即用稳定运行。
- **本地存储极简高内聚**：后台上传的商品图片与品牌徽标持久化于本地物理磁盘，并通过 Spring `WebMvcConfigurer` 映射为标准 HTTP 资源路径（`/uploads/**`）。
- **交互式 OpenAPI 契约**：服务启动后即自动在 `/swagger-ui/index.html` 暴露全量 OpenAPI 规范的交互式接口文档，支持在线参数调试。
- **全景答辩与技术精进体系**：`docs/principles/` 成为可以直接用于课程设计答辩、毕业设计论文编写以及高质量面试复习的专业级参考资料，全面扩充并收录了 11 篇深度讲义，并配套 1 篇新手导读：
  - 阅读入口：[`00-beginner-guide.md`](file:///D:/JavaWeb/docs/principles/00-beginner-guide.md) — 前置基础清单、项目代码地图、完整请求链路与排错手册
  1. [`01-springboot3-internals-and-architecture.md`](file:///D:/JavaWeb/docs/principles/01-springboot3-internals-and-architecture.md) — Spring Boot 3 核心底层原理与深模块架构设计
  2. [`02-auth-and-satoken.md`](file:///D:/JavaWeb/docs/principles/02-auth-and-satoken.md) — 无状态认证与 Sa-Token 权限鉴权原理
  3. [`03-orm-and-mybatis-plus.md`](file:///D:/JavaWeb/docs/principles/03-orm-and-mybatis-plus.md) — ORM 演进、AST 语法树重写与 MySQL 索引事务原理
  4. [`04-redis-caching-patterns.md`](file:///D:/JavaWeb/docs/principles/04-redis-caching-patterns.md) — Redis 缓存设计模式与经典高并发攻防
  5. [`05-concurrency-and-inventory-safety.md`](file:///D:/JavaWeb/docs/principles/05-concurrency-and-inventory-safety.md) — 高并发库存防超卖原理与极简原子化设计
  6. [`06-vue3-and-vite-modern-frontend.md`](file:///D:/JavaWeb/docs/principles/06-vue3-and-vite-modern-frontend.md) — Vue 3 响应式原理、Vite ESM 极速构建与现代前端架构
  7. [`07-course-defense-qa.md`](file:///D:/JavaWeb/docs/principles/07-course-defense-qa.md) — 课程答辩高频核心考问与满分应答秘籍（Top 12 Q&A Cheat Sheet）
  8. [`08-defense-knowledge.md`](file:///D:/JavaWeb/docs/principles/08-defense-knowledge.md) — 项目安全防护体系与网络传输防御（答辩知识全景指南）
  9. [`09-scheme1-production-deployment-guide.md`](file:///D:/JavaWeb/docs/principles/09-scheme1-production-deployment-guide.md) — 生产环境安全部署方案对比与 Cloudflare 边缘加固实战指南
  10. [`10-cloud-trial-deployment-guide.md`](file:///D:/JavaWeb/docs/principles/10-cloud-trial-deployment-guide.md) — 云端试用环境部署实战教程（阿里云 ECS 单机容器化、镜像加速与踩坑实录）
  11. [`11-remote-ops-and-credential-hardening.md`](file:///D:/JavaWeb/docs/principles/11-remote-ops-and-credential-hardening.md) — 远程运维接入与默认凭据加固实战（Workbench CLI 免密通道、默认口令处置与公网 SSH 收敛）
