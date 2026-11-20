## 2026-11-05T07:41:26Z
You are a teamwork_worker assigned to execute Milestone 2: README.md Synchronization.

Working directory: D:\JavaWeb\.agents\teamwork\worker_m2_readme\
Original request path: D:\JavaWeb\.agents\teamwork\ORIGINAL_REQUEST.md
Scope document: D:\JavaWeb\PROJECT.md

MANDATORY FIRST STEP:
Read D:\JavaWeb\.agents\teamwork\ORIGINAL_REQUEST.md.
Also refer to D:\JavaWeb\PROJECT.md and the survey reports at D:\JavaWeb\.agents\teamwork\spec_miner_survey_docs\docs_inventory.md and D:\JavaWeb\.agents\teamwork\explorer_survey_frontend\survey_report.md.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

EXCLUSIVE FILE OWNERSHIP:
You own and may modify ONLY this file:
- D:\JavaWeb\README.md

TASKS:
Review the entire current codebase and synchronize D:\JavaWeb\README.md so it accurately reflects:
1. Overview & Monorepo Architecture:
   - Java 21, Spring Boot 3.3.4, MyBatis-Plus 3.5.7, Sa-Token 1.39.0 (Simple JWT), SpringDoc 2.6.0.
   - Vue 3.5.12, TypeScript 5.6.3, Vite 5.4.9, Pinia 2.2.4, Element Plus 2.8.5.
2. Configuration, Ports, and Environment Settings:
   - Backend port 8080 (Swagger UI at http://localhost:8080/swagger-ui/index.html).
   - Frontend dev port 5173 (http://localhost:5173).
   - MySQL port 3306 (database: shop_db, user: root).
   - Redis port 6379.
   - Production Nginx ports 80 (HTTP redirect) and 443 (HTTPS with SSL).
   - Environment variables (backend .env.example and deploy/docker-compose.prod.yml parameters like DB_HOST, DB_NAME, REDIS_HOST, etc.).
3. Startup & Shutdown Commands:
   - Document start.bat and start.ps1: Redis 6379 check (WSL Ubuntu-22.04 service start or Docker fallback), MySQL 3306 check, backend startup (mvn spring-boot:run), frontend startup (npm run dev), auto-opening browser to http://localhost:5173 after 6s.
   - Document stop.bat and stop.ps1: Terminates listening processes on port 8080 and 5173, leaves MySQL and Redis running.
4. Pinia Stores:
   - Update directory tree and explanation: replace stale '(auth, cart)' with '(user, cart)'. Explain that useUserStore handles user info, and JWT token is stored directly in localStorage.
5. Testing Stats:
   - State exact passing test counts: 44 backend tests (mvn test) covering auth, catalog, cart, orders, and optimistic lock concurrency; 15 frontend tests (npm test) covering cart, request interceptors, and router guards.
6. Defense Cheat Sheet:
   - Update '答辩 Top 10 核心问题快速索引' to '答辩 Top 12 核心问题快速索引（Cheat Sheet）' to match all 12 questions in docs/principles/07-course-defense-qa.md:
     - Include Q11 (DDD 领域模型与物理表解耦，customerId 与 user_id 映射，DeliveryAddress 值对象封装).
     - Include Q12 (前端路由守卫与权限自动化测试，Vitest 模拟 beforeEach 校验 requiresAuth 与 requiresAdmin).
7. Complete Project Tree Layout:
   - Accurately describe the directory tree: backend/, frontend/, deploy/ (docker-compose.prod.yml, Dockerfiles, nginx conf, ssl README), docs/ (adr 0001-0003, principles 01-09, agents, sql).
8. Maintain impeccable Markdown formatting and preserve all existing styling conventions.

OUTPUT REQUIREMENTS:
- Produce your self-contained handoff report at: D:\JavaWeb\.agents\teamwork\worker_m2_readme\handoff.md with all 5 sections (Observation, Logic Chain, Caveats, Conclusion, Verification Method).
- Update progress.md in your working directory.
- Send completion message to orchestrator when finished.
