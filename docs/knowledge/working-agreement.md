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
- **Continuing constraint:** Remain in discovery and write no application code until the user changes that instruction.
- **6 November 2026 — Foundation design and Git delivery:** The user authorized the next language/engine design pass and requested reasonably sized commits with merge/push to GitHub. Keep the existing changes, use cohesive commit boundaries, and merge/push the reviewed work within that scope. This delivery concerns the drafts and first-slice specification; the engine playground is the following milestone.

The existing root `AGENTS.md` remains unchanged under the earlier instruction not to modify it. Its initial empty-directory description is historical; consult the knowledge index and backlog for current project state.

## Git access troubleshooting

The current execution policy permits project-file edits but protects `.git` as read-only and disables sandbox approval requests. User authorization to commit and push is already recorded. The user completed the initial push locally; authentication and remote connectivity from agent-run commands have not been tested.

The [official sandbox guidance](https://learn.chatgpt.com/docs/agent-approvals-security) confirms that `.git` remains protected under workspace-write. The [permission-mode guidance](https://learn.chatgpt.com/docs/permission-modes) describes the permissions control below the composer and `/permissions` in the CLI. An approval-enabled mode can permit requests for otherwise restricted operations; Full access removes the sandbox boundary more broadly. Available modes depend on the app, configuration, and managed requirements. [Custom permission profiles](https://learn.chatgpt.com/docs/permissions) can provide narrower filesystem and network access where supported.

No permission-mode change has been confirmed. The initial push no longer requires action, but future agent-run staging and commits still require an effective permission change. Do not interpret either the user's successful local push or a UI setting alone as proof of agent access. Subsequent documentation edits remain local until committed and pushed.

The latest delivery attempt failed to create the work branch `docs/danish-learning-foundations` because Git could not write its ref under `.git`. A read-only `gh api repos/liveweird/astrid` request also failed to connect to `api.github.com`. The 7 November environment update still lists `.git` as read-only. GitHub authentication and the live remote branch state remain unverified; authorization is already present. Track completion in BL-023 and keep commit-sized patch groups available until the execution policy allows the requested Git work.
