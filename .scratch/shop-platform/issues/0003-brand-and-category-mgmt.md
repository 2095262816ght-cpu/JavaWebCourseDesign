---
title: Brand and Category Catalog Management Slice
slug: 0003-brand-and-category-mgmt
blocked-by: [0001-scaffold-and-infra]
---

# 0003 - Brand and Category Catalog Management Slice

## Delivers
Brand and Category catalog taxonomy engineered with deep caching and tree construction modules:

1. **Backend (`com.shop.modules.catalog`)**:
   - **Deep `CategoryService`**:
     - *Interface*: `getCategoryTree(): List<CategoryNodeVO>`, `saveCategory(CategoryCommand cmd): void`, `deleteCategory(Long id): void`.
     - *Hidden Implementation*: In-memory recursive tree construction and transparent Cache-Aside caching in Redis (`catalog:category:tree`) with automatic invalidation on updates.
   - **Deep `BrandService`**:
     - *Interface*: `getFeaturedBrands(int limit): List<BrandVO>`, `listBrands(BrandQuery query): PageResult<BrandVO>`, `saveBrand(BrandCommand cmd): void`, `deleteBrand(Long id): void`.
     - *Hidden Implementation*: Sorting, pagination queries, and Cache-Aside caching (`catalog:brand:featured`) with automated cache eviction.
   - **Endpoints**:
     - Public (Cached): `GET /api/portal/categories/tree`, `GET /api/portal/brands/featured`.
     - Admin: `POST /api/admin/categories`, `PUT /api/admin/categories/{id}`, `DELETE /api/admin/categories/{id}`, `GET/POST/PUT/DELETE /api/admin/brands`.
     - Note: File uploading uses the `StorageService` seam from Ticket 0001.

2. **Frontend (`frontend/src/views/admin/` & components)**:
   - **Admin Brand Management**:
     - Paginated table showing logo image, brand name, sort order, and status.
     - Add/Edit dialog integrating standard Element Plus upload component connected to `/api/common/upload`.
   - **Admin Category Management**:
     - Hierarchical tree-table view with add sub-category, edit name/icon, and delete actions with confirmation dialogs.

3. **Code Simplification & Seam Discipline**:
   - High locality: Cache read-through and cache eviction logic are contained strictly within the services, not leaked into controllers.
   - Category tree construction is encapsulated in a pure, testable Java 21 stream utility behind the service interface.
   - Elimination of redundant DTO layers: Use records `CategoryNodeVO` and `BrandVO`.

## Verification
- Admin creates a brand and uploads a logo -> logo saved via `StorageService` and brand saved in MySQL.
- Public request to `GET /api/portal/categories/tree` returns full nested tree structure; second request hits Redis cache directly.
- Admin updates a category name -> Redis cache is immediately invalidated; subsequent public query loads fresh data.
