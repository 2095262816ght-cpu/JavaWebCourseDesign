# JavaWeb 电商系统 - 一键启动 PowerShell 脚本
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host "          JavaWeb 电商系统 - 一键启动启动器           " -ForegroundColor Cyan
Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host ""

# 1. 检查 Redis (端口 6379)
Write-Host "[1/4] 检查 Redis 缓存服务 (端口 6379)..." -ForegroundColor Yellow
$redisActive = Get-NetTCPConnection -LocalPort 6379 -State Listen -ErrorAction SilentlyContinue
if (-not $redisActive) {
    Write-Host "[提示] Redis 未在 6379 端口运行，正在尝试启动..." -ForegroundColor DarkYellow
    if (Get-Command wsl -ErrorAction SilentlyContinue) {
        # 以 daemonize no 前台常驻方式运行，防止命令结束后 WSL 发行版自动关闭把 Redis 一起带走
        Start-Process -FilePath "wsl.exe" `
            -ArgumentList "-d", "Ubuntu-22.04", "-u", "root", "redis-server", "/etc/redis/redis.conf", "--daemonize", "no" `
            -WindowStyle Hidden
    } elseif (Get-Command docker -ErrorAction SilentlyContinue) {
        # 试用编排里的 shop-redis 不映射宿主端口且强制密码，本地开发需要独立的无密码 Redis
        $devRedis = docker ps -a --filter 'name=^shop-dev-redis$' --format '{{.Names}}'
        if ($devRedis) {
            docker start shop-dev-redis | Out-Null
        } else {
            docker run -d --name shop-dev-redis -p 6379:6379 redis:7.2-alpine | Out-Null
        }
    } else {
        Write-Host "[警告] 未检测到 WSL 或 Docker，无法自动启动 Redis。" -ForegroundColor Red
    }

    for ($i = 0; $i -lt 30; $i++) {
        Start-Sleep -Seconds 2
        $redisActive = Get-NetTCPConnection -LocalPort 6379 -State Listen -ErrorAction SilentlyContinue
        if ($redisActive) { break }
    }

    if ($redisActive) {
        Write-Host "[成功] Redis 已启动并在 6379 端口就绪！" -ForegroundColor Green
    } else {
        Write-Host "[警告] 未能自动启动 Redis：登录、购物车等需要鉴权的接口会报错，请先手动启动 Redis 后重试。" -ForegroundColor Red
    }
} else {
    Write-Host "[成功] Redis 缓存服务已就绪！" -ForegroundColor Green
}

Write-Host ""

# 2. 检查 MySQL (端口 3306)
Write-Host "[2/4] 检查 MySQL 数据库服务 (端口 3306)..." -ForegroundColor Yellow
$mysqlActive = Get-NetTCPConnection -LocalPort 3306 -State Listen -ErrorAction SilentlyContinue
if (-not $mysqlActive) {
    Write-Host "[警告] 端口 3306 未检测到 MySQL 服务，请确认本地 MySQL 服务已启动！" -ForegroundColor Red
} else {
    Write-Host "[成功] MySQL 数据库服务已就绪！" -ForegroundColor Green
}

Write-Host ""

# 3. 启动 Spring Boot 后端
Write-Host "[3/4] 正在启动 Spring Boot 后端服务 (端口 8080)..." -ForegroundColor Yellow
$backendPath = Join-Path $PSScriptRoot "backend"
Start-Process cmd.exe -ArgumentList "/k cd /d `"$backendPath`" && title [后端] Spring Boot 8080 && mvn spring-boot:run"

Write-Host ""

# 4. 启动 Vue 3 + Vite 前端
Write-Host "[4/4] 正在启动 Vue 3 + Vite 前端工程 (端口 5173)..." -ForegroundColor Yellow
$frontendPath = Join-Path $PSScriptRoot "frontend"
Start-Process cmd.exe -ArgumentList "/k cd /d `"$frontendPath`" && title [前端] Vite Vue3 5173 && npm run dev"

Write-Host ""
Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host "               🎉 服务启动指令已全部派发！             " -ForegroundColor Green
Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host " - 前台商城页面:       http://localhost:5173" -ForegroundColor White
Write-Host " - 后台管理系统:       http://localhost:5173/admin" -ForegroundColor White
Write-Host " - 接口文档 (Swagger): http://localhost:8080/swagger-ui/index.html" -ForegroundColor White
Write-Host "-------------------------------------------------------" -ForegroundColor DarkGray
Write-Host " 默认测试账号：" -ForegroundColor Yellow
Write-Host "   - 平台管理员：admin    / ShopNest@2026#Adm1n" -ForegroundColor White
Write-Host "   - 示例顾客：  customer / 123456" -ForegroundColor White
Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host ""

# 5. 等待后端与前端真正就绪后再打开浏览器，避免首屏请求撞上启动窗口
function Wait-ServiceReady {
    param(
        [string]$Name,
        [string]$Url,
        [int]$MaxSeconds
    )
    Write-Host "正在等待$Name 就绪（最长 $MaxSeconds 秒）..." -ForegroundColor DarkGray
    $deadline = (Get-Date).AddSeconds($MaxSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $resp = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
            if ($resp.StatusCode -eq 200) {
                Write-Host "[成功] $Name 已就绪！" -ForegroundColor Green
                return $true
            }
        } catch {
            # 服务尚未就绪，继续轮询
        }
        Start-Sleep -Seconds 2
    }
    Write-Host "[警告] $Name 在 $MaxSeconds 秒内未就绪，请查看对应窗口的启动日志。" -ForegroundColor Red
    return $false
}

$backendReady = Wait-ServiceReady -Name "后端服务 (8080)" -Url "http://localhost:8080/api/portal/home" -MaxSeconds 180
$frontendReady = Wait-ServiceReady -Name "前端服务 (5173)" -Url "http://localhost:5173/" -MaxSeconds 120

if ($backendReady -and $frontendReady) {
    Write-Host "正在打开浏览器..." -ForegroundColor DarkGray
    Start-Process "http://localhost:5173"
}
