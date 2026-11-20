# 10 - 云端试用环境部署实战教程（阿里云 ECS 单机容器化）

> 本文档记录本项目从**本地开发环境**到**公网可访问**的完整部署过程，包含每一步的实际命令、预期输出、踩过的坑与解决方案。
> 全部内容均为实操验证过的版本，可直接照着复制执行。

---

## 目录

1. [部署目标与最终结果](#1-部署目标与最终结果)
2. [资源清单与环境参数](#2-资源清单与环境参数)
3. [整体架构与关键决策](#3-整体架构与关键决策)
4. [第一步：本地构建与打包](#4-第一步本地构建与打包)
5. [第二步：上传部署包到服务器](#5-第二步上传部署包到服务器)
6. [第三步：服务器初始化](#6-第三步服务器初始化)
7. [第四步：启动容器](#7-第四步启动容器)
8. [第五步：验证部署结果](#8-第五步验证部署结果)
9. [第六步：安全组与域名配置](#9-第六步安全组与域名配置)
10. [第七步：HTTPS 证书与 Cloudflare 接入](#10-第七步https-证书与-cloudflare-接入)
11. [踩坑实录（问题 → 原因 → 解决）](#11-踩坑实录问题--原因--解决)
12. [日常运维命令](#12-日常运维命令)
13. [代码更新与重新部署](#13-代码更新与重新部署)
14. [流量、费用与到期说明](#14-流量费用与到期说明)
15. [附录 A：docker-compose.yml 全文](#15-附录-adocker-composeyml-全文)
16. [附录 B：Nginx 配置全文](#16-附录-bnginx-配置全文)
17. [附录 C：命令速查表](#17-附录-c命令速查表)

---

## 1. 部署目标与最终结果

把本地开发的前后端分离电商系统（Spring Boot 3 + Vue 3 + MySQL + Redis）部署到一台公网云服务器，实现：

- 任何人通过浏览器访问商城前台与后台管理
- 完整的下单、结算、后台发货闭环可用
- 图片上传功能正常（写入服务器磁盘并可通过 URL 访问）

### 最终可访问地址

| 访问方式 | 地址 | 说明 |
| :--- | :--- | :--- |
| **HTTPS 域名（唯一入口）** | `https://shopnest.dpdns.org` | Cloudflare 边缘证书，浏览器显示锁标，推荐对外展示 |
| **IP 直连** | `http://123.57.26.237` | 80 端口，最稳，答辩首选（明文 HTTP） |

> **实测状态（2026-11-18）**：`https://shopnest.dpdns.org`（经 Cloudflare 回源）与 `/api/portal/home`、`/api/portal/products` 均返回 HTTP 200（`code=200`）且证书校验通过；
> `http://123.57.26.237` 同样返回 200。
>
> **端口收敛（2026-11-17 加固后）**：`8080`、`8443` 两条备用入口已下线，公网 SSH `22` 亦收敛至仅阿里云 Workbench 内网段 `100.104.0.0/16`；
> 网关容器对外只映射 `80`，源站不再加载 TLS 证书。历史过程保留在第 10 章，仅供理解原理。
> 若浏览器地址栏仍显示「不安全」，说明访问的是 `http://` 或带非 443 端口的地址，改用
> `https://shopnest.dpdns.org`（不带端口）即可。

### 账号与种子数据说明

| 角色 | 用户名 | 本地种子初始口令 | 入口 |
| :--- | :--- | :--- | :--- |
| 系统管理员 | `admin` | 线上已修改为独立强口令（`data.sql` 初始值为 `admin123`） | `/admin` |
| 演示顾客 | `customer` | `123456` | `/` |

> **安全提示（2026-11-17 加固）**：当前 `deploy/trial/docker-compose.yml` 已默认**不自动挂载 `data.sql`**，防止重装时把公开默认口令自动写入公网库。新机器首次部署若需演示商品数据，可在启动后手动导入 `data.sql` 并立即通过 `PUT /api/auth/profile` 接口或数据库 SQL 修改 `admin` 密码（详见 [11 远程运维与凭据加固](file:///D:/JavaWeb/docs/principles/11-remote-ops-and-credential-hardening.md) 第 9 节）。

---

## 2. 资源清单与环境参数

### 2.1 云服务器（阿里云 ECS 短期试用实例）

| 项目 | 值 |
| :--- | :--- |
| 实例规格 | `ecs.u2a-c1m2.large`（2 vCPU / 4 GiB） |
| 操作系统 | Alibaba Cloud Linux 3.2104 LTS 64 位 |
| 系统盘 | ESSD Entry 40 GiB |
| 地域 / 可用区 | 华北2（北京）/ 随机分配 |
| 公网 IP | `123.57.26.237` |
| 私网 IP | `172.21.200.38`（**外网不可达，配置里别填**） |
| 带宽计费 | 按使用流量，峰值 2 Mbps，¥0.80/GB |
| 到期时间 | 2026-12-12 23:59:59（到期自动释放，数据清空） |

### 2.2 域名（免费二级域名 + Cloudflare 托管）

| 项目 | 值 |
| :--- | :--- |
| 域名 | `shopnest.dpdns.org` |
| 来源 | DigitalPlat FreeDomain（免费，需注册审核） |
| 委派模式 | **Cloudflare 托管**（NS 已从 `dns1/dns2.digitalplat.org` 改为 `bowen.ns.cloudflare.com` / `priscilla.ns.cloudflare.com`） |
| DNS 记录 | `A @ → 123.57.26.237`（橙色云代理）、`A www → 123.57.26.237`（橙色云代理） |
| SSL 模式 | Cloudflare「灵活 / Flexible」，回源为 HTTP 80 |
| 证书 | 仅 Cloudflare 边缘通用证书（自动续期）；源站已不部署证书 |

> 域名后缀 `dpdns.org` 属于公共后缀列表（PSL），Cloudflare 可以把它当作独立站点托管。
> 历史上用于 Let's Encrypt DNS-01 验证的 `TXT _acme-challenge` 记录，在 2026-11-16 证书体系停用后已可一并删除。

### 2.3 软件版本（安装后实际版本）

| 组件 | 版本 |
| :--- | :--- |
| Docker Engine | 26.1.3 |
| Docker Compose | v2.27.0（插件版，命令为 `docker compose`，无连字符） |
| MySQL 镜像 | `mysql:8.4` |
| Redis 镜像 | `redis:7.2-alpine` |
| Nginx 镜像 | `nginx:1.27-alpine` |
| JRE 镜像 | `eclipse-temurin:21-jre` |

---

## 3. 整体架构与关键决策

### 3.1 容器拓扑（5 个容器，仅 1 个对外端口）

```
                   浏览器 https://shopnest.dpdns.org
                              │
                     ┌────────▼────────┐
                     │   Cloudflare    │  边缘证书 + 代理（橙色云）
                     └────────┬────────┘
                              │  回源 HTTP 80（Flexible）
                              ▼
                     公网
                       │
              ┌────────┴────────┐
              │  shop-nginx     │  80  ← 唯一对外暴露
              │  (边缘网关)      │
              └───┬─────────┬───┘
                  │         │
       ┌──────────┘         └──────────┐
       ▼                               ▼
 shop-frontend (Nginx 静态)      shop-backend (Spring Boot)
       挂载 ./dist                      挂载 ./app/app.jar
                                          │
                                  ┌───────┴────────┐
                                  ▼                ▼
                            shop-mysql         shop-redis
                            (内网 3306)        (内网 6379)
```

端口职责：

| 端口 | 用途 | 现状 |
| :--- | :--- | :--- |
| `80` | 公网 IP 直连；Cloudflare 回源端口 | 正常，唯一对外链路 |
| ~~`8080`~~ | 曾用于域名 HTTP 直连（备用） | 2026-11-16 下线，安全组与端口映射均已删除 |
| ~~`8443`~~ | 曾用于源站 HTTPS（Let's Encrypt 证书） | 2026-11-16 下线；公网本就受 SNI 拦截，仅作过自测 |

### 3.2 关键决策：为什么在本地构建，不在服务器上构建

原生产方案（`deploy/docker-compose.prod.yml`）使用多阶段构建，服务器上跑 Maven + Node 编译。这台试用机只有 **2 核 4G 且无 swap**，编译 Spring Boot 项目耗时十几分钟并且极易 OOM 被杀。

因此改为：

```
本地 mvn package / npm run build  →  只上传产物（jar + dist）
服务器 pull 现成镜像              →  容器直接运行挂载的产物
```

好处：服务器只做"运行"，不做"编译"，启动时间从十几分钟降到 5 秒。

### 3.3 与生产编排的差异

| 项目 | 生产编排 `deploy/docker-compose.prod.yml` | 试用编排 `deploy/trial/docker-compose.yml` |
| :--- | :--- | :--- |
| HTTPS | 80 → 443 强制跳转，需证书 | Cloudflare 边缘 HTTPS 终结 + 回源 HTTP 80，源站无证书 |
| 对外端口 | 80 / 443 | 仅 80 |
| 镜像来源 | 服务器多阶段构建 | 本地构建产物 + 官方镜像 |
| 内存控制 | 无限制 | 每容器 `mem_limit`，JVM 限 768m |
| 数据卷 | 命名卷 | 本地目录绑定（方便查看和替换） |
| 限流 | `limit_req` 防爆破 | 未启用（简化） |

---

## 4. 第一步：本地构建与打包

### 4.1 环境要求（本地 Windows）

- JDK 21 或 25（实测 JDK 25 构建、JDK 21 运行正常）
- Maven 3.9+
- Node.js 18+ / npm

### 4.2 构建后端

```powershell
cd d:\JavaWeb\backend
mvn -B clean package -DskipTests
```

产物：

```
d:\JavaWeb\backend\target\shop-backend-1.0.0-SNAPSHOT.jar   （约 44.5 MB）
```

### 4.3 构建前端

```powershell
cd d:\JavaWeb\frontend
npm run build
```

产物：

```
d:\JavaWeb\frontend\dist\
├── index.html
├── assets/index-xxxx.css
├── assets/index-xxxx.js
├── brand/logo-mark.png
├── favicon.ico
└── favicon.svg
```

> 构建时 Vite 会提示 `Some chunks are larger than 500 kB`，属于正常警告，不影响运行。

### 4.4 打包成 zip

在 PowerShell 中执行（把产物汇总到临时目录并压缩）：

```powershell
$stage = Join-Path $env:TEMP "shop-trial"
Remove-Item $stage -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path "$stage\app","$stage\nginx","$stage\sql","$stage\uploads","$stage\data","$stage\dist" | Out-Null

Copy-Item -LiteralPath "d:\JavaWeb\deploy\trial\docker-compose.yml" -Destination "$stage\docker-compose.yml"
Copy-Item -LiteralPath "d:\JavaWeb\deploy\trial\.env.example"        -Destination "$stage\.env.example"
Copy-Item "d:\JavaWeb\deploy\trial\nginx\shop.conf"     -Destination "$stage\nginx\shop.conf"
Copy-Item "d:\JavaWeb\deploy\trial\nginx\frontend.conf" -Destination "$stage\nginx\frontend.conf"
Copy-Item "d:\JavaWeb\backend\target\shop-backend-1.0.0-SNAPSHOT.jar" -Destination "$stage\app\app.jar"
Copy-Item "d:\JavaWeb\frontend\dist\*" -Destination "$stage\dist\" -Recurse -Force
Copy-Item "d:\JavaWeb\docs\sql\schema.sql" -Destination "$stage\sql\schema.sql"
Copy-Item "d:\JavaWeb\docs\sql\data.sql"   -Destination "$stage\sql\data.sql"
Copy-Item "d:\JavaWeb\backend\uploads\*"   -Destination "$stage\uploads\" -Recurse -Force

# 统一换行符为 LF，避免 shell 脚本在 Linux 上报错
$sh = [IO.File]::ReadAllText("d:\JavaWeb\deploy\trial\setup.sh") -replace "`r`n","`n"
[IO.File]::WriteAllText("$stage\setup.sh", $sh, (New-Object System.Text.UTF8Encoding($false)))

$zip = Join-Path $env:TEMP "shop-trial.zip"
Remove-Item $zip -Force -ErrorAction SilentlyContinue
Compress-Archive -Path "$stage\*" -DestinationPath $zip -CompressionLevel Optimal
Get-Item $zip | Select-Object FullName, Length
```

产物：`C:\Users\jasper\AppData\Local\Temp\shop-trial.zip`（约 42.5 MB）

> **注意**：`Compress-Archive` 打出来的 zip 使用反斜杠作为路径分隔符，Linux 解压时会提示
> `warning: appears to use backslashes as path separators`，**可以忽略**，解压结果正常。

### 4.5 部署包目录结构

```
shop-trial.zip
├── docker-compose.yml     # 容器编排
├── .env.example           # 密码模板（可选）
├── setup.sh               # 服务器初始化脚本
├── app/app.jar            # ★ 本地构建的后端产物
├── dist/                  # ★ 本地构建的前端产物
├── nginx/shop.conf        # 边缘网关配置（仅监听 80）
├── nginx/frontend.conf    # 前端 SPA 静态站点配置
├── sql/schema.sql         # 建表脚本（容器首次启动自动执行）
├── sql/data.sql           # 可选演示种子数据（默认不自动挂载，按需手动导入并立即改密）
└── uploads/               # 预置商品/品牌/头像图片
```

---

## 5. 第二步：上传部署包到服务器

### 5.1 方式一：Workbench 网页上传（推荐，零安装）

1. 阿里云控制台 → **云服务器 ECS** → **实例** → 实例右侧 **远程连接** → **Workbench**
2. 连接方式选 **免密连接**（云助手代登录，无需密码），点 **登录**
3. 登录后，左侧是 **文件管理** 面板
4. 双击进入目标目录（建议 `/root`），点 **上传** 按钮，或用鼠标把 zip **拖拽** 到面板里
5. 等待进度 100%

> **坑 1**：上传位置是"左侧文件管理面板当前所在的目录"，不是固定 `/root`。
> 如果面板停在 `/`，文件就会落到 `/shop-trial.zip`。用 `ls -lh /root` 找不到时，先执行
> `find / -maxdepth 4 -name "shop-trial.zip" 2>/dev/null` 定位实际路径。
>
> **坑 2**：不要**双击**文件，双击会触发"下载/打开"动作。

### 5.2 方式二：scp 命令行（需密码或密钥登录正常）

```powershell
scp "$env:TEMP\shop-trial.zip" root@123.57.26.237:/root/
```

### 5.3 方式三：WinSCP 图形客户端

主机 `123.57.26.237`，用户 `root`，密码登录后直接拖拽文件到 `/root`。

### 5.4 解压

```bash
cd /root
dnf install -y unzip
mkdir -p /opt/shop
unzip -o shop-trial.zip -d /opt/shop
ls /opt/shop
```

期望输出：

```
app  data  dist  nginx  sql  uploads  docker-compose.yml  setup.sh
```

---

## 6. 第三步：服务器初始化

### 6.1 获取镜像加速地址

国内直连 Docker Hub 拉镜像会超时，必须配置加速器。

- 阿里云」**容器镜像服务 ACR** → 左侧「镜像工具」→「镜像加速器」→ 复制形如 `https://xxxxxx.mirror.aliyuncs.com` 的地址。
- 如果该地址拉取报 `manifest unknown`（见踩坑第 5 条），改用公共镜像源。

### 6.2 执行初始化脚本

```bash
cd /opt/shop
bash setup.sh https://你的加速地址.mirror.aliyuncs.com
```

脚本依次完成 5 件事：

| 步骤 | 内容 |
| :--- | :--- |
| 1/5 | 安装 `dnf-plugins-core`、`device-mapper-persistent-data`、`lvm2` |
| 2/5 | 添加阿里云 Docker CE yum 源（`mirrors.aliyun.com/docker-ce`） |
| 3/5 | 安装 `docker-ce`、`docker-ce-cli`、`containerd.io`、`docker-compose-plugin` |
| 4/5 | 写 `/etc/docker/daemon.json`（镜像加速 + 日志轮转 10MB×3），启动并启用 Docker |
| 5/5 | 创建 2G swap、`vm.swappiness=10`（4G 内存机器必备） |

### 6.3 期望输出

```
===== 环境就绪 =====
Docker version 26.1.3, build b72abbb
Docker Compose version v2.27.0
              total        used        free      shared  buff/cache   available
Mem:          3.5Gi       515Mi       125Mi       1.0Mi       3.1Gi       3.0Gi
Swap:         2.0Gi       0.0Ki       2.0Gi
Filesystem      Size  Used Avail Use% Mounted on
/dev/nvme0n1p3   40G  7.1G   31G  19% /
```

### 6.4 验证镜像加速已生效

```bash
docker info | grep -A 3 "Registry Mirrors"
```

> **坑 3**：Workbench 终端会把 URL 自动加上反引号显示（如 `` `https://xxx` ``），
> 这是**显示效果**，不代表文件里有反引号。要确认真实内容用：
> `od -c /etc/docker/daemon.json | head -5`，看字节里是否真存在 `` ` ``。

### 6.5 预拉镜像（可选，推荐后台执行）

```bash
cd /opt/shop
nohup docker compose pull > /tmp/pull.log 2>&1 &
tail -f /tmp/pull.log
```

- `nohup ... &` 让拉取在服务器后台运行，**关掉浏览器/断网都不会中断**
- Docker 按层缓存，中途断了只重下未完成的那几层，不会从零开始
- 4 个镜像合计约 350 MB，2 Mbps 带宽下约 20 分钟

---

## 7. 第四步：启动容器

```bash
cd /opt/shop
docker compose up -d
```

期望输出：

```
[+] Running 6/6
 ✔ Network shop-trial-network  Created
 ✔ Container shop-redis        Healthy
 ✔ Container shop-frontend     Started
 ✔ Container shop-mysql        Healthy
 ✔ Container shop-backend      Started
 ✔ Container shop-nginx        Started
```

### 7.1 关于 MySQL 初始化

`docker-compose.yml` 把建表 DDL 挂载到 `/docker-entrypoint-initdb.d/`（为防止默认口令随重装自动写入公网环境，已移除 `data.sql` 自动挂载）：

```yaml
volumes:
  - ./data/mysql:/var/lib/mysql
  - ./sql/schema.sql:/docker-entrypoint-initdb.d/01_schema.sql:ro
  # 刻意不挂载 data.sql：首次部署后按 README 手动初始化管理员，或手动导入演示数据后立即改密
```

**只在 `./data/mysql` 为空（首次启动）时执行**。之后再改 SQL 不会自动重跑。

重置数据库：

```bash
cd /opt/shop
docker compose down
rm -rf ./data/mysql
docker compose up -d
```

---

## 8. 第五步：验证部署结果

### 8.1 容器状态

```bash
cd /opt/shop
docker compose ps
```

期望：

```
NAME            IMAGE                    STATUS
shop-backend    eclipse-temurin:21-jre   Up 25 seconds
shop-frontend   nginx:1.27-alpine        Up
shop-mysql      mysql:8.4                Up (healthy)
shop-nginx      nginx:1.27-alpine        Up    0.0.0.0:80->80/tcp
shop-redis      redis:7.2-alpine         Up (healthy)
```

### 8.2 服务器内自测

```bash
curl -I http://127.0.0.1/
curl -I http://127.0.0.1/api/portal/home
ss -tlnp | grep -E ':80|:8080|:8443'   # 只应看到 :80
```

期望：静态页返回 `Content-Type: text/html`，接口返回 `Content-Type: application/json` 且状态 200。

### 8.3 外网自测（本地 PowerShell）

```powershell
foreach ($p in 80,22) {
  $r = Test-NetConnection -ComputerName 123.57.26.237 -Port $p -InformationLevel Quiet -WarningAction SilentlyContinue
  Write-Output ("{0} -> {1}" -f $p, $r)
}
```

期望：`80 -> True`；而在完成 [11 远程运维与凭据加固](file:///D:/JavaWeb/docs/principles/11-remote-ops-and-credential-hardening.md) 将 SSH 收敛至阿里云内网后，本机公网测 `22 -> False` 属于预期安全状态（首次未收敛前为 `True`）。

> 8080/8443 已下线，探测这两个端口返回 `False` 属于预期，不是故障。

### 8.4 浏览器验证

打开 `https://shopnest.dpdns.org`（或 `http://123.57.26.237`），注册买家账号（或使用演示买家账号 `customer`）登录，走一遍"浏览商品 → 加购物车 → 下单"。

---

## 9. 第六步：安全组与域名配置

### 9.1 安全组放行端口（必须）

控制台 → ECS → 实例 → **安全组** → 配置规则 → **入方向** → 手动添加。

**当前（2026-11-17 安全加固后）入方向只保留以下规则（3 条允许 + 1 条拒绝）：**

| 协议类型 | 端口范围 | 授权对象 | 说明 |
| :--- | :--- | :--- | :--- |
| TCP | `80/80` | `0.0.0.0/0` | HTTP，Cloudflare 回源与 IP 直连 |
| TCP | `22/22` | `100.104.0.0/16` | SSH（仅阿里云 Workbench 内网通道可达，公网已关闭） |
| ICMP | `全部(-1/-1)` | `0.0.0.0/0` | 允许 ping（可选，删除可减少被扫描暴露） |
| TCP | `3389/3389` | `0.0.0.0/0` | **拒绝**（Linux 机器用不到 RDP，优先级 1 高于允许规则的 100） |

**已删除：** `8080/8080`、`8443/8443` 两条允许规则。端口收敛后它们不再有任何用途，留着只是多余攻击面。

> **关键**：端口范围必须成对书写，如 `8080/8080`。
> 写单个 `8080` 可能报格式错误；写 `80/8080` 会一次开放 80~8080 共 8000 个端口，属于严重过度暴露。
>
> **收窄 22 的顺序**：先新增一条「你的公网 IP → 22/22 允许」，确认新开一个终端能登录成功，
> 再删除 `0.0.0.0/0` 那条旧规则。顺序反了会把自己锁在门外；万一锁了用控制台
> Workbench（云助手免密登录）救回。
>
> **安全组与容器端口是两层**：安全组是"大门"，Docker 的 `ports` 映射是"房间的窗"。
> 只删安全组规则，容器仍在监听（`ss -tlnp` 还能看到 8080/8443），
> 一旦误加规则就又暴露；只删端口映射不改安全组，规则留着同样没意义。
> 两层同时收才叫最小暴露面——本次即同时改了安全组与 `docker-compose.yml`。

### 9.2 DNS 记录

**初次部署阶段**（尚未接入 Cloudflare）在 DigitalPlat 的 DNS 页面添加两条 A 记录：

| 类型 | 名称 | 值 | TTL |
| :--- | :--- | :--- | :--- |
| A | `@` | `123.57.26.237` | 3600 |
| A | `www` | `123.57.26.237` | 3600 |

- `@` 是裸域名，`www` 是独立的另一条记录，两条都要
- 名称里不要写完整域名，填 `www` 而不是 `www.shopnest.dpdns.org`
- 值里不要带 `http://` 和端口

验证：

```powershell
nslookup shopnest.dpdns.org 223.5.5.5
```

> 接入 Cloudflare 之后，这两条记录改到 Cloudflare 面板维护，并且要打开橙色云代理，
> 具体见第 10 章。DigitalPlat 侧的旧记录只保留 7 天，之后自动失效。

### 9.3 关于大陆节点的域名访问限制（实测修正版）

早期判断是「未备案域名访问 80 端口会被拦截」，但 2026-11-13 的实测结果是：

| 访问方式 | 实测结果 |
| :--- | :--- |
| `http://123.57.26.237`（IP + 80） | 200 正常 |
| `http://shopnest.dpdns.org`（域名 + 80） | 200 正常 |
| `http://shopnest.dpdns.org:8080`（域名 + 8080） | 200 正常（该端口已于 2026-11-16 下线） |
| `https://123.57.26.237:8443`（IP + 8443） | 200 正常（该端口已于 2026-11-16 下线） |
| `https://shopnest.dpdns.org:8443`（域名 + HTTPS） | **连接被重置** |

结论：**真正的拦截点是 TLS 握手里的 SNI（域名）**，与端口和协议版本无关。
只要 ClientHello 里带了未备案域名，阿里云网络设备就回 RST；
纯 IP 或纯 HTTP 则不受影响。

这也解释了为什么「换端口到 8443 并不能解决 HTTPS 问题」，最终必须借 Cloudflare 兜住浏览器侧 TLS。

---

## 10. 第七步：HTTPS 证书与 Cloudflare 接入

> 本章记录把 `http://` 升级为 `https://shopnest.dpdns.org` 的完整过程。
> 中途踩了两个大坑：**阿里云的未备案域名 SNI 拦截**，以及**证书必须在 DNS 侧验证**。
> 按本章顺序照做即可复现。

### 10.1 起点：为什么会提示「不安全」

浏览器对 `http://` 页面一律标记「不安全」，页面里有密码框（登录、注册、结算）时提示更强。
目标就是让地址栏变成 `https://` 并带上锁标。

### 10.2 先做可行性判断（很重要，省掉大量返工）

| 方案 | 是否可行 | 原因 |
| :--- | :--- | :--- |
| 直接开 443 + 证书 | ❌ | 大陆未备案域名，带 SNI 的 TLS 直接被重置 |
| 换成 8443 非标端口 + 证书 | ❌ | 端口不是拦截点，SNI 才是 |
| 用 IP 上 HTTPS | ⚠️ 技术上可行 | 浏览器不认 IP 证书，仍需域名 |
| 备案后再上 HTTPS | ❌ | `dpdns.org` 是境外免费域名，无法在国内备案 |
| **Cloudflare 代理 + 源站 HTTP** | ✅ | 浏览器到 CF 走 TLS，CF 到源站走 HTTP，绕开 SNI 拦截 |

判断用的对照实验（结论见 10.5）：

```powershell
curl.exe -k -sS -I https://123.57.26.237:8443     # 纯 IP
curl.exe -k -sS -I https://shopnest.dpdns.org:8443 # 带域名
```

### 10.3 申请 Let's Encrypt 证书（DNS-01 手动模式，历史步骤）

> **当前线上已不使用源站证书**：2026-11-16 端口收敛后，源站只跑 HTTP 80，
> acme.sh 已卸载、`/root/.acme.sh` 已删除。本节保留完整过程，仅用于理解 DNS-01 原理或将来恢复全链路加密时参考。

为什么用 DNS-01：HTTP-01 要求 80 端口可被 CA 访问、TLS-ALPN-01 要求 443，
两者都受备案/拦截影响；只有 DNS TXT 验证不依赖入站端口。

**① 安装 acme.sh**（必须在 `/tmp` 目录下用 `./` 执行）

```bash
cd /tmp
curl -o acme.sh https://raw.githubusercontent.com/acmesh-official/acme.sh/master/acme.sh
chmod +x acme.sh
./acme.sh --install -m 你的邮箱
~/.acme.sh/acme.sh --set-default-ca --server letsencrypt
```

> **坑**：直接 `bash /tmp/acme.sh --install` 会报
> `cp: cannot stat 'acme.sh'`，因为安装脚本只从**当前目录**拷贝自身。
> 必须先 `cd /tmp` 再 `./acme.sh`。
>
> 邮箱用于 ACME 账户联系（到期/吊销通知），不验证、不收验证码。
> `--set-default-ca --server letsencrypt` 必须执行，否则新版 acme.sh 默认走 ZeroSSL（强制要求有效邮箱）。

**② 发起验证，拿到 TXT 值**

```bash
~/.acme.sh/acme.sh --issue --dns -d shopnest.dpdns.org --yes-I-know-dns-manual-mode-enough-go-ahead-please
```

输出：

```text
Add the following TXT record:
Domain: '_acme-challenge.shopnest.dpdns.org'
TXT value: '2iQEaRkTNty6E5vHx3cAU_SisN-JIMxWgwNP-8taiXs'
```

**③ 去 DNS 面板添加 TXT 记录**

此时 DNS 还在 DigitalPlat：域名 → DNS → 添加记录，类型 `TXT`，
主机记录 `_acme-challenge`，值粘贴上面那串（不带引号），TTL 取最小。

**④ 验证 TXT 已生效**（服务器上没有 nslookup 也能查）

```bash
curl -s "https://dns.alidns.com/resolve?name=_acme-challenge.shopnest.dpdns.org&type=TXT"
```

返回 JSON 的 `data` 字段里能看到那串值即可。若返回 `"Status":3` 或空，等 2～5 分钟再查。

**⑤ 完成签发**

```bash
~/.acme.sh/acme.sh --renew -d shopnest.dpdns.org --yes-I-know-dns-manual-mode-enough-go-ahead-please
ls -l ~/.acme.sh/shopnest.dpdns.org_ecc/
```

成功输出：

```text
Cert success.
Your cert is in:  /root/.acme.sh/shopnest.dpdns.org_ecc/shopnest.dpdns.org.cer
Your cert key is in: /root/.acme.sh/shopnest.dpdns.org_ecc/shopnest.dpdns.org.key
The full-chain cert is in: /root/.acme.sh/shopnest.dpdns.org_ecc/fullchain.cer
```

### 10.4 服务器端启用 8443（历史步骤，端口已于 2026-11-16 下线）

> 本节保留当时的完整操作，用于理解「源站 HTTPS」这条路为什么最终不可行。
> **当前线上已不启用 8443**：新部署请跳过本节，直接在 10.6 接入 Cloudflare 即可。
> 如需下线清理，见 10.4.6。

**① 给 compose 加端口和证书挂载**

```bash
cd /opt/shop
/bin/cp -f docker-compose.yml docker-compose.yml.bak
sed -i '/- "8080:8080"/a\      - "8443:8443"' docker-compose.yml
sed -i '/nginx\/shop.conf:\/etc\/nginx\/conf.d\/default.conf:ro/a\      - ./nginx/ssl:/etc/nginx/ssl:ro' docker-compose.yml
grep -A14 'shop-nginx:' docker-compose.yml
```

**② 在 shop.conf 末尾追加 8443 站点**

```bash
/bin/cp -f nginx/shop.conf nginx/shop.conf.bak
cat >> nginx/shop.conf <<'EOF'

server {
    listen 8443 ssl;
    server_name shopnest.dpdns.org;

    http2 on;

    ssl_certificate     /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;
    ssl_session_timeout 1d;
    ssl_session_cache shared:SSL_8443:10m;
    ssl_session_tickets off;

    server_tokens off;
    client_max_body_size 20m;

    add_header X-Content-Type-Options "nosniff" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    location /api/ {
        proxy_pass http://shop-backend:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_connect_timeout 10s;
        proxy_read_timeout 60s;
        proxy_send_timeout 60s;
    }

    location /uploads/ {
        alias /usr/share/nginx/uploads/;
        expires 30d;
        add_header Cache-Control "public, no-transform";
        access_log off;
    }

    location / {
        proxy_pass http://shop-frontend:80;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_read_timeout 60s;
    }
}
EOF
```

**③ 放证书并校验语法**

```bash
mkdir -p nginx/ssl
/bin/cp -f ~/.acme.sh/shopnest.dpdns.org_ecc/fullchain.cer nginx/ssl/cert.pem
/bin/cp -f ~/.acme.sh/shopnest.dpdns.org_ecc/shopnest.dpdns.org.key nginx/ssl/key.pem
chmod 600 nginx/ssl/key.pem
docker run --rm --network shop-trial-network \
  -v /opt/shop/nginx/shop.conf:/etc/nginx/conf.d/default.conf:ro \
  -v /opt/shop/nginx/ssl:/etc/nginx/ssl:ro \
  nginx:1.27-alpine nginx -t
```

> **坑**：临时校验容器必须带 `--network shop-trial-network`，
> 否则会报 `host not found in upstream "shop-backend"`，那是网络不通导致的误报，不是配置错误。
>
> 也不能在旧容器里跑 `docker compose exec shop-nginx nginx -t`，
> 因为旧容器还没挂载 `ssl` 目录，会报 `cannot load certificate`。

**④ 只重建网关容器**

```bash
cd /opt/shop
docker compose up -d --force-recreate shop-nginx
docker compose ps
docker logs --tail 30 shop-nginx
```

期望 `shop-nginx` 端口包含 `8443->8443/tcp`，日志里出现 `start worker processes` 且无 `emerg`。

**⑤ 服务器本机自测**

```bash
curl -k -I https://127.0.0.1:8443
```

返回 `HTTP/2 200`、证书为 `CN=shopnest.dpdns.org` 即说明源站 HTTPS 完全正常。

#### 10.4.6 下线 8443 / 8080（2026-11-16 实操）

Cloudflare 接管浏览器侧 TLS 后，源站的 8443 与 8080 已无用途，属于多余暴露面。
下线分两步：**先收安全组，再改配置重建容器**。

**① 安全组**：删除 `8080/8080`、`8443/8443` 两条入方向允许规则（见 9.1）。

**② 服务器上改配置**（`sed` 比上传文件更稳，避免 scp 的终端兼容问题）：

```bash
cd /opt/shop
/bin/cp -f docker-compose.yml docker-compose.yml.bak
/bin/cp -f nginx/shop.conf nginx/shop.conf.bak

# 删掉两个端口映射与证书目录挂载
sed -i '/- "8080:8080"/d; /- "8443:8443"/d; /nginx\/ssl:\/etc\/nginx\/ssl:ro/d' docker-compose.yml

# 删掉 listen 8080 与整个 8443 SSL 站点
sed -i '/listen 8080;/d; /^# 8443 HTTPS 站点/,$d' nginx/shop.conf

# 核对
grep -nA4 'ports:' docker-compose.yml
grep -n 'listen\|server {' nginx/shop.conf
```

**③ 重建并验证**

```bash
docker compose up -d --force-recreate shop-nginx
docker exec shop-nginx nginx -t
ss -tlnp | grep -E ':80|:8080|:8443'
```

期望：`nginx -t` 通过，`ss` 只剩 `0.0.0.0:80` 与 `[::]:80`。
`sed` 删完后 `shop.conf` 末尾会残留一行孤立的 `# ====` 注释，不影响解析。

> **坑**：只做①不做②，`ss -tlnp` 仍能看到 8080/8443（容器还在监听），
> 只是外网连不进来；只做②不做①，安全组规则留着，哪天误加回来就又暴露。
> 两层必须一起收。
>
> **验证顺序**：`docker compose up --force-recreate` 之后一定要看 `ss`，
> 而不是只看 `docker compose ps`——容器状态 Up 不代表端口映射已按新配置生效。

### 10.5 定位：公网访问为什么还是失败

此时会出现一个很迷惑的现象：

| 测试位置 | 目标 | 结果 |
| :--- | :--- | :--- |
| 服务器内部 | `https://127.0.0.1:8443` | ✅ 200 |
| 本机 | `https://123.57.26.237:8443`（无域名） | ✅ 200 |
| 本机 | `https://shopnest.dpdns.org:8443`（带域名） | ❌ 连接被重置 |
| 服务器 | `docker logs shop-nginx` | 没有任何握手失败日志 |

关键结论：

- 带域名被重置、不带域名正常 → **按 SNI 拦截**，不是 Nginx 问题
- Nginx 日志完全没有记录 → 流量根本没到服务器，在云网络层就被丢弃/RST

所以 8443 直连这条路到此为止，对外入口必须交给 Cloudflare。

### 10.6 接入 Cloudflare（浏览器侧 HTTPS）

**① 添加站点**

dash.cloudflare.com → 添加站点 → 输入 `shopnest.dpdns.org` → 选 **Free** 计划。
完成后 Cloudflare 会分配两个名称服务器，例如：

```text
bowen.ns.cloudflare.com
priscilla.ns.cloudflare.com
```

**② 在 Cloudflare 建记录**

| 类型 | 名称 | 内容 | 代理状态 |
| :--- | :--- | :--- | :--- |
| A | `shopnest.dpdns.org`（或 `@`） | `123.57.26.237` | **已代理（橙色云）** |
| A | `www` | `123.57.26.237` | **已代理（橙色云）** |
| TXT | `_acme-challenge` | 之前申请的 TXT 值 | 仅 DNS（灰色云） |

> 橙色云是重点：灰色云（仅 DNS）会把访客直接引到源站 IP，HTTPS 仍然被 SNI 拦截。

**③ 设置 SSL 模式**

SSL/TLS → 概述 → 加密模式选 **「灵活 / Flexible」**。

> 不能选「完全 / Full」：Full 会用 HTTPS 回源，而源站只有明文 HTTP 80、没有证书，
> 浏览器会看到 521/526。本项目源站没有证书，加密模式只能停在「灵活」。

**④ 在 DigitalPlat 修改名称服务器**

DigitalPlat → 域名 → DNS → **域名服务器** 页签：

| 位置 | 填写 |
| :--- | :--- |
| 名称服务器 1 | `bowen.ns.cloudflare.com` |
| 名称服务器 2 | `priscilla.ns.cloudflare.com` |
| 名称服务器 3～8 | 留空 |

点 **「更新名称服务器」**。注意此页提示：DigitalPlat 原 DNS 记录只保留 7 天。

**⑤ 等待激活**

Cloudflare 概览页会从「等待你的注册商传播你的新名称服务器」变为
**「你的域名现在由 Cloudflare 保护」**，通常几分钟到 2 小时。

**⑥ 验证（用 DoH 绕开本地缓存）**

```powershell
# 1. 看权威解析是否变成 Cloudflare 代理 IP（172.67.x / 104.x）
curl.exe "https://dns.alidns.com/resolve?name=shopnest.dpdns.org&type=A"

# 2. 直接访问
curl.exe -sS -I https://shopnest.dpdns.org

# 3. 接口连通性（验证返回 JSON 包含 "code":200）
curl.exe -sS https://shopnest.dpdns.org/api/portal/home | Select-String '"code":200'
```

期望：解析返回 `172.67.167.138`、`104.21.73.253` 这类地址；
HTTP 返回 `200`、响应头含 `Server: cloudflare`；接口返回 `200`。

### 10.7 最终链路与安全边界

```
浏览器 ──TLS──> Cloudflare 边缘 ──HTTP 80──> shop-nginx ──> 前端/后端
         证书由 CF 自动签发与续期        明文，仅限试用
```

- 浏览器侧证书、HTTP/2、TLS1.3 全部由 Cloudflare 处理，自动续期
- Cloudflare 到源站是明文 HTTP，因为带域名的 HTTPS 回源会被拦
- 若要全链路加密，只能：给域名备案，或把服务器迁到境外/香港

前端无需改代码：`frontend/src/api/request.ts` 的 `baseURL` 是空字符串，接口走相对路径，
页面切到 HTTPS 后自动同源继承 HTTPS，不会触发混合内容警告。

### 10.8 证书续期（已停用）

**2026-11-16 起源站不再部署证书**：8443 下线后，`/opt/shop/nginx/ssl/` 不再被容器挂载，acme.sh 那套 DNS-01 手动续期流程也随之彻底退出。

实际执行过的清理：

```bash
acme.sh --remove -d shopnest.dpdns.org      # 移除该域名的续期登记
/root/.acme.sh/acme.sh --uninstall          # 卸载 acme.sh 并清除 cron
/bin/rm -rf /root/.acme.sh                  # 删除残留目录
/bin/rm -rf /opt/shop/nginx/ssl              # 删除不再挂载的证书目录
crontab -l                                  # 确认已无 acme 续期任务
```

> 源站那份 Let's Encrypt 证书已随 `/root/.acme.sh` 一并删除，无法找回。
> 将来若需要恢复，按 10.3 重新签发即可（DNS-01 手动模式，约 5 分钟）。

浏览器侧证书完全由 Cloudflare 自动签发与续期，**无需任何人工操作**。

如果出于某种原因要恢复全链路加密（Cloudflare「完全（严格）」模式），正确做法是：

1. 源站重新启用 443 并挂载证书（不是 8443），安全组放行 443；
2. Cloudflare SSL 模式改为「完全（严格）」；
3. 再恢复 acme.sh 续期：`_acme-challenge` TXT 记录要加到 **Cloudflare DNS**（不是 DigitalPlat）。

> 注意：源站 443 承载未备案域名时仍会被 SNI 拦截，所以「严格模式」只能配合
> Cloudflare 的源站证书（Origin CA）或把服务器迁到境外，否则会 521。

### 10.9 HTTPS 相关故障速查

| 现象 | 原因 | 解决 |
| :--- | :--- | :--- |
| 带域名 HTTPS 被重置 | 阿里云未备案 SNI 拦截 | 走 Cloudflare 代理，别直连源站 HTTPS |
| 浏览器提示「此站点的连接不安全」 | 访问的是 `http://`，或地址带了非 443 端口（如 `:8080`） | 换成 `https://shopnest.dpdns.org`（不带端口） |
| 浏览器 521 | CF SSL 模式选了「完全」 | 改回「灵活」 |
| 解析仍指向源站 IP | 本地 DNS 缓存 / UDP 53 被劫持 | `ipconfig /flushdns`，用 DoH 验证 |
| `nginx -t` 报 upstream 找不到 | 临时容器没加 `--network` | 加上 `--network shop-trial-network` |
| `cp: cannot stat 'acme.sh'` | 没在 `/tmp` 下用 `./acme.sh` | `cd /tmp && ./acme.sh --install` |
| `curl.exe` 在服务器上不存在 | 那是 Windows PowerShell 命令 | 服务器上用 `curl` |
| 端口已从安全组删除，`ss` 仍显示监听 | 只改了安全组，容器端口映射没改 | 同步改 `docker-compose.yml` 后 `--force-recreate` |

---

## 11. 踩坑实录（问题 → 原因 → 解决）

> 本节是部署过程中真实遇到的全部问题，按发生顺序排列。

### 坑 1：`ssh`/`scp` 提示 yes/no，键盘输入无反应

**现象**

```
Are you sure you want to continue connecting (yes/no/[fingerprint])?
Please type 'yes', 'no' or the fingerprint:
```
反复出现，输入 `yes` 无效，只有回车能被识别。

**原因**：Windows 自带 OpenSSH 的 `ssh.exe` 通过控制台底层 API 读取确认输入，在部分终端（IDE 内置终端、某些 Windows Terminal 配置）下字符无法送达。

**解决**：预先写入主机指纹，绕过交互提示。

```powershell
New-Item -ItemType Directory -Force -Path "$env:USERPROFILE\.ssh" | Out-Null
ssh-keyscan -t ed25519,rsa 123.57.26.237 2>$null | Out-File -Append -Encoding ascii "$env:USERPROFILE\.ssh\known_hosts"
```

---

### 坑 2：`Permission denied (publickey,gssapi-keyex,gssapi-with-mic)`

**现象**：`scp` 直接报权限拒绝，根本没有出现密码输入提示。

**原因**：实例创建时选的是「密钥对」而不是「自定义密码」，服务器禁用了密码登录，而私钥 `.pem` 未保存。

**解决（二选一）**

方案 A —— 控制台重置密码：

1. ECS 控制台 → 实例 → 更多 → 密码/密钥 → **重置实例密码**
2. 勾选「重置后自动重启实例」，等待重启
3. 重新执行 `scp`，这次会正常提示输入密码

方案 B —— 绑定新密钥对：

```powershell
ssh-keygen -t ed25519 -f "$env:USERPROFILE\.ssh\aliyun_shop" -N '""'
Get-Content "$env:USERPROFILE\.ssh\aliyun_shop.pub"
```

把公钥内容粘贴到：控制台 → 网络与安全 → 密钥对 → 创建密钥对 → **导入已有密钥对** → 绑定到实例（会重启）。

之后：

```powershell
scp -i "$env:USERPROFILE\.ssh\aliyun_shop" "$env:TEMP\shop-trial.zip" root@123.57.26.237:/root/
```

---

### 坑 3：密码提示出现但打字无反应（连密码也输不进去）

**现象**：`root@123.57.26.237's password:` 出现后，输入任何字符都没反应，只有回车有效。

**原因**：同上，Windows OpenSSH 的控制台输入问题，与输入法无关。

**解决**：放弃本地终端，改用 **Workbench 网页远程连接**（浏览器输入正常），或 WinSCP 图形客户端。

> 另外要区分：Linux 密码输入**本来就不回显**，敲完直接回车是正常的，不要在"看不到星号"这一点上误判。

---

### 坑 4：Workbench 上传后 `ls /root` 是空的

**现象**：明明上传完成，`/root` 目录为空，`unzip` 报 `cannot find or open shop-trial.zip`。

**原因**：文件被上传到了左侧文件管理面板**当前所在目录**，实际落在 `/shop-trial.zip`（根目录）。

**解决**：先定位真实路径，再按实际路径解压。

```bash
find / -maxdepth 4 -name "shop-trial.zip" 2>/dev/null
unzip -o /shop-trial.zip -d /opt/shop
```

---

### 坑 5：`docker compose up -d` 长时间无动静，镜像列表为空

**现象**

```bash
docker images            # 空的
du -sh /var/lib/docker   # 一直 268K，不增长
```

**原因 1（最常见）**：`/etc/docker/daemon.json` 里的镜像地址被写入了反引号。复制聊天/文档里的 URL 时很容易带上 `` ` ``，导致地址变成非法域名，Docker 一直等待。

**排查**

```bash
cat /etc/docker/daemon.json
od -c /etc/docker/daemon.json | head -5    # 看真实字节
```

**解决**：不含反引号地重建配置。

```bash
printf '%s\n' '{' '  "registry-mirrors": ["https://docker.m.daocloud.io", "https://docker.1ms.run"],' '  "log-driver": "json-file",' '  "log-opts": { "max-size": "10m", "max-file": "3" }' '}' > /etc/docker/daemon.json
systemctl restart docker
docker info | grep -A 3 "Registry Mirrors"
docker pull mysql:8.4
```

> 补充：Workbench 终端会**把 URL 自动加上反引号显示**，所以 `cat` 看到反引号未必是真的。
> 判断标准只有一个：`od -c` 的字节输出，以及 `docker pull` 是否能下动。
>
> 另外：脚本里收到的 URL 参数若带反引号（例如 `bash setup.sh https:`https://xxx`` 这种手误），
> 会被原样写进 `daemon.json`，这是"真的带了反引号"的典型场景。

**原因 2**：阿里云 ACR 加速地址是"缓存代理"，回源失败时直接报
`manifest for xxx/library/mysql:8.4 not found: manifest unknown`，说明该地址不可用。

**解决**：换公共镜像源。先扫描可用性：

```bash
for m in docker.m.daocloud.io docker.1ms.run docker.xuanyuan.me hub.rat.dev dockerpull.org dockerproxy.net; do
  code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 6 "https://$m/v2/");
  echo "$m -> $code";
done
```

结果解读：

| 返回码 | 含义 |
| :--- | :--- |
| `401` / `200` | 可用（`401` 是注册表正常的鉴权响应） |
| `302` | 可能有跳转，需实测 |
| `403` / `404` | 不可用 |
| `000` | 连不上 / 超时 |

实测结果（2026-11 华北2 节点）：`docker.m.daocloud.io` → 401 ✅、`docker.1ms.run` → 401 ✅，其余不可用。

---

### 坑 6：拉镜像很慢，担心断网要重拉

**原因**：实例公网带宽只有 2 Mbps。

**事实**：

- 已下载完成的镜像层（`Download complete` / `Pull complete`）会缓存在 `/var/lib/docker`，**断线不丢**
- 只有正在下载、未完成的那一层会重下
- `docker pull` 跑在**服务器**上，本地断网只影响你查看进度

**解决**：用 nohup 挂后台，关闭浏览器也不中断。

```bash
cd /opt/shop
nohup docker compose pull > /tmp/pull.log 2>&1 &
tail -f /tmp/pull.log      # Ctrl+C 只退出查看，不影响下载
```

---

### 坑 7：`shop-backend` 反复 `Restarting (0)`

**现象**

```
shop-backend  eclipse-temurin:21-jre  Restarting (0) 7 seconds ago
```

静态页能打开，但接口 502。

**原因**：compose 里只写了 `image:` 没写 `command:`。`eclipse-temurin:21-jre` 镜像的默认入口脚本不启动应用，进程立即以 0 退出，被 `restart: always` 反复拉起。

**解决**：给 `shop-backend` 补上启动命令。

```bash
sed -i '/container_name: shop-backend/a\    command: ["java", "-jar", "/app/app.jar"]' /opt/shop/docker-compose.yml
grep -n -A 3 "container_name: shop-backend" /opt/shop/docker-compose.yml
cd /opt/shop && docker compose up -d --force-recreate shop-backend
docker compose logs --tail 40 shop-backend
```

成功标志：

```
Started ShopApplication in 4.91 seconds (process running for 5.958)
```

---

### 坑 8：外网访问返回 502 / 端口探测 False

**现象**：服务器内 `curl` 正常，本机访问 502 或超时；`Test-NetConnection` 对 80 返回 False，但 22 返回 True。

**原因**：安全组没有放行 80（当前入方向只保留 22、80、ICMP，并显式拒绝 3389）。502 是本地代理（TUN 模式）在连接失败时返回的。

**解决**：安全组入方向添加 `80/80`，授权对象 `0.0.0.0/0`，立即生效，无需重启。

> `8080`、`8443` 已于 2026-11-16 下线并从安全组删除，排障时不要再重新放行；对外只需确认 80 可达。

---

### 坑 9：本机 `nslookup` 全部超时

**现象**

```
服务器:  UnKnown
Address:  fe80::1234
DNS request timed out.
```

**原因**：本机 WLAN 下发的 IPv6 DNS `fe80::1234`（路由器链路本地地址）不响应。

**解决（任选，均为本机设置，不影响服务器）**

- 临时：指定公共 DNS 查询 `nslookup shopnest.dpdns.org 223.5.5.5`
- 彻底：WLAN 属性里取消勾选 IPv6，并把 IPv4 DNS 手动设为 `223.5.5.5` / `119.29.29.29`
- 恢复：`Enable-NetAdapterBinding -Name "WLAN" -ComponentID ms_tcpip6` +
  `Set-DnsClientServerAddress -InterfaceAlias "WLAN" -ResetServerAddresses`

> 也可以什么都不改，直接用 `http://123.57.26.237` 访问，域名只影响"好不好看"。

---

### 坑 10：Workbench 页面刷新后终端内容全没了

**原因**：只清空了本地显示缓存，服务器上的进程、容器、文件都不受影响。

**解决**：刷新后重新执行命令查看即可，`docker compose ps` 能看到容器仍在运行。

---

### 坑 11：`mv` / `cp` / `rm` 卡在覆盖确认，加 `-f` 也没用

**现象**

```
[root@... shop]# cp -f docker-compose.yml docker-compose.yml.bak
cp: overwrite 'docker-compose.yml.bak'? [root@... shop]#
```

明明加了 `-f` 还是会问，直接回车等于"否"，文件没被覆盖。

**原因**：阿里云 Linux 对 root 默认启用 `alias cp='cp -i'`、`alias mv='mv -i'`、`alias rm='rm -i'`。
别名展开后实际执行的是 `cp -i -f ...`，`-i` 优先生效，`-f` 被吃掉。

**解决（任选）**

```bash
/bin/cp -f src dst        # 用绝对路径，绕过别名（最稳）
command cp -f src dst     # 临时屏蔽别名
unalias cp                # 当前会话去掉别名，之后 cp -f 正常
```

`rm -rf` 不受影响（`-i` 与 `-f` 同时存在时 `-f` 优先），但 `mv`/`cp` 覆盖时表现如上。

---

### 坑 12：`scp` 出现密码提示但完全打不进字符

**现象**

```text
PS C:\Users\jasper> scp "d:\JavaWeb\shop.conf" root@123.57.26.237:/opt/shop/nginx/shop.conf
root@123.57.26.237's password:
```

光标停住，敲什么都没反应。

**原因**：Windows OpenSSH 的控制台读取方式与部分终端不兼容（同坑 1、坑 3）。

**解决**：不要走 scp 传文件，改成**在服务器上直接用 heredoc 写文件**：

```bash
cat > nginx/shop.conf <<'EOF'
...配置内容...
EOF
```

或者用 Workbench 网页端 / WinSCP 拖拽。

> **另一个高频错误**：把 `scp "d:\JavaWeb\xxx" root@123.57.26.237:/opt/shop/xxx`
> 敲在了**服务器的 Workbench 终端里**，会报：
>
> ```text
> ssh: Could not resolve hostname d: Name or service not known
> ```
>
> 因为 `d:` 被当成了主机名。`scp` 必须在**本地 Windows PowerShell** 里执行，
> 源路径是本地路径、目标是服务器路径；反过来说，服务器上不需要也不应该再 scp 到自己。
>
> 如果本地 scp 又遇到本文坑 1/坑 3 的终端输入问题，最省事的替代方案是
> **在服务器上用 `sed` 直接改配置**（见 10.4.6），只改几行时比传整个文件更可靠。

---

### 坑 13：acme.sh 安装报 `cp: cannot stat 'acme.sh'`

**现象**

```text
[root@... ~]# bash /tmp/acme.sh --install -m xxx@gmail.com
[..] Installing to /root/.acme.sh
cp: cannot stat 'acme.sh': No such file or directory
[..] Installation failed, cannot copy acme.sh
```

**原因**：安装逻辑只从**当前工作目录**拷贝 `acme.sh`，在 `/root` 下执行 `bash /tmp/acme.sh` 时找不到文件。

**解决**：

```bash
cd /tmp
chmod +x acme.sh
./acme.sh --install -m 你的邮箱
```

看到 `Installed to /root/.acme.sh/acme.sh` 并生成 cron 任务即为成功。

---

### 坑 14：`raw.githubusercontent.com` 拉取中断

**现象**：`curl: (35) OpenSSL SSL_connect: SSL_ERROR_SYSCALL`，或下载到一半断开。

**原因**：GitHub 直连在大陆网络下不稳定，属于偶发。

**解决**：原命令重试一次通常就能成功（实测第二次拉完整 297KB）。仍不行时用 Gitee 镜像：

```bash
git clone https://gitee.com/neilpang/acme.sh.git /tmp/acme
cd /tmp/acme && ./acme.sh --install -m 你的邮箱
```

---

### 坑 15：安全组漏开 8443，表现和 SNI 拦截很像（历史，端口已下线）

**现象**：`https://shopnest.dpdns.org:8443` 一会儿超时、一会儿连接被重置；
`Test-NetConnection shopnest.dpdns.org -Port 8443` 有时 True 有时 False。

**原因**：8443 端口规则没加（或只加了 8080），TCP 层就没放行；不同时段/不同代理路径表现不一致。

**解决**：

1. 控制台 → 安全组 → 入方向，确认存在 `TCP 8443/8443 0.0.0.0/0`
2. 本机复测：

```powershell
Test-NetConnection shopnest.dpdns.org -Port 8443 -InformationLevel Quiet
```

3. 如果 TCP 通了、但一发数据就被重置，并且 `docker logs shop-nginx` 里没有任何记录，
   那就不是安全组问题，而是坑 16 的 SNI 拦截。

> 2026-11-16 起 8443 已下线，这条坑只作为排查思路参考：现在探测 8443 返回
> `False`/超时是**预期结果**，不是漏配。

---

### 坑 16：带域名 HTTPS 被重置，纯 IP 却正常（阿里云 SNI 拦截）

**现象**

| 请求 | 结果 |
| :--- | :--- |
| `https://123.57.26.237:8443` | 200 |
| `https://shopnest.dpdns.org:8443` | `Recv failure: Connection was reset` |
| 服务器内 `https://127.0.0.1:8443` | 200，证书链正常 |
| `docker logs shop-nginx` | 没有任何 TLS 握手失败日志 |

**原因**：阿里云对解析到大陆节点的**未备案域名**做了 SNI 拦截。
只要 TLS ClientHello 里携带该域名，就在云网络层回 RST，流量根本到不了 Nginx。
与端口（443/8443）、TLS 版本（1.2/1.3）都无关。

**解决（三选一）**

1. **接 Cloudflare**（本项目采用）：NS 切到 Cloudflare，A 记录开橙色云，
   SSL 模式设「灵活」，浏览器侧由 CF 提供证书，回源走 HTTP 80。完整步骤见第 10.6 节。
2. 给域名完成 ICP 备案（`dpdns.org` 免费域名不满足条件，不可行）。
3. 把服务器迁到香港/境外地域（域名无需备案，可直连 443）。

**排查口诀**：DNS 用 DoH 查（绕开本地缓存），HTTPS 先用 IP 试，再看 Nginx 有没有日志。

---

### 坑 17：本地解析一直返回源站 IP，浏览器仍打不开

**现象**：Cloudflare 后台已经显示「你的域名现在由 Cloudflare 保护」，
但本机 `Resolve-DnsName` 仍返回 `123.57.26.237`，浏览器打开报 `ERR_CONNECTION_RESET`。

**原因**：本地运营商 DNS 缓存未过期，或 UDP 53 被中间设备劫持返回旧记录。
（用 DoH 查询可立即看到 Cloudflare 的 `172.67.x` / `104.x` 地址。）

**解决**

```powershell
ipconfig /flushdns
curl.exe "https://dns.alidns.com/resolve?name=shopnest.dpdns.org&type=A"
```

若仍不生效，在 Chrome 打开「设置 → 隐私和安全 → 安全 → 使用安全 DNS」，
选择「自定义」并填 `https://cloudflare-dns.com/dns-query`，或等缓存 TTL 过期。

---

### 坑 18：升级 HTTPS 后浏览器仍显示「不安全」

**现象**：Cloudflare 已配好、`https://shopnest.dpdns.org` 也返回 200，但地址栏仍提示
「此站点的连接不安全」，点开显示 `shopnest.dpdns.org:8080`。

**原因**：访问的地址不是 443 端口的 HTTPS。浏览器只对「`https://` + 有效证书 + 443 端口」
显示锁标；`http://` 或任何带非 443 端口的地址（`:8080`、`:8443`）一律标记不安全，
与服务器、证书、Cloudflare 配置都无关。

**解决**

1. 把地址改成 `https://shopnest.dpdns.org`（**不带端口**）；
2. 若浏览器自动补全了旧地址，清掉历史记录或直接手输完整 URL；
3. 确认不是从收藏夹/历史里的 `:8080` 链接点进来的。

> 2026-11-16 端口收敛后，`:8080`、`:8443` 已彻底不可用，这类地址只会报错或加载缓存页面。

---

### 坑 19：端口已从安全组删除，`ss` 里却还在监听

**现象**

```bash
docker compose up -d --force-recreate shop-nginx
ss -tlnp | grep -E ':80|:8080|:8443'
# 仍能看到 0.0.0.0:8080、0.0.0.0:8443（docker-proxy）
```

**原因**：只改了安全组，没改 `docker-compose.yml`，或改了但没真正重建容器
（`docker compose ps` 显示 Up 不代表新配置已加载）。

**解决**：确认 compose 的 `ports` 只剩 `80:80`，然后强制重建并核对：

```bash
grep -nA4 'ports:' docker-compose.yml
docker compose up -d --force-recreate shop-nginx
ss -tlnp | grep -E ':80|:8080|:8443'
```

`ss` 只剩 `:80` 才算真正收敛；此时从公网探测 8080/8443 应返回 `False`。

---

## 12. 日常运维命令

所有命令在 `/opt/shop` 目录下执行。

```bash
cd /opt/shop

docker compose ps                        # 查看 5 个容器状态
docker compose logs -f shop-backend      # 实时看后端日志（Ctrl+C 退出不影响服务）
docker compose logs --tail 100 shop-mysql
docker compose restart shop-backend      # 只重启后端
docker compose restart                   # 重启全部
docker compose stop                      # 停止全部（容器保留）
docker compose down                      # 停止并删除容器与网络（数据卷目录保留）
docker compose up -d                     # 按当前配置重新启动
docker compose up -d --force-recreate shop-nginx   # 改了网关配置后（如端口、代理规则）

curl -I https://shopnest.dpdns.org       # 主入口（经 Cloudflare，看锁标链路是否通）
curl -I http://127.0.0.1/                # 源站自测（应返回 HTTP/1.1 200）
ss -tlnp | grep -E ':80|:8080|:8443'     # 核对对外监听端口，应只剩 80

docker stats                             # 实时资源占用
free -h                                  # 内存与 swap

docker exec -it shop-mysql mysql -uroot -p'Shop_Trial_2026!' shop_db
```

查看磁盘占用：

```bash
df -h /
du -sh /var/lib/docker
docker system df
```

清理无用镜像/缓存（谨慎）：

```bash
docker image prune -a      # 删除未被任何容器使用的镜像
docker builder prune       # 清理构建缓存
```

---

## 13. 代码更新与重新部署

### 13.1 只改前端

```powershell
cd d:\JavaWeb\frontend
npm run build
```

用 Workbench 文件面板把 `dist` 里的内容覆盖到服务器的 `/opt/shop/dist/`，然后**无需重启**（Nginx 直接读磁盘），浏览器强刷 `Ctrl + F5` 即可。

### 13.2 只改后端

```powershell
cd d:\JavaWeb\backend
mvn -B clean package -DskipTests
```

上传新的 jar 覆盖 `/opt/shop/app/app.jar`，然后：

```bash
cd /opt/shop && docker compose restart shop-backend
```

### 13.3 改了 SQL

数据库只在首次初始化时执行 SQL。要重新导入：

```bash
cd /opt/shop
docker compose down
rm -rf ./data/mysql
docker compose up -d
```

> ⚠️ 这会**清空所有订单与用户数据**，只适合演示环境重置。

### 13.4 改了 compose 或 Nginx 配置

```bash
cd /opt/shop
docker compose up -d --force-recreate
```

---

## 14. 流量、费用与到期说明

| 项目 | 说明 |
| :--- | :--- |
| 到期时间 | **2026-12-12 23:59:59**，到期自动释放，数据全部清空 |
| 计费方式 | 按使用流量，仅**公网出方向**收费，¥0.80/GB |
| 空闲消耗 | 容器空转接近 0；MySQL/Redis 走内网不计费 |
| 单次访问 | 打开首页含图片约 2~5 MB，不到 1 分钱 |
| 是否要关机 | 不必须；关机不退已付费用，但能避免被扫描/爬虫刷流量 |
| 开机自启 | Docker 已 `enable`，容器 `restart: always`，开机自动恢复 |

**演示前建议**：提前把前台、后台各点一遍"预热"，2 Mbps 带宽下首次加载较慢，预热后浏览器缓存命中就是秒开。

---

## 15. 附录 A：docker-compose.yml 全文

服务器实际使用版本（`/opt/shop/docker-compose.yml`，本地源文件为 `deploy/trial/docker-compose.yml`）：

> 项目根目录还保留了打包用的平铺副本：`docker-compose.yml`、`shop.conf`、`frontend.conf`、`.env.example`。日常维护请修改 `deploy/trial/` 下的源文件，再同步到这些平铺副本，避免两份配置漂移。

```yaml
name: shop-trial

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
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
      - --innodb-buffer-pool-size=256M
      - --performance-schema=OFF
      - --max-connections=150
    volumes:
      # 直接在本目录运行前，请先把 docs/sql/schema.sql 复制到 ./sql/，否则 Docker 会建出空目录导致初始化失败
      - ./data/mysql:/var/lib/mysql
      - ./sql/schema.sql:/docker-entrypoint-initdb.d/01_schema.sql:ro
      # 刻意不挂载 data.sql：该文件含演示账号种子，重新初始化会写入可被猜到的默认口令。
      # 首次部署后请按 README「管理员账号初始化」手动创建管理员账号。
    healthcheck:
      test: ["CMD-SHELL", "mysqladmin ping -h 127.0.0.1 -uroot -p$$MYSQL_ROOT_PASSWORD --silent"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 45s
    networks:
      - shop

  shop-redis:
    image: redis:7.2-alpine
    container_name: shop-redis
    restart: always
    mem_limit: 192m
    environment:
      REDIS_PASSWORD: ${REDIS_PASSWORD:-Shop_Trial_Redis_2026!}
    command:
      - sh
      - -c
      - exec redis-server --requirepass "$$REDIS_PASSWORD" --appendonly yes --maxmemory 128mb --maxmemory-policy allkeys-lru
    volumes:
      - ./data/redis:/data
    healthcheck:
      test: ["CMD-SHELL", "redis-cli -a \"$$REDIS_PASSWORD\" ping | grep -q PONG"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 10s
    networks:
      - shop

  shop-backend:
    image: eclipse-temurin:21-jre
    container_name: shop-backend
    command: ["java", "-jar", "/app/app.jar"]     # ★ 必需，缺失会导致 Restarting (0)
    restart: always
    mem_limit: 1200m
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_HOST: shop-mysql
      DB_PORT: 3306
      DB_NAME: ${DB_NAME:-shop_db}
      DB_USER: root
      DB_PASSWORD: ${DB_ROOT_PASSWORD:-Shop_Trial_2026!}
      REDIS_HOST: shop-redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD:-Shop_Trial_Redis_2026!}
      TZ: Asia/Shanghai
      JAVA_TOOL_OPTIONS: -Xms256m -Xmx768m -XX:+UseSerialGC -Duser.timezone=Asia/Shanghai -Dfile.encoding=UTF-8
    volumes:
      - ./app/app.jar:/app/app.jar:ro
      - ./uploads:/app/uploads
      - ./logs:/app/logs
    depends_on:
      shop-mysql:
        condition: service_healthy
      shop-redis:
        condition: service_healthy
    networks:
      - shop

  shop-frontend:
    image: nginx:1.27-alpine
    container_name: shop-frontend
    restart: always
    mem_limit: 64m
    volumes:
      - ./dist:/usr/share/nginx/html:ro
      - ./nginx/frontend.conf:/etc/nginx/conf.d/default.conf:ro
    networks:
      - shop

  shop-nginx:
    image: nginx:1.27-alpine
    container_name: shop-nginx
    restart: always
    mem_limit: 64m
    ports:
      - "80:80"
    volumes:
      - ./nginx/shop.conf:/etc/nginx/conf.d/default.conf:ro
      - ./uploads:/usr/share/nginx/uploads:ro
    depends_on:
      - shop-backend
      - shop-frontend
    networks:
      - shop

networks:
  shop:
    driver: bridge
    name: shop-trial-network
```

---

## 16. 附录 B：Nginx 配置全文

### 16.1 边缘网关 `nginx/shop.conf`

```nginx
# ========================================================
# 试用环境边缘网关（仅监听 80，TLS 由 Cloudflare 终结）
# ========================================================
server {
    listen 80;
    server_name _;

    server_tokens off;
    client_max_body_size 20m;

    add_header X-Content-Type-Options "nosniff" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    location /api/ {
        proxy_pass http://shop-backend:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 10s;
        proxy_read_timeout 60s;
        proxy_send_timeout 60s;
    }

    location /uploads/ {
        alias /usr/share/nginx/uploads/;
        expires 30d;
        add_header Cache-Control "public, no-transform";
        access_log off;
    }

    location / {
        proxy_pass http://shop-frontend:80;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;
    }
}
```

> 2026-11-16 起不再包含 `listen 8080` 与 8443 SSL 站点，源站只承载明文 HTTP 80。
> 如需查看历史版本，可参考服务器上的 `nginx/shop.conf.bak`（备份于下线前）。

### 16.2 前端静态站点 `nginx/frontend.conf`

```nginx
server {
    listen 80;
    server_name _;

    root /usr/share/nginx/html;
    index index.html;

    gzip on;
    gzip_min_length 1k;
    gzip_comp_level 6;
    gzip_types text/plain text/css text/javascript application/json application/javascript application/x-javascript text/xml application/xml image/svg+xml;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 30d;
        add_header Cache-Control "public, no-transform";
    }

    location ~ /\. {
        deny all;
    }
}
```

---

## 17. 附录 C：命令速查表

### 服务器端（Workbench / SSH）

```bash
# ---- 首次部署 ----
cd /root && unzip -o shop-trial.zip -d /opt/shop
cd /opt/shop && bash setup.sh https://加速地址
docker compose pull
docker compose up -d
docker compose ps

# ---- 验证 ----
curl -I http://127.0.0.1/
curl -I http://127.0.0.1/api/portal/home
ss -tlnp | grep -E ':80|:8080|:8443'   # 应只剩 80

# ---- 排查 ----
docker compose logs --tail 60 shop-backend
docker info | grep -A 3 "Registry Mirrors"
cat /etc/docker/daemon.json
od -c /etc/docker/daemon.json | head -5
free -h && df -h /
docker stats --no-stream

# ---- 重启/更新 ----
docker compose restart shop-backend
docker compose up -d --force-recreate
docker compose down && docker compose up -d
```

### 本机（PowerShell）

```powershell
# ---- 构建 ----
cd d:\JavaWeb\backend;  mvn -B clean package -DskipTests
cd d:\JavaWeb\frontend; npm run build

# ---- 连通性 ----
Test-NetConnection 123.57.26.237 -Port 80 -InformationLevel Quiet
Invoke-WebRequest "http://123.57.26.237/api/portal/home" -UseBasicParsing
Resolve-DnsName shopnest.dpdns.org -Server 223.5.5.5

# ---- HTTPS / Cloudflare ----
curl.exe -sS -I https://shopnest.dpdns.org
curl.exe "https://dns.alidns.com/resolve?name=shopnest.dpdns.org&type=A"
curl.exe -sS https://shopnest.dpdns.org/api/portal/products
Test-NetConnection 123.57.26.237 -Port 8080 -InformationLevel Quiet  # 期望 False
Test-NetConnection 123.57.26.237 -Port 8443 -InformationLevel Quiet  # 期望 False
ipconfig /flushdns

# ---- 免交互 SSH ----
New-Item -ItemType Directory -Force -Path "$env:USERPROFILE\.ssh" | Out-Null
ssh-keyscan -t ed25519,rsa 123.57.26.237 2>$null | Out-File -Append -Encoding ascii "$env:USERPROFILE\.ssh\known_hosts"
```

### 关键路径速查

| 内容 | 路径 |
| :--- | :--- |
| 部署根目录 | `/opt/shop` |
| 后端 jar | `/opt/shop/app/app.jar` |
| 前端静态文件 | `/opt/shop/dist` |
| 上传图片 | `/opt/shop/uploads` |
| 后端日志 | `/opt/shop/logs/shop-backend.log` |
| MySQL 数据 | `/opt/shop/data/mysql` |
| Redis 数据 | `/opt/shop/data/redis` |
| SQL 初始化脚本 | `/opt/shop/sql/schema.sql`（`data.sql` 已不再自动挂载） |
| Let's Encrypt 证书（已停用） | `/opt/shop/nginx/ssl/cert.pem`、`key.pem` |
| acme.sh 证书源（已停用） | `/root/.acme.sh/shopnest.dpdns.org_ecc/` |
| Docker 配置 | `/etc/docker/daemon.json` |
| 拉取日志 | `/tmp/pull.log` |

---

> 本地配置源文件位于 `deploy/trial/`：`docker-compose.yml`、`setup.sh`、`nginx/shop.conf`、`nginx/frontend.conf`、`.env.example`。
> 修改后需重新打包上传，或直接在服务器上编辑对应文件并 `docker compose up -d --force-recreate`。
