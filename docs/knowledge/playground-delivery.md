# Engine Playground Delivery

## Verified baseline — 7 November 2026

The first runnable milestone implements the authorized reading/writing pilot: six skills, 23 lexical entries, three formats, 99 licensed sentences (92 for article practice), deterministic variation, explicit grading facets and persisted session-owned attempts. The red Mantine shell follows the sibling UI family; the Denmark-related mark remains provisional.

The complete private stack is available at `http://localhost:8088`. Docker Compose keeps PostgreSQL data across ordinary restarts. Kubernetes manifests retain ClusterIP access through port-forwarding; no public deployment was performed for this baseline.

## Observed checks

| Check | Result |
| --- | --- |
| Strict Gradle checks, detekt and distribution packaging | Passed |
| Engine tests | 22 passed; 85% Kover floor passed |
| API/persistence tests | 11 passed; 80% Kover floor passed |
| Real PostgreSQL | Migration, restart reread, session isolation and concurrent idempotency passed |
| Runtime OpenAPI validation and generated frontend type drift | Passed |
| Frontend tests | 19 passed; full-source coverage floors passed |
| Frontend lint and production build | Passed |
| Packaged Playwright journeys | 4 passed: all formats, lost-response replay, narrow keyboard input, dark-OS/light-theme override |
| npm dependency audits | Web and E2E high-severity gates passed |
| Gradle dependency scan | High/critical gate passed |
| Packaged runtime scan | Fixable high/critical gate passed |
| Kubernetes schemas | All 7 resources and rendered Kustomize output passed pinned schema validation |
| Documented local gate | `./scripts/check.sh` passed |
| Independent engineering review | Confirmed findings resolved |

Native Firefox additionally verified rendering and a correct present-form submission with persisted feedback. Browser-host access later became unavailable; the packaged Playwright run supplied the final theme and journey checks. HEAD requests are not the static GET checks: the actual JavaScript GET returned 200 with JavaScript MIME.

## Boundaries and follow-up

These are software checks, not Danish teaching approval or evidence of learning effectiveness. The content remains draft; no mastery projection, consumer account system, audio, public hosting or final artwork is claimed. Keep production and linguistic review work visible in the [backlog](../../BACKLOG.md).

The user authorized continued hardening through at least 08:00 Europe/Warsaw. The [hardening plan](pilot-hardening-plan.md) focuses on compatibility fixtures, negative content validation, transaction failure/replay evidence and measured accessibility. Git delivery and remote CI outcomes are recorded after observing them.

## Git delivery

The six baseline commits were fast-forwarded from `feat/engine-playground` into `master` and pushed to GitHub at `3b462a9a3920958d46e77890aadabfc76a88dd20`. `git ls-remote origin refs/heads/master` confirmed the same object. The first remote run exposed a platform-dependent test assumption about whether collapsed content stays mounted. Commit `a724372` checks the intended accessibility behavior instead; [all baseline CI jobs passed](https://github.com/liveweird/astrid/actions/runs/42316209265). Subsequent overnight work uses `feat/pilot-hardening`; its separate results are in the [hardening delivery record](hardening-delivery.md).
