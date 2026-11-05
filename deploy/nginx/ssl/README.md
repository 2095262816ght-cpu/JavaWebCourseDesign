# SSL 证书放置指南

本目录下存放 Nginx 开启 HTTPS 所需的证书密钥文件：

- `cert.pem`：公钥证书文件（包含完整证书链 Fullchain）
- `key.pem`：私钥密钥文件

---

### 获取方式推荐（二选一）：

#### 方式 A：配合 Cloudflare 免费 Origin CA 证书（推荐，最省心）
1. 登录 Cloudflare 控制台 -> 点击你的域名 -> 进入 **SSL/TLS** -> **源服务器 (Origin Server)**；
2. 点击 **创建证书 (Create Certificate)**，主机名填写 `*.yourdomain.com` 与 `yourdomain.com`，有效期选择 **15 年**；
3. 将生成的“源证书”内容保存为当前目录下的 `cert.pem`；
4. 将生成的“私钥”内容保存为当前目录下的 `key.pem`；
5. 在 Cloudflare 的 **SSL/TLS 概览** 中将加密模式设置为 **完全（严格）/ Full (Strict)**。
*(此证书享受 Cloudflare 15 年免续签保障，且 Cloudflare 与源站 Nginx 之间为原生加密连接)*

#### 方式 B：本地测试临时自签名证书（仅供无域名时本地演练）
在 Linux 或 Git Bash 终端中执行以下命令快速生成测试证书：
```bash
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout key.pem -out cert.pem \
  -subj "/C=CN/ST=Beijing/L=Beijing/O=Shop/CN=localhost"
```
