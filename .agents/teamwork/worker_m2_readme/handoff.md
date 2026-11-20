# Milestone 2: README.md Synchronization Handoff Report

## 1. Observation
1. **Source Code & Library Versions**:
   - `backend/pom.xml`: Spring Boot `3.3.4`, Java `21`, MyBatis-Plus `3.5.7`, Sa-Token `1.39.0`, SpringDoc OpenAPI `2.6.0`.
   - `frontend/package.json`: Vue `^3.5.12`, TypeScript `^5.6.3`, Vite `^5.4.9`, Pinia `^2.2.4`, Element Plus `^2.8.5`.
2. **Ports and Network Configuration**:
   - `backend/src/main/resources/application.yml`: `server.port: 8080`, `spring.datasource.url: jdbc:mysql://localhost:3306/shop_db`, `spring.data.redis.port: 6379`, `springdoc.swagger-ui.path: /swagger-ui/index.html`.
   - `frontend/vite.config.ts`: `server.port: 5173`, proxying `/api` and `/uploads` to `http://localhost:8080`.
   - `deploy/docker-compose.prod.yml`: services `shop-mysql` (port 3306 internal), `shop-redis` (port 6379 internal), `shop-backend` (port 8080 internal), `shop-frontend` (port 80 internal), `shop-nginx` (ports `"80:80"`, `"443:443"` external).
   - `deploy/nginx/conf.d/shop.conf`: Port 80 returns HTTP 301 redirect to HTTPS; Port 443 terminates SSL, applies Cloudflare real IP header (`CF-Connecting-IP`), rate limits login (`limit_req zone=login_limit burst=5 nodelay`), zero-copy disk alias for `/uploads/`, and proxies `/api/` to backend.
3. **Startup and Shutdown Automation Scripts**:
   - `start.bat` & `start.ps1`:
     - Checks Redis port 6379; attempts `wsl -u root -d Ubuntu-22.04 service redis-server start`, falls back to `docker compose up -d redis`.
     - Checks MySQL port 3306.
     - Starts backend (`mvn spring-boot:run`) on port 8080 in a new console titled `[后端] Spring Boot 8080`.
     - Starts frontend (`npm run dev`) on port 5173 in a new console titled `[前端] Vite Vue3 5173`.
     - Pauses 6 seconds, prints default accounts (`admin/admin123`, `customer/123456`), and opens `http://localhost:5173` in default browser.
   - `stop.bat` & `stop.ps1`:
     - Finds and kills listening process on port 8080 and port 5173.
     - Leaves MySQL (3306) and Redis (6379) intact.
4. **State Management & Authentication**:
   - `frontend/src/stores/`: Contains `user.ts` (exporting `useUserStore`), `cart.ts` (exporting `useCartStore`), `index.ts`. No `auth.ts` exists.
   - `frontend/src/api/request.ts`: Directly reads `localStorage.getItem('token')` to attach `satoken` header. On 401 response, directly invokes `localStorage.removeItem('token')` and `localStorage.removeItem('user')`.
5. **Test Executions and Counts**:
   - Ran `mvn test -f backend/pom.xml`: Result: `Tests run: 44, Failures: 0, Errors: 0, Skipped: 0` across 13 test classes.
   - Ran `npm --prefix frontend test`: Result: `Test Files: 3 passed (3), Tests: 15 passed (15)` (`cart.spec.ts` 4 tests, `request.spec.ts` 4 tests, `router.spec.ts` 7 tests).
   - Ran `npm --prefix frontend run build`: Result: Successful production build (`dist/` generated, 1720 modules transformed in 8.03s).
6. **Principles and Q&A Documentation**:
   - `docs/principles/`: Contains 9 principle documents (`01` through `09`).
   - `docs/principles/07-course-defense-qa.md`: Contains 12 questions (Q1 through Q12), including Q11 (DDD customerId vs user_id and DeliveryAddress value object) and Q12 (Vue Router guard and Vitest automated authorization testing).

---

## 2. Logic Chain
1. *From Observation 1*: The previous README mentioned generic versions like "Spring Boot 3.3+", "Vite 5", and omitted exact library versions. Updating to `Java 21 / Spring Boot 3.3.4`, `MyBatis-Plus 3.5.7`, `Sa-Token 1.39.0`, `SpringDoc 2.6.0`, `Vue 3.5.12`, `TypeScript 5.6.3`, `Vite 5.4.9`, `Pinia 2.2.4`, `Element Plus 2.8.5` provides accurate reference for evaluators.
2. *From Observation 2 & 3*: Developers need clear documentation of operational ports, environment variable injection, and script orchestration. Adding the Ports Matrix table, Environment Settings section, and detailing the 5-step start script logic (WSL Ubuntu-22.04 / Docker Redis fallback, MySQL 3306 check, 8080 and 5173 spawn, 6s timeout and browser auto-launch) alongside the stop script behavior (terminating 8080/5173, keeping MySQL/Redis running) eliminates deployment guesswork.
3. *From Observation 4*: The prior README referenced stale `stores/ (auth, cart)`. Clarifying `useUserStore` and `useCartStore`, as well as the direct `localStorage` JWT token storage mechanism utilized by Axios interceptors, aligns documentation with actual frontend behavior.
4. *From Observation 5*: Verifying tests through live commands confirmed exactly 44 backend tests and 15 frontend tests. Documenting these precise counts, their coverage breakdown, and the concurrency test parameters establishes verifiable integrity.
5. *From Observation 6*: Aligning the Defense Cheat Sheet title to "答辩 Top 12 核心问题快速索引（Cheat Sheet）" and adding concise, standard answers for Q11 (DDD customerId and DeliveryAddress) and Q12 (router guard and Vitest verification) eliminates the discrepancy between README and `docs/principles/07-course-defense-qa.md`.
6. *From Observations 1-6*: Enhancing the Project Directory Tree to comprehensively include `backend/`, `frontend/`, `deploy/` (docker-compose.prod.yml, Dockerfiles, Nginx conf, ssl README), `docs/` (adr 0001-0003, principles 01-09, agents, sql), `CONTEXT.md`, and root scripts provides an accurate navigational index.

---

## 3. Caveats
- No changes were made to source code, tests, or deployment scripts in this milestone, strictly adhering to the exclusive file ownership constraint (`README.md` only).
- The start scripts (`start.bat` / `start.ps1`) specify WSL Ubuntu-22.04 for Redis auto-start; for environments without WSL or with other distributions, the documented Docker Compose fallback (`docker compose up -d redis`) is clearly instructed.

---

## 4. Conclusion
`D:\JavaWeb\README.md` has been completely synchronized and verified:
- Technology stack versions, Mermaid architecture diagram, and highlights match actual pom.xml and package.json configurations.
- Ports matrix, environment configurations (dev vs prod), and script execution workflows are accurately documented.
- Pinia store structure (`user`, `cart`) and `localStorage` token storage mechanism are clearly stated.
- Test counts (44 backend, 15 frontend) are verified and documented with coverage details.
- The Defense Cheat Sheet is expanded from Top 10 to Top 12 (including Q11 and Q12).
- The project layout tree reflects all directories and submodules in the current workspace.
- Impeccable Markdown syntax and link conventions are maintained.

---

## 5. Verification Method
1. **Inspect README.md**:
   - View `D:\JavaWeb\README.md` to verify all sections, tables, mermaid diagrams, and links.
2. **Verify Tests**:
   - Backend: `mvn test -f backend/pom.xml` -> Passes 44/44 tests.
   - Frontend: `npm --prefix frontend test` -> Passes 15/15 tests across 3 suites.
   - Frontend Build: `npm --prefix frontend run build` -> Compiles cleanly with zero errors.
3. **Verify Git Diff**:
   - `git diff README.md` -> Confirms only README.md content is changed, matching all M2 task specifications.
