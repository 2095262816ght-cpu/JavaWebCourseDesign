# 11 - 远程运维接入与默认凭据加固实战（Workbench CLI / 安全组 / 种子数据）

> 本文记录 2026-11-17 一次完整的线上加固过程：从「如何让 AI 代理安全地接入云服务器」，
> 到「发现并处置默认管理员口令」，再到「收敛公网 SSH 暴露面」。
> 每个结论都有实测输出支撑，每一步都保留回滚路径。

---

## 目录

1. [背景与结论摘要](#1-背景与结论摘要)
2. [远程接入方案选型](#2-远程接入方案选型)
3. [方案一：SSH 公钥授权](#3-方案一ssh-公钥授权)
4. [方案二：Workbench CLI（最终采用）](#4-方案二workbench-cli最终采用)
5. [RAM 最小权限策略与授权踩坑](#5-ram-最小权限策略与授权踩坑)
6. [凭证落地位置与权限](#6-凭证落地位置与权限)
7. [首次只读体检结果](#7-首次只读体检结果)
8. [高危发现：默认管理员口令仍然有效](#8-高危发现默认管理员口令仍然有效)
9. [处置一：修改管理员口令](#9-处置一修改管理员口令)
10. [处置二：失效存量登录会话](#10-处置二失效存量登录会话)
11. [处置三：根治仓库种子账号](#11-处置三根治仓库种子账号)
12. [处置四：收敛公网 SSH 暴露面](#12-处置四收敛公网-ssh-暴露面)
13. [100.104.0.0/16 的来源、观测与变更预案](#13-1001040016-的来源观测与变更预案)
14. [演示与日常登录说明](#14-演示与日常登录说明)
15. [踩坑实录](#15-踩坑实录)
16. [命令速查表](#16-命令速查表)
17. [遗留事项](#17-遗留事项)

---

## 1. 背景与结论摘要

### 1.1 背景

项目已部署在阿里云 ECS（`123.57.26.237` / `i-2zeczdp8tjthshnkmv1r`，华北 2 北京），
唯一对外入口是 Cloudflare 代理的 443 与源站 80 端口。
需要让 AI 代理（Trae）能够直接登录服务器排查问题，而不是每次都人工转述命令与输出。

### 1.2 结论摘要

| 项目 | 处置前 | 处置后 |
| :--- | :--- | :--- |
| 管理员口令 | `admin` + 种子默认口令，可被公开猜到 | 已改为独立强口令，旧口令返回 401 |
| 存量登录会话 | 5 个 Sa-Token 会话仍在 Redis | 全部清除，需重新登录 |
| 生产种子数据 | 编排挂载 `data.sql`，重装即复现漏洞 | 生产不再挂载，仅有表结构 |
| 公网 SSH | `TCP 22 ← 0.0.0.0/0` 全网开放 | 仅 `100.104.0.0/16` 内网可达，公网 TCP 握手失败 |
| 远程接入方式 | 人工输密码 / 无稳定自动化通道 | Workbench CLI 非交互执行，结构化输出 |

> **一句话结论**：这次真正的收获不是"改了个密码"，而是建立了一条
> **可重复、可回收、不依赖明文口令**的运维通道，并把"重装即复发"的根因切掉了。

---

## 2. 远程接入方案选型

阿里云提供三条免公网 / 免密码的连接路径，实际对比后选了第二条：

| 能力 | Workbench 网页版 | Workbench CLI | 会话管理 CLI（ali-instance-cli） |
| :--- | :--- | :--- | :--- |
| 交互形式 | 浏览器 GUI | 本机命令行 | 本机命令行 |
| 是否需公网 IP | 否 | 否 | 否 |
| 远程命令与文件传输 | 需手动操作 | 原生 `exec` / `upload` / `download` | 仅终端会话 |
| 面向 AI Agent | 否 | **是**（`--output json` + 退出码透传） | 否 |

选型理由：

1. 网页版无法被程序调用，每次都要人手工点；
2. CLI 有 `exec`，可以把命令结果直接回传给代理解析；
3. `ali-instance-cli` 只能开交互式终端，仍需要额外脚本封装。

> **重要认知**：Workbench 不是"另一条网络通道"，它最终仍以 SSH 连实例的 22 端口，
> 只是发起方是阿里云内网（`100.104.x.x`），不是操作者本机。这一点在第 12 章被实测验证。

---

## 3. 方案一：SSH 公钥授权

最早尝试的方案，作为对照保留。

### 3.1 生成密钥

```powershell
ssh-keygen -t ed25519 -N '""' -f "$env:USERPROFILE\.ssh\shopnest_ecs" -C "trae-agent@shopnest"
Get-Content "$env:USERPROFILE\.ssh\shopnest_ecs.pub"
```

生成的公钥（可公开）：

```
ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIAlUJwFmJQOvJ94WX/BRxuJHdY2HjrQQ1cwlpKE9nNvr trae-agent@shopnest
```

### 3.2 为什么没有优先采用

- 需要把公钥写进服务器 `/root/.ssh/authorized_keys`，但**写之前必须先有一次登录**，
  形成了"先有鸡还是先有蛋"的问题；
- 私钥文件丢失即失去入口，对使用者是额外的心理负担；
- 相比之下 Workbench CLI 用阿里云账号体系授权，天然不需要密钥分发。

> 该方案并未废弃。第 17 章把它列为"更省事替代方案"的备选，
> 适合在禁用密码登录时使用。

---

## 4. 方案二：Workbench CLI（最终采用）

### 4.1 安装（Windows）

官方命令：

```powershell
irm https://workbench-cli.oss-cn-hangzhou.aliyuncs.com/install.ps1 | iex
```

**直接执行会失败**：

```
New-Item : 对路径"workbench"的访问被拒绝。
```

原因：脚本把安装目录写死为 `$env:ProgramFiles\workbench`，即 `C:\Program Files`，需要管理员权限。

### 4.2 无管理员权限的安装法

脚本里唯一决定路径的是 `Join-Path $env:ProgramFiles "workbench"`，
所以只要在**当前会话**里改写这个环境变量即可：

```powershell
$env:ProgramFiles = Join-Path $env:LOCALAPPDATA 'Programs'
irm https://workbench-cli.oss-cn-hangzhou.aliyuncs.com/install.ps1 | iex
```

成功输出：

```
Installing workbench (windows/amd64)
  to:      C:\Users\<user>\AppData\Local\Programs\workbench\workbench.exe
  checksum OK
workbench v1.0.4 (commit: 693d621, built: 2026-10-13T06:49:24Z)
```

脚本会自动把安装目录写入**用户级 PATH**（注册表 `HKCU\Environment`），
新开终端生效；当前会话用 `$env:PATH` 临时追加。

### 4.3 验证

```powershell
$wb = "$env:LOCALAPPDATA\Programs\workbench\workbench.exe"
& $wb exec -i <实例ID> -r cn-beijing -c "hostname; uptime; id"
```

首次实测输出：

```
iZ2zeczdp8tjthshnkmv1rZ
 10:49:31 up 4 days, 14:22, 2 users, load average: 0.00, 0.00, 0.00
uid=0(root) gid=0(root) groups=0(root)
```

拿到的是 **root**，且无需任何 SSH 密码或密钥。

---

## 5. RAM 最小权限策略与授权踩坑

### 5.1 策略内容

不要用控制台默认模板（`Action: "*"` 全账号权限）。实际使用的最小策略：

```json
{
  "Version": "1",
  "Statement": [
    { "Effect": "Allow", "Action": ["ecs-workbench:*"], "Resource": "*" },
    { "Effect": "Allow",
      "Action": ["ecs:DescribeInstances", "ecs:DescribeCloudAssistantStatus", "ecs:StartTerminalSession"],
      "Resource": "*" },
    { "Effect": "Allow", "Action": "ram:CreateServiceLinkedRole", "Resource": "*",
      "Condition": { "StringEquals": { "ram:ServiceName": "workbench.ecs.aliyuncs.com" } } }
  ]
}
```

各 Action 的作用：

| Action | 用途 | 缺失后果 |
| :--- | :--- | :--- |
| `ecs-workbench:*` | 通道登录、执行命令、文件传输 | 完全无法连接 |
| `ecs:DescribeInstances` | `list ecs` 列出实例 | `Forbidden.RAM` 403 |
| `ecs:DescribeCloudAssistantStatus` | 检查云助手 Agent | 连接前置检查失败 |
| `ecs:StartTerminalSession` | 建立终端会话 | 无法进入会话 |
| `ram:CreateServiceLinkedRole` | 首次使用自动建服务关联角色 | 首次连接失败 |

### 5.2 授权链路

```
RAM 用户（workbench-agent）
   └─ 被授予 → 自定义策略 WorkbenchAgentPolicy
                    └─ 授权范围：整个云账号
```

### 5.3 踩坑：策略建好了但依然 403

**现象**：策略 `WorkbenchAgentPolicy` 内容完全正确，但 `list ecs` 仍返回：

```
StatusCode: 403
Code: Forbidden.RAM
"NoPermissionType":"ImplicitDeny",
"PolicyType":"AccountLevelIdentityBasedPolicy"
```

**原因**：策略只是"创建完成"，**没有绑定到 RAM 用户**。未绑定的策略是闲置对象。

**判定方法**：报错里的 `AuthPrincipalType: SubUser` 说明身份识别正常，
`ImplicitDeny` 说明是权限缺失而非 AK 错误 —— 二者要分清：

| 报错 | 含义 |
| :--- | :--- |
| `InvalidAccessKeyId` | AK 本身无效 |
| `ImplicitDeny` | AK 有效，但该用户没有此权限 |
| `SignatureDoesNotMatch` | Secret 填错 |

**解决**：RAM 控制台 → 用户 → 权限管理 → 新增授权 → 授权范围「整个云账号」 →
策略类型必须切到「**自定义策略**」→ 勾选 `WorkbenchAgentPolicy` → 确定。

> 特别注意：策略类型下拉默认是「系统策略」，选错会搜不到自定义策略。

### 5.4 踩坑：实例不在默认地域

授权通过后 `list ecs` 返回 `No instances found`，因为 CLI 默认地域是 `cn-hangzhou`。
实例实际在 `cn-beijing`：

```powershell
& $wb list ecs -r cn-beijing
# i-2zeczdp8tjthshnkmv1r  ecs.u2a-c1m2.large  Running  172.21.200.38  123.57.26.237
```

`exec` / `connect` 时 `-i i-2ze...` 前缀可自动推断地域，`list` 则必须显式 `-r`。

---

## 6. 凭证落地位置与权限

### 6.1 位置

`workbench` CLI 的官方凭证文件：

```
C:\Users\<user>\.workbench\config.json
```

内容：

```json
{
  "current": "default",
  "profiles": {
    "default": {
      "mode": "AK",
      "access_key_id": "LTAI5t******",
      "access_key_secret": "************"
    }
  }
}
```

> 本文档按要求记录全过程，但**不写入真实 AccessKey Secret 与管理员口令**。
> 它们是活的凭据，一旦进入仓库、文档或截图，等于永久泄露。
> 实际值请查阅本地凭证文件与密码管理器。

### 6.2 权限收紧

默认创建的文件可能继承父目录权限，需显式限制为仅当前用户：

```powershell
New-Item -ItemType Directory -Force -Path "$env:USERPROFILE\.workbench" | Out-Null
icacls "$env:USERPROFILE\.workbench" /inheritance:r /grant:r 'jasper:(OI)(CI)F'
icacls "$env:USERPROFILE\.workbench\config.json" /inheritance:r /grant:r 'jasper:F'
```

结果只剩一条 ACE：

```
config.json  DESKTOP-XXXX\jasper:(F)
```

### 6.3 为什么放在用户目录而不是项目目录

- 项目目录 `d:\JavaWeb` 会进 git，`.gitignore` 再全也可能被误提交；
- 用户目录天然在仓库之外，且 CLI 自动读取，不需要每次传参。

---

## 7. 首次只读体检结果

**体检原则：先只读，不改任何数据。**

| 检查项 | 结果 |
| :--- | :--- |
| 实例状态 | Running，已运行 4 天 14 小时，负载 0.00 |
| 容器 | 5 个全部 Up：nginx / backend / frontend / mysql(healthy) / redis(healthy) |
| 宿主机监听端口 | 仅 `0.0.0.0:80`（docker-proxy）；3306 / 6379 / 8080 均未映射到宿主机 |
| HTTP 响应 | `/` 200、`/api/portal/home` 200（`code=200`）、`/api/portal/products` 200（`code=200`） |
| 后端日志 | 最近 400 行无 error / exception |
| 网关日志 | 仅静态资源缓冲告警（`proxy_temp`），无害 |
| 磁盘 | 8.6G / 40G（23%） |
| 内存 | 1.2G / 3.5G，Swap 基本未用 |
| 公网扫描 | Censys 探测 `/phpinfo` 等路径返回 200（SPA 兜底），非漏洞 |

> **结论**：运行层面健康。真正的风险不在"跑得稳不稳"，而在"谁能进来"。

---

## 8. 高危发现：默认管理员口令仍然有效

### 8.1 判定方法（不尝试登录，纯读库比对）

种子文件 [`docs/sql/data.sql`](file:///D:/JavaWeb/docs/sql/data.sql) 里硬编码了管理员哈希。
把它与线上库的值逐字符比对：

```bash
docker exec shop-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N \
  -e "select username,password from shop_db.sys_user;"'
```

线上实际值：

```
admin     $2a$10$7W6pZavnkFW5AgosezBYnOx3i.15aTEg8o3jNELhKL/OC1oEbI.jK
customer  $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi
```

与 `data.sql` **完全一致**，说明默认口令从未被修改。

### 8.2 危害链

```
MySQL 首次初始化 → 自动执行 data.sql → 写入 admin/admin123
        ↓
攻击者 POST /api/auth/login 携带 admin/admin123
        ↓
拿到 ROLE_ADMIN 的 Sa-Token
        ↓
商品改价、订单导出、读取全部用户收件人手机号与地址
```

关键点：源码中**不存在**首次登录强制改密逻辑，漏洞仅在新数据卷首次初始化时生效，
因此"部署完没手动改密"就等于永久开门。

### 8.3 顺带确认的低风险项

- `customer` 演示号同样是种子口令，但角色为 `ROLE_CUSTOMER`，`/api/admin/**` 被 `ROLE_ADMIN` 拦截；
- 实测该账号名下 **0 条订单**、无独立地址表（收货地址是订单内嵌值对象），
  因此保留它 ≈ "公开密码的演示买家号"，风险可接受。

---

## 9. 处置一：修改管理员口令

### 9.1 原理

数据库里存的不是明文，而是 BCrypt 哈希（`$2a$10$...`，含随机盐）。
改密码 = **用新口令重新算出一串哈希并覆盖原值**。

因此功能上零变化：

- 不改账号本身（`id=1`、`ROLE_ADMIN`、`status=1`）
- 不改任何业务数据、不改代码
- 只是旧口令再也算不出匹配哈希

### 9.2 两种改法

**方式 A：调用认证修改接口**（本次采用，前端暂未暴露个人中心 UI，直接通过后端 `PUT /api/auth/profile` 接口完成）

```bash
TOKEN=$(curl -s -X POST http://127.0.0.1/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"<原口令>"}' \
  | grep -o '"tokenValue":"[^"]*"' | cut -d'"' -f4)

curl -s -X PUT http://127.0.0.1/api/auth/profile \
  -H "satoken: $TOKEN" -H 'Content-Type: application/json' \
  -d '{"oldPassword":"<原口令>","newPassword":"<新口令>"}'
```

实现细节（见 [`AuthServiceImpl.java`](file:///D:/JavaWeb/backend/src/main/java/com/shop/modules/auth/service/impl/AuthServiceImpl.java)）：

- 新口令长度必须 6–32 位；
- 必须先校验 `oldPassword`，防止 token 被盗后直接改密；
- `nickname` 为空时不覆盖，所以**只传两个密码字段是安全的**。

**方式 B：不经过登录，直接改库**
借前台注册接口生成哈希，再覆盖 admin（适用于怀疑口令已被他人改掉的情况）：

```bash
# 1) 用目标口令注册临时账号，让服务端帮你算 BCrypt
curl -s -X POST http://127.0.0.1/api/auth/register -H 'Content-Type: application/json' \
  -d '{"username":"tmp_pwgen9","password":"<新口令>","nickname":"tmp"}'

# 2) 取出哈希
docker exec shop-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N \
  -e "select password from shop_db.sys_user where username=\"tmp_pwgen9\";"'

# 3) 覆盖 admin，并删掉临时账号
```

### 9.3 验证矩阵（必须逐项过）

| 验证项 | 期望结果 | 实测 |
| :--- | :--- | :--- |
| 新口令登录 | 200 登录成功 | ✅ |
| 旧口令登录 | 401 用户名或密码错误 | ✅ |
| 库内哈希前缀 | 由 `7W6pZavn...` 变为新值 | ✅ `6VwqHXp4...` |
| 昵称/头像/角色 | 保持不变 | ✅ `系统管理员` / `ROLE_ADMIN` |

> **必须验证"旧口令被拒"**。只验证"新口令能进"是不够的——
> 如果覆盖失败但旧口令仍有效，你会误以为已经修好了。

---

## 10. 处置二：失效存量登录会话

### 10.1 为什么改密还不够

项目使用 Sa-Token 的 `StpLogicJwtForSimple`（见 [`SaTokenConfig.java`](file:///D:/JavaWeb/backend/src/main/java/com/shop/config/SaTokenConfig.java)）：

- token 本身是标准 JWT；
- 但**服务端仍在 Redis 保存会话**，`is-share: true`、`timeout: 604800`（7 天）。

改密码**不会**撤销已签发的 token。攻击者若已偷到 token，改密后 7 天内仍可继续使用。

### 10.2 清理命令

```bash
# 清理前计数
docker exec shop-redis sh -c \
  'redis-cli -a "$REDIS_PASSWORD" --scan --pattern "satoken*" | wc -l'

# 一次性删除全部会话
docker exec shop-redis sh -c \
  'redis-cli -a "$REDIS_PASSWORD" --scan --pattern "satoken*" \
   | xargs -r redis-cli -a "$REDIS_PASSWORD" del'
```

实测：`5 keys → 0 keys`，随后新口令重新登录返回 200，站点健康检查 200。

> 口令由容器自身环境变量 `REDIS_PASSWORD` 提供，命令里不出现明文，
> 也避免密码落进 shell 历史。

### 10.3 副作用

所有已登录用户（含自己浏览器）掉线一次，下次请求 401 → 前端拦截器自动跳登录页。

---

## 11. 处置三：根治仓库种子账号

### 11.1 只改线上库不够

线上改密只解决"这一台"。只要代码里还挂着种子数据，
**换机器、重装、给别人演示部署流程**都会把默认口令重新种进去。

### 11.2 关键取舍：开发保留，生产切断

最初方案是直接删掉 `data.sql` 里的用户行，但会导致本地开发没有 admin 可登录。
最终采用更准确的做法：

| 环境 | 行为 | 理由 |
| :--- | :--- | :--- |
| 本地直连 MySQL 开发 | 保留 `docs/sql/data.sql` 供开发者手动导入 | 仅在本机 `localhost:3306` 运行，提供开箱即用的商品与测试账号 |
| 容器化编排（`deploy/docker-compose.prod.yml`、`deploy/trial/docker-compose.yml` 及根平铺副本） | **统一不自动挂载** `data.sql` | 面向公网或云主机编排，彻底切断重装时自动植入默认凭据的通道 |

### 11.3 实际改动清单

| 文件 | 改动 |
| :--- | :--- |
| [`deploy/docker-compose.prod.yml`](file:///D:/JavaWeb/deploy/docker-compose.prod.yml) | 删除 `data.sql` 挂载行，改为说明注释 |
| [`deploy/trial/docker-compose.yml`](file:///D:/JavaWeb/deploy/trial/docker-compose.yml)（及根 [`docker-compose.yml`](file:///D:/JavaWeb/docker-compose.yml)） | 同步删除 `data.sql` 挂载行，仅保留 `schema.sql` |
| [`docs/sql/data.sql`](file:///D:/JavaWeb/docs/sql/data.sql) | 文件头声明"仅开发/演示使用"；删除 `Password for admin: admin123` 明文提示 |
| [`README.md`](file:///D:/JavaWeb/README.md) | 后台入口改为"生产不预置账号"；新增「管理员账号初始化」章节 |
| [`docs/sql/README.md`](file:///D:/JavaWeb/docs/sql/README.md) | 5.2 节同步为容器编排统一只挂 `schema.sql` |
| 服务器 `/opt/shop/docker-compose.yml` | 同步删除挂载行，原文件备份为 `docker-compose.yml.bak.20261117` |

### 11.4 生产环境的账号初始化方式

生产首次部署后数据库**只有表结构、没有任何账号**，需手动创建管理员：

```sql
-- 方式一：先注册普通账号，再提权
update sys_user set role = 'ROLE_ADMIN' where username = '你的账号';

-- 方式二：直接插入管理员（password 填 BCrypt 哈希）
insert into sys_user (username, password, nickname, avatar, role, status)
values ('admin', '$2a$10$...', '系统管理员', '/uploads/avatar-admin.jpg', 'ROLE_ADMIN', 1);
```

### 11.5 注意：initdb 只执行一次

MySQL 官方镜像的 `docker-entrypoint-initdb.d` **仅在数据目录为空时执行**。
所以改 SQL 文件不会影响已有库，必须配合第 9、10 章的手工处置。

---

## 12. 处置四：收敛公网 SSH 暴露面

### 12.1 处置前的入方向规则

| 优先级 | 策略 | 端口 | 授权对象 |
| :--- | :--- | :--- | :--- |
| 100 | 拒绝 | RDP 3389 | `0.0.0.0/0` |
| 100 | 允许 | HTTP 80 | `0.0.0.0/0` |
| 100 | 允许 | ICMP 全部 | `0.0.0.0/0` |
| 100 | 允许 | SSH 22 | `0.0.0.0/0` ← 风险点 |

### 12.2 服务器侧的真实配置

```bash
grep -Ei '^(PermitRootLogin|PasswordAuthentication)' /etc/ssh/sshd_config
```

```
PermitRootLogin yes
PasswordAuthentication yes
```

叠加结果：**全网可对 root 尝试密码，且未安装 fail2ban**。
历史失败登录统计仅 1 次（来源 `47.113.193.48`），尚未成为目标，但属于被动防守。

### 12.3 关键实测：Workbench 到底走哪条路

在服务器上查看登录来源：

```bash
who
# root  pts/0  2026-11-17 11:21  (100.104.192.155)

grep "Accepted" /var/log/secure | grep -oE 'from [0-9.]+' | sort | uniq -c | sort -rn | head
# 17 from 100.104.192.139
# 10 from 100.104.192.166
#  9 from 100.104.93.27
```

**所有来源都落在 `100.104.0.0/16`，没有一条来自本机公网 IP。**

链路拆解：

```
本机 PowerShell（执行 workbench.exe）
        ↓ HTTPS
阿里云 Workbench 服务（100.104.x.x 内网）
        ↓ SSH
实例 172.21.200.38:22
```

### 12.4 执行步骤（带基线与回滚）

| 步骤 | 动作 | 实测 |
| :--- | :--- | :--- |
| 1 | 新增规则：允许 TCP 22 ← `100.104.0.0/16`（优先级 1，描述「阿里云 Workbench 内网通道」） | 保存成功 |
| 2 | 记录基线：`workbench exec` 执行 `date` | `baseline_ok=11:28:13` |
| 3 | 删除 `0.0.0.0/0` 的 SSH 允许规则 | 已删除 |
| 4 | 复测 `workbench exec` | `retest_ok=11:28:56`，来源 `100.104.93.27` |
| 5 | 从本机测公网端口 | `port22_reachable=False`、`port80_reachable=True` |
| 6 | 站点健康检查 | `/api/portal/home` `code=200`、`/api/portal/products` `code=200`、5 容器 Up |

> **必须用"从本机测公网 22 端口的 TCP 握手"来验证**。
> 只看"我还能连上"是不够的——那可能只是内网通道生效，不能证明公网已关闭。

### 12.5 收敛后的最终规则

```
入方向：
  拒绝  3389  ← 0.0.0.0/0
  允许  80    ← 0.0.0.0/0      （Cloudflare 回源 + IP 直连）
  允许  ICMP  ← 0.0.0.0/0
  允许  22    ← 100.104.0.0/16 （仅阿里云内网）
```

效果：公网对 22 端口**连 TCP 握手都建立不了**，暴力破解链路彻底断开。

### 12.6 附带发现：CLI 并没有自动加规则

官方文档称"首次连接时 CLI 会自动添加内网安全组规则"，
但实际安全组里**没有** `100.104.0.0/16` 这条，而 Workbench 依然能连。

推测原因：22 端口当时已对 `0.0.0.0/0` 开放，全网规则覆盖了内网来源，CLI 判定无需新增。
这也说明**不能假定工具"帮你做了"，必须自己核对规则列表**。

---

## 13. 100.104.0.0/16 的来源、观测与变更预案

### 13.1 来源

1. 官方文档《通过 Workbench CLI 连接实例》「使用限制 → 安全组规则」：
   > 首次连接时 CLI 会自动为实例添加内网安全组规则（TCP 22 端口，授权 `100.104.0.0/16`）
2. 实测 `Accepted` 日志，来源分布于 `100.104.93.x` 与 `100.104.192.x`，被 `/16` 完整覆盖。

注意：该网段**不是**你的 VPC 网段（实例私网为 `172.21.200.38`），
因此安全组下拉里的「本 VPC 网段 `172.16.0.0/12`」**不能替代**这条规则。

### 13.2 会不会变

属于阿里云服务侧保留地址段，官方极少调整，但无法承诺永不变更。
它不会"悄悄失效到无声无息"——变化会表现为：**Workbench 连不上，但网站 80 正常**。

### 13.3 如何自行观测

```bash
grep "Accepted" /var/log/secure | grep -oE 'from [0-9.]+' | sort | uniq -c | sort -rn | head
ss -tn state established '( sport = :22 )' | awk 'NR>1{print $4}' | sort -u
who
```

输出中的 IP 前缀即当前生效网段。

### 13.4 变更预案

| 步骤 | 操作 |
| :--- | :--- |
| 1 | 控制台 → 实例 → 「发送命令」（Cloud Assistant，不走 SSH） |
| 2 | 在云端执行上面的 `grep Accepted`，确认新来源网段 |
| 3 | 安全组加入新网段；或临时加回 `0.0.0.0/0` |
| 4 | 保底入口：控制台「远程连接 → VNC」，绕过网络层，任何情况都能进 |

---

## 14. 演示与日常登录说明

### 14.1 后台登录

| 项 | 值 |
| :--- | :--- |
| 地址 | `https://shopnest.dpdns.org/admin` |
| 用户名 | `admin` |
| 口令 | 已改为独立强口令（不在本文记录） |

改密后的体感变化：

- 浏览器里旧的登录态会因 Redis 会话被清而失效，刷新需重新登录；
- 后台菜单、权限、数据全部不变；
- 前端把 token 存 `localStorage`，401 时拦截器自动跳登录页，不会白屏。

### 14.2 演示分工

| 场景 | 账号 | 说明 |
| :--- | :--- | :--- |
| 后台演示 | `admin` | 看板、商品、品牌、分类、订单、用户管理 |
| 前台演示 | `customer` 或新注册买家 | `customer` 仍启用，名下 0 订单、无地址数据 |

> 演示时若用 `customer` 下单，**不要填真实手机号与地址**——
> 该账号密码是公开的，填写的收货信息等同于公开。

### 14.3 后续强化建议（可选）

- 演示结束后把管理员口令换成随机串（`openssl rand -base64 18`），交密码管理器保存；
- 登录页不要再出现"演示账号"提示文案；
- 若后续要禁用密码登录，先确认 SSH 公钥已可用，避免锁死。

---

## 15. 踩坑实录

### 坑 1：`irm ... | iex` 安装被拒绝

**现象**：`New-Item : 对路径"workbench"的访问被拒绝`。
**原因**：脚本写死 `$env:ProgramFiles`，需要管理员权限。
**解决**：会话内改写 `$env:ProgramFiles` 到 `$env:LOCALAPPDATA\Programs`，走用户目录安装。

### 坑 2：`workbench config` 用管道喂输入会失败

**现象**：`"1\nLTAI...\nsecret\n" | workbench config` 输出 `Error: The handle is invalid.`
**原因**：Secret 输入走隐藏输入（需要真实 TTY）。
**解决**：要么在真实终端手输；要么直接写 `~/.workbench/config.json`（结构见第 6 章）。

### 坑 3：`icacls` 参数被 PowerShell 拆开

**现象**：`Invalid parameter "(OI)(CI)F"`。
**原因**：双引号内 `$env:USERNAME:(OI)(CI)F` 被解析成带盘符的变量。
**解决**：用单引号写完整主体：`'jasper:(OI)(CI)F'`。

### 坑 4：`workbench exec -c` 传多行脚本被解析报错

**现象**：`ParserError: UnexpectedToken`，远端什么都没执行。
**原因**：多行 bash 经 PowerShell 参数传递时引号与换行被吃掉。
**解决**：本地把脚本 base64 编码，远端解码执行，彻底规避引号问题：

```powershell
$b64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($script))
& $wb exec -i <id> -r cn-beijing -c "echo $b64 | base64 -d | bash"
```

### 坑 5：策略建好但仍 `Forbidden.RAM`

**原因**：策略未绑定到用户，而非策略内容错误。
**解决**：用户 → 权限管理 → 新增授权 → 授权范围「整个云账号」→ **自定义策略** → 勾选。

### 坑 6：以为改了密码就安全了

**原因**：Sa-Token 会话存在 Redis，改密不撤销已签发 token。
**解决**：额外清理 `satoken*` 键，并验证旧 token 失效。

### 坑 7：以为 CLI 会自动配安全组

**原因**：文档的自动化行为在"22 已对全网开放"的前提下不会触发。
**解决**：任何"工具应该帮我做了"的事，都要去控制台核对实际规则。

### 坑 8：只验证"新口令能登录"

**原因**：覆盖失败时旧口令仍然有效，只看单向验证会误判。
**解决**：必须同时验证"旧口令被拒（401）"与"库内哈希已变"。

---

## 16. 命令速查表

### 16.1 本地（Windows PowerShell）

```powershell
# 列出实例（注意指定地域）
& "$env:LOCALAPPDATA\Programs\workbench\workbench.exe" list ecs -r cn-beijing

# 远程执行单条命令
& "$env:LOCALAPPDATA\Programs\workbench\workbench.exe" exec -i i-2zeczdp8tjthshnkmv1r -r cn-beijing -c "docker ps"

# 远程执行多行脚本（base64 规避引号）
$b64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($script))
& "$env:LOCALAPPDATA\Programs\workbench\workbench.exe" exec -i i-2zeczdp8tjthshnkmv1r -r cn-beijing -c "echo $b64 | base64 -d | bash"

# 上传 / 下载
& "$env:LOCALAPPDATA\Programs\workbench\workbench.exe" upload -i i-2zeczdp8tjthshnkmv1r -r cn-beijing <本地> <远端>
& "$env:LOCALAPPDATA\Programs\workbench\workbench.exe" download -i i-2zeczdp8tjthshnkmv1r -r cn-beijing <远端> <本地>

# 测试公网端口是否可达
Test-NetConnection -ComputerName 123.57.26.237 -Port 22 -InformationLevel Quiet
```

### 16.2 服务器侧

```bash
# 容器 / 端口
docker ps -a --format '{{.Names}}|{{.Status}}|{{.Ports}}'
ss -lntp | grep -E ':(80|22)\b'

# 后端业务接口健康检查（校验返回 JSON 包含 "code":200，避免 SPA 兜底或异常包装假阳性）
curl -s http://127.0.0.1/api/portal/home | grep -o '"code":200'

# 登录来源（判断网段是否变化）
who
grep "Accepted" /var/log/secure | grep -oE 'from [0-9.]+' | sort | uniq -c | sort -rn | head

# 清理 Sa-Token 会话
docker exec shop-redis sh -c 'redis-cli -a "$REDIS_PASSWORD" --scan --pattern "satoken*" \
  | xargs -r redis-cli -a "$REDIS_PASSWORD" del'

# 查用户与哈希
docker exec shop-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N \
  -e "select username,left(password,20),role,status from shop_db.sys_user;"'
```

---

## 17. 遗留事项

| 事项 | 状态 | 说明 |
| :--- | :--- | :--- |
| 管理员口令 | ✅ 已处置 | 旧口令返回 401，哈希已变 |
| 存量会话 | ✅ 已清理 | Redis `satoken*` 5 → 0 |
| 生产与试用编排种子账号 | ✅ 已切断 | `docker-compose.prod.yml` 与 `deploy/trial/docker-compose.yml` 均不再自动挂载 `data.sql` |
| 公网 SSH | ✅ 已收敛 | 仅 `100.104.0.0/16` 可达 |
| `customer` 演示号 | ⏸ 保留 | 0 订单、无地址数据，风险等同于公开演示号 |
| SSH 密码登录 | ⏸ 保留 | 22 已不对公网开放，意义有限 |
| 调试用 RAM AK | ⚠ 待回收 | 调试期保留；结束后应禁用并重建 |
| `jwt-secret-key` 硬编码 | ⚠ 待处理 | 生产未覆盖默认值，建议改为环境变量注入 |
| 80 端口来源 | ⚠ 可优化 | 现对全网开放，可收敛至 Cloudflare IP 段 |

### 17.1 更省事的替代方案（供后续选择）

若以后希望彻底不依赖"内网网段白名单"，可改为**密钥登录**：

1. 把公钥写入服务器 `/root/.ssh/authorized_keys`（`chmod 700/600`）；
2. 本机验证 `ssh -i <私钥> root@<IP>` 能登录；
3. 确认可用后，再改 `sshd_config`：`PasswordAuthentication no`、`PermitRootLogin prohibit-password`；
4. `sshd -t` 校验语法后 `systemctl reload sshd`。

优点：不怕 IP 变化、不怕爆破；缺点：私钥丢失需 VNC 控制台救场。
**顺序不能颠倒**——必须先验证密钥可用，再关密码。

---

## 附录：本次处置后的完整链路图

```
浏览器用户
   │ HTTPS 443
   ▼
Cloudflare（边缘证书）
   │ HTTP 80
   ▼
┌──────────────── 阿里云 ECS 123.57.26.237 ────────────────┐
│  安全组入方向：80(全网) / ICMP(全网) / 22(仅 100.104/16) │
│  ┌──────── shop-nginx :80 ────────┐                      │
│  │  /        → frontend 容器 :80  │                      │
│  │  /api/**  → backend  容器 :8080│                      │
│  └────────────────────────────────┘                      │
│        │                    │                            │
│   shop-mysql:3306     shop-redis:6379                    │
│   （均未映射宿主机）   （Sa-Token 会话）                  │
└──────────────────────────────────────────────────────────┘
              ▲
              │ SSH（源 100.104.x.x）
       阿里云 Workbench 服务
              ▲
              │ HTTPS（AK 授权）
       本机 workbench.exe / 控制台
```

> 运维通道与用户访问完全分离：用户走 443/80，运维走内网 SSH，
> 两者互不影响；任一通道故障时，都还有 VNC 控制台作为最后兜底。
