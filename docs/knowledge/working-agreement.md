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

**7 November 2026 — Recovery/browser delivery observed:** Focused commits through `3ca0533` were fast-forwarded and pushed to `master`; all five [branch CI jobs](https://github.com/liveweird/astrid/actions/runs/42340567827) and all five [master CI jobs](https://github.com/liveweird/astrid/actions/runs/42341038761) passed. The verified image serves the private preview with the same database container/process/volume. An older open tab submitted and replayed its original exercise after promotion without reloading. The separate QA stack remains owned temporary infrastructure until the remaining scoped checks and cleanup complete.
