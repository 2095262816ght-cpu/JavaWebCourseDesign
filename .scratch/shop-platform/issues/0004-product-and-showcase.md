---
title: Product Management and Storefront Showcase Slice
slug: 0004-product-and-showcase
blocked-by: [0002-auth-and-user-mgmt, 0003-brand-and-category-mgmt]
---

# 0004 - Product Management and Storefront Showcase Slice

## Delivers
Product catalog and storefront showcase engineered as high-leverage, deep query and management modules:

1. **Backend (`com.shop.modules.catalog`)**:
   - **Deep `ProductCatalogService` (Storefront Interface)**:
     - `getHomeShowcase(): HomeShowcaseVO`: Single aggregate entry point returning carousel slides, featured brands, categories, and new/hot products in one network request (eliminating frontend waterfall requests).
     - `searchProducts(ProductQueryCommand cmd): PageResult<ProductCardVO>`: Encapsulates dynamic query composition (fuzzy title search, brand filter, category filter, price range, and sorting by price or created date).
     - `getProductDetail(Long id): ProductDetailVO`: Encapsulates product attributes, sub-image gallery parsing, associated brand/category names, and current stock status.
   - **Deep `ProductAdminService` (Backoffice Interface)**:
     - `saveProduct(ProductCommand cmd): Long` (validates brand/category existence, handles main and sub-image URLs).
     - `updateStatus(Long id, Integer status): void` (toggles on/off shelf).
     - `listAdminProducts(AdminProductQuery query): PageResult<ProductAdminVO>`.
   - **Endpoints**:
     - Storefront: `GET /api/portal/home`, `GET /api/portal/products`, `GET /api/portal/products/{id}`.
     - Admin: `GET /api/admin/products`, `POST /api/admin/products`, `PUT /api/admin/products/{id}`, `PATCH /api/admin/products/{id}/status`, `DELETE /api/admin/products/{id}`.

2. **Frontend (`frontend/src/views/portal/` & `admin/`)**:
   - **Storefront Home (`/`)**:
     - Powered by single call to `/api/portal/home`: Carousel hero, quick brand filter wall, and product recommendation grid.
   - **Storefront Product Search & Detail (`/products`, `/product/:id`)**:
     - Dynamic filter sidebar (category & brand selector, price range, sort dropdown).
     - Detail view with gallery thumbnail switcher, price and stock display, quantity stepper, and "Add to Cart" action.
   - **Admin Product Management (`/admin/products`)**:
     - Data table with filter toolbar, status switch, and comprehensive create/edit modal integrating `StorageService` image uploads.

3. **Code Simplification & Seam Discipline**:
   - High leverage: Aggregated `HomeShowcaseVO` prevents UI waterfall loading.
   - Clean data types: Java 21 `record`s for all query commands and presentation VOs.
   - The interface is the test surface: Unit/integration tests exercise `searchProducts()` and `getHomeShowcase()` directly, asserting on the returned records across diverse filtering criteria.

## Verification
- Admin creates a product with images, brand, and category, and sets status to on-shelf.
- Storefront homepage (`/`) loads and immediately renders the product under hot recommendations.
- Filter products by keyword and brand in storefront -> dynamic query returns matching records.
- Open product detail page -> renders accurate image gallery, brand badge, and real-time stock count.
