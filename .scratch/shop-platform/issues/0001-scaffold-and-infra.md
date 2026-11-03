---
title: Project Scaffold and Infrastructure Foundation
slug: 0001-scaffold-and-infra
blocked-by: []
---

# 0001 - Project Scaffold and Infrastructure Foundation

## Delivers
Monorepo skeleton ready for development, engineered with clean seams and minimal boilerplate:

1. **Backend (`backend/`)**: Spring Boot 3.3+ (Java 21)
   - Dependencies: Spring Web, MyBatis-Plus 3.5+, MySQL Connector J, Sa-Token (JWT), Redis, SpringDoc OpenAPI 3, Lombok, Commons Pool.
   - **Infrastructure Seams & Deep Modules**:
     - **Storage Seam (`StorageService`)**: Interface with `store(MultipartFile): String`, `loadAsResource(String): Resource`, and `delete(String): void`. Provides `LocalStorageService` adapter storing files in local `./uploads` mapped to `/uploads/**`. Eliminates file I/O complexity across user avatars, brand logos, and product images.
     - **Unified Response & Error Surface**: Lightweight `Result<T>` record wrapper and `GlobalExceptionHandler` intercepting `BusinessException`, Sa-Token security exceptions, and Bean Validation errors into standardized JSON.
     - **Data Access & Auditing**: MyBatis-Plus config with `PaginationInnerInterceptor` and `MetaObjectHandler` for automated `created_at` / `updated_at` timestamps.
   - **Persistence & Containers**:
     - `docker-compose.yml` for local Redis service.
     - `docs/sql/schema.sql` and `docs/sql/data.sql` for MySQL table initialization and rich seed data.

2. **Frontend (`frontend/`)**: Vue 3 + Vite 5 + TypeScript + Pinia + Vue Router + Element Plus + Axios
   - Clean API client seam: Axios instance with base URL, timeout, and response unwrapping.
   - Dual-layout skeleton: Storefront Portal layout (`/`) and Admin Console layout (`/admin`).

3. **Code Simplification Rules**:
   - Favor Java 21 `record`s for immutable DTOs and API payloads over verbose getter/setter classes.
   - No unnecessary multi-layer wrapper abstractions (no empty pass-through BaseServices).

## Verification
- Run backend: `/swagger-ui/index.html` opens and renders OpenAPI 3 interactive documentation.
- Database: Successfully connects to local MySQL `shop_db` and executes auto-audited queries.
- Storage Seam: Calling `/api/common/upload` saves a test image to local disk and returns an accessible URL.
- Run frontend: `npm run dev` starts Vite server; browser renders portal and admin skeleton layouts.
