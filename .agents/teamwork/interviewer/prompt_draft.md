Thoroughly review the latest codebase and update `README.md` and the documentation in `D:\JavaWeb\docs` so that all documentation accurately reflects the current state of the project.

Working directory: D:\JavaWeb
Integrity mode: development

## Requirements

### R1. README.md Synchronization
Review the entire current codebase (frontend, backend, deploy scripts, docker configurations) and update `README.md` to accurately reflect the latest project overview, architecture, prerequisite dependencies, configuration settings, environment variables, startup and shutdown commands (`start.bat`, `start.ps1`, `stop.bat`, `stop.ps1`), and project layout.

### R2. Documentation Directory (docs) Alignment
Thoroughly inspect all documentation under `D:\JavaWeb\docs` (including architecture, ADRs in `docs/adr/`, guidelines in `docs/principles/`, agent instructions in `docs/agents/`, and database schema documentation in `docs/sql/`) and update them to be completely consistent with the latest source code implementations, removing stale references and updating any modified data structures or design decisions.

## Acceptance Criteria

### Codebase Alignment & Verifiability
- [ ] All configuration options, ports, environment variables, and script commands documented in `README.md` match the actual scripts and configuration files in the repository.
- [ ] Every architecture description and module boundary documented in `docs` accurately corresponds to the current codebase structure in `frontend/`, `backend/`, and `deploy/`.
- [ ] Database documentation in `docs/sql/` matches the latest database schemas, entity definitions, or migrations used by the backend.
- [ ] Existing ADRs and principle documents in `docs/adr/` and `docs/principles/` reflect the latest design decisions and patterns implemented in code.
- [ ] All modified files conform to standard Markdown formatting and maintain existing stylistic conventions in the repository without broken internal links.
