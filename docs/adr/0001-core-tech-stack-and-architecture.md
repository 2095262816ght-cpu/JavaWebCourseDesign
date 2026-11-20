# 核心技术栈与系统架构选型决策 (ADR 0001: Core Tech Stack and Architecture Selection)

我们决定将 Shop 电商平台构建为 Monorepo（单体大仓）工程，包含基于 Spring Boot 3.3+ 的企业级后端和基于 Vue 3 的现代化前端；后端采用 Sa-Token 处理身份认证与 RBAC 权限控制，依托 MySQL 8.4+ 并采用 MyBatis-Plus 负责数据持久化，前端基于现代化的 Vite 极速构建工具链。

## 备选方案评估与选型对比 (Considered Options)
- **Backend Runtime**：采用基于 Java 21/25 的 Spring Boot 3.3+，摒弃基于陈旧 Java 8/11 的传统 Spring Boot 2.7 架构。
- **Authentication & Authorization**：采用轻量高效的 Sa-Token（JWT Simple 令牌 + Redis 会话混合模式 + 动态 RBAC），而非配置繁琐厚重的 Spring Security 6 或裸写原生 HandlerInterceptor 拦截器。
- **Persistence ORM**：采用 MyBatis-Plus 3.5+，而非学习曲线陡峭且不易做精细 SQL 调优的 Spring Data JPA / Hibernate，亦非样板代码沉重的原生 MyBatis。
- **Frontend Stack**：采用 Vue 3 (Composition API) + Vite + TypeScript + Element Plus，摒弃老旧的服务端模板渲染方案（Thymeleaf / JSP）。
- **Repository Layout**：采用 Monorepo 统一大仓（包含 `backend/` 与 `frontend/`），而非跨多仓库维护。

## 决策影响与架构收益 (Consequences)
- **JDK 规范**：严格遵循 JDK 21+ 现代标准规范（本地开发与运行环境经由 JDK 25 严格验证兼容）。
- **Jakarta EE 迁移**：全站后端代码全面使用最新的 Jakarta EE 命名空间（`jakarta.*`），彻底消除陈旧的 `javax.*` 历史包袱。
- **无状态传输与可控风控**：Sa-Token 结合 `sa-token-jwt`（Simple 模式）与 `sa-token-redis-jackson`，客户端通过自定义 Header 携带防篡改 JWT 令牌，服务端依托 Redis 维护可控会话并配合动态 RBAC 判定，无需引入沉重的 Filter 过滤器链配置。
- **单表零 SQL 与原生可控**：MyBatis-Plus 对单表 CRUD 实现零 SQL 样板代码与 AST 分页增强，同时针对多表深度业务查询保留纯粹原生 SQL 的可控性。
- **Monorepo 交付体验**：大仓结构保障了前后端代码变更的原子提交（Atomic Commits）、提供了一键极速自动化启动脚本，极大地简化了课程设计与毕业设计的一键部署与运行演示。
