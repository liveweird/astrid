# Module 1 Worked Exercises and Grading Cases

## Status and common contract

**8 November 2026 — Authorized authoring examples** for the [Module 1 plan](module-one-course-slice.md). The proposed new operations are user inventory #1 (English→Danish word choice), #5 (all word tiles) and #6 (word tiles with distractors). These cases are specifications for future implementation, not assertions that the current engine passes them. No public format ID, wire field or revision string is assigned here.

Each item records its primary target and supplied prerequisites. Judge target satisfaction, requested meaning, response constraints and bounded language diagnosis separately. Green feedback requires every required facet to be met and language to be acceptable within the licensed subset. A grammatical alternative can miss an explicit task instruction. An unsupported form/meaning remains unknown; do not treat every outside-inventory response as invalid Danish. Structural target success alone is not evidence of correct translation.

Preserve raw input. Normalize NFC and harmless whitespace; tolerate optional final terminal punctuation only as declared for the task's speech act. Never globally lowercase Danish or equate `I` (plural you) with `i` (preposition); do not fuzzy-match `æ/ø/å`, silently correct meaning, accept a correct prefix with trailing prose, or flatten multi-sentence writing into one statement. These are new-scope policies only; old evaluators remain exact.

## Word choice: four related meanings

Teaching bundles: `en bog` (book), `en avis` (newspaper), `et brev` (letter, written message), `et magasin` (printed magazine). The English cue identifies the selected sense; the choices use consistent article+noun surfaces. Their articles are teaching support, not an assessed clue supplied by the cue. The authoring target is the lexical sense in the English→Danish recognition direction.

| Case | Prompt / response | Expected result and feedback |
| --- | --- | --- |
| M1-W01 | “Choose the introduced Danish word for **newspaper**.” Options `en bog / en avis / et brev / et magasin`; choose `en avis`. | Meaning target met; choice constraint met. Danish is a licensed choice. |
| M1-W02 | Same item; choose `en bog`. | Target not met; valid Danish choice. “`En bog` means book; the introduced newspaper word is `en avis`.” Do not diagnose bad Danish or unknown gender. |
| M1-W03 | “Magazine (printed publication)”; same four choices; choose `et magasin`. | Correct selected sense; exclude store/depot, broadcast and weapon-container readings. |
| M1-W04 | A generator proposes `avis` and `blad` as separate alternatives for English “newspaper.” | Reject the prompt before presentation: `blad` can also denote a newspaper. Do not fix ambiguity by declaring a legitimate translation wrong. The candidate fourth packet is `magasin`, not `blad`. |
| M1-W05 | A client submits a value not among the supplied four choices. | Reject as an invalid choice response under the eventual request contract; no grading claim about arbitrary Danish text. Preserve ownership/guard ordering. |

Choices must be semantically distinct in the **particular cue and selected senses**, contain one correct answer, and vary answer position deterministically. Introduce alternatives in prior situations rather than teaching all four for the first time in the assessment. No article, label, glossary, introduction or context may uniquely identify the answer unless recorded as answer-revealing support. Typed noun recall remains Q27; this choice contract does not decide it.

## Translation with all word tiles

Prompt: “Write **I read a book** using the supplied words. Begin with the subject.” Bank `[jeg] [læser] [en] [bog]`, shuffled; punctuation supplied outside the bank. Primary target: G005 subject-first order. Vocabulary and finite form are supplied. Each bracket here represents an individual word, unlike E003's existing `[en bog]` constituent block.

| Case | Learner arrangement | Expected result |
| --- | --- | --- |
| M1-W06 | `Jeg læser en bog.` | Target and meaning met; all required occurrences used once; acceptable. |
| M1-W07 | `En bog læser jeg.` | All-tile constraint and meaning met; grammatical fronting, but target not met because subject-first was requested. Explain the instruction rather than marking Danish invalid. |
| M1-W08 | `Jeg læser bog.` leaving `en` unused | All-tile constraint not met. Ask the learner to use the remaining required tile; do not infer noun ignorance or issue an unsupported language-wide judgment about bare nouns. |
| M1-W09 | Duplicate occurrence ID or an ID not in this bank | Invalid request/selection identity under the eventual API contract, not a learner grammar error. The UI must prevent it and the server must validate it. A legitimate different order still receives an ordinary grade. |

Meaning-changing responses to the corresponding typed task stay separate: `Hun læser en bog` has correct subject-first structure but does not translate “I read a book.” A new generalized translation family must not rely on order alone to establish meaning.

## Translation with spare tiles

Prompt: “Write **I read a book** in subject-first order, using the introduced words. Some words are extra.” Bank `[jeg] [læser] [læse] [en] [bog]`. Primary target: G004 finite present of `læse`; subject-first order and word meanings are prerequisites. The extra tile contrasts the target's two known forms rather than an unrelated noun.

| Case | Learner selection | Expected result |
| --- | --- | --- |
| M1-W10 | `Jeg læser en bog.`; leave `læse` | Target/meaning/shape met; acceptable. |
| M1-W11 | `Jeg læse en bog.`; leave `læser` | Required response length can be met while the grammar target is not. Within this licensed corruption, diagnose infinitive instead of finite present; intended meaning is recoverable. No automatic “unused tile = wrong” rule. |
| M1-W12 | `Jeg læse læser en bog.`; use all five | Response constraint not met: an extra word was included. Do not award success because a correct subsequence exists. Diagnose language only if the complete input matches an explicit reviewed rule; otherwise unknown. |
| M1-W13 | An article-target variant supplies `[jeg] [har] [en] [et] [brev]`; response `Jeg har en brev.` | For this separate G006 item, subject-first order/meaning can be met, but the selected unstressed article mismatches the introduced noun's stored gender. Do not assess this target before the article has been taught or explicitly supplied. |

The grader must validate a complete licensed response, not one predetermined tile-ID sequence or “use every bank item.” An answer bank may have more than one grammatical sequence; author accepted variants and task constraints explicitly. If two required/spare selections both satisfy the intended meaning and constraints, accept both or reject/rewrite the ambiguous prompt. Record distractor rationale per item.

## Early syntax and fair alternatives

| Case | Prompt / response | Expected result |
| --- | --- | --- |
| M1-W14 | “Today I work. Begin with today.” → `I dag arbejder jeg.` | G021 met: `i dag` is one constituent, followed by the finite verb and subject. |
| M1-W15 | Same cue → `Jeg arbejder i dag.` | Meaning and language acceptable; explicit fronting constraint not met. |
| M1-W16 | Same cue → `I dag jeg arbejder.` | Target not met; known neutral main-clause V2 issue within the licensed frame. “After `i dag`, place `arbejder` before `jeg`.” |
| M1-W17 | “Do you read a newspaper?” → `Læser du en avis?` | G018 met; finite verb first. No `do` support. A negative question is not an interchangeable alternative. |
| M1-W18 | “What does she read?” → `Hvad læser hun?` | Non-subject wh expression first, finite verb then supplied subject; the questioned object is not repeated. |
| M1-W19 | “Who is working today?” → `Hvem arbejder i dag?` | Subject-wh target met; `hvem` itself is the subject. Do not require an extra pronoun after the verb. |
| M1-W20 | “I do not work today; begin with today.” → `I dag arbejder jeg ikke.` | Negation position met after the postverbal subject. Fronted skeleton and meanings supplied when this item isolates negation. |
| M1-W21 | Explicit inability: “I cannot work.” → `Jeg kan ikke arbejde.` | `kan` is finite, `arbejde` is bare infinitive; `ikke` precedes the infinitive. This does not license automatic `må/skal` negation substitutions. |
| M1-W22 | “I never work at home.” → `Jeg arbejder aldrig hjemme.` | Negative-frequency meaning and the bounded main-clause slot met; “not” and “never” remain different meanings. |

Cases are tagged by the single assessed operation. A full-sentence response still has meaning/shape requirements: a correct syntax pattern with a different person, polarity, action, place or time cannot pass translation.

## Repetition, spelling, unknowns and support

| Case | Trigger | Acceptance requirement |
| --- | --- | --- |
| M1-W23 | “You (several people) live in Aarhus.” Bank includes pronoun `I` and preposition `i`. | Preserve lexical identity/case: `I bor i Aarhus.` Position-aware capitalization must not turn the preposition into the pronoun or lowercase lexical `I`. |
| M1-W24 | The explicitly licensed technical fixture `Jeg arbejder i Aarhus i dag.` contains two visibly identical `i` tiles with distinct occurrence IDs. | Keep separate selectable occurrences and exact multiplicity. Swapping two otherwise indistinguishable `i` occurrences must not create a false language error when the realized licensed sentence is unchanged. Reusing one ID twice remains invalid. Teach the complete two-circumstance variant before using this as a learner assessment; it is not an initial-session order target. |
| M1-W25 | A typed translation uses an unintroduced synonym or an unsupported construction. | Return bounded uncertainty, preserve raw text and offer a known example. Unknown is not proof of ungrammatical Danish. Do not silently map synonyms to the expected headword. |
| M1-W26 | A learner opens a matching Danish solution before retrying, then answers correctly. | Correct supported response; record sticky reveal/retry and possible earlier exposure. Do not classify it as unseen independent recall. Character-entry buttons alone do not reveal language content. |
| M1-W27 | Submission times out after a possible server commit, then the learner retries. | Retain and replay the exact identity/serialized response. A stored grade returns unchanged; no regrade, new attempt or learner-error count caused by the network. |
| M1-W28 | A free-text answer has two sentences or extra trailing prose where one sentence is requested. | Response constraint not met; no correct-prefix acceptance or silently discarded text. |

## Future implementation acceptance

Enumerate licensed prompt/answer/distractor combinations and independent literal expected cases before enabling them. Verify one-answer fairness, no incidental answer leakage, deterministic variation, full field/meaning fingerprints, response-shape boundaries, code-point/NFC handling, duplicate-token behavior, retained revision replay and malformed-request isolation. Use separate tests for each response operation and target; avoid a generator serving as its own sole answer oracle.

The public prompt may carry the English cue, visible choices/tiles, languages and task constraints. Accepted parses, distractor rationale, reference propositions and solution keys remain private. OpenAPI/DTO/type/fingerprint decisions belong to the future implementation contract; no current wire or persistence behavior changes through this document.
