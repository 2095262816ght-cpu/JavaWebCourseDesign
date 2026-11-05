# ==========================================
# 阶段 1: Node 20 编译打包 Vue 3 + Vite 工程
# ==========================================
FROM node:20-alpine AS builder

WORKDIR /app

# 先复制 package.json 充分利用 Docker 构建缓存
COPY package*.json ./
RUN npm install

# 复制前端完整源码并执行构建
COPY . .
RUN npm run build

# ==========================================
# 阶段 2: 极简 Nginx 1.27 静态托管运行时
# ==========================================
FROM nginx:1.27-alpine

# 复制自定义 Nginx SPA 配置文件
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 从构建阶段提取产物到 Nginx 托管目录
COPY --from=builder /app/dist /usr/share/nginx/html

# 仅对外暴露容器内部 80 端口 (由外部网关进行反向代理)
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
