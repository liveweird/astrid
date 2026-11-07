# Sentence Writing and Reading Comprehension Contract

## Authorization and delivery boundary

**7 November 2026 — User-authorized:** “OK, start with these” selects constrained sentence writing and short reading comprehension (BL-043). Implement both in the standalone playground first. Existing practice profiles, their plans, outcomes and follow-ups remain pinned to revision three. Guided integration, error hunting, questions/negation, new words/grammar, unrestricted composition/translation, AI grading, accounts and media remain separate decisions.

Implementation defaults: publish `pilot-4/generator-4/evaluation-4` and app `0.4.0`, retaining all three earlier generation/grading pairs. Current eight formats remain available with their existing behavior; append E009 and E010. Catalog becomes revision four. No SQL migration or new attempt-answer shape is needed. Preserve exact old snapshots, fingerprints, request defaults, result reads and replay-before-regeneration.

Before production edits, freeze revision-three compatibility from unchanged source `1d695c5` (application code equivalent to `e3c7556`). The new 1,320-row engine archive records all eight format cycles, seed/index boundaries and grading cases; it is compatibility evidence, not teaching approval. The server archive adds 1,806 public-wire/inspector/fingerprint cases (SHA-256 `ef8f62beb500a0e25da6d94030271043c6cb5c7b987544089e0ca6579024c93d`); engine SHA-256 is `ab467eeb5a97c334b9af2d201625de3f17ff9dcde338a319b3b372aeb1e12f0b`. Both exporters reject overwrites. Do not regenerate any published fixture to accommodate new behavior.

## E009: write a subject-first statement

Primary target is G005; G001–G004 and G006 where relevant are supplied/assumed prerequisites. Generate from all 99 currently licensed C001–C003 sentences. Give an English meaning and explicit introduced-vocabulary constraints. Do not expose a complete Danish solution or mandatory fully inflected word bank in the ordinary prompt. The collapsed optional introduction provides an unordered language reference (pronoun, present form, article/noun); opening it already records sticky support. A generic hint explains subject-first order. This is whole-sentence typing, rather than selecting supplied blocks.

English cues must distinguish singular/plural “you,” clarify referential `det`, use the selected noun senses and use idiomatic English for each verb. Do not invent a generic English inflection algorithm that produces misleading possession or aspect cues. Teaching remains draft pending BL-022.

Judge four facets independently: target satisfaction (G005 subject then finite verb), requested meaning, bounded Danish diagnosis, and response shape. A well-ordered sentence can express the wrong meaning; a meaning-preserving fronted sentence can miss G005. Green feedback requires target met, meaning met, acceptable language and met response constraints. Unknown writing records bounded inability to evaluate, not a general incorrect-Danish diagnosis.

For the cue “She reads a newspaper”:

| Answer | Target | Meaning | Language | Shape |
| --- | --- | --- | --- | --- |
| `Hun læser en avis.` | met | met | acceptable | met |
| `En avis læser hun.` | not-met | met | acceptable | met |
| `Han læser en avis.` | met | not-met | acceptable | met |
| `Hun har en avis.` | met | not-met | acceptable | met |
| `Hun læser et avis.` | met | met | known-issue | met |
| `Hun læse en avis.` | not-met | met | known-issue | met |
| `Hun studerer en avis.` | unevaluable | unknown | unknown | met |
| `Hun læser en avis. Han sover.` | unevaluable | unknown | unknown | not-met |

Recognize canonical licensed sentences and C002 complement-fronted counterparts. Diagnose only one unambiguous controlled deviation: a finite form replaced by its lemma, wrong `en/et`, or lowercase `i` replacing plural-you `I`. Multiple/ambiguous repairs remain unknown. For the unique lowercase-`i` case, recover the intended proposition only for diagnosis: order can be met and meaning compared, but language is known-issue (`PRONOUN_I_CASE`), never an accepted capitalization variant.

Preserve raw input; normalize NFC and harmless whitespace. Comparison permits one optional final period and ordinary first-letter sentence capitalization. Never globally lowercase Danish, remove arbitrary punctuation, ignore trailing prose, fuzzy-match spelling or accept a correct prefix of a larger answer. `completedSentence` echoes normalized submitted writing, never a silently corrected solution. Return `meaningSatisfaction: met|not-met|unknown` and `linguisticScope: danish-sentence` only for writing. Response-shape failure gets an explicit one-statement message; it must not display the existing “supplied choice” label.

## E010: read a short text

Use a separate reading descriptor, not a grammar or headword-recall target: `ReadingTask(facet = "participant-action-complement")`. Its catalog field is `readingTask`; its exercise field is `readingTarget`. Grammar/lexical targets are null. Public descriptors contain no answer noun IDs or key.

The initial pool has 24 semantic units: target subject `han/hun` × queried verb `har/læser` × a permutation of the three distinct readable nouns `bog/avis/brev`. Each unit has three licensed C002 facts:

1. Target subject, queried verb, noun X.
2. Other subject, queried verb, noun Y.
3. Target subject, other verb, noun Z.

X/Y/Z are all distinct. This supplies one distractor sharing the action and another sharing the participant; each answer noun appears once. Seed both sentence order and English-choice order, and cover all evidence positions across the cycle. Ask an English question such as “According to the text, what does she read?” Use the three existing selected English noun meanings as choices, with appropriate English markup. Danish passage sentences have Danish markup. Do not introduce new Danish question syntax, human names or pronoun antecedent assumptions. All mentions of each pronoun denote the same explicitly described participant.

Derive the answer by querying the actual generated propositions for the question's subject and verb; require exactly one matching fact. A fact missing from the text is not false. No negative, inferential or yes/no absence questions are generated. Optional recorded support may reveal the answer and must be treated as supported practice. This first version tests explicitly stated participant/action/object binding, not broad reading proficiency.

## Shared API and private metadata

Trailing nullable/default-omitted additions:

- Optional `IntroductionSection.bodyLanguage` and DTO mirror: E009 references use English section titles and Danish-only bodies tagged `da`, with noun/verb/pronoun sections ordered separately from the sentence. This avoids mixed-language reference strings; old sections omit the field. Pair-four fingerprints include the ordered language metadata.
- Engine `ReadingTask(val facet: String)`; `ExerciseFormat.readingTask`; `GeneratedExercise.readingTarget` (constructor parameter after source); `ExercisePrompt.passage: List<String>?`.
- `Evaluation.meaningSatisfaction: MeaningSatisfaction?`, enum `MET/met`, `NOT_MET/not-met`, `UNKNOWN/unknown`; add `LinguisticScope.DANISH_SENTENCE`.
- Private inspector `ReadingSourceDetails(propositions, questionSubjectId, questionVerbId, answerNounId, answer)` with `ReadingPropositionDetails(sentenceId, subjectId, verbId, objectId)`; append `ExerciseInspector.readingSource`.
- Serializable DTO mirrors use `@EncodeDefault(NEVER)` for every new default so older bytes stay exact. `ReadingTaskDto` serves both catalog and exercise descriptors. Writing uses existing `prompt.cue`/English and text answers; reading uses `passage`/Danish, `cue`/English question and choices/English. Existing before/after fields are empty and reading punctuation is empty.

Writing retains its single-sentence inspector. Reading has no fake singular canonical sentence/construction: those fields are absent, clauseRoles empty, lexicalIds the distinct union, and private propositions align with passage order. The internal reading source stores actual licensed sentences; the new answer key derives from their relation. Public exercise reads omit inspector solutions as before.

Fingerprint dispatch must explicitly retain literal pairs one/two/three before advancing CURRENT. Pair four includes old fields plus reading descriptor, ordered passage and all reading-source details, using unambiguous length/count framing. Extend fingerprint overrides with trailing `readingTargetForFingerprint`; existing inspector override includes reading source. Mutation tests must detect every nested value/order change. Pin old practice/review plans to three; do not let a CURRENT constant silently upgrade them.

## Verification and ownership

Engine owns generation, bounded grading, explicit retained snapshots/review exports and meaningful compatibility/variation tests. Server owns optional DTO omission, OpenAPI/types contract, private mapping/fingerprints, persisted read/replay and owner isolation. Web owns sentence input/character assistance, passage/question/choice layout, correct language/meaning/constraint feedback, recovery and component tests. Coordinator owns this contract, E2E, release files, shared Gradle/Docker/browser execution, independent reviews, Git and protected preview promotion.

Verify all 99 writing meanings, all 24 reading units, unique and related choices, evidence in every text position, deterministic cycles, known/off-target/unknown/shape/case examples, optional support leakage and all retained archive/wire bytes. Run meaningful engine/API/PostgreSQL and both-browser tests, theme/keyboard/narrow enlarged-text checks, actual persistence/replay and runtime scanning. Back up before app-only preview promotion; preserve the exact main database process/mounts and all immutable histories. Fault injection and fixture cleanup are confined to ownership-labelled QA resources. Record observed evidence before completing BL-043. No specialist approval or learning efficacy is inferred from software checks.


Rollback boundary: strict older readers cannot safely decode new pair-four presentation fields. Disable new-pair creation and retain evidence; an incompatible binary alone is not a supported downgrade. Resume new instances only with a compatible reader.

## Review corrections and pre-package checks

Independent engine, server and web reviews have no remaining actionable findings after corrections. Engine review added semicolon/colon shape rejection, protected the single-error boundary when lowercase plural `i` accompanies another error, and required independent literal meaning/gloss/passage oracles. The current review export now exposes all 99 writing cues/references/hints and all 24 reading passages/questions/ordered choices/answers. Retained revision-three rendering remains separate.

Web review made all four writing facets explicitly required for green feedback, including conservative handling of missing meaning/shape fields, and added Danish language tags to the optional reference. Server review correlated format, target, revision, lexical direction and prompt category in complete closed OpenAPI variants; strict positive/negative matrices cover invalid combinations. The generated web schema matches this final contract.

Observed local checks: 104 engine tests passed in both normal and fresh Arabic locales, engine/server lint and coverage gates passed, all four review exports succeeded, and 71 server tests passed including PostgreSQL persistence/replay and all immutable wire/fingerprint archives. The web passed 144 tests, API type checks, lint and build with unchanged coverage floors (94.45% statements, 87.46% branches, 96.85% functions, 97.25% lines). Toolchain/startup checks and eleven infrastructure-script tests passed. The packaged and operational results below complete the remaining delivery gates.


## Observed delivery — 7 November 2026

Delivered app `0.4.0` from source `607b046`, image `sha256:fa22db886519ef9519a574bbf7c80728238ed959ab171f0280eaef77c1b3893b`. Under **Engine playground → Generation controls → Exercise type**, select **Write a Danish sentence** or **Read a short text**. The catalog now has ten entries; guided practice and follow-ups retain their existing revision-three plans.

All five [source CI jobs](https://github.com/liveweird/astrid/actions/runs/42418723841) passed. Both the native packaged matrix and Linux CI passed all **198 browser journeys without retries or flaky results**, including 24 new-family journeys across Chromium/WebKit. Independent engine/API/web review has no remaining actionable findings. Twelve final rendered captures cover both themes, expanded references, feedback and fresh 320px contexts at 200% text; the latter reflow without horizontal overflow. Earlier resize-transition screenshot artifacts were replaced before acceptance.

Isolated upgrade checks preserved exact old results and all three older evaluators. An actual app-only QA restart preserved old mixed/verb practice and source-linked follow-up identity, and exact writing/reading reads, grading replay and fresh pair-four submissions. Runtime Git metadata/tooling is absent; the pinned image vulnerability policy passed.

A private **333,622-byte** custom-format database backup, mode `0600`, was verified readable before final acceptance. App-only preview promotion preserved the PostgreSQL container/process/mounts, schema versions 1/2/3, and immutable evidence for **1,985 exercises, 709 attempts, 136 practice sessions, 938 practice items and 484 attempt contexts**. Retained pairs one/two/three still submit and replay. All **40 post-promotion browser smoke journeys** passed without retries. Ownership-checked cleanup removed only the temporary QA stack and preserved the main app/database processes.

Private logs, backup and machine proofs remain under ignored `.local/writing-reading/`. The running build identifies the verified source commit; this documentation-only completion record accompanies the authorized merge/push. BL-043 is complete. User review (BL-035), Danish teaching approval (BL-022), broader language material and guided integration remain open; no Git tag or GitHub Release was published.
