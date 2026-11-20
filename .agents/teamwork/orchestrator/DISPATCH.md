# DISPATCH

## 2026-11-05T04:09:59Z
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


## 2026-11-05T07:15:31Z
系统服务已重启恢复，用户指令“继续”。请恢复推进任务执行，检查并唤醒你的 3 个勘测子智能体（如已进入 idle 状态则发送消息激活），加快汇总勘测报告，输出 PROJECT.md 并推进具体文档修订（README.md 及 docs/ 目录）。
