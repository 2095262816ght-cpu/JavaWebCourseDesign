@echo off
chcp 65001 >nul
title JavaWeb 电商项目 - 一键启动
echo =======================================================
echo          JavaWeb 电商系统 - 一键启动启动器
echo =======================================================
echo.

:: 1. 检查 Redis 状态
echo [1/4] 检查 Redis 缓存服务 (端口 6379)...
netstat -ano | findstr ":6379 " | findstr "LISTENING" >nul
if %errorlevel% neq 0 (
    echo [提示] 检测到 Redis 未在 6379 端口运行，正在尝试启动...
    where wsl >nul 2>nul
    if %errorlevel% equ 0 (
        wsl -u root -d Ubuntu-22.04 service redis-server start >nul 2>nul
    ) else (
        docker compose up -d redis >nul 2>nul
    )
    timeout /t 2 >nul
    netstat -ano | findstr ":6379 " | findstr "LISTENING" >nul
    if %errorlevel% neq 0 (
        echo [警告] 未能自动启动 Redis，请确认 WSL 或 Docker 中的 Redis 服务已开启。
    ) else (
        echo [成功] Redis 已启动并在 6379 端口就绪！
    )
) else (
    echo [成功] Redis 缓存服务已就绪！
)

echo.
:: 2. 检查 MySQL 状态
echo [2/4] 检查 MySQL 数据库服务 (端口 3306)...
netstat -ano | findstr ":3306 " | findstr "LISTENING" >nul
if %errorlevel% neq 0 (
    echo [警告] 端口 3306 未检测到 MySQL 服务，请确认本地 MySQL 服务已启动！
) else (
    echo [成功] MySQL 数据库服务已就绪！
)

echo.
:: 3. 启动 Spring Boot 后端服务
echo [3/4] 正在启动 Spring Boot 后端服务 (端口 8080)...
start "JavaWeb 后端服务 (Spring Boot: 8080)" cmd /k "cd /d %~dp0backend && title [后端] Spring Boot 8080 && mvn spring-boot:run"

echo.
:: 4. 启动 Vue 3 + Vite 前端工程
echo [4/4] 正在启动 Vue 3 + Vite 前端工程 (端口 5173)...
start "JavaWeb 前端工程 (Vite: 5173)" cmd /k "cd /d %~dp0frontend && title [前端] Vite Vue3 5173 && npm run dev"

echo.
echo =======================================================
echo               🎉 服务启动指令已全部派发！
echo =======================================================
echo  - 前台商城页面:       http://localhost:5173
echo  - 后台管理系统:       http://localhost:5173/admin
echo  - 接口文档 (Swagger): http://localhost:8080/swagger-ui/index.html
echo -------------------------------------------------------
echo  默认测试账号：
echo   - 平台管理员：admin    / admin123
echo   - 示例顾客：  customer / 123456
echo =======================================================
echo.
echo 正在等待服务启动并自动打开浏览器...
timeout /t 6 >nul
start http://localhost:5173
