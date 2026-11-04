# 02 - 无状态认证与 Sa-Token 权限鉴权原理

> 本文是电商平台课程作业核心技术讲义第二篇。深入剖析 **无状态 Token 认证架构**、**JWT 令牌内部机理与防伪算法**、**Sa-Token 框架底层拦截与 ThreadLocal 上下文绑定**、以及 **基于 RBAC 的精细化权限路由体系**。

---

## 1. 传统 Session 认证 vs. 无状态 Token 认证

在 Web 开发演化历程中，用户鉴权经历了从服务端有状态 Session 到客户端无状态 Token 的深刻技术迭代。

```
【传统 Session 架构】
Client  --->  [LB]  --->  Server 1 (Session 内存)
                      \-> Server 2 (无法识别 Session，需 Session 黏滞或 Redis 广播)

【无状态 Token 架构】
Client (携带 Token)  --->  [LB]  --->  任意 Server 节点 (基于密钥验签，无缝水平扩展)
```

### 1.1 对比分析矩阵

| 特性维度 | 服务端 Session 模式 | 无状态 Token / JWT 模式 |
| :--- | :--- | :--- |
| **状态存储位置** | 服务端内存（或集中式 Session 共享服务） | 客户端存储（LocalStorage / Cookie），服务端只存验签规则或黑名单 |
| **集群横向扩展性** | 差。依赖 Session 复制、Nginx IP Hash 黏性会话或分布式 Session 中间件 | **极佳**。任何后端实例只要持有相同签名密钥，即可独立验证合法性 |
| **移动端/跨端支持** | 较差。原生 APP、微信小程序对 Cookie 支持不佳，需定制处理 | **原生友好**。统一在 HTTP Request Header (`satoken: <token>`) 中传输 |
| **CSRF 跨站请求伪造** | 易受攻击，必须配合复杂的 CSRF Token 防御机制 | **天然免疫**。只要 Token 存储在前端内存/Header，浏览器跨站不会自动携带 |

---

## 2. JWT（JSON Web Token）内部机理与签名防伪

JWT 是一种开放标准（RFC 7519），它定义了一种紧凑且自包含的方式，用于在各方之间以 JSON 对象安全地传输信息。

### 2.1 令牌的三段式结构剖析

一个合法的 JWT 字符串由两个点号 `.` 分割为三部分：`Header.Payload.Signature`。

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJsb2dpbklkIjoxLCJyb2xlIjoiUk9MRV9BRE1JTiIsImV4cCI6MTczMDcwOTAwMH0.dGVzdHNpZ25hdHVyZXZhbHVlaGVyZTEyMzQ1Njc4OTA
\___________________________________/ \_____________________________________________________________/ \____________________________________________/
             1. Header                                           2. Payload                                              3. Signature
```

#### 1. Header（头部）
包含令牌类型与所采用的密码学加密算法：
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```
经过 Base64URL 编码后形成第一部分。

#### 2. Payload（载荷）
承载实际业务声明（Claims），包含标准声明与自定义业务字段：
```json
{
  "loginId": 1,
  "role": "ROLE_ADMIN",
  "iat": 1730700000,
  "exp": 1733292000
}
```
经过 Base64URL 编码后形成第二部分。**注意：Payload 属于可逆编码而非加密，绝不能存放用户明文密码等高敏感数据。**

#### 3. Signature（防篡改数字签名）
由服务端持有绝密的私钥（Secret Key），将前两部分的 Base64 编码字符串使用指定算法（如 HMAC-SHA256）生成哈希签名：
$$\text{Signature} = \text{HMACSHA256}(\text{base64UrlEncode}(\text{Header}) + "." + \text{base64UrlEncode}(\text{Payload}), \text{SecretKey})$$

> [!IMPORTANT]
> **防伪核心原理**：客户端若试图篡改 Payload 中的 `loginId` 或 `role`（如将 `ROLE_CUSTOMER` 篡改为 `ROLE_ADMIN`），由于黑客无法获知服务端的 `SecretKey`，篡改后的内容必定无法计算出相吻合的 `Signature`。服务端验签时会立即捕获异常并拦截请求。

---

## 3. Sa-Token 框架底层设计与拦截链路

在本项目技术选型中（见 [0001-core-tech-stack-and-architecture.md](file:///D:/JavaWeb/docs/adr/0001-core-tech-stack-and-architecture.md)），我们摒弃了配置极其繁重、滤镜链黑盒重重的 Spring Security 6，引入了轻量级、响应式友好的 **Sa-Token**。

### 3.1 请求拦截全流程时序图

```mermaid
sequenceDiagram
    autonumber
    participant Client as 前端/客户端
    participant Interceptor as WebMvcConfig (SaInterceptor)
    participant ThreadCtx as ThreadLocal (SaHolder)
    participant StpUtil as StpUtil / StpLogic
    participant Router as SaRouter
    participant Interface as StpInterfaceImpl
    participant Controller as 业务 Controller

    Client->>Interceptor: 发起 HTTP 请求 (携带 satoken 请求头)
    Interceptor->>ThreadCtx: 将当前 HttpServletRequest/Response 绑定到 ThreadLocal
    Interceptor->>StpUtil: 解析 Header 中的 satoken 提取 LoginId
    Interceptor->>Router: 执行路由匹配规则 (SaRouter.match)
    alt 匹配 /api/admin/** 路径
        Router->>StpUtil: checkRole("ROLE_ADMIN")
        StpUtil->>Interface: getRoleList(loginId, "login")
        Interface-->>StpUtil: 返回数据库用户的真实角色列表
        alt 角色不包含 ROLE_ADMIN
            StpUtil-->>Client: 抛出 NotRoleException -> 全局异常捕获返回 403 Forbidden
        end
    end
    Interceptor->>Controller: 鉴权通过，放行请求至业务层
    Controller-->>Client: 返回统一结果 Result<T>
    Interceptor->>ThreadCtx: 请求结束，清理 ThreadLocal 上下文防止内存泄漏
```

### 3.2 路由级声明式拦截配置源码分析

在 `com.shop.config.WebMvcConfig` 中，我们通过注册 `SaInterceptor` 构建了全链路路由防护墙：

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new SaInterceptor(handle -> {
        // 管理员专用接口防护：必须持有 ROLE_ADMIN 角色
        SaRouter.match("/api/admin/**", r -> StpUtil.checkRole("ROLE_ADMIN"));
        
        // 顾客核心交互接口防护：必须完成登录认证
        SaRouter.match("/api/auth/profile", r -> StpUtil.checkLogin());
        SaRouter.match("/api/auth/logout", r -> StpUtil.checkLogin());
        SaRouter.match("/api/cart/**", r -> StpUtil.checkLogin());
        SaRouter.match("/api/orders/**", r -> StpUtil.checkLogin());
    })).addPathPatterns("/**")
       .excludePathPatterns(
            "/api/auth/login",
            "/api/auth/register",
            "/api/portal/**",      // 商城前台浏览接口公开
            "/api/common/**",
            "/uploads/**",         // 静态图片资源放行
            "/swagger-ui/**",      // 交互式 API 文档放行
            "/v3/api-docs/**",
            "/favicon.ico"
       );
}
```

### 3.3 ThreadLocal 上下文与内存泄漏防范

Sa-Token 在底层利用 `ThreadLocal` 实现了线程级上下文共享容器（`SaHolder`）。
- **生命周期**：每次 HTTP 请求进入 Tomcat 线程池分发的工作线程时，拦截器将 `HttpServletRequest` 与 `HttpServletResponse` 挂载入当前线程局部变量中。
- **免传参调用**：在系统任何 Service、Mapper 或工具类中，直接调用静态方法 `StpUtil.getLoginIdAsLong()`，底层均自动从当前线程变量中提取当前登录凭证，无需在所有业务方法形参中层层透传 `HttpServletRequest`。
- **内存回收机制**：在拦截器的 `afterCompletion` 回调中，框架强制执行 `SaHolder.clear()`，彻底清空当前线程绑定的上下文数据，彻底杜绝了因 Tomcat 线程池线程复用所导致的**上下文串号**与 **ThreadLocal 内存泄漏（Memory Leak）**。

---

## 4. RBAC（基于角色的访问控制）与动态权限接口

本项目采用业界通用的 **RBAC（Role-Based Access Control）** 模型，用户实体与角色直接绑定。

### 4.1 动态角色加载实现：`StpInterfaceImpl`

通过实现 Sa-Token 核心 SPI 接口 `cn.dev33.satoken.stp.StpInterface`，框架在执行权限鉴权校验时动态回调业务查询：

```java
@Component
public class StpInterfaceImpl implements StpInterface {

    private final SysUserMapper sysUserMapper;

    public StpInterfaceImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (loginId == null) {
            return Collections.emptyList();
        }
        try {
            Long userId = Long.valueOf(loginId.toString());
            SysUser user = sysUserMapper.selectById(userId);
            // 实时校验：用户存在、拥有角色且账号状态必须为启用 (status == 1)
            if (user != null && user.getRole() != null && user.getStatus() == 1) {
                return List.of(user.getRole());
            }
        } catch (Exception ignored) {
        }
        return Collections.emptyList();
    }
}
```

> [!TIP]
> **实时风控优势**：若管理员在后台将某一违规用户的 `status` 置为 `0`（封禁），该用户在下一次发起需要权限的请求时，`StpInterfaceImpl` 实时查出其不再具备可用角色，立即阻断其操作并强制拦截，实现了零时延的动态权限下发。

---

## 5. 密码安全加密算法：BCrypt 原理

在用户注册与登录模块（`AuthServiceImpl`）中，严禁在数据库明文存储密码，更不得采用早已被彩虹表碰撞攻破的普通 MD5。

### 5.1 为什么 MD5 + 静态 Salt 已不再安全？
- **MD5 执行速度过快**：现代 GPU 算力下一秒可计算数百亿次 MD5 哈希，即便加上固定 Salt，黑客通过离线彩虹表暴力字典也能迅速反推密码。
- **哈希结果确定性**：相同密码加相同 Salt 产生的密文完全相同。

### 5.2 BCrypt 加密的工作原理
本项目使用 `cn.dev33.satoken.secure.BCrypt`（基于 Blowfish 算法的自适应单向散列）：
1. **自带动态随机盐（Random Salt）**：每次调用 `BCrypt.hashpw(password, BCrypt.gensalt())`，算法都会在内部生成一个全新的 128 位随机 Salt，并将其与计算后的 Hash 值合并打包存储在一串以 `$2a$10$...` 开头的 60 字符字符串中。
2. **相同明文产生不同密文**：即使用户设置完全相同的密码（如 `123456`），由于每次生成的 Salt 随机，数据库持久化的密文完全不同，彻底粉碎了彩虹表攻击。
3. **工作因子（Work Factor / Cost）自适应耗时**：BCrypt 支持调整计算复杂度（默认迭代 $2^{10}=1024$ 轮）。算法故意设计为 CPU 与内存密集型（约耗时 50-100ms），极大增加了黑客暴力枚举的攻击成本，同时保证了正常的业务登录响应体验。
