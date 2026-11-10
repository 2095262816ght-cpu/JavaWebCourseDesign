# Project: JavaWeb Documentation Synchronization & Codebase Alignment

## Architecture
- **Monorepo Structure**:
  - `backend/`: Java 21, Spring Boot 3.3.4, MyBatis-Plus 3.5.7, Sa-Token 1.39.0 (Simple JWT), SpringDoc OpenAPI 2.6.0. Server port: `8080`.
  - `frontend/`: Vue 3.5.12, TypeScript 5.6.3, Vite 5.4.9, Pinia 2.2.4 (`user`, `cart`), Element Plus 2.8.5. Dev server port: `5173`. Proxies `/api` and `/uploads` to `8080`.
  - `deploy/`: Multi-stage Dockerfiles (`backend/Dockerfile`, `frontend/Dockerfile`), `docker-compose.prod.yml`, Nginx 1.27 reverse proxy with SSL termination, Cloudflare real IP, rate limiting.
  - `docs/`: 9 deep technical lecture notes (`docs/principles/01-09`), 3 ADRs (`docs/adr/0001-0003`), agent guidelines (`docs/agents/`), SQL DDL and seed data (`docs/sql/schema.sql`, `data.sql`).
  - Root Scripts: `start.bat`, `start.ps1`, `stop.bat`, `stop.ps1`.

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Frontend API Contract Fix | Fix missing `/api` prefix in `frontend/src/api/product.ts` (`/portal/...`, `/admin/...`) and `ProductManagementView.vue` (`/common/upload`) so Vite dev proxy and Nginx production proxy correctly route requests | M1 | survey_frontend |
| 2 | Prod Token Style Config Alignment | Align `backend/src/main/resources/application-prod.yml` `token-style: jwt` with `SaTokenConfig` and documentation | M1 | survey_backend |
| 3 | Core Build & Test Verification | Verify `mvn test` (44/44 passing) and `npm test` (15/15 passing) and `npm run build` | M1 | survey_frontend & survey_backend |
| 4 | README Overview & Architecture Sync | Update `README.md` project overview, technology versions, and architecture diagrams | M2 | survey_spec_miner & ORIGINAL_REQUEST |
| 5 | README Configuration & Scripts Sync | Update `README.md` startup/shutdown commands (`start.bat`, `start.ps1`, `stop.bat`, `stop.ps1`), WSL/Docker Redis fallback details, MySQL port 3306, backend 8080, frontend 5173 | M2 | survey_frontend & ORIGINAL_REQUEST |
| 6 | README Pinia Store Layout Sync | Update `README.md` tree from `(auth, cart)` to `(user, cart)` matching `frontend/src/stores/user.ts` | M2 | survey_frontend & survey_spec_miner |
| 7 | README Top 12 Defense Questions Sync | Expand `README.md` Defense Cheat Sheet from Top 10 to Top 12 (including Q11 customerId DDD and Q12 route guard tests) matching `docs/principles/07-course-defense-qa.md` | M2 | survey_spec_miner |
| 8 | Docs Physical Broken Links Fix | Correct `[DeliveryAddress]` links in `docs/principles/01-...` and `docs/principles/03-...` to point to `backend/src/main/java/com/shop/modules/commerce/dto/DeliveryAddress.java` | M3 | survey_spec_miner |
| 9 | Docs Sa-Token & JWT Key Sync | Update `docs/principles/02-auth-and-satoken.md` JWT secret key to `shop-platform-jwt-secret-key-32chars-min-abcdef123456`, replace `useAuthStore` with `useUserStore` / `localStorage` | M3 | survey_spec_miner |
| 10 | Docs Vue 3 & Pinia Architecture Sync | Update `docs/principles/06-vue3-and-vite-modern-frontend.md` store references (`useUserStore`) and `request.ts` token retrieval | M3 | survey_frontend & survey_spec_miner |
| 11 | Docs Router Guard Citation Drift Fix | Update `docs/principles/07-course-defense-qa.md` router guard line citation (lines 103-136) and route meta checks (`requiresAdmin`, `userStore.isAdmin`) | M3 | survey_spec_miner |
| 12 | Docs ADR-0003 Lecture Count Sync | Update `docs/adr/0003-infrastructure-and-educational-artifacts.md` lecture count from 6 to 9, enumerating all 9 lectures | M3 | survey_spec_miner |
| 13 | Docs SQL Schema & Data Dictionary | Create `docs/sql/README.md` documenting all 7 database tables, fields (including `origin`), types, indexes, and mapping to backend entities | M3 | survey_backend & ORIGINAL_REQUEST |
| 14 | Docs Markdown Format & Link Integrity | Ensure zero broken internal links, consistent markdown tables, and code snippets across all modified docs | M4 | ORIGINAL_REQUEST |
| 15 | Forensic Integrity & Verification Review | Independent review, adversarial challenge, and forensic integrity audit confirming clean non-facade implementation | M4 | Project Pattern |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Codebase Contract & Config Synchronization | Fix `/api` path bug in `frontend/src/api/product.ts` and `ProductManagementView.vue`; update `application-prod.yml` token-style; verify builds & tests pass | none | DONE |
| M2 | README.md Synchronization | Comprehensive overhaul of `README.md` reflecting architecture, prerequisites, configuration, scripts, ports, stores, test stats, and Q1-Q12 cheat sheet | M1 | DONE |
| M3 | Documentation Directory (docs) Alignment | Fix broken links, citations, store references, ADR-0003 counts, and create `docs/sql/README.md` data dictionary | M1 | DONE |
| M4 | Multi-Agent Review, Verification & Forensic Audit | Reviewers, Challengers, and Forensic Auditor verify build integrity, link validity, and complete alignment | M2, M3 | DONE |

## Interface Contracts
- **Backend Base Path**: All REST endpoints are rooted at `/api/` (e.g. `/api/auth/**`, `/api/portal/**`, `/api/cart/**`, `/api/orders/**`, `/api/admin/**`, `/api/common/**`).
- **Frontend Axios Client**: Proxied via Vite `/api` -> `http://localhost:8080/api`. All API functions in `frontend/src/api/` must request paths starting with `/api/`.
- **Database Tables**: 7 tables (`sys_user`, `shop_brand`, `shop_category`, `shop_product`, `shop_cart_item`, `shop_order`, `shop_order_item`). `shop_brand` contains `origin VARCHAR(64)`.

## Code Layout
- `README.md`: Root project document.
- `backend/`: Java Spring Boot source code and resources.
- `frontend/`: Vue 3 TypeScript frontend source code.
- `docs/`: Technical documentation repository.
  - `docs/adr/`: Architectural Decision Records (0001, 0002, 0003).
  - `docs/principles/`: 9 deep technical lecture notes (01 through 09).
  - `docs/agents/`: Agent tooling documentation.
  - `docs/sql/`: Database schema, seed data, and data dictionary.
- `deploy/`: Production deployment configurations (Docker, Nginx, Compose, SSL).
