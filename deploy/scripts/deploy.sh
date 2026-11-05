#!/usr/bin/env bash
set -e

# ========================================================
# JavaWeb Shop 方案一生产环境一键部署与更新脚本
# ========================================================

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$BASE_DIR"

echo "========================================================"
echo "🚀 开始执行 JavaWeb Shop 生产安全部署 (方案一: CF + Nginx + Docker)"
echo "========================================================"

# 1. 检查 Docker 环境
if ! command -v docker &> /dev/null; then
    echo "❌ 错误: 未检测到 Docker，请先安装 Docker: https://docs.docker.com/engine/install/"
    exit 1
fi

# 2. 准备 .env 环境变量
if [ ! -f .env ]; then
    echo "⚠️  未检测到 .env 生产环境变量文件，正在从 .env.example 自动生成..."
    cp .env.example .env
    echo "✅ 已生成 .env 文件，建议稍后使用文本编辑器修改其中的默认强密码！"
fi

# 3. 准备 SSL 证书占位符 (防止首次启动 Nginx 因缺失证书而退出)
SSL_DIR="$BASE_DIR/nginx/ssl"
mkdir -p "$SSL_DIR"

if [ ! -f "$SSL_DIR/cert.pem" ] || [ ! -f "$SSL_DIR/key.pem" ]; then
    echo "⚠️  未检测到正式 SSL 证书，正在生成临时自签名证书以确保 Nginx 正常拉起..."
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
        -keyout "$SSL_DIR/key.pem" \
        -out "$SSL_DIR/cert.pem" \
        -subj "/C=CN/ST=Beijing/L=Beijing/O=Shop/CN=localhost" \
        2>/dev/null
    echo "✅ 临时 SSL 证书生成完成。若已绑定域名，请参考 nginx/ssl/README.md 替换为 Cloudflare 官方证书！"
fi

# 4. 构建并启动容器编排集群
echo "📦 正在拉取基础设施镜像并编译构建工程..."
docker compose -f docker-compose.prod.yml build

echo "🔄 启动全栈隔离容器集群..."
docker compose -f docker-compose.prod.yml up -d

# 5. 校验运行状态
echo "⏳ 等待各服务就绪..."
sleep 5

docker compose -f docker-compose.prod.yml ps

echo "========================================================"
echo "🎉 部署完成！"
echo "🌐 访问入口: https://<你的服务器IP或域名>"
echo "🔒 安全说明: 3306(MySQL) 和 6379(Redis) 已完全收敛于内网，公网仅开放 80/443"
echo "📜 查看后端实时日志: docker compose -f deploy/docker-compose.prod.yml logs -f shop-backend"
echo "========================================================"
