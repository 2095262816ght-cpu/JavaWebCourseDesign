## 2026-11-05T07:31:21Z

You are a teamwork_worker assigned to execute Milestone 1: Codebase Contract & Config Synchronization.

Working directory: D:\JavaWeb\.agents\teamwork\worker_m1\
Original request path: D:\JavaWeb\.agents\teamwork\ORIGINAL_REQUEST.md
Scope document: D:\JavaWeb\PROJECT.md

MANDATORY FIRST STEP:
Read D:\JavaWeb\.agents\teamwork\ORIGINAL_REQUEST.md.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

EXCLUSIVE FILE OWNERSHIP:
You own and may modify ONLY these files:
- D:\JavaWeb\frontend\src\api\product.ts
- D:\JavaWeb\frontend\src\views\admin\ProductManagementView.vue
- D:\JavaWeb\backend\src\main\resources\application-prod.yml

TASKS:
1. In D:\JavaWeb\frontend\src\api\product.ts:
   Add missing '/api' prefix to all endpoints that lack it:
   - '/portal/home' -> '/api/portal/home'
   - '/portal/products' -> '/api/portal/products'
   - '/portal/products/${id}' -> '/api/portal/products/${id}'
   - '/admin/products' -> '/api/admin/products'
   - '/admin/products/${id}' -> '/api/admin/products/${id}'
   - '/admin/products/${id}/status' -> '/api/admin/products/${id}/status'
2. In D:\JavaWeb\frontend\src\views\admin\ProductManagementView.vue:
   Update '/common/upload' -> '/api/common/upload'
3. In D:\JavaWeb\backend\src\main\resources\application-prod.yml:
   Update line 58: 'token-style: uuid' -> 'token-style: jwt'
4. Run verification commands:
   - Execute 'npm --prefix frontend test' and document test results (expected: 15 passed).
   - Execute 'npm --prefix frontend run build' and document build results (expected: 0 errors).
   - Execute 'mvn -f backend/pom.xml test' and document test results (expected: 44 passed).

OUTPUT REQUIREMENTS:
- Produce your self-contained handoff report at: D:\JavaWeb\.agents\teamwork\worker_m1\handoff.md with all 5 sections (Observation, Logic Chain, Caveats, Conclusion, Verification Method).
- Update progress.md in your working directory.
- Send completion message to orchestrator when finished.
