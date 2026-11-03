# Infrastructure and Educational Artifacts

We decided to use a dedicated MySQL database `shop_db` with automated seed scripts, local static file storage with Spring resource mapping for product images, SpringDoc OpenAPI 3 for interactive API contracts, and dedicated educational lectures in `docs/principles/` explaining core principles.

## Considered Options
- Database setup: Automatic schema and data seed scripts for `shop_db` vs. manual user-provided database.
- Image storage: Local disk storage with URL mapping vs. public cloud OSS (Aliyun/Tencent) vs. Docker MinIO.
- API documentation: SpringDoc OpenAPI 3 + uniform `Result<T>` structure vs. manual Postman collections or raw controllers.
- Educational artifacts: Dedicated lecture notes in `docs/principles/` covering 6 core topics + rich code annotations vs. bare code implementation.

## Consequences
- The application remains 100% runnable offline without external cloud dependencies or credentials.
- Image uploads are stored locally and served via Spring MVC resource handlers (`/uploads/**`).
- Complete Swagger documentation will be available out-of-the-box at `/swagger-ui/index.html`.
- `docs/principles/` serves as direct study and defense material for the course assignment.
