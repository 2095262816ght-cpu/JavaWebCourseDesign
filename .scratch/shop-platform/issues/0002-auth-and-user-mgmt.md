---
title: Authentication and User Management Slice
slug: 0002-auth-and-user-mgmt
blocked-by: [0001-scaffold-and-infra]
---

# 0002 - Authentication and User Management Slice

## Delivers
End-to-end authentication and user management built as deep, high-leverage modules:

1. **Backend (`com.shop.modules.auth` & `system`)**:
   - **Deep `AuthService` Interface**:
     - `register(RegisterCommand cmd): UserProfileVO`
     - `login(LoginCommand cmd): AuthTokenVO`
     - `getCurrentUser(): UserProfileVO`
     - `logout(): void`
     - *Hidden Implementation*: Password salting & BCrypt verification, Sa-Token JWT session creation, role assignment (`ROLE_ADMIN` vs `ROLE_CUSTOMER`), and account active-status enforcement.
   - **User Management Operations**:
     - Admin endpoints: `/api/admin/users` (paged list, toggle active status).
     - User profile endpoint: `/api/auth/profile` and avatar update via `StorageService`.
   - **Security Interception**:
     - Sa-Token route interceptor protecting `/api/admin/**` (requires `ROLE_ADMIN`) and authenticated customer actions (`/api/cart/**`, `/api/order/**`).

2. **Frontend (`frontend/src/stores/user.ts` & views)**:
   - **Deep `useUserStore`**:
     - Small interface: `login(credentials)`, `register(form)`, `fetchProfile()`, `logout()`.
     - *Hidden Implementation*: Persistent token synchronization with `localStorage`, reactive profile cache, and automated authorization header integration in Axios requests.
   - **Views**:
     - Storefront login/register modal and dedicated sign-in page.
     - Vue Router navigation guards redirecting unauthenticated users from `/admin` or checkout.
     - Admin User Management view: Customer table with status toggle switch and role badges.

3. **Code Simplification & Testing Guidelines**:
   - Use Java 21 `record`s for commands and query responses (`LoginCommand`, `RegisterCommand`, `AuthTokenVO`, `UserProfileVO`).
   - Eliminate shallow pass-through services: business constraints live directly inside `AuthService`.
   - **Interface as Test Surface**: Tests invoke `authService.register()` and `authService.login()` directly, asserting token issuance and state without mocking internal database mappers.

## Verification
- Register a new customer via API and UI -> verified in database with BCrypt hashed password (no plaintext).
- Log in with valid credentials -> receives valid JWT token.
- Access `/api/admin/users` with customer token -> receives 403 Forbidden.
- Log in as admin -> successfully loads user list and toggles customer account active status.
