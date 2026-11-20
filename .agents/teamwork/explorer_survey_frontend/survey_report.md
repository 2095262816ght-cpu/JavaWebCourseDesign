# Frontend, Root Scripts & DevOps Technical Survey Report

**Survey Target**: Frontend Codebase, Root Automation Scripts, Docker & Deployment Configurations  
**Working Directory**: `D:\JavaWeb\.agents\teamwork\explorer_survey_frontend\`  
**Date & UTC Timestamp**: 2026-11-05T07:25:00Z  
**Surveyor**: teamwork_explorer (`explorer_survey_frontend`)

---

## Executive Summary

A comprehensive investigation of `D:\JavaWeb` was conducted across three technical domains:
1. **Frontend Architecture (`frontend/`)**: Vue 3.5.12 + TypeScript 5.6.3 + Vite 5.4.9 + Pinia 2.2.4 + Element Plus 2.8.5. Tested with Vitest 2.1.9 (15 passing tests across 3 suites). Verified clean compilation via `npm run build` (`vue-tsc && vite build`).
2. **Root Automation Scripts (`start.bat`, `start.ps1`, `stop.bat`, `stop.ps1`, `docker-compose.yml`)**: Windows Batch and PowerShell scripts orchestrating Redis checks (WSL/Docker fallback on port 6379), MySQL health assertions (port 3306), Spring Boot launch (port 8080), and Vite frontend launch (port 5173). Stop scripts precisely terminate processes on ports 8080 and 5173 while leaving data containers intact.
3. **DevOps & Production Deployment (`deploy/`, Dockerfiles, Nginx)**: Multi-stage Dockerfiles for Backend (`maven:3.9.9-eclipse-temurin-21-alpine` -> `eclipse-temurin:21-jre-alpine` non-root `appuser`) and Frontend (`node:20-alpine` -> `nginx:1.27-alpine`). Production compose file (`deploy/docker-compose.prod.yml`) isolates 5 containers into internal bridge network `shop-internal-network` with zero external ports exposed except edge gateway Nginx on ports `80` and `443` with Cloudflare real-IP restoration, HSTS, rate limiting (`limit_req`), and static file zero-copy bypass for `/uploads/`.

**Key Finding / High-Severity Defect Identified**:
A critical URL path inconsistency exists in `frontend/src/api/product.ts` and `frontend/src/views/admin/ProductManagementView.vue`, where API calls omit the `/api` prefix (calling `/portal/...`, `/admin/...`, and `/common/upload`). While backend controllers are mapped to `/api/portal`, `/api/admin/products`, and `/api/common`, and both Vite dev proxy and production Nginx only proxy `/api/` and `/uploads/` to the backend, these requests fail or return HTML fallbacks. All other API modules (`auth.ts`, `brand.ts`, `cart.ts`, `category.ts`, `order.ts`, `user.ts`) correctly use `/api/...`.

---

## 1. Frontend Codebase Detailed Survey

### 1.1 Project Structure & Core Packages
- **Path**: `D:\JavaWeb\frontend`
- **Package Manager**: `npm` (governed by `package-lock.json`; lockfile version 3, 87 KB). No Yarn or pnpm lockfiles exist.
- **Node.js Compatibility**:
  - `frontend/Dockerfile` uses `node:20-alpine`.
  - `README.md` documents Node.js 18.0+ or 20.0+.
  - `package.json` contains no explicit `engines` restriction.
- **Manifest (`package.json`)**:
  - Name: `shop-frontend`, Version: `1.0.0`, Type: `module` (ESM).
  - Scripts:
    - `"dev": "vite"`
    - `"build": "vue-tsc && vite build"`
    - `"preview": "vite preview"`
    - `"test": "vitest run"`
  - Runtime Dependencies:
    - `vue`: `^3.5.12`
    - `vue-router`: `^4.4.5`
    - `pinia`: `^2.2.4`
    - `element-plus`: `^2.8.5`
    - `@element-plus/icons-vue`: `^2.3.1`
    - `axios`: `^1.7.7`
  - Dev Dependencies:
    - `vite`: `^5.4.9`
    - `@vitejs/plugin-vue`: `^5.1.4`
    - `typescript`: `^5.6.3`
    - `vue-tsc`: `^2.1.6`
    - `vitest`: `^2.1.9`
    - `happy-dom`: `^21.0.4`
    - `@types/node`: `^22.7.5`

### 1.2 Build & Dev Server Configuration (`vite.config.ts`)
- **Port**: `5173`
- **Path Alias**: `@` resolves to `D:\JavaWeb\frontend\src`
- **Test Runner Config**: Vitest configured with `globals: true` and `environment: 'happy-dom'`
- **Dev Server Proxies**:
  ```ts
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
  ```

### 1.3 Environment Variables
- In code: `import.meta.env.VITE_API_BASE_URL` is referenced in `frontend/src/api/request.ts` (line 12) and `frontend/src/views/admin/BrandManagementView.vue` (line 273).
- In filesystem: No `.env`, `.env.development`, or `.env.production` file exists in `frontend/`.
- Behavior: When `VITE_API_BASE_URL` is undefined, `baseURL` falls back to `''` (relative URL), which works seamlessly with Vite dev proxy in local development and Nginx proxy in Docker. However, lack of `.env.example` in `frontend/` leaves external configurations undocumented.

### 1.4 State Management & Authentication Flow
- Stores (`frontend/src/stores/`):
  - `index.ts`: Pinia instance initialization.
  - `user.ts` (`useUserStore`):
    - Reactive state: `token`, `user` (profile).
    - Computed: `isLoggedIn`, `isAdmin` (checks `user.value?.role === 'ROLE_ADMIN'`), `displayName`, `userAvatar`.
    - Persistence: syncs `token` and `user` to `localStorage`.
    - Actions: `login`, `register`, `fetchProfile`, `updateProfile`, `logout`, `clearAuth`.
  - `cart.ts` (`useCartStore`):
    - Reactive state: `cartSummary` (items, totalItems, checkedCount, totalAmount).
    - Computed: `cartCount`, `checkedCount`, `totalAmount`, `isAllChecked`.
    - Persistence: caches cart state to `localStorage` under key `shop_cart_cache`.
- Axios Interceptor (`frontend/src/api/request.ts`):
  - Request: extracts `token` directly from `localStorage.getItem('token')` and attaches header `config.headers['satoken'] = token`.
  - Response: Unwraps `ApiResponse<T> = { code, message, data, timestamp }`. If `code === 200`, returns `res.data`. If `code === 401`, clears `token` and `user` from `localStorage` and rejects with error.

### 1.5 Frontend Router Architecture (`frontend/src/router/index.ts`)
- Dual-Portal Layout:
  1. **Customer Storefront (`PortalLayout.vue`)**:
     - `/` (Home) -> `HomeView.vue`
     - `/products` (ProductList) -> `ProductListView.vue`
     - `/product/:id` (ProductDetail) -> `ProductDetailView.vue`
     - `/cart` (Cart, requiresAuth) -> `CartView.vue`
     - `/orders` (Orders, requiresAuth) -> `OrderHistoryView.vue`
  2. **Auth Surface**:
     - `/login` (Login) -> `LoginView.vue` (unified login and registration tabs)
  3. **Admin Control Console (`AdminLayout.vue`, requiresAdmin)**:
     - `/admin` (AdminDashboard) -> `DashboardView.vue`
     - `/admin/users` (AdminUsers) -> `UserManagementView.vue`
     - `/admin/brands` (AdminBrands) -> `BrandManagementView.vue`
     - `/admin/categories` (AdminCategories) -> `CategoryManagementView.vue`
     - `/admin/products` (AdminProducts) -> `ProductManagementView.vue`
     - `/admin/orders` (AdminOrders) -> `OrderManagementView.vue`
- Router Navigation Guards:
  - If authenticated user navigates to `/login`, redirects to `/admin` (for admin) or `/` (for customer).
  - Protected routes (`requiresAuth`): unauthenticated visitors are redirected to `/login?redirect=...`.
  - Admin routes (`requiresAdmin`): unauthenticated visitors redirected to `/login`; logged-in customers without `ROLE_ADMIN` receive error toast and are redirected to `/`.

### 1.6 Verification of Frontend Build & Tests
- `npm test`: Ran Vitest 2.1.9 across all 3 test suites (`cart.spec.ts`, `request.spec.ts`, `router.spec.ts`).
  - **Result**: 15 tests passed (100% success rate, duration ~3.32s).
- `npm run build`: Ran `vue-tsc && vite build`.
  - **Result**: Successfully transformed 1720 modules and built production bundle in 10.93s (`dist/index.html` 0.47 kB, CSS 399 kB, JS 1,376 kB).

---

## 2. Root Automation Scripts Survey

### 2.1 `start.bat`
- **Location**: `D:\JavaWeb\start.bat` (UTF-8, 67 lines)
- **Execution Flow**:
  1. Checks port 6379 via `netstat -ano | findstr ":6379 " | findstr "LISTENING"`.
     - If absent: checks `where wsl`. If WSL found, attempts `wsl -u root -d Ubuntu-22.04 service redis-server start`. Otherwise executes `docker compose up -d redis`.
  2. Checks port 3306 via `netstat -ano | findstr ":3306 " | findstr "LISTENING"`. Warns if not listening.
  3. Launches Backend: `start "JavaWeb 后端服务 (Spring Boot: 8080)" cmd /k "cd /d %~dp0backend && title [后端] Spring Boot 8080 && mvn spring-boot:run"`
  4. Launches Frontend: `start "JavaWeb 前端工程 (Vite: 5173)" cmd /k "cd /d %~dp0frontend && title [前端] Vite Vue3 5173 && npm run dev"`
  5. Displays service summary and default test accounts (`admin/admin123`, `customer/123456`). Waits 6 seconds (`timeout /t 6`) and opens `http://localhost:5173` in default browser.

### 2.2 `start.ps1`
- **Location**: `D:\JavaWeb\start.ps1` (UTF-8, 73 lines)
- **Execution Flow**:
  - Exact PowerShell equivalent of `start.bat`.
  - Uses `Get-NetTCPConnection` to inspect ports 6379 and 3306.
  - Spawns backend and frontend processes in separate console windows with titles `[后端] Spring Boot 8080` and `[前端] Vite Vue3 5173`.
  - Sleeps 6 seconds and calls `Start-Process "http://localhost:5173"`.

### 2.3 `stop.bat`
- **Location**: `D:\JavaWeb\stop.bat` (UTF-8, 26 lines)
- **Execution Flow**:
  - Searches for listening PID on port 8080 (`netstat -ano | findstr ":8080 " | findstr "LISTENING"`) and kills it with `taskkill /F /PID %%a`.
  - Searches for listening PID on port 5173 (`netstat -ano | findstr ":5173 " | findstr "LISTENING"`) and kills it with `taskkill /F /PID %%a`.
  - Does NOT terminate Redis or MySQL, ensuring persistence services remain ready for next startup.

### 2.4 `stop.ps1`
- **Location**: `D:\JavaWeb\stop.ps1` (UTF-8, 33 lines)
- **Execution Flow**:
  - Implements `Stop-PortProcess([int]$port, [string]$name)`.
  - Terminates owning processes on ports 8080 and 5173 via `Stop-Process -Id $pidToKill -Force`.

### 2.5 Root `docker-compose.yml`
- **Location**: `D:\JavaWeb\docker-compose.yml` (16 lines)
- **Purpose**: Dev-environment fallback helper for Redis.
- **Service**: `redis` (`redis:7.2-alpine`), container `shop-redis`, port mapping `"6379:6379"`, volume `redis_data:/data`.

---

## 3. DevOps & Container Deployment Survey

### 3.1 Multi-Stage Dockerfiles
1. **Backend Dockerfile (`backend/Dockerfile`)**:
   - Stage 1 (`builder`): `maven:3.9.9-eclipse-temurin-21-alpine`. Executes `mvn dependency:go-offline -B` for dependency layer caching, then `mvn clean package -DskipTests -B`.
   - Stage 2 (`runner`): `eclipse-temurin:21-jre-alpine`.
     - Sets timezone to `Asia/Shanghai`.
     - Security hardening: creates non-privileged user `appuser:appgroup` (UID/GID 10001).
     - Storage directories: `/app/uploads` and `/app/logs` owned by `appuser`.
     - Port: `EXPOSE 8080` (only consumed within Docker network).
     - Healthcheck: `curl -f http://localhost:8080/v3/api-docs || exit 1` (`interval: 30s`, `start-period: 40s`).
     - Entrypoint: `["java", "-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=prod", "-jar", "/app/app.jar"]`.
2. **Frontend Dockerfile (`frontend/Dockerfile`)**:
   - Stage 1 (`builder`): `node:20-alpine`. Caches `package*.json`, runs `npm install`, copies source, runs `npm run build`.
   - Stage 2 (`runner`): `nginx:1.27-alpine`.
     - Copies `nginx.conf` -> `/etc/nginx/conf.d/default.conf`.
     - Copies `/app/dist` -> `/usr/share/nginx/html`.
     - `EXPOSE 80`. Runs `nginx -g "daemon off;"`.
3. **Frontend Nginx Config (`frontend/nginx.conf`)**:
   - Port 80 SPA server.
   - Gzip compression enabled (level 6).
   - SPA fallback: `try_files $uri $uri/ /index.html;`.
   - Long-term cache headers for assets: `expires 30d; add_header Cache-Control "public, no-transform";`.

### 3.2 Production Container Orchestration (`deploy/docker-compose.prod.yml`)
- **Isolation Architecture**:
  - Network: single internal bridge network `shop-internal-network`.
  - Volumes: `shop_prod_mysql_data`, `shop_prod_redis_data`, `shop_prod_uploads_data`, `shop_prod_logs_data`.
  - Services:
    1. `shop-mysql`: `mysql:8.4`. ZERO external ports. Seeded automatically via `../docs/sql/schema.sql` and `../docs/sql/data.sql`.
    2. `shop-redis`: `redis:7.2-alpine`. ZERO external ports. Secured via `--requirepass ${REDIS_PASSWORD}`.
    3. `shop-backend`: Built from `backend/Dockerfile`. ZERO external ports. Depends on healthy MySQL and Redis.
    4. `shop-frontend`: Built from `frontend/Dockerfile`. ZERO external ports.
    5. `shop-nginx`: `nginx:1.27-alpine`. THE ONLY EXPOSED SERVICE: `"80:80"` and `"443:443"`.

### 3.3 Production Edge Gateway (`deploy/nginx/`)
- **`deploy/nginx/nginx.conf`**:
  - `server_tokens off;`
  - Cloudflare Real IP Restoration: sets 15 IPv4 CIDRs, 7 IPv6 CIDRs, plus private Docker bridge ranges. Reads `real_ip_header CF-Connecting-IP;` with `real_ip_recursive on;`.
  - Rate Limiting Pools:
    - `zone=login_limit:10m rate=5r/s` (login/register bruteforce defense).
    - `zone=api_limit:20m rate=30r/s` (general API DoS/crawler defense).
    - Over-limit HTTP status: `429 Too Many Requests`.
- **`deploy/nginx/conf.d/shop.conf`**:
  - Port 80: HTTP 301 unconditional permanent redirect to HTTPS (`https://$host$request_uri`).
  - Port 443: HTTPS + HTTP/2. Configured for `/etc/nginx/ssl/cert.pem` and `/etc/nginx/ssl/key.pem`.
  - Security Headers: HSTS (`Strict-Transport-Security "max-age=31536000; includeSubDomains; preload"`), `X-Content-Type-Options: nosniff`, `X-Frame-Options: SAMEORIGIN`, `X-XSS-Protection: 1; mode=block`, `Referrer-Policy: strict-origin-when-cross-origin`.
  - Route Handlers:
    - `^/api/auth/(login|register)$`: `zone=login_limit burst=5 nodelay; proxy_pass http://shop-backend:8080;`
    - `/api/`: `zone=api_limit burst=20 nodelay; proxy_pass http://shop-backend:8080/api/;`
    - `/uploads/`: Direct zero-copy disk alias `alias /usr/share/nginx/uploads/; expires 30d;` (bypasses Spring Boot).
    - `/`: Vue 3 SPA proxy `proxy_pass http://shop-frontend:80;`
- **SSL Guidelines & Deploy Scripts**:
  - `deploy/nginx/ssl/README.md`: Explains Cloudflare 15-year Origin CA certificate setup and local self-signed fallback.
  - `deploy/scripts/deploy.sh`: Auto-generates `.env` and temporary self-signed SSL cert if absent, then triggers compose build and up.
  - `deploy/scripts/deploy.bat`: Windows equivalent for local production simulation.

---

## 4. Discrepancy Matrix: Codebase vs. Documentation (`README.md` & `docs/`)

The following matrix documents every observed discrepancy, its exact locations, technical impact, and recommendation:

| # | Domain | Documented Description | Actual Codebase Implementation | Technical Severity & Impact | Recommended Action |
|---|---|---|---|---|---|
| **D1** | **Frontend API URLs** | All docs & backend specs declare endpoints under `/api/...` (`/api/portal/home`, `/api/portal/products`, `/api/admin/products`, `/api/common/upload`). | In `frontend/src/api/product.ts` (lines 125, 132, 139, 148, 155, 162, 169, 176, 185), endpoints omit `/api` (`/portal/home`, `/portal/products`, `/admin/products`). In `frontend/src/views/admin/ProductManagementView.vue` (lines 631, 648), image upload calls `/common/upload`. | **High (Defect)**: Vite dev proxy and production Nginx ONLY proxy `/api/` and `/uploads/` to backend. Requests to `/portal` and `/admin` fail with 404 or return SPA HTML fallback. | Fix `frontend/src/api/product.ts` and `ProductManagementView.vue` to prepend `/api`, aligning with backend controllers and all other frontend API modules. |
| **D2** | **Pinia Store Naming** | `README.md` (line 223) lists `stores/ (auth, cart)`. `docs/principles/02-auth-and-satoken.md` (line 257) & `06-vue3-and-vite-modern-frontend.md` (line 191) describe interceptor pulling JWT from `useAuthStore`. | `frontend/src/stores/` has `user.ts` (exporting `useUserStore`) and `cart.ts` (exporting `useCartStore`). There is NO `auth.ts` or `useAuthStore`. In `request.ts`, the interceptor reads `localStorage.getItem('token')` directly. | **Medium (Doc Drift)**: Inaccurate store naming and mechanism description in lecture notes and README. | Update `README.md` directory tree to list `(user, cart)` and update lectures 02 and 06 to describe `useUserStore` and `localStorage` direct read. |
| **D3** | **WSL Distribution Hardcoding in Start Scripts** | `README.md` (line 58) states: "自动检测并启动 Redis（WSL 或 Docker 环境）". | `start.bat` (line 16) & `start.ps1` (line 16) execute `wsl -u root -d Ubuntu-22.04 service redis-server start`. If WSL is installed with a differently named distro (e.g. `Ubuntu`, `Ubuntu-24.04`, `Debian`), `where wsl` succeeds, but the command fails without falling back to Docker Compose. | **Low (Script Brittleness)**: Users with standard `Ubuntu` or non-`Ubuntu-22.04` WSL distros will see failure to auto-start Redis. | Enhance scripts to check default distro or attempt Docker if WSL command returns error; document prerequisite in README. |
| **D4** | **Frontend Environment Template** | `frontend/src/api/request.ts` reads `import.meta.env.VITE_API_BASE_URL`. | No `.env*` file exists in `frontend/`. (The only `.env.example` in repo is `deploy/.env.example`). | **Low (Config Clarity)**: While default `''` works for relative proxy, developers configuring external backend endpoints have no template. | Add `frontend/.env.example` defining `VITE_API_BASE_URL=` with explanatory comments. |
| **D5** | **ADR 0003 Lecture Count** | `docs/adr/0003-infrastructure-and-educational-artifacts.md` (line 9) states: "covering 6 core topics". | `docs/principles/` actually contains 9 documents (`01` through `09`). | **Low (Doc Drift)**: Historical ADR was written when only 6 lectures existed. | Update ADR 0003 to reflect the expanded 9-lecture suite (including defense Q&A and production deployment). |
| **D6** | **Frontend Scripts in package.json** | `README.md` mentions modern code standards and linting. | `frontend/package.json` only contains `dev`, `build`, `preview`, `test`. It has no `lint` or `format` script. | **Low (Doc Completeness)**: Clarify script availability in README. | Document exact npm scripts (`dev`, `build`, `preview`, `test`) in README. |

---

## 5. Verification Commands for Independent Reproduction

To independently verify all findings in this survey report:

1. **Frontend Tests**:
   ```powershell
   npm --prefix frontend test
   ```
   *Expected*: 3 passed suites, 15 passed tests.
2. **Frontend Type Check & Build**:
   ```powershell
   npm --prefix frontend run build
   ```
   *Expected*: `vue-tsc` succeeds, `vite build` outputs production assets in `frontend/dist/`.
3. **Verify API Endpoint Inconsistency (Code Search)**:
   ```powershell
   # Observe missing /api in product.ts:
   rg -n "'/portal|" frontend/src/api/product.ts
   rg -n "'/admin|" frontend/src/api/product.ts
   # Observe missing /api in ProductManagementView upload:
   rg -n "'/common/upload'" frontend/src/views/admin/ProductManagementView.vue
   # Observe all backend controllers have /api prefix:
   rg -n "@RequestMapping" backend/src/main/java/com/shop
   ```
4. **Inspect Scripts and Compose Files**:
   - `start.bat` / `start.ps1`
   - `stop.bat` / `stop.ps1`
   - `deploy/docker-compose.prod.yml`
   - `deploy/nginx/conf.d/shop.conf`
