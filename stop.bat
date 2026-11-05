@echo off
chcp 65001 >nul
title JavaWeb 电商项目 - 一键停止
echo =======================================================
echo          JavaWeb 电商系统 - 一键停止服务
echo =======================================================
echo.

echo 正在停止占用端口 8080 (后端 Spring Boot) 的进程...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080 " ^| findstr "LISTENING"') do (
    echo [关闭] 终止端口 8080 进程 PID: %%a
    taskkill /F /PID %%a >nul 2>nul
)

echo 正在停止占用端口 5173 (前端 Vite) 的进程...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173 " ^| findstr "LISTENING"') do (
    echo [关闭] 终止端口 5173 进程 PID: %%a
    taskkill /F /PID %%a >nul 2>nul
)

echo.
echo =======================================================
echo [成功] 所有前后台开发服务已完全停止！
echo =======================================================
timeout /t 3 >nul
