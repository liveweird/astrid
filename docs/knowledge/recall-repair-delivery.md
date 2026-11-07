# Recall and Guided Repair Delivery

## Selected scope

On 7 November 2026, the user approved the recommended typed finite-present recall followed by guided single-word repair after a positive first playground review. The implementation adds three target-specific entries:

| Entry | Target | Eligible sentences | Response |
| --- | --- | ---: | --- |
| E004 · Supply the present form | G004 | 99 | One finite-present word for the named lemma |
| E005 · Repair the verb form | G004 | 99 | Replace the displayed infinitive with its present form |
| E006 · Repair the indefinite article | G006 | 92 | Replace the swapped article with unstressed `en` or `et` |

All six catalog entries share the existing 23 lexical records, 99 licensed sentences and G001–G006. Corrupted prompts never enter the canonical inventory. Optional lessons teach the exact forms and record supported practice; Danish character buttons assist input without setting hint evidence. This remains draft content awaiting Danish teaching review, with no mastery or efficacy claim.

## Compatibility and grading

Current sessions/catalog advertise `pilot-2/generator-2/evaluation-2`. The exact legacy pair remains available with its original grading, IDs, fingerprints and request defaults. All 335 archived compatibility cases and their provenance remain unchanged. Mixed pairs and new formats on the legacy pair are rejected.

For named-verb recall/repair, another finite verb is acceptable but off target only when it reconstructs an existing licensed sentence with the same construction, subject and complement. Unrecognized forms remain unknown. Legacy E001 grading is unchanged. Article repairs retain the gender-versus-numeral distinction. See the [full contract](exercise-extension-proposal.md).

No database migration is needed. New prompt metadata is omitted from legacy responses; immutable attempts replay before regeneration checks. A bare pre-change binary cannot decode the new prompt fields: rollback requires the compatible DTO reader, preserving stored evidence.

## Verification observed

- Strict JVM checks passed: 45 engine tests in normal and fresh Arabic-locale JVMs, 39 server tests, lint, coverage gates, current/legacy review exports and packaged server build.
- Frontend lint, build, generated API drift check and 53 tests passed. Coverage remains above all unchanged floors: 94.27% statements, 87.09% branches, 94.30% functions and 96.63% lines.
- All 46 native Chromium/WebKit journeys passed without retries, covering new grading states, character selection/caret, support tracking, uncertain replay, six-format navigation and enlarged layouts. Axe scans include light/dark prompts and new feedback states.
- Real PostgreSQL retained all three new prompt snapshots, exact saved results/replays and successful regenerated submissions after replacing the app process.
- Independent engine/API and web reviews completed. Fixed the repair export omitting its corruption, dark repair-word contrast, complement-free punctuation, and a dark feedback-footer contrast issue exposed by expanded Axe coverage.
- Toolchain, knowledge, startup-budget and documentation guards passed. The exact runtime image passed the configured fixed HIGH/CRITICAL scan. npm audits passed the high-severity threshold; the unchanged frontend tooling tree still reports moderate advisories.

Linux browser verification, GitHub CI, main-preview promotion and final Git delivery are pending at this checkpoint. BL-034 remains active until those checks complete.
