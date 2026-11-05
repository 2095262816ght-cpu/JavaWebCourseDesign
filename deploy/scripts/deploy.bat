@echo off
chcp 65001 >nul
echo ========================================================
echo 🚀 开始执行 JavaWeb Shop 方案一本地生产容器化演练
echo ========================================================

cd /d "%~dp0\.."

REM 1. 检查 .env 文件
if not exist ".env" (
    echo ⚠️  正在从 .env.example 生成 .env 文件...
    copy .env.example .env >nul
    echo ✅ 已生成 .env 文件
)

REM 2. 检查 SSL 证书
if not exist "nginx\ssl\cert.pem" (
    echo ⚠️  检测到缺少 SSL 证书，请确保 nginx\ssl\ 目录下存在 cert.pem 和 key.pem
    echo 提示: 可参考 nginx\ssl\README.md 使用 openssl 生成临时证书
)

REM 3. 运行 Docker Compose
echo 📦 正在编译并启动容器集群...
docker compose -f docker-compose.prod.yml up -d --build

echo ========================================================
echo 🎉 容器集群已在后台启动！
docker compose -f docker-compose.prod.yml ps
echo ========================================================
pause
