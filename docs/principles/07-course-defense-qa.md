# 07 - 课程答辩高频核心考问与满分应答秘籍（Top 12 Q&A Cheat Sheet）

> 本文是电商平台课程作业答辩决胜指南。汇集了高校课程答辩与技术面试中**最常被老师/考官提问的 12 大核心技术考点**。每个问题均提供：**考官提问意图解密**、**标准满分回答话术**、**本项目对应源码精确路径与代码引用**、以及**原理深挖反击点**。

---

## 考问目录速览

- [Q1: 为什么不用成熟的 Spring Security，而是选用 Sa-Token？两者的核心优劣是什么？](#q1-为什么不用成熟的-spring-security而是选用-sa-token两者的核心优劣是什么)
- [Q2: 电商系统高并发场景下最怕超卖，你的系统是如何解决库存并发扣减的？代码在哪里？](#q2-电商系统高并发场景下最怕超卖你的系统是如何解决库存并发扣减的代码在哪里)
- [Q3: 为什么你这里的 Service 层很多没有写冗余的包装接口，而是强调“深模块”设计？](#q3-为什么你这里的-service-层很多没有写冗余的包装接口而是强调深模块设计)
- [Q4: 系统的密码是如何存储的？为什么不用 MD5 加密？BCrypt 是什么原理？](#q4-系统的密码是如何存储的为什么不用-md5-加密bcrypt-是什么原理)
- [Q5: MyBatis-Plus 的分页插件是怎么在底层起作用的？它是怎么改写 SQL 的？](#q5-mybatis-plus-的分页插件是怎么在底层起作用的它是怎么改写-sql-的)
- [Q6: 项目里用了 Redis 做了什么缓存？怎么解决缓存和数据库的数据一致性？](#q6-项目里用了-redis-做了什么缓存怎么解决缓存和数据库的数据一致性)
- [Q7: 什么是缓存穿透、击穿、雪崩？你在项目中如何进行针对性防范？](#q7-什么是缓存穿透击穿雪崩你在项目中如何进行针对性防范)
- [Q8: Vue 3 相比 Vue 2 底层响应式原理发生了什么重大变革？Proxy 为什么优于 Object.defineProperty？](#q8-vue-3-相比-vue-2-底层响应式原理发生了什么重大变革proxy-为什么优于-objectdefineproperty)
- [Q9: 前端项目为什么选用 Vite 而不是传统的 Webpack？启动和热更新机制有何不同？](#q9-前端项目为什么选用-vite-而不是传统的-webpack启动和热更新机制有何不同)
- [Q10: 订单生成后如果商家后续修改了商品标题或价格，历史订单会不会被篡改？你是如何设计的？](#q10-订单生成后如果商家后续修改了商品标题或价格历史订单会不会被篡改你是如何设计的)
- [Q11: 数据库表列名是 user_id，为什么 Java 实体和 Service 统一使用 customerId？](#q11-数据库表列名是-user_id为什么-java-实体和-service-统一使用-customerid)
- [Q12: 前端如何防止普通顾客越权访问后台管理控制台？如何通过自动化测试证明安全性？](#q12-前端如何防止普通顾客越权访问后台管理控制台如何通过自动化测试证明安全性)

---

### Q1: 为什么不用成熟的 Spring Security，而是选用 Sa-Token？两者的核心优劣是什么？

#### 🎯 考官意图
考察学生的技术选型思维：是盲目人云亦云，还是真正结合了项目场景、开发成本和架构轻量化进行了权衡。

#### 💡 满分回答话术
“老师，我们在做架构调研时（详见 [ADR-0001](file:///D:/JavaWeb/docs/adr/0001-core-tech-stack-and-architecture.md)），对比了 Spring Security 6 和 Sa-Token。
1. **Spring Security 的痛点**：Spring Security 6 强制采用极其繁琐的 `SecurityFilterChain` 过滤器链配置，概念极其重且黑盒多（如 `AuthenticationManager`、`Provider`、`SecurityContextPersistenceFilter`），排查鉴权 Bug 成本高；同时默认基于 Session，在前后端分离无状态 Token 架构下需要手写大量的过滤器适配。
2. **Sa-Token 的核心优势**：
   - **轻量且低侵入**：底层基于标准 Spring MVC `HandlerInterceptor` 拦截器，代码完全可控；
   - **Simple JWT 模式（`StpLogicJwtForSimple`）**：引入 `sa-token-jwt` 生成标准 HMAC-SHA256 签名的 JWT 令牌，在客户端与 Request Header 中无状态安全流转，同时在服务端保留 Redis Session 支持，兼顾了 JWT 的无状态便携性与管理员即时封禁下线（Force Logout）的风控能力；
   - **声明式路由保护**：直接通过 `SaRouter.match("/api/admin/**", r -> StpUtil.checkRole("ROLE_ADMIN"))` 清晰直观地保护后台，一目了然；
   - **与现代 Spring Boot 3 天然兼容**：原生支持 Jakarta 命名空间、无状态 Token 生成、以及分布式 Redis 扩展。”

#### 📂 本项目源码佐证
- Sa-Token JWT 配置：[`com.shop.config.SaTokenConfig`](file:///D:/JavaWeb/backend/src/main/java/com/shop/config/SaTokenConfig.java)
- 路由防护拦截配置：`com.shop.config.WebMvcConfig` 的 `addInterceptors()` 方法
- 自定义角色动态加载：`com.shop.config.StpInterfaceImpl`（`getRoleList()`）

---

### Q2: 电商系统高并发场景下最怕超卖，你的系统是如何解决库存并发扣减的？代码在哪里？

#### 🎯 考官意图
考察学生对并发竞态条件（Race Condition）、数据库锁机制及事务隔离性的掌握深度。

#### 💡 满分回答话术
“老师，我们在库存扣减上采用了**数据库单条 SQL 原子条件更新 + 事务提交后双重缓存淘汰**方案，坚决摒弃了初学者常犯的‘先在 Java 查库存、再在内存判断、最后更新’的 Check-then-Act 错误模式。
1. **核心 SQL 语句**：
   ```sql
   UPDATE shop_product 
   SET stock = stock - #{quantity} 
   WHERE id = #{productId} 
     AND stock >= #{quantity} 
     AND is_deleted = 0;
   ```
2. **底层原理**：MySQL InnoDB 存储引擎在执行 UPDATE 时会自动为该行记录加排他行级锁（X 锁），让所有并发请求在内核层面排队。由于更新条件带有 `stock >= #{quantity}`，即使 100 个线程同时到达，当库存扣减至不足时，数据库直接返回受影响行数 `affectedRows = 0`。
3. **事务熔断与双重缓存淘汰**：我们在 `OrderCheckoutServiceImpl` 中判断如果受影响行数为 0，立即抛出 `BusinessException`，触发 Spring `@Transactional` 事务无痕回滚，绝对杜绝超卖和负库存。同时，扣减成功后即刻失效商品详情缓存，并注册 `TransactionSynchronization.afterCommit` 钩子在事务完全提交后二次淘汰，彻底杜绝并发脏缓存。
4. **集成测试验证**：我们编写了 `OrderConcurrencyTest`，使用 `CountDownLatch` 门闩并发模拟 10 个线程抢购仅 1 件库存，测试结果严格断言为 1 笔成功、9 笔被拦截报错、最终库存精准归 0。”

#### 📂 本项目源码佐证
- 原子 SQL 声明：`com.shop.modules.catalog.mapper.ShopProductMapper.decrementStock()`（第 18-19 行）
- 业务扣减、回滚与事务同步缓存淘汰：`com.shop.modules.commerce.service.impl.OrderCheckoutServiceImpl`（`checkout` 方法与 `evictProductDetailCache`）
- 多线程高并发测试用例：`backend/src/test/java/com/shop/modules/commerce/OrderConcurrencyTest.java`（第 78-162 行）

---

### Q3: 为什么你这里的 Service 层很多没有写冗余的包装接口，而是强调“深模块”设计？

#### 🎯 考官意图
考察学生对经典软件工程设计原则（如 John Ousterhout《A Philosophy of Software Design》）的理解，是否具备抵抗无意义过度设计和样板代码泛滥的能力。

#### 💡 满分回答话术
“老师，传统学生作业中经常存在一种形式主义反模式：‘每个表都写一个只有透传一行的 Service，Controller 像写流水账一样去调用 5 个 Service’，这属于典型的**浅模块（Shallow Module）**。
我们参考了斯坦福教授 John Ousterhout 的《软件设计哲学》：
1. **深模块的本质**：**接口极简，实现极深**。对外暴露的调用表面积极小，但背后隐藏了大量复杂的状态流转与容灾控制。
2. **以 `OrderCheckoutService.checkout()` 为例**：调用方 Controller 只需传 `customerId` 和收货信息命令对象，单次调用就封装了：
   - 购物车勾选过滤与 `ProductBatchLookupHelper` 批处理防穿透检索；
   - 委托充血实体 `ShopCartItem.calculateSubtotal()` 计算行小计（消除 Feature Envy）；
   - 并发单 SQL 原子防超卖库存扣减与事务提交后缓存二次淘汰；
   - 生成不可变商品快照并持久化 `DeliveryAddress` 值对象（消除数据泥团）；
   - 数据库事务一致性保障与购物车已购条目物理清理。
3. **删除测试（Deletion Test）**：如果把这个深模块删掉，这 6 大核心逻辑就会散落在各个 Controller 里变得不可维护，证明该模块具有极高的抽象价值和重构防御力。”

#### 📂 本项目源码佐证
- 详见技术讲义：[01-springboot3-internals-and-architecture.md](file:///D:/JavaWeb/docs/principles/01-springboot3-internals-and-architecture.md) 第 3 节与第 4 节。

---

### Q4: 系统的密码是如何存储的？为什么不用 MD5 加密？BCrypt 是什么原理？

#### 🎯 考官意图
考察 Web 安全意识、密码学哈希常识以及防暴力破解机制。

#### 💡 满分回答话术
“老师，我们系统坚决杜绝明文存储和普通 MD5 加密，采用的是业界标准的 **BCrypt 自适应单向哈希算法**。
1. **为什么不用 MD5**：
   - MD5 计算速度极快（现代 GPU 每秒可算数十亿次），且相同输入产生固定输出；黑客利用离线彩虹表（Rainbow Table）和字典碰撞能在几秒内破解常用密码；即使用固定 Salt，也难逃彩虹表逆向。
2. **BCrypt 的核心原理**：
   - **动态随机盐（Random Salt）**：每次注册生成密码时，BCrypt 内部生成一个随机的 128 位盐，拼接在最终的 Hash 字符串中（如 `$2a$10$...`）。即使两个用户输入完全相同的密码，数据库中保存的密文也截然不同；
   - **自适应工作因子（Cost Factor）**：故意设计为 CPU/内存密集型（默认迭代 1024 轮），单次哈希耗时约 50~100ms。这对正常登录用户完全无感知，但让黑客离线暴力破解的算力成本呈指数级剧增。”

#### 📂 本项目源码佐证
- 用户密码加盐与校验：`com.shop.modules.auth.service.impl.AuthServiceImpl`（注册 `BCrypt.hashpw`：第 48 行；登录 `BCrypt.checkpw`：第 72 行）

---

### Q5: MyBatis-Plus 的分页插件是怎么在底层起作用的？它是怎么改写 SQL 的？

#### 🎯 考官意图
考察对 MyBatis 底层四剑客（Executor、StatementHandler、ParameterHandler、ResultSetHandler）、拦截器链（Plugin）以及 SQL 解析机制的认知。

#### 💡 满分回答话术
“老师，MyBatis-Plus 分页插件（`PaginationInnerInterceptor`）底层是利用 **MyBatis 插件动态代理机制** 与 **JSqlParser AST（抽象语法树）SQL 改写** 实现的。
1. **执行拦截**：在我们在 `MybatisPlusConfig` 中注册拦截器后，每当执行 `selectPage()`，拦截器会在 `Executor.query()` 阶段环绕拦截当前的 SQL 语句。
2. **AST 智能改写**：
   - 首先利用 `JSqlParser` 将用户原始 SQL 解析为抽象语法树，剔除其中的 `ORDER BY` 等对行数统计无意义的节点，自动生成高效的 `SELECT COUNT(*) FROM ...`，先行查询获取总记录数 `total`；
   - 然后，在原语法树末尾追加 MySQL 方言的物理分页节点，生成带有 `LIMIT ?, ?` 的目标物理 SQL，并由 `ParameterHandler` 绑定分页偏移量；
3. **优势**：相比简单的字符串正则拼接，AST 改写能完美处理包含多表关联、子查询或复杂 `GROUP BY` 的复杂 SQL，杜绝语法解析错误。”

#### 📂 本项目源码佐证
- 分页拦截器注册：`com.shop.config.MybatisPlusConfig`（第 20-25 行）
- 原理解析图表：详见讲义 [03-orm-and-mybatis-plus.md](file:///D:/JavaWeb/docs/principles/03-orm-and-mybatis-plus.md) 第 2 节。

---

### Q6: 项目里用了 Redis 做了什么缓存？怎么解决缓存和数据库的数据一致性？

#### 🎯 考官意图
考察分布式缓存的使用场景、Cache-Aside 经典模式以及双写一致性保障。

#### 💡 满分回答话术
“老师，我们在商品陈列的高频读场景中（如首页推荐品牌、全部分类树以及品牌分页检索）深度实践了 **Cache-Aside 旁路缓存模式**：
1. **Cache-Aside 模式与分区键策略**：
   - **读操作与分级 TTL**：先查 Redis，命中直接返回；未命中则查 MySQL 并按业务分级赋予过期时间（首页聚合橱窗 `15分钟`、商品详情 `30分钟`、品牌列表与推荐 `6小时`、全部分类树 `24小时`）；
   - **防分页冲突的分区 Key 设计**：后台品牌默认首屏存入 `catalog:brand:all`，前台全量启用品牌存入 `catalog:brand:portal:all`，带特定分页或搜索条件的查询动态路由至 `catalog:brand:list:{page}:{size}:{kw}:{status}`，避免不同客户端的分页大小互相覆盖；
   - **写操作主动淘汰**：后台修改或删除品牌/分类/商品时，先更新 MySQL 数据库，随后**批量主动淘汰（DEL）**对应的缓存 Key。
2. **为什么‘淘汰缓存’优于‘更新缓存’？**：
   - 如果两个线程并发写，更新缓存可能会因线程调度乱序导致旧值覆盖新值产生永久脏数据；而直接淘汰缓存消除了并发覆盖风险，同时避免了无读请求时频繁更新缓存造成的计算资源浪费。
3. **事务同步双重淘汰（TransactionSynchronization.afterCommit）**：
   - 在下单扣减库存等写事务中，单纯在事务体内部淘汰缓存容易因并发读线程在事务提交前读到旧快照而将脏数据重新回写进 Redis。我们封装了 `TransactionalCacheEvictor` 注册 Spring 的 `afterCommit` 事务同步钩子，确保在数据库事务真正 commit 后二次发起淘汰，彻底杜绝脏缓存回写。
4. **容灾降级**：如果 Redis 服务暂时宕机，我们在 Service 内部捕获异常并平滑降级直查 MySQL，绝不让缓存异常拖垮整个业务。”

#### 📂 本项目源码佐证
- 读写生命周期、分区 Key 与主动淘汰：`com.shop.modules.catalog.service.impl.BrandServiceImpl`（`getFeaturedBrands`、`listBrands`、`evictBrandCache`）
- 事务提交后双重淘汰执行器：[`TransactionalCacheEvictor.java`](file:///D:/JavaWeb/backend/src/main/java/com/shop/common/cache/TransactionalCacheEvictor.java) 及 `OrderCheckoutServiceImpl`（`evictProductDetailCache`）

---

### Q7: 什么是缓存穿透、击穿、雪崩？你在项目中如何进行针对性防范？

#### 🎯 考官意图
考察高并发架构中经典的缓存三大灾难及其工程级防御策略。

#### 💡 满分回答话术
“老师，这三者是高并发系统中最核心的缓存防护命题，我们在项目中结合业务体量做了务实落地与演进预案：
1. **缓存穿透（查询压根不存在的数据）**：恶意用户伪造非法 ID 反复请求导致直击数据库。
   - *本项目落地与演进*：我们在结算与购物车链路中通过 `ProductBatchLookupHelper` 先去重判空防无效查询，并在详情查询中快速拦截 `null` 或非法 ID；若演进至公网大流量秒杀场景，可在未命中时写入短 TTL 空值哨兵（Cache Null）或前置布隆过滤器（Bloom Filter）。
2. **缓存击穿（单一爆款 Key 到期瞬间海量并发涌入）**：
   - *本项目落地与演进*：首页橱窗与推荐品牌等热点接口体积小、单库索引查询在毫秒级，配合写操作即时淘汰平滑过渡；超高并发场景下可升级为基于 Redis `SETNX` 互斥锁单线程重建缓存，或采用热点数据逻辑过期异步刷新。
3. **缓存雪崩（海量 Key 同一时刻集中大面积过期失效或缓存节点宕机）**：
   - *本项目已落地防线*：第一，我们将不同业务域缓存按 **`15m` / `30m` / `6h` / `24h` 四档错峰分级设置 TTL**（超大规模批量预热时可在此基础上叠加随机抖动时间打散过期窗口）；第二，所有缓存读写均内置 `try-catch` 毫秒级超时容灾降级（`timeout: 300ms`），即使 Redis 故障也能无缝降级直查 MySQL 保障核心交易可用。”

#### 📂 本项目源码佐证
- 详见技术讲义：[04-redis-caching-patterns.md](file:///D:/JavaWeb/docs/principles/04-redis-caching-patterns.md) 第 3 节。

---

### Q8: Vue 3 相比 Vue 2 底层响应式原理发生了什么重大变革？Proxy 为什么优于 Object.defineProperty？

#### 🎯 考官意图
考察现代前端框架底层原理、对 ES6 语言新特性的理解以及响应式机制。

#### 💡 满分回答话术
“老师，Vue 3 对响应式引擎进行了彻底的底层重构，用 ES6 `Proxy` 全面取代了 Vue 2 中的 `Object.defineProperty`。
1. **Vue 2 的致命缺陷**：
   - `Object.defineProperty` 只能劫持对象的现有属性，无法感知新增属性或删除属性（必须借助 `$set` / `$delete`）；
   - 无法原生监听数组下标修改（`arr[0] = x`）及 `length` 变化，被迫篡改了数组的 7 个原型方法；
   - 初始化时必须深度递归遍历所有属性，大对象会导致明显的卡顿。
2. **Vue 3 `Proxy` 的压倒性优势**：
   - `Proxy` 拦截的是整个对象本身，提供 13 种拦截操作，对动态新增属性、删除属性、数组下标变更完全原生支持；
   - 实现了**惰性响应式（Lazy Proxy）**，只有深层嵌套属性被真正访问时才动态代理，大幅降低初始化内存与 CPU 开销。”

#### 📂 本项目源码佐证
- 详见技术讲义：[06-vue3-and-vite-modern-frontend.md](file:///D:/JavaWeb/docs/principles/06-vue3-and-vite-modern-frontend.md) 第 1 节。

---

### Q9: 前端项目为什么选用 Vite 而不是传统的 Webpack？启动和热更新机制有何不同？

#### 🎯 考官意图
考察对现代前端工程化工具链与构建工具演化的认知。

#### 💡 满分回答话术
“老师，我们前端选择 Vite 主要是为了获得极致的开发效率与即时反馈：
1. **启动机制的根本差异**：
   - **Webpack 属于打包型工具**：本地启动前必须从入口递归抓取整个项目的所有依赖、解析 AST、全量打包生成 Bundle 后才能启动开发服务器，随着代码规模增大，启动时间从几秒恶化到数分钟；
   - **Vite 基于现代浏览器原生 ESM**：启动时几乎不做全量打包，直接秒级启动本地 HTTP 服务；当浏览器真正加载到某一个 `.vue` 组件时，Vite 才按需对其进行实时转译，冷启动时间恒定在毫秒级（$O(1)$ 复杂度）。
2. **依赖预构建优化**：首次启动时使用由 Go 编写的 **esbuild** 预构建 CommonJS 依赖，转译速度比基于 Node.js 的 Webpack 快 10~100 倍。”

#### 📂 本项目源码佐证
- 前端工程配置：`frontend/vite.config.ts`
- 详见技术讲义：[06-vue3-and-vite-modern-frontend.md](file:///D:/JavaWeb/docs/principles/06-vue3-and-vite-modern-frontend.md) 第 2 节。

---

### Q10: 订单生成后如果商家后续修改了商品标题或价格，历史订单会不会被篡改？你是如何设计的？

#### 🎯 考官意图
考察电商核心领域模型、商业一致性原则与数据快照设计（Snapshot Pattern）。

#### 💡 满分回答话术
“老师，这是一个非常经典也是初学者极易踩坑的商业系统问题。**在我们的系统中，历史订单的商品信息绝对不会被商家后续的修改所篡改。**
1. **反模式痛点**：如果订单项表（`shop_order_item`）只关联了 `product_id`，每次都连表查 `shop_product`，那么商家一旦给商品涨价或修改描述，用户过去已完成的订单金额和名称就会跟着被篡改，造成法律纠纷。
2. **不可变快照设计（Snapshot Pattern）**：
   - 我们在数据库设计中专门解耦了 `shop_order_item`（见 `docs/sql/schema.sql`）；
   - 在用户下单结算的瞬间，`OrderCheckoutServiceImpl` 会提取商品当时的 `name`、`main_image`、`price`，作为**不可变快照（Immutable Snapshot）**深拷贝持久化到 `shop_order_item` 表的 `product_name`、`product_image`、`unit_price` 字段中；
   - 此后即使原商品下架、降价或删除，历史订单中的数据仍然真实还原下单那一刻的历史原貌，充分保障了商业契约的严肃性与审计合规。”

#### 📂 本项目源码佐证
- 数据库表结构快照字段：`docs/sql/schema.sql`（`shop_order_item` 表，第 136-149 行）
- 结算时写入不可变快照：`com.shop.modules.commerce.service.impl.OrderCheckoutServiceImpl`（订单项快照循环，第 130-143 行）

---

### Q11: 数据库表列名是 user_id，为什么 Java 实体和 Service 统一使用 customerId？

#### 🎯 考官意图
考察学生对**领域驱动设计（DDD）通用语言（Ubiquitous Language）**、模块边界划分以及 ORM 防腐层（Anticorruption Layer）的理解。

#### 💡 满分回答话术
“老师，这是一个体现代码工程素养和领域建模严谨性的重要设计细节：
1. **消除泛化词汇的二义性**：在电商交易闭环中，浏览商品、加入购物车、下单结算的操作主体在业务概念上是明确的**顾客（Customer）**；而维护商品、审核订单的主体是**系统管理员（Administrator）**。如果业务代码和接口形参中通篇充斥着泛化的 `userId`，当读到 `order.getUserId()` 时很难第一时间确认这究竟是顾客 ID 还是操作员工号，极易引发业务混淆；
2. **构建 ORM 防腐映射层**：数据库历史表设计中由于惯性往往将外键定义为 `user_id`。如果强行修改数据库物理列名，可能波及历史数据备份或外部脚本。我们通过 MyBatis-Plus 的注解：
   ```java
   @TableField("user_id")
   private Long customerId;
   ```
   在持久化层与领域模型之间建立了优雅的防腐转换，既保留了数据库列名的向后兼容性，又在上层 Java 领域模型、DTO 与 Service 方法中实现了 100% 清晰严谨的领域通用语言。”

#### 📂 本项目源码佐证
- 实体属性解耦映射：[`ShopCartItem.java`](file:///D:/JavaWeb/backend/src/main/java/com/shop/modules/commerce/entity/ShopCartItem.java)、[`ShopOrder.java`](file:///D:/JavaWeb/backend/src/main/java/com/shop/modules/commerce/entity/ShopOrder.java)
- 领域服务契约定义：[`OrderCheckoutService.java`](file:///D:/JavaWeb/backend/src/main/java/com/shop/modules/commerce/service/OrderCheckoutService.java)

---

### Q12: 前端如何防止普通顾客越权访问后台管理控制台？如何通过自动化测试证明安全性？

#### 🎯 考官意图
考察学生对全栈纵深防御体系、前端单页面应用（SPA）动态路由拦截、以及前端工程化自动化测试（Automated Testing）的掌握程度。

#### 💡 满分回答话术
“老师，我们构建了**前端体验阻断 + 后端物理熔断 + 自动化测试断言**的三重立体防护：
1. **前端路由守卫拦截（Router Guard）**：
   - 在 `frontend/src/router/index.ts` 中配置了全局前置守卫 `router.beforeEach`。所有 `/admin` 路由元信息均声明了 `meta: { requiresAdmin: true }`；
   - 当普通顾客登录后试图在浏览器地址栏手动输入 `http://localhost:5173/admin` 试图越权时，守卫判定 `userStore.isAdmin` 为 false，在客户端即刻拦截，阻止后台管理组件渲染并弹窗提示无权访问；
2. **后端 Sa-Token 强校验绝对兜底**：
   - 前端拦截主要为了提升用户交互体验，不能作为绝对安全屏障。后端在 `WebMvcConfig` 中配置了 `SaRouter.match("/api/admin/**", r -> StpUtil.checkRole("ROLE_ADMIN"))`，黑客即便绕过前端直接用 Postman 调用管理 API，依然会被 Sa-Token 判定 403 物理拦截；
3. **前端自动化测试套件（Vitest + happy-dom）硬核证明**：
   - 我们编写了自动化测试套件 `frontend/test/router.spec.ts`，在轻量 DOM 仿真环境下真实模拟未登录访客、普通顾客、管理员三种身份分别跳转 `/admin` 路径，严格断言未登录重定向至 `/login`、顾客拦截跳转、管理员合法放行；
   - 执行 `npm test` 即可在毫秒级内跑完全套 19 个测试断言，用绿色自动化测试用例向答辩老师硬核证明鉴权逻辑的安全与完备。”

#### 📂 本项目源码佐证
- 前端路由守卫：`frontend/src/router/index.ts`（第 103-136 行）
- 前端自动化测试用例：[`frontend/test/router.spec.ts`](file:///D:/JavaWeb/frontend/test/router.spec.ts)
- 后端路由拦截：`com.shop.config.WebMvcConfig`（`addInterceptors()`，第 68-92 行）
