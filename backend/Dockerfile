# ==========================================
# 阶段 1: Maven 源码构建
# ==========================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# 复制 pom.xml 先行缓存 Maven 依赖
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源码并执行编译打包 (跳过单元测试加快镜像构建)
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==========================================
# 阶段 2: 极简安全运行时镜像 (Temurin JRE 21 Alpine)
# ==========================================
FROM eclipse-temurin:21-jre-alpine AS runner

# 设置系统时区为东八区 (Asia/Shanghai)
RUN apk add --no-cache tzdata curl \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && apk del tzdata

# 安全加固：创建非特权专属运行用户 appuser (UID/GID 10001)
RUN addgroup -g 10001 appgroup \
    && adduser -u 10001 -G appgroup -s /bin/sh -D appuser

WORKDIR /app

# 创建数据挂载目录并赋权
RUN mkdir -p /app/uploads /app/logs \
    && chown -R appuser:appgroup /app

# 从构建阶段复制 jar 包
COPY --from=builder --chown=appuser:appgroup /build/target/shop-backend-1.0.0-SNAPSHOT.jar /app/app.jar

# 切换为非 root 用户执行
USER appuser

# 暴露后端应用端口 (仅在 Docker 内部网络被 Nginx 反向代理使用)
EXPOSE 8080

# 健康检查探针
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/v3/api-docs || exit 1

# 启动参数：启用生产 profile 与 /dev/urandom 熵池优化
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=prod", "-jar", "/app/app.jar"]
