#!/usr/bin/env bash
# ========================================================
# 试用服务器初始化脚本（Alibaba Cloud Linux 3）
# 用法：
#   bash setup.sh                          # 使用默认镜像源
#   bash setup.sh https://xxx.mirror.aliyuncs.com   # 指定镜像加速器
# ========================================================
set -euo pipefail

MIRROR="${1:-}"

if [ "$(id -u)" != "0" ]; then
  echo "请用 root 执行：bash setup.sh"
  exit 1
fi

echo "==> 1/5 安装基础工具"
dnf install -y dnf-plugins-core device-mapper-persistent-data lvm2 >/dev/null

echo "==> 2/5 添加 Docker 源（阿里云镜像站）"
dnf config-manager --add-repo https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo >/dev/null 2>&1 || true
sed -i 's#download.docker.com#mirrors.aliyun.com/docker-ce#g' /etc/yum.repos.d/docker-ce.repo 2>/dev/null || true

echo "==> 3/5 安装 Docker Engine 与 Compose 插件"
dnf install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

echo "==> 4/5 配置 Docker（镜像加速 + 日志轮转）"
mkdir -p /etc/docker
if [ -n "$MIRROR" ]; then
  cat > /etc/docker/daemon.json <<EOF
{
  "registry-mirrors": ["$MIRROR"],
  "log-driver": "json-file",
  "log-opts": { "max-size": "10m", "max-file": "3" }
}
EOF
else
  cat > /etc/docker/daemon.json <<'EOF'
{
  "log-driver": "json-file",
  "log-opts": { "max-size": "10m", "max-file": "3" }
}
EOF
fi

systemctl enable --now docker
systemctl restart docker

echo "==> 5/5 配置 2G Swap（4G 内存必备）"
if [ ! -f /swapfile ]; then
  dd if=/dev/zero of=/swapfile bs=1M count=2048 status=none
  chmod 600 /swapfile
  mkswap /swapfile >/dev/null
  swapon /swapfile
  grep -q '^/swapfile' /etc/fstab || echo '/swapfile swap swap defaults 0 0' >> /etc/fstab
fi
sysctl -w vm.swappiness=10 >/dev/null
grep -q '^vm.swappiness' /etc/sysctl.conf || echo 'vm.swappiness=10' >> /etc/sysctl.conf

echo
echo "===== 环境就绪 ====="
docker --version
docker compose version
free -h
df -h /
