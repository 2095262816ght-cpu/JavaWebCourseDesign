# Core Tech Stack and Architecture Selection

We decided to build the Shop platform as a monorepo containing a Spring Boot 3.3+ backend and a Vue 3 frontend, utilizing Sa-Token for authentication, MyBatis-Plus for persistence over MySQL 8.4, and Vite for modern frontend tooling.

## Considered Options
- Backend runtime: Spring Boot 3.3+ on Java 21 vs. legacy Spring Boot 2.7 on Java 8/11.
- Authentication: Sa-Token (JWT mode) vs. Spring Security 6 vs. bare-metal interceptors.
- Persistence: MyBatis-Plus 3.5+ vs. Spring Data JPA / Hibernate vs. raw MyBatis.
- Frontend: Vue 3 + Vite + TypeScript + Element Plus vs. React vs. server-side templating (Thymeleaf/JSP).
- Repository layout: Monorepo (`backend/` + `frontend/`) vs. multi-repo.

## Consequences
- Requires JDK 21+ compliance (running on local JDK 25).
- Jakarta EE namespace (`jakarta.*`) is adopted everywhere instead of legacy `javax.*`.
- Sa-Token handles both stateless JWT token issuance and RBAC authorization without heavy filter configuration overhead.
- MyBatis-Plus enables zero-SQL boilerplate for single-table CRUD operations while retaining full custom SQL flexibility for multi-table queries.
- Monorepo ensures atomic commits, single-command setup, and straightforward assignment packaging.
