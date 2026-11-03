---
title: Principles Lecture Series and Defense Docs Slice
slug: 0006-principles-lectures-and-defense-docs
blocked-by: [0005-cart-and-order-checkout]
---

# 0006 - Principles Lecture Series and Defense Docs Slice

## Delivers
Comprehensive educational lectures and defense preparation materials in `docs/principles/`, highlighting modern design principles, deep modules, and code simplification:

1. `01-springboot3-internals-and-architecture.md`:
   - Deep dive into Spring Boot 3 auto-configuration (`@EnableAutoConfiguration`, `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`).
   - Bean lifecycle, IoC/DI container instantiation, and **Deep Modules vs. Shallow Layering** (why we eliminate redundant pass-through service layers in favor of high-leverage service interfaces).
2. `02-auth-and-satoken.md`:
   - Principles of stateless JWT tokens: Header, Payload, Signature.
   - Sa-Token request filter interceptors, ThreadLocal context binding, and role-based access control (RBAC).
3. `03-orm-and-mybatis-plus.md`:
   - ORM vs. JDBC: AST (Abstract Syntax Tree) SQL rewriting in MyBatis-Plus.
   - Pagination interceptor mechanism (`PaginationInnerInterceptor`).
   - Database index design (B+ Tree) and MySQL ACID transaction characteristics.
4. `04-redis-caching-patterns.md`:
   - Cache-Aside pattern: Encapsulating caching logic inside deep modules (`CategoryService`, `BrandService`).
   - Strategies and defenses against Cache Penetration, Cache Breakdown, and Cache Avalanche.
5. `05-concurrency-and-inventory-safety.md`:
   - Preventing overselling under concurrent requests: comparison of pessimistic locking, optimistic locking, distributed Redis locks, and single-statement atomic updates (`stock >= quantity`).
   - Why atomic database decrement embodies the **Code Simplifier** philosophy (maximum safety with minimum moving parts).
6. `06-vue3-and-vite-modern-frontend.md`:
   - Vue 3 reactivity based on ES6 `Proxy` vs. Vue 2 `Object.defineProperty`.
   - Vite native ESM (ECMAScript Modules) bundling vs. traditional Webpack cold starts.
   - Deep Pinia stores as reactive client-side modules.
7. `README.md` and Course Defense Q&A Cheat Sheet:
   - One-command quickstart guide (setup, accounts, database credentials).
   - Top 10 anticipated teacher questions and model answers with direct code citations.

## Verification
- All 6 markdown lecture files are generated with clear technical diagrams and code references.
- README provides step-by-step reproduction instructions that any grader can follow without external assistance.
