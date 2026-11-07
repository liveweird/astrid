# Beginner Learning Engine Slice

## Scope and status

This is a reviewable first implementation specification, authorized for drafting on 6 November 2026. It connects the [grammar map](danish-grammar-map.md), [sentence catalogue](danish-sentence-catalogue.md), and [23-entry vocabulary seed](danish-vocabulary-seed.md). The user selected English-speaking complete beginners and general everyday Danish. Teaching sequence and implementation policies below are proposed choices for this slice, not validated learning outcomes or a completed curriculum.

The first application remains an engine playground in the shared sibling UI/layout. Reading and writing are in scope; listening, speaking, production artwork, public onboarding, and a full adaptive course remain later work. Docker Compose and Kubernetes are retained infrastructure requirements. The private playground now implements this slice; this document remains its language/behavior specification. See the [delivery record](playground-delivery.md) for observed software evidence and [implementation decisions](implementation-decisions.md) for the adopted technical boundaries.

## Observable learning objective

After the short explanations and supported practice, a learner should be able to interpret and assemble simple affirmative present-tense statements using the introduced vocabulary, choose the finite present form of a known verb, and supply `en` or `et` for an introduced singular count noun. Assess these three capabilities separately on previously unseen combinations; target attainment must not be inferred solely from immediate retries of the same question.

The slice tests engine behavior first. Actual learning effectiveness requires learner trials and delayed checks, whose success thresholds remain open.

## Supported knowledge and boundaries

- **G001–G006:** written form and word units; basic word classes/roles; selected subject pronouns; finite present verbs; simple subject-first statements; noun gender and indefinite singular articles.
- **C001:** selected human pronoun + `sover`.
- **C002:** selected human pronoun + `har`, `læser`, or `spiser` + an explicitly licensed noun phrase.
- **C003:** fixed identifying `Det er` + a licensed indefinite noun phrase.
- **Lexicon:** L001–L023, with form activation and semantic allowlists defined in the vocabulary seed.

Only these construction uses are generated. Questions, non-subject fronting, negation, plural/definite nouns, adjective agreement, multi-clause sentences, and productive past tense are inventoried elsewhere but inactive here. Danish has more valid constructions than the pilot accepts as task responses; feedback must distinguish task constraints from language-wide grammaticality.

## Introduction and exercise progression

Explain one distinction in English, show an annotated example, offer guided practice, then remove the hint. Introduce each content item before assessing it. Show a noun with its article when introducing gender. Present a verb's infinitive and finite present before asking the learner to distinguish them.

Proposed order: subject/verb roles and subject pronouns; present forms including `har`/`er`; simple subject-first statements; count nouns with their articles; combined known constructions. This sequence is revisable after reviewing the broader grammar map.

## Three exercise formats

| ID | Target and prerequisites | Task | Validity and feedback |
| --- | --- | --- | --- |
| E001 | G004 finite present; G001–G003 and introduced verb | Choose the finite present form in `Hun ___ en avis`, with an explicit instruction to use the present. Alternatives: `læse`, `læser`. The completed sentence also uses familiar G005/G006 material. | Accept `læser`. Explain that this statement needs the finite present form; `læse` is the infinitive. Other words and their meanings remain fixed. Use two alternatives rather than inventing an unrelated third. |
| E002 | G006 article/gender; G001–G005 and introduced noun | Type the indefinite article: `Jeg har ___ hus.` State that the intended meaning is “I have a house.” | Accept `et`. Explain that this introduced noun uses `et` in the singular indefinite phrase. `en` is a gender mismatch here; `ét` expresses the numeral “one” and does not meet the article instruction. |
| E003 | G005 subject-first order; G001–G004, familiar phrase and G006 support | Arrange blocks `[har] [en bog] [Jeg]`. Instruction: “Make a statement beginning with the subject Jeg.” Punctuation is supplied outside the movable blocks. | Accept `[Jeg] [har] [en bog]`. Keep the noun phrase together to isolate clause order. `En bog har jeg` can be a valid fronted Danish statement, but does not satisfy this task. Explain the requested construction rather than calling it invalid Danish. |

All three exercise formats carry an intended target and non-target support skills. Keep vocabulary choices fixed within a question unless vocabulary is explicitly the target. Changing vocabulary across questions requires the new entries to be introduced first.

For a typed alternative to E001, accept only the appropriate finite present of the named lemma. Productive present/past tense choice becomes a later exercise after both tense uses are introduced; a vague time word or English simple/progressive translation alone must not establish a unique tense answer.

## Concrete worked cases

| Case | Target | Learner response | Expected result |
| --- | --- | --- | --- |
| W001 | E001, `Hun ___ en avis`, present form of læse | læser | Correct target. Render `Hun læser en avis.` |
| W002 | Same exercise | læse | Incorrect target; identify infinitive versus finite present. |
| W003 | E002, `Jeg har ___ hus` | et | Correct target. |
| W004 | Same exercise | en | Incorrect target; explain the noun's stored gender. |
| W005 | E002, `Vi har ___ bog` | en | Correct target; content substitution preserves the article target. |
| W006 | E002, `Det er ___ æble` | et | Correct target; retain the identifying `det` construction. |
| W007 | E003, arrange Jeg / har / en bog | Jeg / har / en bog | Correct subject-first order. |
| W008 | Same task, realized as `En bog har jeg.` | en bog / har / jeg | Target not met, linguistic status acceptable: the known fronted construction is valid but outside the requested subject-first order. Render capitalization for position while preserving lexical `I`. |
| W009 | Typed present form of læse | laeser | Incorrect spelling; preserve the intended Danish letter distinction. Do not apply search transliteration to grading. |
| W010 | E002 answer with surrounding spaces | ` et ` | Correct target after trimming; retain raw input in the attempt record. |
| W011 | E002, article requested for hus | ét | Target not met, linguistic status acceptable: `Jeg har ét hus` is grammatical with the numeral “one,” but does not meet the unstressed-article instruction. Explain the difference. |
| W012 | Completed subject-first statement | `Jeg har en bog` without final period | Target correct; final punctuation is not assessed in this slice. |
| N001 | Unicode normalizer utility, outside generated vocabulary | `å` as U+00E5 versus `a` + U+030A | Normalize to the same NFC sequence. Retain both raw inputs; do not equate either with unaccented `a`. |

Original examples are built from the cited vocabulary/construction rules; these cases are draft acceptance examples, not results from an implemented grader or independent teacher review.

## Answer normalization and diagnosis

Preserve raw input. Normalize canonical Unicode composition (NFC), trim surrounding whitespace, and tolerate harmless spacing for these tasks. Keep `æ`, `ø`, and `å` distinct from replacement letter sequences; do not use Flow's accent-insensitive search helper. Unicode utility fixtures may include characters absent from the generated pilot vocabulary.

Never lowercase all input blindly: the pronoun `I` and preposition `i` are distinct. Store lexical identity separately from display capitalization. For non-capitalization targets, a sentence-initial case slip can be reported as a non-target observation when it does not change lexical identity. The pilot's typed slots contain verb forms or articles; pronoun rewriting is not required.

Represent two independent facets: **target satisfaction** (`met`, `not-met`, or `unevaluable`) and **linguistic status** (`acceptable`, `known-issue`, or `unknown`). Add specific issue codes and an explanation only when supported by the implemented rules. W008 and W011 must both be `not-met` plus `acceptable`; an unrecognized free-text construction can have linguistic status `unknown` without being declared incorrect Danish. The specific gender mismatch `en hus` in W004 is `not-met` plus `known-issue`; a different determiner must not automatically receive that classification.

Evaluation unavailability is an application failure: return `unevaluable` with linguistic status `unknown`, and do not count it as learner error. Track hints, retries, and non-target observations separately. Do not claim certainty about the cause of an error when the answer is compatible with several causes.

## Generation and variation rules

Construct sentences from structured records, then realize their forms and display text. Do not use arbitrary substitutions into a string. Each instance records its construction, selected lemma/sense IDs, grammatical features, semantic roles, clause roles, and learning target.

Use the vocabulary seed's finite combination set with a deterministic seed or equivalent recorded choices. Keep the content snapshot and generator revision stable for reproduction. A subject variation changes discourse reference; the prompt must establish it instead of relying on gendered pictures or an ambiguous English “you.”

Vary subjects, compatible verb/object combinations, noun gender, and exercise format across instances. Schedule separate presentation and linguistic novelty: reshuffling choices does not create a new sentence. Avoid immediate reuse where alternatives remain; when the candidate pool is exhausted, repeat deliberately rather than generating unsupported material. Track a canonical sentence identity and a target/exercise signature.

Candidate alternatives must be distinct and have one correct answer under the stated constraints. For ambiguous or unsupported cases, reject the exercise before showing it. Revalidate gender, activated forms, valency, semantic compatibility, and prerequisite coverage after each variation. A generation failure is not a learner failure.

## Proposed content and API record responsibilities

These are design responsibilities, not a selected wire schema:

- **Skill:** stable G ID, prerequisites, objective, explanation, introduced forms/constructions, review status.
- **Construction:** stable C ID, slots and roles, constraints, realization rules, allowed transformations.
- **Lexical sense:** stable L ID/sense, class, grammatical features/forms, English gloss, semantic tags, permitted constructions, sources, review state.
- **Published content revision:** immutable snapshot linking those records; separate draft edits from a revision used by a learner.
- **Exercise instance:** identity, target/support skills, format, public prompt/choices/blocks, construction and lexical references, content/generator revisions, generation inputs. Keep answer keys and grading internals server-side for the online design.
- **Attempt:** stable client request identity, instance ID, raw response, evaluation revision, target-satisfaction result, linguistic-status result, supported issue codes/feedback, hint/retry context, server timestamp. Associate it with the intended learner or development session.
- **Derived progress:** a projection of attempt evidence; a hint-assisted retry does not automatically equal independent mastery.

Attempt submission must deduplicate transactionally. Repeating the same request identity with the same payload returns the original result; reusing it with different content is a conflict. Evaluation and persistence must not partially double-count an attempt. Detailed authentication, wire schemas, retention, and progress formulas remain open.

## Playground behavior

Within the shared shell, provide target and exercise selectors, generate/next controls, answer input, hint, check, feedback, and retry/advance states. The development-only inspector can show roles, lexical choices, versions, and reproduction information. Use the preferred red accent and existing neutral family surfaces; keep primary actions, incorrect answers, and system failures distinguishable with explicit labels and icons.

Support a narrow viewport and keyboard-only operation. Sentence ordering needs an accessible move/select alternative to dragging. Mark Danish text as Danish within English instructions. Production logo artwork and polished media are separate work.

## Acceptance checks and remaining decisions

The initial implementation required validation of content IDs, a cycle-free skill prerequisite graph, active forms, licensed combinations, and worked-case consistency. The pilot allowlists yield 99 abstract sentences; the implemented engine tests now enumerate this bounded set. Keep independently checked examples and counterexamples as an oracle, rather than having one realization rule validate itself.

The [delivered software checks](hardening-delivery.md) cover deterministic generation, malformed inputs and response contracts, duplicate attempts/concurrency, and assembled browser journeys through generate → answer → feedback → next. They include incorrect/unknown outcomes, uncertain-result replay and keyboard ordering. Retain those regressions alongside Docker Compose full-stack checks and Kubernetes validation as content expands.

Still open: independent Danish review and approved content status; sampling/retention measures; broader-course content storage, consumer accounts and production infrastructure. The pilot wire schema, private development sessions and PostgreSQL attempt storage are implemented choices in [implementation decisions](implementation-decisions.md). Software delivery does not establish teacher-reviewed content, validated learning efficacy or a production-ready application.

## Initial draft review and checks

Independent agent review on 7 November 2026 found no remaining actionable defect after corrections. The review checked the 99 allowed combinations, dictionary forms/senses, prerequisite graph, scope boundaries, and the worked grading cases. Corrections narrowed the initial pronoun target, removed an out-of-seed example, and made grammatical-but-off-target outcomes explicit for both fronting and numeral `ét`. This is source/engineering review, not approval by a Danish teaching specialist.

At the initial pre-implementation draft review, content checks passed for 58 grammar records, 124 prerequisite edges without cycles, 30 construction records, 23 vocabulary records, all cross-references, the three format records, 12 worked cases, and the NFC fixture. Local documentation links/anchors and `git diff --check` passed. Those were document checks; subsequent software evidence is recorded separately in the [delivery](playground-delivery.md) and [hardening](hardening-delivery.md) records. The later [foundation source review](foundation-source-review.md) refined the graph to 123 edges while preserving the six-skill pilot rules.
