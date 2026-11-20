# Milestone 1 Handoff Report: Codebase Contract & Config Synchronization

## 1. Observation

Direct file observations and modifications:
1. **Frontend API Endpoints (`frontend/src/api/product.ts`)**:
   - Lines 125, 132, 139: Storefront public endpoints previously defined as:
     - `'/portal/home'`
     - `'/portal/products'`
     - `'/portal/products/${id}'`
   - Lines 148, 155, 162, 169, 176, 185: Admin endpoints previously defined as:
     - `'/admin/products'` (in both `listAdminProductsApi` and `createProductApi`)
     - `'/admin/products/${id}'` (in `getAdminProductApi`, `updateProductApi`, and `deleteProductApi`)
     - `'/admin/products/${id}/status'` (in `updateProductStatusApi`)
   - All 9 invocations across these 6 distinct endpoints lacked the `/api` prefix, which was inconsistent with Vite proxy config (`/api` -> `http://localhost:8080/api`) and the backend controller mappings (`@RequestMapping("/api/portal/...")` and `@RequestMapping("/api/admin/...")`).
   - Modified all 9 invocations to include the `/api` prefix:
     - `'/api/portal/home'`
     - `'/api/portal/products'`
     - `'/api/portal/products/${id}'`
     - `'/api/admin/products'`
     - `'/api/admin/products/${id}'`
     - `'/api/admin/products/${id}/status'`

2. **Admin Image Upload URL (`frontend/src/views/admin/ProductManagementView.vue`)**:
   - Lines 631 and 649: In `handleUploadMainImage` and `handleUploadSubImage`:
     - Verbatim prior code: `request.post<any, any>('/common/upload', form, { ... })`
     - Modified to: `request.post<any, any>('/api/common/upload', form, { ... })`

3. **Backend Sa-Token Production Configuration (`backend/src/main/resources/application-prod.yml`)**:
   - Line 58 previously defined: `token-style: uuid`
   - In `backend/src/main/resources/application.yml` line 52 and `SaTokenConfig.java`, the system utilizes JWT authentication (`token-style: jwt` with `jwt-secret-key: shop-platform-jwt-secret-key-32chars-min-abcdef123456`).
   - Modified line 58 of `application-prod.yml` from `token-style: uuid` to `token-style: jwt`.

4. **Verification Test & Build Results**:
   - Command: `npm --prefix frontend test`
     ```
      RUN  v2.1.9 D:/JavaWeb/frontend
      ✓ test/request.spec.ts (4 tests) 9ms
      ✓ test/cart.spec.ts (4 tests) 13ms
      ✓ test/router.spec.ts (7 tests) 20ms

      Test Files  3 passed (3)
           Tests  15 passed (15)
        Duration  2.85s
     ```
   - Command: `npm --prefix frontend run build`
     ```
     > shop-frontend@1.0.0 build
     > vue-tsc && vite build

     vite v5.4.21 building for production...
     transforming...
     ✓ 1720 modules transformed.
     rendering chunks...
     computing gzip size...
     dist/index.html                     0.47 kB │ gzip:   0.33 kB
     dist/assets/index-BTaW-Juf.css    399.48 kB │ gzip:  54.54 kB
     dist/assets/index-CO53oaFz.js   1,376.18 kB │ gzip: 442.36 kB
     ✓ built in 10.44s
     ```
     Exit code: 0, Errors: 0.
   - Command: `mvn -f backend/pom.xml test`
     ```
     [INFO] Results:
     [INFO] 
     [INFO] Tests run: 44, Failures: 0, Errors: 0, Skipped: 0
     [INFO] 
     [INFO] ------------------------------------------------------------------------
     [INFO] BUILD SUCCESS
     [INFO] ------------------------------------------------------------------------
     [INFO] Total time:  14.692 s
     ```
     Exit code: 0, Failures: 0, Errors: 0.

5. **File Boundary Enforcement**:
   - `git diff` strictly confirmed that changes were restricted solely to:
     - `backend/src/main/resources/application-prod.yml`
     - `frontend/src/api/product.ts`
     - `frontend/src/views/admin/ProductManagementView.vue`

## 2. Logic Chain

1. **Endpoint Resolution**:
   - Per `PROJECT.md` § Interface Contracts: "All REST endpoints are rooted at `/api/` (e.g. `/api/auth/**`, `/api/portal/**`, `/api/cart/**`, `/api/orders/**`, `/api/admin/**`, `/api/common/**`)."
   - In `frontend/src/api/request.ts`, `axios.create` sets `baseURL: import.meta.env.VITE_API_BASE_URL || ''`.
   - `vite.config.ts` sets up proxy matching `^/api` and forwarding to backend `http://localhost:8080/api`.
   - Production Nginx config (`deploy/nginx.conf`) routes `location /api/ { proxy_pass http://shop-backend:8080; }`.
   - Therefore, any request originating from the frontend without the `/api` prefix (such as `/portal/home`, `/portal/products`, `/admin/products`, `/common/upload`) fails proxy routing in both development and production environments.
   - Updating `frontend/src/api/product.ts` and `frontend/src/views/admin/ProductManagementView.vue` to prepend `/api` restores full contract conformance with the backend and proxy routing rules.

2. **Configuration Synchronization**:
   - `SaTokenConfig.java` enables Simple-JWT integration for stateless authentication across the shop platform.
   - `application.yml` explicitly specifies `token-style: jwt`.
   - `application-prod.yml` previously drifted to `token-style: uuid`, which would cause token generation mismatch when the prod profile was activated.
   - Updating line 58 of `application-prod.yml` to `token-style: jwt` ensures behavioral parity across dev and prod profiles.

3. **Verification Validity**:
   - Frontend unit test suite (`npm --prefix frontend test`) executes 15 Vitest assertions testing request interceptor token injection, 401 handling, Pinia cart reactivity, and router navigation guards (`requiresAuth`, `requiresAdmin`). All 15 tests passed cleanly.
   - TypeScript checking and production bundle creation (`npm --prefix frontend run build`) compiled 1,720 modules via `vue-tsc && vite build` with 0 type errors and 0 build errors.
   - Backend unit and integration test suite (`mvn -f backend/pom.xml test`) executes 44 tests covering auth controllers, brand services, product catalog services, and concurrent inventory deduction with optimistic locking. All 44 tests passed with 0 failures and 0 errors.

## 3. Caveats

- **No Caveats**: All required modifications were within the designated exclusive file ownership set. No unexpected regressions or side effects were introduced.

## 4. Conclusion

Milestone 1 is complete. All contract drift in `frontend/src/api/product.ts` and `frontend/src/views/admin/ProductManagementView.vue` has been resolved by aligning all API paths to the `/api` prefix. Production configuration for Sa-Token token style in `backend/src/main/resources/application-prod.yml` is now aligned with JWT specifications. All 15 frontend tests, 44 backend tests, and production frontend build pass with 100% success rate. The codebase is ready for subsequent documentation synchronization milestones (M2 & M3).

## 5. Verification Method

To independently verify these changes:

1. **Verify frontend tests**:
   ```powershell
   npm --prefix frontend test
   ```
   *Expected Result*: 3 test files passed, 15 tests passed, 0 failed.

2. **Verify frontend build**:
   ```powershell
   npm --prefix frontend run build
   ```
   *Expected Result*: `vue-tsc && vite build` completes with exit code 0 and bundles `dist/index.html`.

3. **Verify backend tests**:
   ```powershell
   mvn -f backend/pom.xml test
   ```
   *Expected Result*: 44 tests run, 0 failures, 0 errors, `BUILD SUCCESS`.

4. **Verify git diff exclusivity**:
   ```powershell
   git status --short
   ```
   *Expected Result*: Code changes exist only in `frontend/src/api/product.ts`, `frontend/src/views/admin/ProductManagementView.vue`, and `backend/src/main/resources/application-prod.yml`.
