# Open Questions

This register tracks open and resolved questions through the delivered private pilot and foundation source review on 7 November 2026. Resolved entries are marked explicitly. Remaining questions are planning work; their presence does not mean every question blocks current progress.

| ID | Question or open point | Related work |
| --- | --- | --- |
| Q01 | **Resolved:** The initial material targets English-speaking complete beginners. See [Product direction](product-direction.md). | BL-001 |
| Q02 | **Partly resolved:** General everyday Danish is the confirmed initial priority. The [first slice](beginner-slice-spec.md) proposes observable present-statement/article/form outcomes; final learning-success measures, proficiency range, and use of CEFR remain open. | BL-001, BL-008 |
| Q03 | What does coverage of sentence variants include: register, colloquial forms, exceptional constructions, and advanced grammar? | BL-001, BL-003 |
| Q04 | What session structure, progression, topic choice, and learner control do we want? | BL-009 |
| Q05 | The [grammar draft](danish-grammar-map.md) defines 58 skills and prerequisite relationships. How will a Danish teaching specialist validate the sequence and coverage? | BL-022 |
| Q06 | The [pilot seed](danish-vocabulary-seed.md) defines 23 entries and licensed senses/combinations. Which broader vocabulary, frequency/usage evidence, and reusable data sources should guide expansion? | BL-004, BL-025 |
| Q07 | The [pilot specification](beginner-slice-spec.md) proposes explicit grading and normalization cases. Which policies should extend to later formats and free writing, including partial correctness? | BL-007, BL-022 |
| Q08 | **Pilot resolved:** deterministic typed constructions and explicit bounded grading rules; no runtime AI dependency. Broader generation methods remain open. | BL-010 |
| Q09 | How should progress, mastery, review timing, and uncertainty about error causes be represented? | BL-011 |
| Q10 | A six-skill, three-construction, 23-entry slice with three exercise formats is implemented with draft language content. What specialist review, sampling, and learner evidence should establish its readiness and effectiveness? | BL-008, BL-022 |
| Q11 | Which proposed principles should become broader-course design rules? The pilot already operationalizes the bounded [implementation decisions](implementation-decisions.md). | BL-006, BL-007, BL-008 |
| Q12 | **Pilot resolved:** pure Kotlin engine, Ktor/Exposed R2DBC/Flyway/PostgreSQL and React/Mantine, with Compose and private Kubernetes. See [implementation decisions](implementation-decisions.md). | BL-012 |
| Q13 | Which specific mobile distribution approach should deliver the online web interface: browser installation, a web wrapper, or another approach? | BL-013 |
| Q14 | With Kubernetes and Docker Compose retained, which Kubernetes environment and hosting/database services should be used? What ingress/TLS, secrets, database placement, backup/restore, rollout, and availability expectations apply? | BL-012, BL-020 |
| Q15 | **Pilot resolved:** isolated development-cookie sessions, hashed tokens and database-owned exercises/attempts. Consumer registration, recovery and content administration remain deferred. | BL-020 |
| Q16 | **Pilot resolved:** see the concrete [Flow reuse manifest](flow-reuse-manifest.md). Reassess when adding consumer accounts or production delivery. | BL-020 |
| Q17 | **Pilot resolved:** revisioned reproduction inputs and presentation fingerprint, immutable session-scoped attempts and database-serialized idempotency. Long-term progress remains open; see [implementation decisions](implementation-decisions.md). | BL-007, BL-008, BL-020 |
| Q18 | Which red palette and light/dark accent shades should Astrid use, and how should brand, incorrect-answer, destructive-action, and system-error treatments be distinguished? | BL-021 |
| Q19 | Which unique logo direction should be developed: a Denmark-related symbol, an Astrid character portrait, or a combination? What illustration and small-icon treatment should it use? | BL-021 |

## Resolved clarifications

- **6 November 2026 — Initial learner and purpose:** The user selected complete beginners and general everyday Danish. Q01 is resolved; Q02's remaining outcome questions can be refined through the [first engine milestone](first-engine-milestone.md).

- **6 November 2026 — Mobile reuse:** The user confirmed that online mobile apps with reuse of the web interface are acceptable. Native UI and offline lessons are not current requirements. This narrows Q12 and Q13 without selecting a mobile packaging technology; see [Product direction](product-direction.md).
- **6 November 2026 — Infrastructure platforms:** The user confirmed retaining Kubernetes and Docker Compose, superseding the recommendation to defer Kubernetes. This partially resolves Q12 and Q16; Q14 tracks the remaining deployment design. See [Confirmed infrastructure decisions](product-direction.md#confirmed-infrastructure-decisions).
- **6 November 2026 — UI family and brand brief:** The user requires the shared sibling UI style/layout, an individual main color, and a unique Denmark-related or Danish-girl logo. Red is the preferred color direction; exact palette and logo decisions remain in Q18 and Q19. See [Visual identity](visual-identity.md).

Keep detailed context in the linked [backlog](../../BACKLOG.md) and topic notes. When resolving a question, record the answer and rationale in its owning document, then mark this entry resolved with a reference. Add newly discovered open points here even if they do not yet have a scheduled work item.

- **7 November 2026 — Implementation authorized:** The user approved the runnable playground. Q08, Q12, Q15–Q17 are resolved for that limited milestone through the implementation decisions and reuse manifest. Public hosting, teacher validation, full accounts and progression remain open.
