# 领域边界划分与商业交易闭环设计决策 (ADR 0002: Domain Boundaries and Commerce Flow)

我们决定将 Shop 电商系统设计为双端统一体验架构（Customer Portal 商城前台 + Admin Console 管理后台），采用高内聚的增强型独立商品模型（Enhanced Standalone Product Model），构建端到端的“购物车至订单结算”商业完整闭环，并引入 Dockerized Redis 实例负责 Cache-Aside 旁路缓存与令牌会话管理。

## 备选方案评估与选型对比 (Considered Options)
- **Surface Architecture**：采用单套 Vue 前端工程统一整合双端（通过 Vue Router 路由守卫划分为前台 `/` 与后台 `/admin`），而非前后端割裂或仅提供单一 Admin Console。
- **Product Model Complexity**：采用高内聚的增强型独立商品模型（Standalone Product Model），而非过度复杂的企业级多维 SPU/SKU 笛卡尔积规格矩阵。
- **Commerce Flow Scope**：实现完整的端到端电商闭环（Browse -> Cart -> Checkout -> Stock Deduction -> Fulfillment），而非仅支持只读浏览的商品 Catalog。
- **Caching Middleware**：通过 Docker / 本地服务引入 Redis 7.2 实现 Cache-Aside 旁路缓存与快速失效，而非单纯依赖 MySQL 物理数据库强抗高并发读压力。

## 决策影响与架构收益 (Consequences)
- **Dual-Surface 统一视窗**：单套前端代码库同时承载 Customer Portal 与 Admin Console，配合 Vue Router 导航守卫与 Axios 请求拦截器全方位杜绝越权访问。
- **消除 SKU 笛卡尔积膨胀**：商品实体直接包含销售价格、物理库存、品牌/分类外键及画廊图片列表，在保留充足教学与演示扩展性的同时，避免了多维 SPU/SKU 导致的笛卡尔积复杂度。
- **事务与并发防超卖保证**：下单结算环节严格在 `@Transactional` 声明式事务管理下运行，采用带状态条件的单条原子更新 SQL 完成库存校验与扣减（利用 InnoDB 行级排他锁），为数据库事务 ACID、并发安全与防超卖提供了极佳的学习范例。
- **Cache-Aside 经典实战**：引入 Redis 搭建标准的 Cache-Aside 缓存读写模式，配套主动淘汰、防击穿降级以及事务提交后二次淘汰机制（`TransactionSynchronization.afterCommit`），成为深入掌握分布式缓存原理的优质实战载体。
