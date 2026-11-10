# First Module 1 Sessions: Activation Contract

## Authorization and status

**9 November 2026 — Runtime implementation authorized (BL-052):** the user's subsequent
“go forth” selects implementation of this bounded three-session contract and the standing
verified 0.9.0 commit/merge/push, tag/GitHub Release and backed-up private-preview delivery.
The planning-only boundary below is historical; runtime delivery is verified below at 0.9.0. Wider
Module 1 content, targeted-review expansion and teaching approval remain outside this selection.

**9 November 2026 — User-authorized planning milestone (BL-051):** “go-go” selects the
recommended activation contract for the first three authored sessions: singular human
subjects, group human subjects and stored present `være → er`. Deliver concrete teaching,
answer and compatibility requirements before implementation. The proposed runtime milestone
was **0.9.0**; it was a separate selection, now authorized above. The planning document added no running course content,
published identifiers, API fields or release tag.

This narrows the [Module 1 authoring packet](module-one-course-slice.md), rather than activating
all 21 sessions. The three-session boundary is user-selected; six assessed items per session,
the names below and technical identifiers are assistant design defaults for implementation
review. Language remains a source-checked draft awaiting [Danish teaching review](pilot-teaching-review.md).
No duration, mastery threshold or learning-effectiveness claim is established.

### Runtime implementation decisions

The following are bounded implementation choices for the selected milestone, not claims of
new user requirements or already delivered behavior:

- Publish `pilot-8/generator-8/evaluation-8`. Keep all old E001–E015 paths and shared pools
  unchanged; add E016/E017 for subject-pronoun selection/recall and E018/E019 for bounded
  `er` selection/recall, reusing the existing choice/text answer transports.
- Pin six-item profiles `module-one-singular/module-one-singular-1`,
  `module-one-groups/module-one-groups-1`, and `module-one-er/module-one-er-1` literally to
  pair eight. English titles are “Talking about one person”, “Talking to and about groups”,
  and “Using the verb to be”. The chooser groups them under “Module 1: First steps”.
- Derive each exercise's `targetScopeId` from its authored task/index; do not add a client
  scope-selection axis to generation requests. New scope metadata is carried in exercise,
  frozen practice and summary descriptors; absent values preserve old serialization/keys.
- Persist `introductionRequired`, `completionRuleId=module-one-known-outcome-1` and
  `exposurePolicyId=module-one-exposure-1` on new plans, with omitted legacy defaults.
  Reuse the existing introduction support mutation; under the owner lock, require its sticky
  saved state before grading a new bound attempt. Stored-attempt replay still returns first.
  A new-plan-only public `canAdvance` projection lets the UI render the server's decision.
- Keep introduction sections in English explanation or Danish form/example blocks using
  their explicit language tags. Supply fictional names without independently assessing them;
  allocate `L025/L025-S01` to the teacher noun only after confirming that identity is unused.

The complete published values, content counts and exposure keys will be recorded with
implementation evidence. No targeted follow-up fallback is added for these profiles.

**Measured CI budget:** the unchanged planning baseline's [CI run](https://github.com/liveweird/astrid/actions/runs/42709008358)
passed all five jobs and 312 Linux journeys without retries/flakies. Its image job consumed
1,476 seconds of the former 1,500-second budget (browser/startup step: 1,205 seconds).
The additional first-step journeys require a 35-minute image-job budget; the existing
40-minute nightly budget is retained. All tests, scans, browser per-test limits, worker/retry
settings and coverage floors remain enforced. This is an execution-budget adjustment based
on observed workload, not omitted verification.

## Learner flow and completion

Show a **Module 1: First steps** group in the existing practice chooser, containing three
independent profiles in recommended order. Reuse the current shell, development-session
ownership, six-item run lifecycle and summary/repeat interactions. Each profile can be opened
directly: prerequisite labels are explanatory, not proof of prior learning. Present that
profile's teaching/reentry material before its first assessment, and keep essential unassessed
sentence glosses inline. An existing learner may deliberately reread it. Do not require a
course graph, new account, global learning history or score-based unlocks.

The flow is explanation and annotated example → selection → typed practice in a changed
context → per-operation summary. The final typed items are the short review; a separate
fourth review session is outside this contract. On completion, offer repeat and a link to the
recommended next profile, with its own introduction. Do not silently create the next run.

For these new plan revisions only, an item can advance after feedback for its latest saved
**completion-eligible** attempt. Choose latest by persisted retry number and check under the
existing owner lock; a newer unknown answer supersedes an earlier eligible result. Eligibility is:

```text
(targetSatisfaction in {met, not-met}
 AND linguisticStatus in {acceptable, known-issue})
OR
(responseConstraintStatus == not-met
 AND issues contains a revision-defined known-form/shape code)
```

The second branch's proposed closed issue-code allowlist is `STANDALONE_PRONOUN_CASE_REQUIRED`
and `ONE_WORD_RESPONSE_REQUIRED`. It permits a certain written-form/shape correction even
when the grammar target is `UNEVALUABLE`; feedback text or a generic unknown code cannot
qualify. A known wrong introduced word may advance. An unsupported single word, blank input,
timeout or no saved attempt cannot. An unresolved item offers support, retry or leaving the
run resumable; it never requires guessing until correct. Persist this rule with the plan.
Existing plans keep their current advance behavior. Session completion means all six
activities received eligible attempts, not that every grammar target was evaluable or mastered.
Network recovery does not add learner errors or completed activities.

### Required foundation support on direct entry

G003/G004 depend on G002 and, through it, G001 in the [grammar map](danish-grammar-map.md).
Supply the relevant foundation subset before using its terminology, including on direct
S02/S03 entry; a prerequisite ID alone does not satisfy this requirement:

- Explain a written word versus a complete sentence using that profile's supplied material.
  In S03, show `en bog` as two words learned as one noun bundle, and `at være` as the
  dictionary teaching bundle versus one-word `er`. Show Danish `æ/ø/å` and the
  character-entry controls without requiring pronunciation.
- Annotate supplied `Jeg er Anna.`: `Jeg` is a pronoun (word class) functioning as the subject;
  `er` is the present verb; `Anna` names that subject, not an object. Explain “subject” in plain
  English before asking for a subject word. Give the English sentence meaning throughout.
- Before S03, identify noun versus article in `en bog`/`et brev`, and explain dictionary
  infinitive `være` versus the present form used in a sentence, `er`. These are supplied
  distinctions, not new article, role-labelling or productive-inflection assessments.
- Explain the difference between a standalone introduced word and its capitalization at
  the beginning of a sentence. Reintroduce the exact casing instruction before typed tasks.

This is task-specific foundation support, not a claim of complete G001/G002 coverage or
an edit to their prerequisite graph. Its annotated examples also create recorded answer
exposure. Unused word classes such as adjectives remain outside the assessed slice.

## Session cards and exact assessment budget

Local S01–S03 labels refer to the existing authoring packet; they are not published IDs.
Choice banks contain related introduced pronouns or the introduced forms of the named verb.
Every item has one primary target; all other language is supplied or glossed.

| Session | Teaching and worked example | Six assessed items, in order | Explicit limit |
| --- | --- | --- | --- |
| **S01 — Talking about one person** | Introduce `jeg` (speaker), `du` (one addressee), `han` and `hun` (the explicitly described referent). Read the four mappings in two small sections. Supply `er` and fictional names where a sentence is shown: Anna speaks about herself, `Jeg er Anna.` Explain that only the subject word is requested. | 1–4: one choice for each of the four pronouns, using explicit English situations and the bank `jeg / du / han / hun`. 5–6: type two distinct introduced pronouns in changed situations; rotate the pair deterministically across seeds. | A run checks all four selections but only two recalls. Do not infer gender from names, a picture or appearance. Standalone reference tasks cover `han/hun` without requiring a new complement. `er` is support, not a verb assessment. |
| **S02 — Talking to and about groups** | Introduce `vi` (group including the speaker), `I` (several addressees), `de` (a mentioned group). Review `du` explicitly. Supply `at sove → sover`, glossed as sleep/sleeps in every sentence task. Worked example: the speaker and another person are asleep, `Vi sover.` | 1–3: select each group pronoun once, with the bank `vi / I / de / du`. 4–6: type each group pronoun once in changed situations. | State who speaks, who is addressed and who is discussed. English “you” always specifies one/several people. `sover` is never graded here. `de` has plural human reference only in this subset; other Danish uses are outside scope. |
| **S03 — Using er** | Teach `at være → er` explicitly as a stored irregular form. Review the human subjects. Introduce fixed identifying `det`, bundles `en bog`, `et brev`, `en lærer`, and the supplied occupational frame `Jeg er lærer.` Contrast `er` with infinitive `være`; do not teach a past-tense distractor. | 1–2: choose `er` from `er / være`, once in fixed identification and once with a singular human subject. 3–6: type only the missing verb: `Det ___ en bog.`, `Det ___ et brev.`, `Jeg ___ lærer.`, `Hun ___ lærer.` Rotate the two human subjects within the licensed singular set across seeds. | All answers are the same invariant `er`: these are supported form checks across contexts, not six different verb skills. Article, noun, identifying `det`, profession and word order are supplied. No adjective agreement, plural professions or referential `den/det`. |

The table specifies a finite minimum inventory, not unrestricted generation. An implementation
must enumerate each situation, reference role, cue, gloss, answer and choice rationale.
Paraphrasing an English cue or shuffling a bank is presentation variation. A changed reference
situation is a new contextual task, not evidence of unseen Danish vocabulary or transfer.
Do not use inferred learner knowledge to remove the teaching/reference material.

## Language ledger and licensed frames

| Material | Introduction and assessment | Licensing boundary |
| --- | --- | --- |
| L001–L004 `jeg/du/han/hun` | Explicitly introduced in S01; subject reference assessed by selection and one-word recall. Reintroduced as needed on direct entry to later sessions. | Only the stated speaker/addressee/referent role. An English identity context must establish the intended person; no unstated pronoun assumption. |
| L005–L007 `vi/I/de` | Introduced in S02; selection and recall. S03 supplies the subjects its particular items use. | Preserve lexical uppercase `I`; exclude formal `De`, object forms and unrestricted reference. Exclusion from the exercise inventory does not make other Danish pronouns invalid. |
| L010 `at være → er`, Anna/Nora | Supported S01 example; S03 explicitly teaches and assesses the finite form. Fictional names are supplied literals, not personal-data input. | New bounded C004 subframe: singular `jeg/du/hun` with the explicitly contextualized name Anna or Nora. Naming, not general adjective/profession inference. Do not generate group subject + one person's name. |
| L013 `at sove → sover` | Supplied/glossed in S02; neither choice nor recall of the verb is assessed. | Reuse C001 human subject + `sover`, without extra circumstances, tense, negation or questions. |
| L008 `det`, L014 `en bog`, L018 `et brev` | Introduced/supplied in S03, including noun articles and English meanings. | Existing C003 identifying frame only: `Det er en bog/et brev.` Identify an explicitly indicated item in text; no media dependency or referential-pronoun assessment. |
| `en lærer`, teacher sense | New candidate lexical packet from the [language pack](module-one-language-pack.md); allocate a stable sense ID only during implementation. | New narrow C004 subframe: singular `jeg/du/han/hun + er lærer`. Introduce the noun with its article; explain this supplied bare occupation frame separately. No universal profession-article rule, plural noun or noun recall target. |

These are complete allowlists for this slice. C004 is a catalogue family, not permission to
generate all copular complements. Do not change C001–C003 permissions in retained snapshots.
Early statements demonstrate subject–finite-verb order, but neither this slice nor its summary
claims G005/G021 coverage. The author's later V2, question and negation sessions remain in
their dependency order; do not teach subject-first order as the only possible Danish order.

### Source checks and teaching limits

Checked **9 November 2026**: [DSN/sproget.dk personal pronouns](https://sproget.dk/typiske-problemer/pronomener-stedord/)
lists the nominative forms; [DSN capitalization](https://sproget.dk/typiske-problemer/store-eller-smaa-bogstaver/)
distinguishes uppercase address pronoun `I`. [DDO være](https://ordnet.dk/ddo/ordbog/v%C3%A6re)
records present `er`; [DDO lærer](https://ordnet.dk/ddo/ordbog/l%C3%A6rer) identifies the common-gender
teacher noun, separately from the homographic verb. [DDO subjektsprædikat](https://ordnet.dk/ddo/ordbog/subjektspr%C3%A6dikat)
illustrates a bare occupational complement. Existing [language-pack sources](module-one-language-pack.md)
cover the remaining supplied forms and frame qualifications. These facts support the bounded
language analysis, not the proposed item count, sequence or teaching efficacy. Examples here
are original authoring drafts; no dictionary definitions, audio or corpus dataset is imported.

## Assessment identity, grading and worked cases

Keep operation and bounded target scope separate. Proposed additive `targetScopeId` values
are `human-singular`, `human-group` and `vaere-present`. Only the following new grammar
skill/scope/mode combinations are valid, with literal summary identities:

- `grammar:G003:human-singular:selection` and `grammar:G003:human-singular:recall`.
- `grammar:G003:human-group:selection` and `grammar:G003:human-group:recall`.
- `grammar:G004:vaere-present:selection` and `grammar:G004:vaere-present:recall`.

Reject unknown/crossed combinations in creation and frozen-source validation. A missing scope
must retain every existing key and serialized default exactly. Summary labels describe only
the observed operation/subset: no completion of all G003/G004 and no merged selection/recall
score. The typed target is a single word, not a full-sentence translation. Preserve raw input;
normalize NFC and surrounding whitespace only before the new pronoun grading rules. State
the English-only instruction: “Type only the introduced subject word using its taught
standalone spelling.” The profile's teaching/reference gives its own lowercase forms and,
where introduced, capital `I`; the instruction does not print unintroduced word lists.
Exact canonical forms pass. A case-only deviation has a
known written-form constraint failure, with specific capitalization feedback. This is not a
wrong-person diagnosis or a claim about sentence-initial Danish. Keep reference meaning
unevaluable where lexical identity is ambiguous (`i` or `De`); do not silently reinterpret
those words as plural-you or plural-they. Other case-only matches can retain reference meaning
while failing this task's standalone written-form constraint. Verb-only S03 retains exact `er`
after the current NFC/trim/whitespace normalization; no global case-folding is introduced.

| Case | Literal trigger/response | Required outcome |
| --- | --- | --- |
| M1-A01 | Anna speaks about herself; select `jeg`. | Singular reference selection met. Supplied `er/Anna` carry no extra assessment. |
| M1-A02 | One explicitly described referent uses “she”; select `han` rather than `hun`. | Reference target not met; selected word is Danish. Explain the mismatched reference, not malformed Danish. |
| M1-A03 | Speaker talks about a group that includes the speaker; type `vi`. | Group recall met. Do not require any knowledge of supplied `sover`. |
| M1-A04 | Several addressees; type `du`. | Known number/reference mismatch; group recall not met. English cue must say several people. |
| M1-A05 | Several addressees; type lowercase `i`. | Known written-form constraint failure with `STANDALONE_PRONOUN_CASE_REQUIRED`, reference target unevaluable: no silent success or wrong-person inference. Preserve raw `i`; show expected `I`. The analogous `De` versus canonical group `de` case gets written-form feedback without inventing formal-address meaning. This known form failure permits advance, never full success. |
| M1-A06 | Several addressees; type ` I ` with surrounding spaces. | Normalize surrounding spaces; `I` met, raw text retained. |
| M1-A07 | `Det ___ et brev.`; type `er`. | Bounded present-form recall met. `det`, article and noun are supplied; no G006/G038 evidence. |
| M1-A08 | Same cue; type `være`. | Named verb's finite-present target not met; explain infinitive versus introduced present. Do not grade it as an unknown noun. |
| M1-A09 | `Jeg ___ lærer.`; type `er`. | Present target met in the new singular occupational subframe; no profession article-rule evidence. |
| M1-A10 | Type `vi sover` where one pronoun is requested. | Response constraint not met with `ONE_WORD_RESPONSE_REQUIRED`; grammar target unevaluable. This certain task failure permits advance, with no correct-prefix acceptance or scoring of incidental verb knowledge. |
| M1-A11 | An unsupported one-word response outside the explicitly reviewed alternatives. | `UNEVALUABLE`/bounded uncertainty; preserve raw input and offer support. No claim that the word or all Danish containing it is ungrammatical. No qualifying form/shape code: cannot advance until a completion-eligible response. |
| M1-A12 | Submit a choice absent from that item's bank, or a forged scope/revision. | Reject the malformed request under the public API contract, separately from a language grade; authorization/ownership ordering unchanged. |
| M1-A13 | Retry after feedback or open a reference before answering. | Later correct answer remains supported/retried evidence. Its earlier attempt and exposure context are immutable. |
| M1-A14 | Submission result is lost after commit; reload/replay. | Same stored request identity/body and exact saved grade; one attempt, no duplicate completion. |

Each new grade distinguishes task constraint, named target and bounded linguistic certainty.
Author known alternatives and capitalization cases explicitly; do not treat every string unequal
to the expected word as known-bad Danish. Feedback gives the expected word on known errors;
that disclosure must update possible exposure even when the target is `NOT_MET`.

**Runtime grading clarification:** S03 `Er`/`ER` are known written-form corrections within
the exact mid-sentence task: target `NOT_MET`, language `KNOWN_ISSUE`, constraint `NOT_MET`,
code `PRESENT_FORM_CASE_REQUIRED`. They may advance through the known-target branch, and
showing the corrected form records exposure. This does not call sentence-initial capitalization
invalid Danish. At the pure grading boundary, blank input has target `UNEVALUABLE`, language
`UNKNOWN`, constraint `NOT_MET`, and `EMPTY_RESPONSE`; it reveals no solution and its code is
outside the completion exception list. Human subject, identifying `det`, article and noun
exposure have distinct explicit namespaces; no non-human or article ID is relabelled a human
subject form.
G003 feedback shows the canonical standalone correction (`de`, `I`, `jeg`) with its Danish
language tag; a sentence-initial capital in a full example must not contradict that correction.
The private canonical clause remains available for reproduction and teaching review.

## Teaching exposure, summaries and recovery

Mandatory introduction/reference display is an answer-bearing event, not optional invisible
background knowledge. Persist its exposure before revealing it; if persistence is uncertain,
resolve/replay the event before enabling assessment. Freeze required presentation/support
with the plan and track acknowledgement separately from claims about actually reading it.
Opening the reference again is available and recorded without clearing unsaved answers.

Use explicit, plan-scoped form exposure keys for each introduced pronoun and present/infinitive
form. Choice banks also expose their displayed forms to later items. Corrective feedback,
examples and noun glosses must account for what they show, including forms shown on wrong
answers. Define the exact keys and bank/feedback events before publishing the plan; preserve
old exposure calculation branches. First/latest attempt context remains frozen at submission.

Report first/latest saved outcomes, including unknowns, by the six scoped operation identities,
alongside retry, extra support and possible earlier answer exposure. Do not filter out unknown
attempts to invent an earlier successful “latest” result. Tally six activities once per run;
an advanced known form failure may remain an unevaluable grammar target, never fully met.
A typed answer after teaching or choices is recall after exposure, not unseen
independent recall; a changed cue does not erase the exposure. Separate independent runs
currently have no complete shared exposure ledger: prior cross-run experience is **unknown**,
not “never seen.” Do not infer mastery, retention or transfer from completing this slice.

Resuming a run uses its saved introduction/support state, current item, unsaved/retry protocol
and exact original item/attempt identities. Preserve owner expiry, multi-tab serialization,
pending support/advance and response-loss recovery. New summaries offer repeat, not existing
targeted follow-up revisions: those planners do not accept scoped G003/`er` sources. Hide the
unsupported action explicitly; no fallback to `follow-up-1/2/3` or broad G004 review.

## Verified reuse, gaps and revision boundary

Baseline: released **0.8.0**, source `cdb6e06` (documentation head `019c05b`). The following
observations are code evidence, not delivered behavior of this contract:

| Current implementation | Implication for implementation |
| --- | --- |
| [PilotContent](../../engine/src/main/kotlin/app/astrid/engine/PilotContent.kt) catalogues G003, but [PilotEngine](../../engine/src/main/kotlin/app/astrid/engine/PilotEngine.kt) has no pronoun-target exercise. | Add explicit G003 selection/recall generation and bounded grading; reusing choice/text controls is not reusing an already implemented assessment. Allocate new format IDs after checking the catalogue. |
| E015 is specifically G004 verb/G006 article gaps. | Do not retrofit it with a G003 slot or relabel article evidence. |
| [PracticePlanner](../../engine/src/main/kotlin/app/astrid/engine/PracticePlanner.kt) and [PracticeModels](../../engine/src/main/kotlin/app/astrid/engine/PracticeModels.kt) offer pinned profiles and optional support, not a course prerequisite ledger. | Add three literal plan/profile revisions, scoped target identity and required-teaching/reentry semantics. No CURRENT-driven upgrades or assumed previous session completion. |
| [PostgresPracticeStore](../../server/src/main/kotlin/app/astrid/server/practice/PostgresPracticeStore.kt) permits advance after a saved attempt without the new evaluability requirement. | Add new-plan-only eligibility in the pure domain rule and both stores; persist it and test status/concurrency behavior without changing old runs. |
| [PracticeFeedbackExposureCalculator](../../engine/src/main/kotlin/app/astrid/engine/PracticeFeedbackExposureCalculator.kt) has legacy success-conditional form disclosure. | Explicitly account for new lesson/bank/error feedback events; do not silently reuse or change the old rule. |
| [PracticeFollowUpPlanner](../../engine/src/main/kotlin/app/astrid/engine/PracticeFollowUpPlanner.kt) recognizes bounded existing sources. | Preserve those paths; targeted reviews for the new scoped sources need a later explicit contract. |

Activate new frame permissions and G003 grading only in a new immutable content/generator/
evaluation pair, provisionally **pilot-8/generator-8/evaluation-8**. Additive scoped DTO fields
must be omitted when absent, with old keys/defaults/field order unchanged. Freeze complete new
presentation, reference context, scope, teaching, answer constraints and private solutions in
the reproduction fingerprint. Pin all three new plans literally to the new pair. Existing
practice/follow-up plans retain their exact pairs and payloads.

Pair-seven engine/server goldens were already frozen before 0.8.0; verify their provenance
and unchanged guards rather than overwriting them. Separately freeze the now-published 0.8.0
guided-gap plan/summary/follow-up wire and exposure behavior from unchanged source before
editing shared scope/support/completion models. All seven old pairs stay executable. A new
SQL migration is required only if schema shape changes; never edit an applied migration.
New optional payloads still require compatible readers for rollback.

Current sentence creation and retained snapshots share base sentence/noun pools. New C004
frames and `lærer` must live in revision-eight-only pools/dispatch: appending to shared
`PilotContent.sentences`, noun parsers or combination inventories would change old seeded
generation or grading. Keep the published 99-combination base and retained parsing behavior
unchanged; new coverage must be explicitly scoped to the new snapshot.

## Implementation packages and acceptance

1. **Compatibility capture and domain:** freeze new 0.8.0 practice evidence in its own commit;
   enumerate licensed content/reference contexts, assign persistent IDs, add the new pair,
   pronoun operations, scoped identities, teaching events and plan-scoped completion rules.
   Literal authored expectations must independently check deterministic generation/grading.
2. **API and persistence:** define exact profile/revision/scope/operation correlations in
   OpenAPI and generated types; update explicit public/private mappings and fingerprints.
   No public answer keys. Extend shared fake/Postgres contracts before relying on route
   outcomes; prove guard order, atomic support/advance, locks, idempotency and exact replay.
3. **Learner flow:** reuse choice/text controls and support/summary shell; add required teaching,
   three ordered chooser entries, direct-entry reintroduction and supported repeat/next links.
   English UI and Danish content must retain language tags, keyboard focus, both themes,
   320px/200% reflow and new-state accessibility scans. Never silently discard a pending run.
4. **Independent verification and delivery:** run `./scripts/check.sh` with Docker-backed
   PostgreSQL, retained pair/plan wire checks and full native/Linux browser journeys without
   unexplained retries. Include all six scoped outcomes; incorrect/unknown/case boundaries;
   error-reveal/bank/teaching exposure; direct S02/S03 entry; completion eligibility, latest
   unknown superseding earlier eligible results, locked retry/advance races and
   completion; retry/draft/reload/multi-tab/expiry/actual-restart recovery; old source reviews.
   If runtime implementation is selected, use a minor version/changelog, verified CI and exact
   release image, backup plus isolated restore, app-only private-preview promotion, preserved
   history/database process and ownership-scoped QA cleanup.

Outstanding product work is Danish teaching validation (BL-022), learner pacing/clarity review
(BL-035), broader course/progress policy (BL-045/BL-046 and Q09) and any future cross-run
exposure inheritance. Q27/Q28 remain open; no noun recall or per-session 15–20-word quota is
introduced here. The next implementation item is separate from completion of BL-051.

## Observed planning delivery — 9 November 2026

Delivered the first-three-session contract: 18 planned activities, six scoped operation
identities, a bounded language ledger, 14 worked assessment/recovery cases, and four
dependency-ordered implementation packages. Architecture inspection verified runtime gaps
against 0.8.0; separate independent review checked the authoring/code boundaries. Corrections
included direct-entry G001/G002 support, preserving first/latest saved unknown outcomes,
distinguishing known form failures from unsupported answers in completion eligibility, exact
existing normalization and the C001 sleep-frame identity. No remaining actionable review
findings were reported. The primary language pages above were checked as references;
specialist teaching approval remains outstanding.

`./scripts/check.sh` passed all configured local gates; unchanged Kotlin tests initially used
Gradle's cache. A separate fresh `:server:cleanTest :server:test` then passed **134 tests, zero
failures/errors/skips**, including real Testcontainers PostgreSQL contract/repository tests.
The web coverage gate passed **185 tests**; API generation/lint, engine/locale/coverage gates,
web/build/static checks, E2E lint/types/scenario/script checks and manifest rendering passed.
Knowledge/link validation covered 58 grammar skills, 30 constructions, 24 vocabulary records
and 63 Markdown files. No browser journey or preview promotion is claimed for this docs-only
milestone.

BL-051 is complete. [BL-052](../../BACKLOG.md) captures the separately proposed runtime
implementation, subsequently selected above. At planning delivery the app remained 0.8.0: no runtime source, public API, golden fixture, applied
migration, dependency, app version or changelog changed, and no new release tag is warranted.

## Runtime compatibility baseline — 9 November 2026

Before production edits, captured the published 0.8.0 guided practice from unchanged source
`0dfc00afdc2bd21f5f06899ff47554689f0ae0c9` (release source `cdb6e06`). New immutable archives:

- Engine: 21 rows, SHA-256 `471d452c76084975ea2d8345c7a27a75198b4467ce5e5d14b53fefc4af0e50da`.
- Server: 22 rows, SHA-256 `ac9d48dfebfd3cd240688dad34fc544847720b8fbf106bfd198680ac6fd69674`.

They cover seed boundaries, exact guided profile/request/private payloads, initial/supported/
attempted/completed public wire, all nine slot states, first/latest unknown outcomes, actual
feedback exposure and reachable recursive reviews. Explicit primitive/list projections avoid
object-representation drift when absent defaults are added; the existing-profile projection
allows additive catalogue entries. Independent review corrections are resolved; fresh-JVM
replay and all retained compatibility guards passed. The coordinator's full
`./scripts/check.sh` passed, including Docker-backed PostgreSQL tests. Earlier fixtures and
migrations are unchanged. This capture is compatibility evidence, not teaching approval or
delivery of the new runtime sessions.

## Observed runtime delivery — 10 November 2026

Released app [0.9.0](https://github.com/liveweird/astrid/releases/tag/v0.9.0), tagged source
`8913dea`, after all five [exact-source CI jobs](https://github.com/liveweird/astrid/actions/runs/42722013690) passed. All **370 native** and
**370 Linux CI** Chromium/WebKit journeys passed without retries or flaky results. The
release tag, authored changelog notes and remote GitHub Release were read back at that source.
The running private preview uses exact image `sha256:2577e50b2058b20c05bde0dbedf2c5b539a9370f7b306f3b0f8947eba858c5b6`; its build stamp identifies the tagged
source. The documentation-only completion commit does not retag or rebuild that image.

The three independent six-item first-step profiles require saved teaching before assessment,
keep all six scoped recognition/recall outcomes separate and apply the new-plan-only known
outcome completion rule. E016–E019 are also available standalone. Direct-entry support covers
written words/statements, Danish letters, subject/pronoun roles, standalone capitalization,
reference meanings and supplied noun/verb forms. Current pair eight contains 25 lexical
records and 109 complete licensed clauses; the original 99 shared combinations, older
format pools, all seven retained evaluators and prior plans remain unchanged. No new schema
migration, dependency, course graph, targeted-review family or mastery policy was introduced.

`./scripts/check.sh` passed with real Docker-backed PostgreSQL: **186 engine tests**, a fresh
**186-test Arabic-locale run**, **149 server tests** and **202 web tests**, with zero failed or
skipped tests. Final engine coverage was 98.08% lines / 85.33% branches (branch floor raised
84→85); server 98.11% / 80.52%; web 95.12% statements / 89.57% branches / 97.82% functions /
97.85% lines. All other floors stayed unchanged. Full regression also corrected a stale
three-profile chooser assertion to the six intended profiles; no app behavior changed for
that correction. API lint/generated-type drift, actual
new-profile HTTP conformance, detekt, lint, knip, builds, scenario parity and manifest gates
passed. All eight deterministic review exports passed. Pilot-8 review has 708 lines,
SHA-256 `341d0b1a798331397ec1c199ad86dbb17cbb02fd515a4574da472a23a6a488f0`;
retained pilot-7 is byte-identical at
`0a1caa4c367e8408ac8e35cbffa0249b524493d5c4122c8361be8081b4a3d0d2`.

Independent engine, API, UI behavior, rendered UI and release-safety reviews are closed.
Corrections included exact visible-surface exposure, complete foundation teaching/review
metadata, old noun-pool isolation, canonical standalone `I`/`de` feedback, scoped operation
validation and focus after creation/replay/resume/chooser cancellation. All 44 working-image
views were independently inspected; the exact release image produced another 44 captures
with final-source spot checks. Browser checks additionally cover keyboard/focus, Axe,
320px/200% text, short-window reflow, lost-response creation/support/answer replay, drafts,
latest-unknown blocking and supported response-shape continuation. Screenshot review alone
does not establish assistive-technology behavior or Danish teaching approval.

Final verification of all seven retained/new history sets followed the same actual owned QA
app restart. Capture/proof hashes and app ID/PID/start time bind that sequence. Old exercise and
practice reads, creation/attempt replay, recursive reviews and fresh retained grading remained
exact. New captures include all three completed scoped summaries, an active latest-unknown
answer with later support, and an unopened required introduction; their saved responses,
frozen contexts and fresh evaluation-eight submission survived the restart.

A **3,254,586-byte mode-0600 backup** was captured before promotion and actually
restored into a uniquely owned isolated database. Full development-session, exercise,
attempt, practice-session/item and attempt-context rows plus Flyway 1/2/3 matched. The scratch
database was removed. App-only promotion preserved full pre-promotion rows, PostgreSQL
identity/process/mounts and schema 1/2/3. All **160 live-preview smoke journeys** passed without
retries; ownership-checked cleanup removed only this QA stack and kept main processes/mounts.
The exact image passed the pinned HIGH/CRITICAL vulnerability policy and non-root/read-only,
capability-drop and no-runtime-Git checks. Private backups, captures and proofs remain ignored
under `.local/first-steps/`; no credentials were committed.

BL-052 is complete. Learner pacing/clarity review (BL-035), Danish teaching validation (BL-022),
Q27/Q28 and wider curriculum/progression remain open. These first three sessions do not
complete Module 1 or establish independent mastery, retention or CEFR attainment.

## Post-release compatibility and lifecycle verification — 10 November 2026

Post-release hardening source `5cdfc0d` passed all five [CI jobs](https://github.com/liveweird/astrid/actions/runs/42730180851),
including **372 Linux browser journeys without retries/flakies**. The matching **372 native
Chromium/WebKit journeys** passed against the released image without changing the app or
PostgreSQL processes. Local `./scripts/check.sh` passed **195 engine tests**, the fresh
**195-test Arabic-locale suite**, **160 server tests** including PostgreSQL, and **202 web
tests**, with no failures/errors/skips. The app remains tagged `0.9.0` at `8913dea`;
these test, quality-gate and planning changes introduce no runtime feature or new release.

Published pair eight and all three first-step plans now have immutable, independently
reviewed compatibility archives captured from unchanged release source `8913dea`:

| Archive | Rows | SHA-256 |
| --- | --- | --- |
| Engine pair eight | 2,674 | `2c7d1851cf84a71cfbecd04da0d332cb01b509904431e60a0646e89a9f058b48` |
| Server pair-eight wire | 3,518 | `d580c5a296ead61fcfd712705e866cedac1956b2896355175a13ffbf8856bb01` |
| Engine first-step practice | 23 | `cf4b90e8eadf468a3b475c363b98cf794d7c8f4e6018816b3ae7219e3a96bdd6` |
| Server first-step practice wire | 100 | `9f09be95bd8f3356e8e1cabbe9b3980829f001674157712454d4131730dba5ec` |

These cover all nineteen formats, thirty first-step tasks, private answer keys, nested
fingerprints, actual review bytes, policy/teaching data, scoped first/latest outcomes,
exposure and continuation eligibility. Capture review added private-key alternatives and
independent nested-value/order mutations before acceptance. Earlier tracked fixtures and
all production sources/dependencies/deployment files remain unchanged. Exporters refuse
overwrite. Before a future CURRENT move, retain the pair-eight review renderer and rewire
the test-source adapter while preserving the pinned review bytes and archive provenance.

The three new lifecycle tests prove fixed expiry after midpoint authenticated use,
all six scoped outcome rows with seven literal first/latest counters, and cross-pool
serialization. The concurrency test holds an external PostgreSQL owner-row lock until
both support and attempt operations are visibly waiting on that lock; it separately
proves an unknown retry blocks a competing advance. The browser case proves missing-cookie
recovery retains the exact old run/outbox, reads ownership before replay, and creates a
new owner/run only after explicit choice. Missing-cookie recovery and injected-clock expiry boundaries
are separate tests; neither reassociates old evidence with a new owner.

Independent review findings are closed. Engine coverage is 98.12% lines / 85.64% branches;
server coverage is 98.17% / 81.34%, raising its branch floor from 80 to **81**. Other floors
remain unchanged. The [S04 proposal](module-one-course-slice.md#proposed-next-activation--s04)
is reviewed but unselected (BL-053); wider curriculum and teaching validation remain open.
