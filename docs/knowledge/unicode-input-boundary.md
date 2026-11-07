# Unicode Input and Exact Replay

## Reproduced defect — 7 November 2026

A real PostgreSQL-backed API probe sent eight ASCII-escaped JSON text answers under an isolated QA session. Ordinary text, a decomposed accent, a supplementary emoji and embedded NUL returned valid JSON and stable same-ID replay. Four unpaired-surrogate cases initially returned 200 but an exact same-body replay returned 409; the original saved response remained readable.

This violates the pilot's exact-attempt recovery behavior for those inputs. It is an encoding/persistence boundary issue, not a Danish spelling diagnosis. The [JSON standard, section 8.2](https://www.rfc-editor.org/rfc/rfc8259#section-8.2) explicitly distinguishes interoperable Unicode strings from unpaired-surrogate sequences that its syntax can nevertheless admit. Do not describe every such request as syntactically invalid JSON.

## Implementation decision

Reject text answers containing unpaired UTF-16 surrogates with **400 before writing an attempt**. Preserve valid surrogate pairs, including supplementary characters. Use a fresh standard UTF-8 encoder for validation rather than silently replacing characters or sharing a mutable encoder across requests.

Retain the declared 512-character boundary, measured as raw Unicode code points rather than Java/Kotlin UTF-16 code units. This aligns the public `maxLength: 512` intent with supplementary characters: 512 emoji may pass, 513 may not. The [JSON Schema validation vocabulary, section 6.3.1](https://json-schema.org/draft/2020-12/json-schema-validation#name-maxlength) defines length in characters, rather than storage units. This is an assistant API-boundary choice; it does not equate a code point with a displayed grapheme or normalize before counting.

Raw accepted answers, NFC evaluation, Danish letter distinctions and grading rules stay intact. The ordinary request-byte limit still applies. NUL remains supported inside JSON string encoding; this does not authorize raw NUL in PostgreSQL text outside that serialization.

No content/generator/evaluation revision or database migration is needed for this transport validation. Existing stored rows are not rewritten. Previously accepted malformed submissions may now receive 400; their original saved responses remain available through the existing authorized GET. Do not claim the corrupted original request can be recovered from persisted replacement characters.

## Acceptance

- Four malformed escape patterns must return 400, save no attempt and consume no retry number; a valid answer using the same ID must still begin at retry 1.
- Accepted decomposed accents, proper supplementary pairs and embedded NUL must survive exact POST replay and saved GET unchanged.
- Check 512/513-code-point boundaries for both BMP and supplementary text.
- Run server tests, strict dependency verification, detekt and coverage, then repeat the probe against the rebuilt real PostgreSQL app. Keep the published engine compatibility fixtures unchanged.

The strict JVM gate passed with **29 server tests**, detekt and coverage verification; the unchanged engine compatibility checks also passed. Independent review found no remaining actionable defect after correcting new tests that had reused an exercise while expecting every valid recovery to be retry 1.

The rebuilt PostgreSQL-backed app passed **12/12** policy cases. Six accepted cases—ordinary text, decomposed accent, supplementary emoji, embedded NUL, 512 BMP characters and 512 emoji—returned valid UTF-8/JSON and byte-identical replay/GET. Four unpaired-surrogate patterns and both 513-character cases returned 400, left the attempted ID absent, then accepted a valid replacement using that ID at retry 1. Each case used a fresh session/exercise. Final Git/CI delivery remains pending under BL-030.
