# Domain Boundaries and Commerce Flow

We decided to structure the Shop application as a unified dual-surface experience (Customer Portal + Admin Console) with an enhanced standalone product model, an end-to-end cart-to-order checkout flow, and a Dockerized Redis instance for cache and token management.

## Considered Options
- Surface architecture: Unified dual-portal (Vue router split) vs. admin-only console.
- Product model complexity: Enhanced standalone product model vs. enterprise SPU/SKU matrix.
- Commerce flow scope: End-to-end shopping loop (Browse -> Cart -> Checkout -> Stock deduction) vs. browse-only catalog.
- Caching middleware: Redis via Docker for Cache-Aside + token persistence vs. pure MySQL.

## Consequences
- Single frontend codebase handles both `/` storefront views and `/admin` administrative views with role-based navigation guards.
- Product entities contain direct pricing, stock quantities, brand/category foreign keys, and gallery assets without multi-dimensional SKU Cartesian product complexity.
- Order creation triggers transactional inventory checks and atomic decrements (`@Transactional`), providing clear educational material on database concurrency and atomicity.
- Redis provides a practical playground for illustrating Cache-Aside caching patterns, cache stampede mitigation, and fast distributed session invalidation.
