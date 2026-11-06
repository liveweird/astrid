# Open Questions

This register tracks open and resolved questions as of 6 November 2026. Resolved entries are marked explicitly. Remaining questions are planning work; their presence does not mean every question blocks current progress.

| ID | Question or open point | Related work |
| --- | --- | --- |
| Q01 | **Resolved:** The initial material targets English-speaking complete beginners. See [Product direction](product-direction.md). | BL-001 |
| Q02 | **Partly resolved:** General everyday Danish is the confirmed initial priority. Exact reading/writing outcomes, proficiency range, success measures, and use of CEFR remain open. | BL-001, BL-008 |
| Q03 | What does coverage of sentence variants include: register, colloquial forms, exceptional constructions, and advanced grammar? | BL-001, BL-003 |
| Q04 | What session structure, progression, topic choice, and learner control do we want? | BL-009 |
| Q05 | How will we establish prerequisite relationships and review the proposed grammar teaching order? | BL-002 |
| Q06 | Which vocabulary meanings and constructions are sufficient, and what frequency, usage, and data sources should guide selection? | BL-004, BL-005 |
| Q07 | How should grading handle valid alternatives, spelling, punctuation, capitalization, Danish letters, and partial correctness? | BL-007 |
| Q08 | Which generation and evaluation methods should we use, and what role, if any, should AI play? | BL-010 |
| Q09 | How should progress, mastery, review timing, and uncertainty about error causes be represented? | BL-011 |
| Q10 | What is the scope of the manual validation subset, and what teacher review or learner evidence is needed before implementation? | BL-008 |
| Q11 | Which proposed engine principles should become agreed design rules? | BL-006, BL-007, BL-008 |
| Q12 | What architecture, storage, and platform technologies should be chosen after the learning requirements are clearer? | BL-012 |
| Q13 | Which specific mobile distribution approach should deliver the online web interface: browser installation, a web wrapper, or another approach? | BL-013 |
| Q14 | With Kubernetes and Docker Compose retained, which Kubernetes environment and hosting/database services should be used? What ingress/TLS, secrets, database placement, backup/restore, rollout, and availability expectations apply? | BL-012, BL-020 |
| Q15 | What account and ownership model is needed: guest practice, learner registration, account recovery, and content administration? What session-storage and scaling requirements follow? | BL-020 |
| Q16 | Which Flow components and conventions should be adopted, adapted, or excluded after reviewing the assessment? | BL-020 |
| Q17 | How should versioned exercise payloads, retry-safe attempts, content updates, and concurrent progress changes be represented? | BL-007, BL-008, BL-020 |
| Q18 | Which red palette and light/dark accent shades should Astrid use, and how should brand, incorrect-answer, destructive-action, and system-error treatments be distinguished? | BL-021 |
| Q19 | Which unique logo direction should be developed: a Denmark-related symbol, an Astrid character portrait, or a combination? What illustration and small-icon treatment should it use? | BL-021 |

## Resolved clarifications

- **6 November 2026 — Initial learner and purpose:** The user selected complete beginners and general everyday Danish. Q01 is resolved; Q02's remaining outcome questions can be refined through the [first engine milestone](first-engine-milestone.md).

- **6 November 2026 — Mobile reuse:** The user confirmed that online mobile apps with reuse of the web interface are acceptable. Native UI and offline lessons are not current requirements. This narrows Q12 and Q13 without selecting a mobile packaging technology; see [Product direction](product-direction.md).
- **6 November 2026 — Infrastructure platforms:** The user confirmed retaining Kubernetes and Docker Compose, superseding the recommendation to defer Kubernetes. This partially resolves Q12 and Q16; Q14 tracks the remaining deployment design. See [Confirmed infrastructure decisions](product-direction.md#confirmed-infrastructure-decisions).
- **6 November 2026 — UI family and brand brief:** The user requires the shared sibling UI style/layout, an individual main color, and a unique Denmark-related or Danish-girl logo. Red is the preferred color direction; exact palette and logo decisions remain in Q18 and Q19. See [Visual identity](visual-identity.md).

Keep detailed context in the linked [backlog](../../BACKLOG.md) and topic notes. When resolving a question, record the answer and rationale in its owning document, then mark this entry resolved with a reference. Add newly discovered open points here even if they do not yet have a scheduled work item.
