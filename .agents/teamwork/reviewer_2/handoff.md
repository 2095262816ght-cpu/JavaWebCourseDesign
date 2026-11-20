# Independent Review and Adversarial Challenge Report

**Reviewer**: reviewer_2 (teamwork_reviewer & teamwork_critic)  
**Date**: 2026-11-05  
**Review Target**: Documentation Synchronization, ADRs, Deployment Configurations, Scripts, and Codebase Contract Alignment  
**Verdict**: **APPROVE**  
**Overall Risk Assessment**: **LOW**

---

## 1. Observation

Direct code and file observations verified during review:

1. **Database Documentation (`docs/sql/README.md`) & Schema (`docs/sql/schema.sql`, `docs/sql/data.sql`)**:
   - `docs/sql/README.md` documents all 7 core tables:
     1. `sys_user` (lines 63–81, mapping to `SysUser.java`)
     2. `shop_brand` (lines 84–105, mapping to `ShopBrand.java`)
     3. `shop_category` (lines 107–124, mapping to `ShopCategory.java`)
     4. `shop_product` (lines 127–158, mapping to `ShopProduct.java`)
     5. `shop_cart_item` (lines 160–178, mapping to `ShopCartItem.java`)
     6. `shop_order` (lines 180–202, mapping to `ShopOrder.java`)
     7. `shop_order_item` (lines 204–225, mapping to `ShopOrderItem.java`)
   - Exact column names, data types, nullability, default values, and indexes in `docs/sql/README.md` match `docs/sql/schema.sql` (lines 1–152) 100%.
   - In `shop_brand`, `origin VARCHAR(64) DEFAULT NULL COMMENT 'Brand Origin / Country'` is documented in section 3.2 (with special architectural notes on Redis cache partitioning `catalog:brand:all`), directly matching `schema.sql` line 38 and `backend/src/main/java/com/shop/modules/catalog/entity/ShopBrand.java` line 23 (`private String origin;`).
   - Section 4 accurately details DDD anti-corruption patterns:
     - Section 4.1 details `customerId` in `ShopCartItem.java` / `ShopOrder.java` mapped to `user_id` via `@TableField("user_id")`.
     - Section 4.2 details `DeliveryAddress` value object (`com.shop.modules.commerce.dto.DeliveryAddress`) encapsulating `receiver_name`, `receiver_phone`, `receiver_address` to eliminate Fowler's "Data Clumps".
     - Section 4.3 details rich domain entity methods (`ShopCartItem.calculateSubtotal()`, `ShopOrderItem.calculateSubtotal()`) eliminating "Feature Envy".
   - Section 5 accurately documents initialization procedures:
     - Dev manual execution: `mysql -u root -p < docs/sql/schema.sql` followed by `mysql -u root -p < docs/sql/data.sql`.
     - Prod declarative initialization via Docker entrypoint: `deploy/docker-compose.prod.yml` lines 36–37 mounting `../docs/sql/schema.sql:/docker-entrypoint-initdb.d/01_schema.sql:ro` and `../docs/sql/data.sql:/docker-entrypoint-initdb.d/02_data.sql:ro`.

2. **ADR Alignment (`docs/adr/0003-infrastructure-and-educational-artifacts.md`)**:
   - Line 9 updates lecture scope to 9 core topics.
   - Lines 15–24 expand educational artifacts to 9 comprehensive lecture notes, explicitly enumerating `01-springboot3-internals-and-architecture.md` through `09-scheme1-production-deployment-guide.md`.
   - ADR-0001 and ADR-0002 were inspected and confirm monorepo layout, Java 21, Spring Boot 3.3.4, Sa-Token JWT mode, and Redis caching.

3. **Deploy Configurations (`deploy/`) and Root Scripts**:
   - `deploy/docker-compose.prod.yml` specifies a 5-container topology (`shop-mysql`, `shop-redis`, `shop-backend`, `shop-frontend`, `shop-nginx`) connected via internal network `shop-internal-network` with zero external port exposure for MySQL and Redis; only Nginx exposes ports `80` and `443`.
   - `deploy/nginx/conf.d/shop.conf`:
     - Lines 4–13 enforce HTTP 301 redirect to HTTPS.
     - Lines 52–62 enforce rate limiting (`burst=5 nodelay`) on `/api/auth/(login|register)`.
     - Lines 67–77 route `/api/` to `http://shop-backend:8080/api/`.
     - Lines 82–87 serve `/uploads/` via zero-copy static alias from `/usr/share/nginx/uploads/` (volume `uploads_data`).
     - Lines 92–98 proxy `/` to `http://shop-frontend:80`.
   - `deploy/scripts/deploy.sh` and `deploy.bat` automate .env creation from `.env.example`, generate fallback self-signed SSL certificates, and execute Docker Compose build & up.
   - Root scripts (`start.bat`, `start.ps1`, `stop.bat`, `stop.ps1`) correctly target backend port 8080, frontend port 5173, check MySQL 3306 and Redis 6379, and preserve data services on stop.

4. **Codebase Contract Fixes**:
   - `frontend/src/api/product.ts` (lines 125, 132, 139, 148, 155, 162, 169, 176, 185) has all storefront and backoffice endpoints explicitly prefixed with `/api/` (e.g. `/api/portal/home`, `/api/portal/products`, `/api/admin/products`).
   - `frontend/src/views/admin/ProductManagementView.vue` (lines 631, 648) and `BrandManagementView.vue` (line 274) use `/api/common/upload`.
   - `backend/src/main/resources/application-prod.yml` (line 58) declares `token-style: jwt` matching `application.yml` and `SaTokenConfig.java` (`StpLogicJwtForSimple`).

5. **Independent Build and Test Execution**:
   - Backend `mvn test` in `backend/`: Total tests run: **44**, Failures: **0**, Errors: **0**, Skipped: **0**, Exit Code: **0**.
   - Frontend `npm test` in `frontend/`: Test Files: **3 passed (3)**, Tests: **15 passed (15)**, Duration: **2.55s**, Exit Code: **0**.
   - Frontend production build `npm run build` in `frontend/`: `vue-tsc && vite build` succeeded cleanly in **8.06s**, Exit Code: **0**.

---

## 2. Logic Chain

1. **Database Schema & Data Dictionary Consistency**:
   - Observation 1 demonstrates that all 7 tables in `schema.sql` are documented in `docs/sql/README.md` with complete column fidelity, including the key domain extension field `origin` on `shop_brand`.
   - The DDD mappings (`customerId` <-> `user_id`, `DeliveryAddress`) and entity calculation methods described in `docs/sql/README.md` were directly corroborated against the Java entity sources (`ShopCartItem.java`, `ShopOrder.java`, `DeliveryAddress.java`, `ShopOrderItem.java`).
   - The dev and prod database initialization workflows described in `docs/sql/README.md` match the actual scripts (`schema.sql`, `data.sql`, `docker-compose.prod.yml`).
   - Therefore, the database documentation is completely synchronized, accurate, and faithful to both the relational schema and the application source code.

2. **Educational Artifacts & ADR Synchronization**:
   - Observation 2 demonstrates that ADR-0003 accurately reflects the expansion from 6 to 9 lectures and enumerates each lecture file (`01-09`).
   - All referenced files exist in `docs/principles/` with valid content and consistent citations.
   - Therefore, ADR-0003 is fully synchronized with the curriculum artifact deliverables.

3. **Deployment Architecture & Routing Integrity**:
   - Observation 3 shows that the deployment configuration enforces strict network isolation (ports 3306 and 6379 internal-only), rate limiting on sensitive auth endpoints, and SSL termination.
   - Observation 4 confirms that all frontend API endpoints and upload calls now explicitly include the `/api/` prefix. Under Nginx, `/api/` is proxied to `shop-backend:8080/api/` while non-API requests route to `shop-frontend:80`. Without `/api/`, API requests would be misrouted to frontend index.html. Fixing this contract in `product.ts` and `ProductManagementView.vue` guarantees operational consistency between dev (Vite proxy) and prod (Nginx proxy).
   - `application-prod.yml` declaring `token-style: jwt` aligns production runtime with `SaTokenConfig` (`StpLogicJwtForSimple`), preventing token incompatibility across environments.

4. **Integrity & Verification Authenticity**:
   - Independent build and test execution (Observation 5) confirmed that all 44 backend tests and 15 frontend tests pass without modification or mock bypasses.
   - In-depth inspection of `OrderConcurrencyTest.java` confirmed a real multi-threaded race condition test (10 threads via `CountDownLatch` and `ExecutorService`) operating on a live MySQL database, verifying atomic single-SQL inventory reduction and zero overselling.
   - Inspection of frontend tests (`router.spec.ts`, `request.spec.ts`, `cart.spec.ts`) confirmed real DOM navigation, route guards, token injection/clearing, and reactive store assertions.
   - No hardcoded test responses, fake facades, or integrity shortcuts exist.

---

## 3. Adversarial Challenges & Stress-Testing

| # | Challenged Assumption / Scenario | Attack Scenario & Blast Radius | Mitigation & Defense Verification | Status |
|---|---|---|---|---|
| **C1** | **Docker Compose volume mount path resolution** | If `docker-compose.prod.yml` uses relative mounts (`../docs/sql/schema.sql`), running compose from root vs. from `deploy/` could fail. | Verified: Compose resolves relative volume paths relative to the compose file directory (`deploy/`). Both `deploy.sh` (line 9) and `deploy.bat` (line 7) explicitly `cd` into `deploy/` before invoking `docker compose -f docker-compose.prod.yml`. Mount paths resolve accurately to `docs/sql/schema.sql` and `docs/sql/data.sql`. | **PASS** |
| **C2** | **Nginx URL stripping on `/api/` proxy_pass** | `proxy_pass http://shop-backend:8080/api/;` with a trailing URI could accidentally rewrite paths or strip subpaths. | Verified: In `shop.conf`, `location /api/ { proxy_pass http://shop-backend:8080/api/; }` maps `/api/foo` to `/api/foo` on backend. Backend Spring Boot routes are explicitly mapped under `/api/` (e.g. `/api/portal/**`, `/api/admin/**`). The path alignment is 1:1. | **PASS** |
| **C3** | **Production MySQL initial seed race condition** | If `shop-backend` boots before MySQL completes `01_schema.sql` and `02_data.sql`, backend could crash on startup. | Verified: `docker-compose.prod.yml` configures `shop-mysql` with a comprehensive healthcheck (`mysqladmin ping`) and `shop-backend` declares `depends_on: shop-mysql: condition: service_healthy`. Container startup order is deterministic. | **PASS** |
| **C4** | **Static upload directory permissions across containers** | Uploaded images written by `shop-backend` must be readable by `shop-nginx`. | Verified: Volume `uploads_data` is mounted to `shop-backend` at `/app/uploads` (rw) and to `shop-nginx` at `/usr/share/nginx/uploads` (ro). Nginx serves `/uploads/` directly via `alias`, eliminating backend proxy overhead and providing read-only safety. | **PASS** |
| **C5** | **Integrity violation check** | Check for hardcoded test results, facade implementations, or bypassed checks. | Verified: Code inspection confirmed real atomic SQL updates (`ShopProductMapper.decrementStock`), real BCrypt hashing, real Sa-Token JWT validation, real Pinia store computations, and real integration tests. No integrity violations found. | **PASS** |

---

## 4. Caveats

- **Active Running Containers**: Review was executed in local development mode without launching a live Docker daemon cluster; Docker Compose configuration validity and syntax were verified structurally and semantically against Docker Compose v3.8 specifications.
- **SSL Certificate Provisioning**: Production deployment scripts generate self-signed certificates as an operational fallback; formal deployment to public domains requires binding valid certificates (e.g. Cloudflare Origin CA or Let's Encrypt), as clearly documented in `deploy/nginx/ssl/README.md`.

---

## 5. Conclusion

**Verdict**: **APPROVE**

All requirements specified in `ORIGINAL_REQUEST.md` and `PROJECT.md` for `docs/sql/README.md`, `docs/adr/`, `deploy/`, scripts, and codebase contract alignment have been completely and accurately fulfilled:
1. `docs/sql/README.md` comprehensively covers all 7 database tables, fields (including `origin`), types, indexes, DDD mappings, and dev/prod deployment mechanisms.
2. `docs/adr/0003-infrastructure-and-educational-artifacts.md` accurately records all 9 lecture notes (01–09).
3. `deploy/` and root scripts correctly reflect the documented architecture, network isolation, and service orchestration.
4. Contract alignment in `frontend/src/api/product.ts`, `ProductManagementView.vue`, and `backend/src/main/resources/application-prod.yml` is clean, correct, and verified by passing automated test suites.
5. Zero integrity violations detected.

---

## 6. Verification Method

To independently verify these findings:

1. **Verify Backend Build & Unit/Integration Tests**:
   ```powershell
   cd D:\JavaWeb\backend
   mvn test
   ```
   *Expected*: `Tests run: 44, Failures: 0, Errors: 0, Skipped: 0` and `BUILD SUCCESS`.

2. **Verify Frontend Unit/Integration Tests & Production Build**:
   ```powershell
   cd D:\JavaWeb\frontend
   npm test
   npm run build
   ```
   *Expected*: `15 passed (15)` in Vitest and successful Vite bundle compilation (`dist/index.html` generated).

3. **Verify SQL Schema & Data Dictionary Alignment**:
   Inspect `D:\JavaWeb\docs\sql\README.md` against `D:\JavaWeb\docs\sql\schema.sql`.
   Confirm 7 tables (`sys_user`, `shop_brand`, `shop_category`, `shop_product`, `shop_cart_item`, `shop_order`, `shop_order_item`) and `origin` on `shop_brand`.

4. **Verify ADR-0003 Lecture Enumeration**:
   Inspect `D:\JavaWeb\docs\adr\0003-infrastructure-and-educational-artifacts.md` lines 9–24. Confirm 9 lectures listed from 01 to 09.

5. **Verify API Contract Prefix**:
   ```powershell
   git grep -n "request\." D:\JavaWeb\frontend\src\api
   ```
   Confirm all endpoints start with `/api/`.
