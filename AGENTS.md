# Repository Guidelines

This file is the Codex entry point and deliberately a pointer: the conventions live in one place
(`CLAUDE.md`, `web/CLAUDE.md`, `.claude/docs/`) so they cannot drift here. Do not copy feature,
package, command or migration summaries into this file.

## Read first

1. `CLAUDE.md` (repo root) — product, donors ("port, don't reinvent" from Flow, Lettuce, Toadie),
   commands and gates, architecture and package layering, the OpenAPI contract, the working
   agreement. Its "Cross-cutting conventions" table lists the topic docs in `.claude/docs/`; read
   the row for the area you touch **in full** before editing it.
2. `web/CLAUDE.md` for anything under `web/`.
3. `api-guidelines/API-GUIDELINES.md` for any API work; cite its stable rule IDs in reviews.
4. `docs/AGENTS.md` and `docs/knowledge/README.md` for the knowledge base and backlog rules.

If a document and the executable configuration (code, CI, detekt/eslint/knip config, build files)
disagree, the configuration wins; fix the document in the same change.

## Codex-specific notes

- `CLAUDE.md` uses Claude's `@...` import syntax for the always-loaded docs
  (`.claude/docs/testing.md`, `.claude/docs/compatibility.md`). Codex does not expand it — open
  those files directly, along with the rest of the docs table.
- The playbooks in `.claude/skills/` (`verify`, `run-stack`, `api-review`) are plain Markdown and
  usable outside Claude.
- Run `./scripts/check.sh` before reporting work as done; do not skip the Docker-backed tests when
  claiming the whole suite passes.
- Golden fixtures (`*/src/test/resources/golden/`) and applied Flyway migrations are immutable.
