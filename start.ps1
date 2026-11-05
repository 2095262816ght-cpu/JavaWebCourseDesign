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
    $wslAvailable = Get-Command wsl -ErrorAction SilentlyContinue
    if ($wslAvailable) {
        wsl -u root -d Ubuntu-22.04 service redis-server start | Out-Null
    } else {
        docker compose up -d redis | Out-Null
    }
    Start-Sleep -Seconds 2
    $redisActive = Get-NetTCPConnection -LocalPort 6379 -State Listen -ErrorAction SilentlyContinue
    if ($redisActive) {
        Write-Host "[成功] Redis 已启动并在 6379 端口就绪！" -ForegroundColor Green
    } else {
        Write-Host "[警告] 未能自动启动 Redis，请确认 WSL 或 Docker 中的 Redis 服务已开启。" -ForegroundColor Red
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
Write-Host "   - 平台管理员：admin    / admin123" -ForegroundColor White
Write-Host "   - 示例顾客：  customer / 123456" -ForegroundColor White
Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "正在等待服务启动并自动打开浏览器..." -ForegroundColor DarkGray
Start-Sleep -Seconds 6
Start-Process "http://localhost:5173"
