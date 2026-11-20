## 2026-11-05T07:41:48Z
You are a teamwork_worker assigned to execute Milestone 3: Documentation Directory (docs) Alignment.

Working directory: D:\JavaWeb\.agents\teamwork\worker_m3_docs\
Original request path: D:\JavaWeb\.agents\teamwork\ORIGINAL_REQUEST.md
Scope document: D:\JavaWeb\PROJECT.md

MANDATORY FIRST STEP:
Read D:\JavaWeb\.agents\teamwork\ORIGINAL_REQUEST.md.
Also refer to D:\JavaWeb\PROJECT.md and the survey reports at D:\JavaWeb\.agents\teamwork\spec_miner_survey_docs\docs_inventory.md and D:\JavaWeb\.agents\teamwork\spec_miner_survey_docs\handoff.md.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

EXCLUSIVE FILE OWNERSHIP:
You own and may modify ONLY files within:
- D:\JavaWeb\docs\**

TASKS:
1. Fix Broken Physical Links to DeliveryAddress.java:
   - In D:\JavaWeb\docs\principles\01-springboot3-internals-and-architecture.md (line ~199):
     Update [DeliveryAddress](file:///D:/JavaWeb/backend/src/main/java/com/shop/modules/commerce/model/DeliveryAddress.java) to point to backend/src/main/java/com/shop/modules/commerce/dto/DeliveryAddress.java.
   - In D:\JavaWeb\docs\principles\03-orm-and-mybatis-plus.md (line ~149):
     Update the same broken link to point to dto/DeliveryAddress.java.
2. Align Sa-Token & JWT Secret Key in docs/principles/02-auth-and-satoken.md:
   - Line ~109: Update jwt-secret-key snippet from 'shop-secret-key-shop-secret-key-123456' to 'shop-platform-jwt-secret-key-32chars-min-abcdef123456' matching application.yml.
   - Lines ~257-258: Update useAuthStore references to useUserStore, and explain that request.ts reads token directly via localStorage.getItem('token').
3. Align Vue 3 & Pinia Store in docs/principles/06-vue3-and-vite-modern-frontend.md:
   - Lines ~191-192: Update useAuthStore references to useUserStore and note localStorage token handling.
4. Correct Router Guard Citation Drift in docs/principles/07-course-defense-qa.md:
   - Line ~267: Change citation from '第 45-75 行' to '第 103-136 行' (where router.beforeEach guard is physically located in frontend/src/router/index.ts).
   - Line ~258: Update description of route meta to reflect requiresAdmin: true and userStore.isAdmin.
5. Align ADR-0003 Lecture Count in docs/adr/0003-infrastructure-and-educational-artifacts.md:
   - Update lecture count from 6 to 9.
   - List all 9 lecture notes (01 through 09) with their complete filenames and topics.
6. Create Database Schema Documentation in docs/sql/README.md:
   - Create D:\JavaWeb\docs\sql\README.md providing a complete data dictionary for all 7 tables in shop_db:
     `sys_user`, `shop_brand`, `shop_category`, `shop_product`, `shop_cart_item`, `shop_order`, `shop_order_item`.
   - Document all columns, types, defaults, primary/unique/foreign keys, and indexes.
   - Explicitly document the `origin VARCHAR(64)` column in `shop_brand`.
   - Document DDD mapping to entity classes: `@TableField("user_id") private Long customerId;` in `ShopCartItem` and `ShopOrder`, and `DeliveryAddress` value object encapsulation.
   - Explain initialization mechanisms: schema.sql and data.sql in local setup and deploy/docker-compose.prod.yml (/docker-entrypoint-initdb.d/).
7. Verify link integrity across all modified markdown files in docs/.

OUTPUT REQUIREMENTS:
- Produce your self-contained handoff report at: D:\JavaWeb\.agents\teamwork\worker_m3_docs\handoff.md with all 5 sections (Observation, Logic Chain, Caveats, Conclusion, Verification Method).
- Update progress.md in your working directory.
- Send completion message to orchestrator when finished.
