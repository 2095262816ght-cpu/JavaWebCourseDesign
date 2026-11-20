# Handoff Report — Frontend, Root Scripts & DevOps Technical Survey

**Agent**: `explorer_survey_frontend`  
**Working Directory**: `D:\JavaWeb\.agents\teamwork\explorer_survey_frontend\`  
**Date & UTC Timestamp**: 2026-11-05T07:26:00Z  
**Type**: Hard Handoff (Investigation Complete)  
**Parent Conversation ID**: `927daf49-0b06-498d-b694-b35b2efdd98b`

---

## 1. Observation

Direct, verbatim observations across codebase files, configurations, and verification runs:

1. **Frontend Project & Tooling (`frontend/package.json`)**:
   - `name`: `"shop-frontend"`, `version`: `"1.0.0"`, `type`: `"module"`.
   - Scripts: `"dev": "vite"`, `"build": "vue-tsc && vite build"`, `"preview": "vite preview"`, `"test": "vitest run"`.
   - Dependencies: `vue@^3.5.12`, `vue-router@^4.4.5`, `pinia@^2.2.4`, `element-plus@^2.8.5`, `@element-plus/icons-vue@^2.3.1`, `axios@^1.7.7`.
   - DevDependencies: `vite@^5.4.9`, `@vitejs/plugin-vue@^5.1.4`, `typescript@^5.6.3`, `vue-tsc@^2.1.6`, `vitest@^2.1.9`, `happy-dom@^21.0.4`.
   - Package manager: `package-lock.json` present (87 KB). No `yarn.lock` or `pnpm-lock.yaml`.

2. **Vite Dev Server & Proxy (`frontend/vite.config.ts`)**:
   - Port: `5173`.
   - Proxy definitions (lines 20-29):
     ```ts
     proxy: {
       '/api': { target: 'http://localhost:8080', changeOrigin: true },
       '/uploads': { target: 'http://localhost:8080', changeOrigin: true }
     }
     ```

3. **Frontend Tests & Build Verification**:
   - `npm --prefix frontend test`: Vitest ran `test/cart.spec.ts` (4 tests), `test/request.spec.ts` (4 tests), `test/router.spec.ts` (7 tests). Result: **15 passed, 0 failed** in 3.32s.
   - `npm --prefix frontend run build`: `vue-tsc && vite build` succeeded in 10.93s, generating `dist/index.html` (0.47 kB), `dist/assets/index-Dxv2eeUt.css` (399.48 kB), `dist/assets/index-DwKhQgi4.js` (1,376.14 kB).

4. **Frontend API URL Discrepancy (`frontend/src/api/product.ts` vs Backend Controllers)**:
   - In `backend/src/main/java/com/shop/modules/catalog/controller/PortalProductController.java` line 22:
     `@RequestMapping("/api/portal")`
   - In `backend/src/main/java/com/shop/modules/catalog/controller/AdminProductController.java` line 28:
     `@RequestMapping("/api/admin/products")`
   - In `backend/src/main/java/com/shop/common/controller/CommonUploadController.java` line 16:
     `@RequestMapping("/api/common")`
   - In `frontend/src/api/product.ts`:
     - Line 125: `return request.get<any, HomeShowcase>('/portal/home')`
     - Line 132: `return request.get<any, PageResult<ProductCard>>('/portal/products', { params })`
     - Line 139: `return request.get<any, ProductDetail>('/portal/products/${id}')`
     - Line 148: `return request.get<any, PageResult<ProductAdminVO>>('/admin/products', { params })`
     - Line 155: `return request.get<any, ProductAdminVO>('/admin/products/${id}')`
     - Line 162: `return request.post<any, number>('/admin/products', data)`
     - Line 169: `return request.put<any, void>('/admin/products/${id}', data)`
     - Line 176: `return request.patch<any, void>('/admin/products/${id}/status', null, { params: { status } })`
     - Line 185: `return request.delete<any, void>('/admin/products/${id}')`
   - In `frontend/src/views/admin/ProductManagementView.vue` lines 631 & 648:
     `const res = await request.post<any, any>('/common/upload', form, ...)`
   - In contrast, `frontend/src/views/admin/BrandManagementView.vue` line 274:
     `return '${baseUrl}/api/common/upload'`
   - And all other API modules (`auth.ts`, `brand.ts`, `cart.ts`, `category.ts`, `order.ts`, `user.ts`) explicitly invoke `/api/...`.

5. **Root Scripts Inspection**:
   - `start.bat` & `start.ps1`:
     - Redis check (port 6379): tests `where wsl`, tries `wsl -u root -d Ubuntu-22.04 service redis-server start`. If `wsl` not found, falls back to `docker compose up -d redis`.
     - MySQL check (port 3306): warns if port 3306 is not listening.
     - Spawns backend on port 8080 (`mvn spring-boot:run` in `backend`).
     - Spawns frontend on port 5173 (`npm run dev` in `frontend`).
     - Waits 6s and opens default browser to `http://localhost:5173`.
   - `stop.bat` & `stop.ps1`:
     - Kills listening PID on port 8080 (`taskkill /F /PID %%a` or `Stop-Process -Id $pidToKill -Force`).
     - Kills listening PID on port 5173.
     - Leaves Redis and MySQL undisturbed.
   - Root `docker-compose.yml`:
     - Service `redis:7.2-alpine`, port mapping `"6379:6379"`, volume `redis_data:/data`.

6. **Deploy & Production Docker Inspection (`deploy/`)**:
   - `deploy/docker-compose.prod.yml`:
     - 5 services: `shop-mysql` (`mysql:8.4`), `shop-redis` (`redis:7.2-alpine`), `shop-backend` (`backend/Dockerfile`), `shop-frontend` (`frontend/Dockerfile`), `shop-nginx` (`nginx:1.27-alpine`).
     - Port exposure: 0 ports exposed on MySQL, Redis, backend, or frontend. Only `shop-nginx` exposes `"80:80"` and `"443:443"`.
     - Data volume mounts: `mysql_data`, `redis_data`, `uploads_data`, `logs_data`.
     - Direct SQL initialization: `../docs/sql/schema.sql` and `../docs/sql/data.sql` mounted into `/docker-entrypoint-initdb.d/`.
   - `backend/Dockerfile`:
     - Multi-stage: `maven:3.9.9-eclipse-temurin-21-alpine` -> `eclipse-temurin:21-jre-alpine`. Non-root `appuser` (UID 10001). Healthcheck calls `curl -f http://localhost:8080/v3/api-docs`.
   - `frontend/Dockerfile`:
     - Multi-stage: `node:20-alpine` (`npm run build`) -> `nginx:1.27-alpine`.
   - `deploy/nginx/nginx.conf`:
     - Configures Cloudflare real IP restoration (`CF-Connecting-IP` + official Cloudflare CIDRs).
     - Declares rate limit zones: `login_limit` (5r/s), `api_limit` (30r/s).
   - `deploy/nginx/conf.d/shop.conf`:
     - Port 80 redirects to 443 HTTPS.
     - Port 443 terminates SSL (`/etc/nginx/ssl/cert.pem`, `key.pem`).
     - Proxies `location /api/` to `http://shop-backend:8080/api/`.
     - Aliases `location /uploads/` to `/usr/share/nginx/uploads/` directly.
     - Proxies `location /` to `http://shop-frontend:80`.
   - Deploy automation: `deploy/scripts/deploy.sh` and `deploy/scripts/deploy.bat`.

7. **Documentation Discrepancies (`README.md` & `docs/`)**:
   - `README.md` line 223: `stores/ # Pinia 响应式状态管理深模块 (auth, cart)`. Reality: actual files are `user.ts` (exporting `useUserStore`) and `cart.ts`. There is no `auth.ts` or `useAuthStore`.
   - `docs/principles/02-auth-and-satoken.md` line 257 & `06-vue3-and-vite-modern-frontend.md` line 191: both reference `useAuthStore` and claim the interceptor pulls token from the store. Reality: store is `useUserStore`, and `request.ts` reads `localStorage.getItem('token')` directly.
   - `docs/adr/0003-infrastructure-and-educational-artifacts.md` line 9: states "covering 6 core topics". Reality: `docs/principles/` currently contains 9 lecture documents (`01` through `09`).

---

## 2. Logic Chain

1. **Premise 1 (Backend Endpoint Contracts)**: All backend controllers define `@RequestMapping` with `/api/...` prefixes (`PortalProductController` on `/api/portal`, `AdminProductController` on `/api/admin/products`, `CommonUploadController` on `/api/common`).
2. **Premise 2 (Network Routing Contracts)**:
   - Vite dev proxy (`frontend/vite.config.ts`) only intercepts and forwards paths matching `'/api'` and `'/uploads'` to `http://localhost:8080`.
   - Production Nginx (`deploy/nginx/conf.d/shop.conf`) only proxies `^/api/` and `/uploads/` to the backend. The root `/` location forwards to `shop-frontend:80`, which responds with `index.html` for any unmatched SPA path.
3. **Inference 1 (Defect in `product.ts` and `ProductManagementView.vue`)**:
   - When `product.ts` calls `/portal/home` or `/portal/products` or `/admin/products`, Vite in dev mode does NOT proxy them to backend 8080 (resulting in 404), and Nginx in production proxies them to the Vue container which returns `index.html` (causing JSON parse errors).
   - When `ProductManagementView.vue` calls `/common/upload`, it similarly bypasses the proxy.
   - Therefore, `product.ts` and `ProductManagementView.vue` have a concrete URL defect that must be corrected to include the `/api` prefix.
4. **Inference 2 (Documentation Drift in Pinia Store & ADR-0003)**:
   - `README.md` and principles lectures 02 and 06 refer to `auth` store and `useAuthStore`. The source code uses `user.ts` / `useUserStore`. This constitutes documentation drift from early drafting.
   - `ADR-0003` was authored when principles lectures 01-06 existed. Adding 07, 08, and 09 expanded the collection without updating ADR-0003.
5. **Inference 3 (Script Brittleness with WSL)**:
   - `start.bat` and `start.ps1` hardcode `-d Ubuntu-22.04`. Because `where wsl` succeeds whenever any WSL distro is installed, users with default `Ubuntu` or other distros encounter a silent failure and do not reach the Docker Compose fallback.

---

## 3. Caveats

- **No source code or docs were modified** in this turn, strictly adhering to the explorer's read-only mandate.
- **Docker containers were not started locally during this turn** because this investigation is focused on static analysis, configuration inspection, and non-destructive unit/build execution (`npm test` and `npm run build` were run and verified).
- **Backend Java code changes**: Backend service controllers themselves are correctly designed with `@RequestMapping("/api/...")`; no backend modifications are needed for routing.

---

## 4. Conclusion

The frontend, automation scripts, and deployment configurations are robust and modern (Vue 3, Pinia, TypeScript, Vite, multi-stage Dockerfiles, Cloudflare+Nginx security gateway).
However, to achieve complete alignment between code and documentation (as required by `ORIGINAL_REQUEST.md`), the following items must be addressed by downstream implementers:
1. **Frontend Bugfix**: Update `frontend/src/api/product.ts` and `frontend/src/views/admin/ProductManagementView.vue` to prepend `/api` to all product and upload endpoints.
2. **Doc Synchronization**:
   - In `README.md`: update store listing from `(auth, cart)` to `(user, cart)`. Clarify WSL distro vs. Docker fallback in startup instructions.
   - In `docs/principles/02-auth-and-satoken.md` and `06-vue3-and-vite-modern-frontend.md`: correct `useAuthStore` to `useUserStore` and note `localStorage` token retrieval in `request.ts`.
   - In `docs/adr/0003-infrastructure-and-educational-artifacts.md`: update the number of principles lectures from 6 to 9.
3. **Optional Configuration Enhancement**: Provide `frontend/.env.example` demonstrating `VITE_API_BASE_URL`.

All detailed data and code references are compiled in `D:\JavaWeb\.agents\teamwork\explorer_survey_frontend\survey_report.md`.

---

## 5. Verification Method

To independently reproduce and verify this investigation:

1. **Verify Frontend Test Suite**:
   ```powershell
   npm --prefix frontend test
   ```
   *Expected outcome*: 3 test suites, 15 tests passing, exit code 0.

2. **Verify Frontend Type Check and Compilation**:
   ```powershell
   npm --prefix frontend run build
   ```
   *Expected outcome*: `vue-tsc` and `vite build` complete with exit code 0, emitting assets to `frontend/dist`.

3. **Verify Endpoint Missing `/api` Prefix**:
   ```powershell
   # Search for non-/api endpoints in product.ts:
   Select-String -Path "frontend/src/api/product.ts" -Pattern "['\"]/(portal|admin)"
   # Search for /common/upload in ProductManagementView:
   Select-String -Path "frontend/src/views/admin/ProductManagementView.vue" -Pattern "/common/upload"
   # Verify backend controllers require /api:
   Select-String -Path "backend/src/main/java/com/shop/**/*.java" -Pattern "@RequestMapping\(\"/api"
   ```

4. **Verify Store File Existence**:
   ```powershell
   Get-ChildItem -Path "frontend/src/stores"
   ```
   *Expected outcome*: `cart.ts`, `index.ts`, `user.ts` (no `auth.ts`).
