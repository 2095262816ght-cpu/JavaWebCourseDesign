# Specification: Modern Full-Stack Shop Platform

- Status: ready-for-agent
- Created: 2026-11-03
- Feature: shop-platform

## Problem Statement

Students in Java Web course assignments often face a dilemma: existing tutorials and course materials use outdated frameworks (Java 8, Spring Boot 2.x, JSP/Thymeleaf, raw JDBC or heavy XML-based MyBatis), leaving them unprepared for modern enterprise software practices. Furthermore, projects frequently lack real-world architectural design, educational explanations of underlying principles (IoC, AOP, Filter chains, ORM AST, Cache-Aside, ACID transactions), and cleanly separated customer-facing and administrative user experiences.

## Solution

Build a modern, educational, production-grade e-commerce Shop platform in a monorepo architecture. 
- The backend leverages Spring Boot 3.3+ (Java 21), Sa-Token (JWT-based stateless auth), MyBatis-Plus 3.5+ (over MySQL 8.4), and Redis for high-performance caching.
- The frontend is built on Vue 3, Vite 5, TypeScript, Pinia, and Element Plus, delivering both a customer-facing storefront (`/`) and an operator-facing admin console (`/admin`).
- The project includes complete database seed scripts, interactive OpenAPI 3 documentation, and a dedicated 6-topic lecture series in `docs/principles/` explaining the mechanics and design patterns behind the code.

## User Stories

### Customer Experience (Storefront)
1. As a Customer, I want to view a visually engaging storefront homepage with featured banners, top brands, and product recommendations, so that I can easily discover interesting products.
2. As a Customer, I want to filter and search products by keyword, category, and brand with instant pagination and sorting, so that I can quickly find the exact goods I wish to buy.
3. As a Customer, I want to inspect a detailed product page displaying images, current price, inventory count, brand origin, and rich description, so that I have complete product information before deciding to purchase.
4. As a Customer, I want to register a new account and securely log in with username and password, receiving a secure JWT token, so that my personal profile and shopping activity are persisted.
5. As a Customer, I want to add products to my shopping cart, update item quantities, and delete items, so that I can manage my intended purchases prior to checkout.
6. As a Customer, I want to checkout my cart to create an order with shipping details, atomically decrementing real-time inventory and receiving a simulated payment confirmation, so that I can complete the purchasing loop.
7. As a Customer, I want to view my order history and order detail snapshots, so that I can track past purchases and review item prices at the time of purchase.

### Administrator Experience (Backoffice)
8. As an Administrator, I want to log in through a secured admin portal, so that unauthorized users cannot access management capabilities.
9. As an Administrator, I want to perform full CRUD operations on Brands (name, logo, description, sort order), so that the catalog has an organized brand directory.
10. As an Administrator, I want to perform full CRUD operations on Categories (hierarchical tree structure, icon, sort order), so that products can be logically categorized.
11. As an Administrator, I want to create, edit, publish, and unpublish Products with image uploads, stock counts, prices, and brand/category associations, so that the store catalog is kept up to date.
12. As an Administrator, I want to view, search, and update order statuses (e.g., mark as Shipped), so that store operations can be managed smoothly.
13. As an Administrator, I want to inspect registered customers, view their status, and toggle account activation, so that the platform maintains user security.

### Educational & Architectural
14. As a Student Developer, I want to consult OpenAPI 3 Swagger documentation at `/swagger-ui/index.html`, so that I can inspect and test all backend endpoints interactively.
15. As a Student Developer, I want to study the `docs/principles/` lectures and in-code annotations, so that I understand Spring Boot 3 auto-configuration, Sa-Token filters, MyBatis-Plus ORM mappings, Cache-Aside caching, and Vue 3 reactivity in depth for course exams and defense.

## Implementation Decisions

### Modules & Layout
- `backend/`: Maven-based Spring Boot 3.3+ application (`com.shop.*`).
  - `common/`: Uniform `Result<T>` envelope, global exception handler, constants, base entities.
  - `config/`: Sa-Token security config, MyBatis-Plus pagination & auditing interceptor, Redis cache configuration, WebMvc static upload mapping.
  - `modules/auth/`: Customer & Admin login, registration, token verification.
  - `modules/catalog/`: Brand, Category, and Product entities, mappers, services, and controllers.
  - `modules/commerce/`: Cart and Order processing with atomic stock decrement logic.
  - `modules/system/`: User management, upload controller.
- `frontend/`: Vue 3 + Vite 5 + TypeScript + Pinia + Vue Router + Element Plus.
  - `src/views/portal/`: Storefront layouts, home banner, product gallery, product detail, cart, checkout, order history.
  - `src/views/admin/`: Admin layout, dashboard overview, brand management, category tree, product management, order processing, user list.
- `docs/principles/`: 6 structured markdown lecture docs corresponding to the key design and framework principles.
- `docs/sql/`: `schema.sql` and `data.sql` for one-click MySQL initialization.

### Schema & Persistence (MySQL 8.4)
- Tables: `sys_user`, `shop_brand`, `shop_category`, `shop_product`, `shop_cart_item`, `shop_order`, `shop_order_item`.
- Soft delete (`is_deleted`) and automated timestamp auditing (`created_at`, `updated_at`) via MyBatis-Plus meta-object handler.

### Caching Strategy (Redis)
- Cache-Aside pattern applied to high-read catalog queries (e.g. `brand:list`, `category:tree`, `product:detail:{id}`).
- Sa-Token session and token validity maintained in Redis with automatic TTL expiration.

## Testing Decisions
- **Highest Seam**: HTTP REST API level integration tests via Spring Boot Test (`MockMvc` and `TestRestTemplate`), asserting full database state mutations, HTTP response status codes, and JSON response bodies.
- Unit tests for concurrency: specifically validating atomic stock decrement under simultaneous order requests to prove no overselling occurs.
- Frontend verification: Component and API mock smoke tests verifying router guards, token injection into Axios interceptors, and reactive cart computations.

## Out of Scope
- Real third-party payment gateway integrations (Alipay/WeChat Pay merchant account credentials). A clean simulated payment flow is implemented instead.
- Multi-warehouse inventory routing and complex logistics tracking.
- Elasticsearch cluster deployment (MySQL B+ Tree fulltext and indexed prefix search are utilized to avoid heavy memory footprints on student machines).

## Further Notes
- Database seed script will automatically insert realistic brands (Apple, Huawei, Nike, Sony, etc.) and catalog items with royalty-free Unsplash product imagery, allowing immediate out-of-the-box demonstration.
