# Project Working Agreement

## Confirmed documentation rule

From 6 November 2026 onward, continuously document project assumptions, requirements, constraints, open points, questions, decisions, rules, conventions, principles, and tenets. Maintain a separate work backlog with `[next]`, `[todo]`, `[new]`, `[parked]`, and `[blocked]` statuses. This is a standing user instruction, not an occasional documentation task.

Update the relevant records as discussion or work changes the project. Documentation should reflect the current state before reporting a task complete. Capture substantive information concisely rather than copying the conversation verbatim.

## Record locations

| Information | Location |
| --- | --- |
| Product requirements and scope constraints | [Product direction](product-direction.md) |
| Engine proposals and design details | [Learning engine](learning-engine.md) |
| Curriculum, structural grammar, and vocabulary requirements | [Language foundations](language-foundations.md) |
| Shared UI conventions, brand direction, and logo requirements | [Visual identity](visual-identity.md) |
| Assumptions, proposed principles, and tenets | [Assumptions and principles](assumptions-and-principles.md) |
| Unresolved questions and other open points | [Open questions](open-questions.md) |
| Sources and research needs | [Research and sources](research-and-sources.md) |
| Work items, priorities, dependencies, and completion history | [Backlog](../../BACKLOG.md) |

Keep detailed requirements in their topic documents and link from the backlog. The backlog plans work; it does not replace the knowledge base.

## Status and decision conventions

Distinguish confirmed user requirements, confirmed decisions, proposals, assumptions needing validation, and externally sourced facts. Silence does not turn a proposal into an accepted decision. Keep material uncertainty visible and link sourced claims to their supporting references.

For consequential decisions, record what was chosen, why, the source or discussion date, and any relevant alternatives. When a decision changes, mark the prior choice as superseded and explain the replacement. Resolve questions explicitly rather than silently deleting them.

New general principles or rules must retain proposal status unless the user adopts them or an applicable instruction already establishes them. Illustrative examples and suggested sizes are not commitments.

## Backlog conventions

| Status | Meaning |
| --- | --- |
| `[next]` | Immediate proposed work, ready to begin within the authorized scope. Keep this list small. |
| `[todo]` | Planned work waiting its turn or an earlier work item. |
| `[new]` | Captured work or an idea awaiting scope and priority assessment. |
| `[parked]` | Deliberately deferred; record when to reconsider it. |
| `[blocked]` | Work cannot proceed because of a named dependency or restriction; record the unblock condition. |

Use stable item IDs and an observable completion condition. Mark proposed work as such when it goes beyond confirmed requirements. Planning an item does not authorize implementation, external publication, or a change in product scope.

Move completed items into a dated completion history with a result or evidence reference. This preserves the five requested active statuses without inventing another one. Update status when the actual condition changes; `[blocked]` should not merely mean that a task is later in the sequence.

## Confirmed workflow decisions

- **6 November 2026 — Repository documentation:** Keep the project knowledge base in `docs/knowledge/`; the user subsequently established continuous documentation as a standing requirement.
- **6 November 2026 — Separate backlog:** Use root-level `BACKLOG.md` with the five requested statuses. The location and detailed status meanings are initial working conventions chosen to implement the user's request.
- **6 November 2026 — Git initialization:** The user authorized initialization, then initialized the repository locally after the session's attempt was blocked. Verified the local repository and the configured GitHub origin, `https://github.com/liveweird/astrid.git`.
- **6 November 2026 — Commit and push authorization:** The user authorized pushing the project. The agent's staging attempt failed because the session could not create `.git/index.lock`. The user subsequently reported completing the push. Local `master` and the tracking reference `origin/master` both point to `e9521ee`; BL-018 is complete in the [backlog](../../BACKLOG.md).
- **Superseded constraint:** The initial discovery-only restriction ended when the user authorized the engine playground on 7 November 2026.
- **6 November 2026 — Foundation design and Git delivery:** The user authorized the next language/engine design pass and requested reasonably sized commits with merge/push to GitHub. Keep the existing changes, use cohesive commit boundaries, and merge/push the reviewed work within that scope. This delivery concerns the drafts and first-slice specification; the engine playground is the following milestone.

The existing root `AGENTS.md` remains unchanged under the earlier instruction not to modify it. Its initial empty-directory description is historical; consult the knowledge index and backlog for current project state.

## Git access troubleshooting

The workspace sandbox protects `.git` as read-only, but on 7 November 2026 the user enabled escalation requests. Approved escalated Git commands now permit the authorized repository operations. Branch creation, remote fetch, four documentation commits, a fast-forward merge into `master`, and the GitHub push through `1f3f168` succeeded. Git remote connectivity and authentication are verified for that delivery.

The [official sandbox guidance](https://learn.chatgpt.com/docs/agent-approvals-security) confirms that `.git` remains protected under workspace-write. The [permission-mode guidance](https://learn.chatgpt.com/docs/permission-modes) describes the permissions control below the composer and `/permissions` in the CLI. An approval-enabled mode can permit requests for otherwise restricted operations; Full access removes the sandbox boundary more broadly. Available modes depend on the app, configuration, and managed requirements. [Custom permission profiles](https://learn.chatgpt.com/docs/permissions) can provide narrower filesystem and network access where supported.

Use the applicable escalation/approval mechanism for Git writes and network commands that exceed the sandbox. The user's authorization to commit, merge, and push scoped project work remains recorded; command-level execution approval is a separate runtime control. Verify operation results rather than treating a UI setting alone as proof of access.

Earlier attempts to create the work branch and reach GitHub failed under the prior policy. The fallback patch groups preserved the reviewed commit boundaries and were used to stage those four commits without changing the final working files. BL-023 is now complete in the [backlog](../../BACKLOG.md); the earlier permission/network failures are historical rather than active blockers.

## Engine playground authorization

**7 November 2026 — Confirmed user decision:** “OK, go for it!” authorizes the proposed first runnable engine playground. Implement the connected beginner slice, reuse the agreed Flow stack and UI family, retain Docker Compose and Kubernetes, verify the result, and deliver reasonably sized commits merged and pushed to GitHub. This supersedes discovery-only restrictions, without authorizing public production exposure, broader course content, audio, or mobile packaging. See the [implementation decisions](implementation-decisions.md) and [reuse manifest](flow-reuse-manifest.md).

## Overnight continuation

**7 November 2026 — Confirmed user instruction:** After asking whether useful work could continue through 08:00, the user said “OK, keep going.” Continue autonomously through at least 08:00 Europe/Warsaw: finish and push the playground, then prioritize engine edge cases, content validation, accessibility and meaningful test gaps. Preserve the existing reading/writing pilot boundary, focused commits, continuous documentation and independent review. This does not bring forward production media, public hosting, listening/speaking or mobile packaging.

**7 November 2026 — Playground Git delivery observed:** Six focused commits merged by fast-forward into `master`; GitHub push and remote-object verification succeeded at `3b462a9`. The working tree was clean before creating `feat/pilot-hardening` for the explicitly authorized overnight continuation.

**7 November 2026 — Power management:** The user has enabled `caffeinate` themselves. Leave system power settings and sleep-prevention processes under their control.

**7 November 2026 — Hardening Git delivery observed:** Four focused commits merged by fast-forward and pushed to `master` at `527aa3c`; the remote object matched. The verified hardening image now serves the private Compose preview with its existing database volume. Broader foundation source review continues on `docs/foundation-review` without expanding the active learner slice.

**7 November 2026 — Source/reproducibility follow-up observed:** Six focused commits through `85bfba3` were fast-forwarded and pushed to `master`; every remote CI job passed. Follow-up runtime-outage checks use `fix/runtime-recovery` and a uniquely owned temporary Compose project. Never pause or remove the user's main preview/database for fault-injection tests.

**7 November 2026 — Recovery/browser delivery observed:** Focused commits through `3ca0533` were fast-forwarded and pushed to `master`; all five [branch CI jobs](https://github.com/liveweird/astrid/actions/runs/42340567827) and all five [master CI jobs](https://github.com/liveweird/astrid/actions/runs/42341038761) passed. The verified image serves the private preview with the same database container/process/volume. An older open tab submitted and replayed its original exercise after promotion without reloading. The separate QA stack was subsequently removed after exact ownership checks; the main database remained unchanged.

**7 November 2026 — Transport follow-up observed:** Three focused follow-up commits through `e5fbcdf` were fast-forwarded and pushed to master after all five [branch CI jobs](https://github.com/liveweird/astrid/actions/runs/42343476526) passed. Flow's pinned Ktor compression module reduced the measured JavaScript/CSS body transfer from 769,299 to 196,394 bytes with decoded equality. The verified image serves the private preview; both-browser main smoke checks and ownership-scoped QA cleanup passed. Current priority is user playground feedback, with specialist teaching review and any new exercise operation still separate decisions.

**7 November 2026 — Initial user review received:** The user tried the private playground and described it as simple, promising and a start in a good direction, with nothing they disliked or disagreed with. Recorded the [feedback](pilot-review-guide.md#initial-user-review--7-november-2026) and updated BL-031 to distinguish completed feedback capture from the still-open next-iteration choice. The existing typed-recall-then-repair recommendation remains a proposal; this feedback does not activate broader content, media or a new runtime format.

**7 November 2026 — Recall and repair authorized:** The user said “OK, go forth with what you've recommended” after the recommendation of typed verb recall followed by guided single-word error repair. Implement those operations within the existing grammar/vocabulary boundary, with independent review, meaningful verification and focused commits merged/pushed to GitHub. The initial next-step proposal is now selected; teaching approval, vocabulary assessment and broader course features remain separate. BL-034 tracks delivery.

**7 November 2026 — Recall and repair delivery observed:** Six focused commits through `1bfeb80` were pushed to the feature branch and fast-forwarded into local `master` after all five [branch CI jobs](https://github.com/liveweird/astrid/actions/runs/42360766547) passed, including 46 browser journeys without retries. Both local matrices passed, independent review findings were resolved, and the verified image serves the private main preview. Eight main browser checks and a retained legacy submission/replay passed with the same database container/process/mounts. Owned QA resources were removed. The [delivery record](recall-repair-delivery.md) closes BL-034; BL-035 proposes user review of the expanded playground. This final completion record is included in the authorized master delivery.

**7 November 2026 — Vocabulary assessment authorized:** The user said “Go for it” after the recommendation to add Danish-to-English meaning recognition and English-to-Danish introduced-headword recall for existing `bog`, `avis`, and `brev`. Implement this bounded milestone, including a lexical target/grading contract, optional recorded teaching support, meaningful verification and independent review. Preserve all published generation/grading and saved replay. Deliver focused commits merged/pushed to GitHub. Mixed sessions, broader vocabulary/grammar, production media and public hosting remain later work. BL-036 tracks implementation; BL-035 still tracks user review and is not presumed complete.

**7 November 2026 — Vocabulary delivery observed:** Nine focused commits through `7e4a9a4` were pushed and fast-forwarded into local `master` after all five [feature CI jobs](https://github.com/liveweird/astrid/actions/runs/42364486201) passed, including 58 browser journeys without retries. Both final local matrices passed all 58. The main preview passed its backed-up V1→V2 migration, preserved all old exercise/attempt evidence hashes and the database container/process/mounts, and passed ten browser checks. Both retained releases still submit/replay under their original evaluators. Owned QA resources were removed; the [delivery record](vocabulary-delivery.md) closes BL-036. BL-035 remains review of the eight-entry playground; mixed sessions remain later BL-009 work. This completion record is part of the authorized master delivery.

**7 November 2026 — Practice sessions authorized:** The user said “go with that” after the recommendation of short guided practice sessions: introduction, varied current exercises, feedback/retries, per-target summary and reliable resume within a valid development session. Implement BL-009 with explicit persistence, completion and prior-answer-exposure rules, preserving standalone exercises, all published grading/generation and immutable saved results. Use independent review, meaningful verification and focused commits merged/pushed to GitHub. New grammar/vocabulary, adaptive scheduling, streaks, mastery thresholds, accounts, audio/media and public hosting remain later work. Specific session size/focus and transaction details are implementation working choices, not separately user-selected product policy.

## Personal agent configuration follow-up

**7 November 2026 — User-authorized and observed:** Updated the seven personal personas under `~/.codex/agents/` after the user approved the proposed calibration improvements. All seven TOML files parse; names, model tiers and reasoning levels are unchanged. Clarified early contract handoff, lint/type checkpoints, shared-cache ownership and rendered-review evidence. Removed ineffective per-persona sandbox settings and documented inherited session permissions. Added the calibration/permission guidance to the global workflow; the user supplied the updated guidance in this session. These personal files are outside this repository and are not part of its Git delivery. Compare future calibration using observed work and validated findings, not invented cost figures.

The [practice-session contract](practice-sessions.md) records BL-009 implementation defaults, owner continuity, immutable attempt context and recovery acceptance.

## Guided practice delivery and bounded continuation

**7 November 2026 — Observed delivery:** BL-009 source through `d8262cc` passed all five [CI jobs](https://github.com/liveweird/astrid/actions/runs/42373281826), including 94 Linux browser journeys without retries. The private preview was backed up and upgraded to V3 with all prior evidence and database process/mounts preserved; ten main browser checks passed. The [delivery record](practice-delivery.md) captures review corrections and operational limits. The completion record accompanies the authorized merge/push.

**7 November 2026 — Continuation context:** The user said they would be away for approximately two hours and asked whether useful work remained. The assistant committed to finishing delivery, then checking interruption/restart/cross-tab recovery, support/feedback accessibility and meaningful plan variation. BL-037 captures this bounded quality work within the existing reading/writing engine; it does not select new curriculum, media, accounts or progression features. Continue documenting findings, fixing confirmed issues and delivering focused commits under the standing Git authorization.

**7 November 2026 — Bounded continuation delivery observed:** BL-037 source through `7eb7768` passed all five [CI jobs](https://github.com/liveweird/astrid/actions/runs/42380037939), including 114 Linux browser journeys without retries. Independent review and actual app/database restart checks passed. The backed-up main preview passed 30 browser smoke journeys, preserved all five immutable histories and its PostgreSQL process/mounts, and retained all three published evaluators. Owned QA resources were removed. The [delivery evidence](practice-continuity.md) records the support-focus and contrast corrections; the [variation inventory](practice-variation-audit.md) records the finite 294-plan semantic bound. This completion/review record accompanies the authorized merge/push. User review of guided practice and the expanded playground remains BL-035; broader content and teaching approval remain separate.

**7 November 2026 — Original practice Git delivery observed:** Fast-forwarded and pushed the practice milestone to `master` at `c9aa046`; the live GitHub ref matched. All five [master CI jobs](https://github.com/liveweird/astrid/actions/runs/42374564854) passed. BL-037 continues separately on `fix/practice-continuity`. Its local evidence now includes 114 passing browser journeys, readable normal/retried support, real shared-outbox retries, actual owned app/PostgreSQL restarts and the [variation inventory](practice-variation-audit.md); follow-up CI/promotion remain pending.

**7 November 2026 — Additional navigation follow-up observed:** BL-038 fixes the confirmed standalone picker detachment found during final CI inspection. Source `addae74` passed all five [CI jobs](https://github.com/liveweird/astrid/actions/runs/42383404784), including 114 Linux browser journeys without retries; independent review, 24 repeated candidate cases and 114 packaged native journeys also passed. The backed-up preview passed 14 further smoke checks, preserved all five immutable histories, PostgreSQL process/mounts and all three evaluators. Owned QA resources were removed. The [follow-up record](practice-continuity.md#standalone-format-navigation-follow-up--bl-038) closes BL-038 without expanding content or persistence scope. This completion record accompanies the authorized merge/push.

## Targeted follow-up practice authorization

**7 November 2026 — Confirmed user decision:** “OK, proceed” selects the recommended BL-039 milestone: choose existing grammar/vocabulary targets from a completed summary and start a separate varied review run. Define selection, prerequisites, size, lineage, revision and cross-run possible-exposure rules as documented implementation defaults, then implement, independently review and verify them. Preserve existing runs, frozen outcomes, published generators/graders and exact owner-bound recovery. Deliver focused commits merged/pushed under the standing Git authorization. This does not authorize broader curriculum, accounts, automatic scheduling, mastery claims or production media. BL-035 remains user review rather than a prerequisite presumed satisfied.
