## 2026-11-05T04:27:12Z
You are a teamwork_spec_miner tasked with surveying and inventorying all existing documentation in D:\JavaWeb.

Working directory: D:\JavaWeb\.agents\teamwork\spec_miner_survey_docs\
Original request path: D:\JavaWeb\.agents\teamwork\ORIGINAL_REQUEST.md

MANDATORY FIRST STEP:
Read D:\JavaWeb\.agents\teamwork\ORIGINAL_REQUEST.md.

YOUR MISSION:
Perform a comprehensive audit and inventory of all documentation files currently in D:\JavaWeb:
1. Examine README.md in detail (structure, documented commands, ports, prerequisites, project layout, claims).
2. Examine all files and subdirectories under D:\JavaWeb\docs\:
   - Architecture documents (docs/architecture/ or root docs/*.md)
   - ADRs in D:\JavaWeb\docs\adr\
   - Principles/guidelines in D:\JavaWeb\docs\principles\
   - Agent instructions in D:\JavaWeb\docs\agents\
   - Database schema docs in D:\JavaWeb\docs\sql\
   - Any other docs, diagrams, or guides in docs/
3. For every file:
   - Record its title, purpose, target audience, and key claims.
   - Check internal links, referenced file paths, code blocks, port numbers, script names.
   - Note which files are stale, deprecated, incomplete, or referencing non-existent files/patterns.
   - Extract the precise specification requirements for what documentation needs updating to achieve 100% synchronization with code.

OUTPUT REQUIREMENTS:
- Write your full documentation inventory and audit to: D:\JavaWeb\.agents\teamwork\spec_miner_survey_docs\docs_inventory.md
- Produce your standard self-contained handoff report at: D:\JavaWeb\.agents\teamwork\spec_miner_survey_docs\handoff.md
- Update your progress.md regularly with timestamps.
- When done, send a concise completion message back to the orchestrator.

## 2026-11-05T07:16:59Z
**Context**: Documentation Spec Mining and Audit
**Content**: The system service has restarted. Please resume your documentation mining and audit immediately.
**Action**: Finalize your inspection of README.md and all docs in docs/ (including architecture, docs/adr/, docs/principles/, docs/agents/, docs/sql/). Compile your findings into docs_inventory.md and write handoff.md in D:\JavaWeb\.agents\teamwork\spec_miner_survey_docs\, then notify orchestrator upon completion.
