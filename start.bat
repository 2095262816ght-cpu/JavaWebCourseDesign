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
if %errorlevel% equ 0 goto REDIS_READY

echo [提示] 检测到 Redis 未在 6379 端口运行，正在尝试启动...
where wsl >nul 2>nul
if %errorlevel% equ 0 goto REDIS_START_WSL
where docker >nul 2>nul
if %errorlevel% equ 0 goto REDIS_START_DOCKER
echo [警告] 未检测到 WSL 或 Docker，无法自动启动 Redis。
goto REDIS_WAIT

:REDIS_START_WSL
:: 以 --daemonize no 前台常驻方式运行，防止命令结束后 WSL 发行版自动关闭把 Redis 一起带走
start "JavaWeb Redis (WSL)" /min wsl -d Ubuntu-22.04 -u root redis-server /etc/redis/redis.conf --daemonize no
goto REDIS_WAIT

:REDIS_START_DOCKER
:: 试用编排里的 shop-redis 不映射宿主端口且强制密码，本地开发需要独立的无密码 Redis
docker ps -a --filter "name=^shop-dev-redis$" --format "{{.Names}}" | findstr /i "shop-dev-redis" >nul 2>nul
if %errorlevel% equ 0 (
    docker start shop-dev-redis >nul 2>nul
) else (
    docker run -d --name shop-dev-redis -p 6379:6379 redis:7.2-alpine >nul 2>nul
)
goto REDIS_WAIT

:REDIS_WAIT
set /a REDIS_WAIT_COUNT=0
:REDIS_WAIT_LOOP
netstat -ano | findstr ":6379 " | findstr "LISTENING" >nul
if %errorlevel% equ 0 goto REDIS_READY
set /a REDIS_WAIT_COUNT+=1
if %REDIS_WAIT_COUNT% geq 30 goto REDIS_TIMEOUT
timeout /t 2 >nul
goto REDIS_WAIT_LOOP

:REDIS_TIMEOUT
echo [警告] 未能自动启动 Redis：登录、购物车等需要鉴权的接口会报错，请先手动启动 Redis 后重试。
goto REDIS_DONE

:REDIS_READY
echo [成功] Redis 缓存服务已就绪！

:REDIS_DONE

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
echo   - 平台管理员：admin    / ShopNest@2026#Adm1n
echo   - 示例顾客：  customer / 123456
echo =======================================================
echo.
echo 正在等待后端服务就绪（最长约 180 秒）...
set /a BACKEND_WAIT=0
:WAIT_BACKEND
set BACKEND_CODE=0
for /f %%C in ('powershell -NoProfile -Command "try{(Invoke-WebRequest -Uri 'http://localhost:8080/api/portal/home' -UseBasicParsing -TimeoutSec 3).StatusCode}catch{0}"') do set BACKEND_CODE=%%C
if "%BACKEND_CODE%"=="200" goto BACKEND_READY
set /a BACKEND_WAIT+=1
if %BACKEND_WAIT% geq 90 goto BACKEND_TIMEOUT
timeout /t 2 >nul
goto WAIT_BACKEND
:BACKEND_READY
echo [成功] 后端接口已就绪！

echo.
echo 正在等待前端开发服务器就绪（最长约 120 秒）...
set /a FRONTEND_WAIT=0
:WAIT_FRONTEND
set FRONTEND_CODE=0
for /f %%C in ('powershell -NoProfile -Command "try{(Invoke-WebRequest -Uri 'http://localhost:5173/' -UseBasicParsing -TimeoutSec 3).StatusCode}catch{0}"') do set FRONTEND_CODE=%%C
if "%FRONTEND_CODE%"=="200" goto FRONTEND_READY
set /a FRONTEND_WAIT+=1
if %FRONTEND_WAIT% geq 60 goto FRONTEND_TIMEOUT
timeout /t 2 >nul
goto WAIT_FRONTEND
:FRONTEND_READY
echo [成功] 前端页面已就绪，正在打开浏览器...
start http://localhost:5173
goto END

:BACKEND_TIMEOUT
echo [警告] 后端 180 秒内未就绪，请查看"后端"窗口中的启动日志后再手动打开浏览器。
pause
goto END

:FRONTEND_TIMEOUT
echo [警告] 前端 120 秒内未就绪，请查看"前端"窗口中的启动日志后再手动打开浏览器。
pause

:END
