# Next Vocabulary Practice Situation

Proposal under BL-073, reviewed against the shipped source `57548fb` on 19 November 2026. The next release still needs user selection. P2 and P3 language remains a draft under BL-022. See the [learner review](learner-flow-review.md) and the [delivered work-and-study contract](vocabulary-library-plan.md#observed-targeted-practice-delivery--19-november-2026).

## Recommendation

Start with Food, cafés and shopping. Three released P2 single-target meaning choices already fit, while two narrowly worded new recall activities can use a mass word and a polite expression. That gives a second situation without settling count-noun recall (Q27), using multi-target boards as if they were single-word questions, or introducing a tense assessment.

| Operation | Exact library form and taught situation | Candidate and admission witness |
| --- | --- | --- |
| Recognize price label | L051-S01-F1 `billig`, inexpensive | Reuse `p2-1.2` E040 from `vocabulary-p2-s1-1` |
| Recognize milk | L052-S01-F1 `mælk`, milk as a drink or substance | Reuse `p2-2.1` E040 from `vocabulary-p2-s2-1` |
| Recognize the supplied present action | L058-S01-F2 `køber`, buys | Reuse `p2-4.1` E040; `vocabulary-p2-s4-1` or `vocabulary-p2-s5-1` |
| Type milk | L052-S01-F1 `mælk` | New proposal: “Type the taught Danish word for milk as a drink.” `vocabulary-p2-s2-1` |
| Type a way to get attention | L053-S01-F1 `undskyld` | New proposal: “Type the taught Danish expression for ‘Excuse me’ to get a shopkeeper’s attention.” `vocabulary-p2-s3-1` or `vocabulary-p2-s5-1` |

Counts use exact meaning plus form per sense. The Java inventory ran the shipped `PracticePlanner` and `VocabularyTeachingMapper` on each current lesson and found the named witnesses. The [food and shopping activation contract](vocabulary-food-shopping-activation.md) owns the glosses and boundaries. New target rows need direct review. The English prompts above are proposed practice cues over already introduced material.

## Grading and exposure boundaries

- `køber` is supplied Danish meaning recognition. It grants no evidence of independently recalling `købe`, forming present tense, or paying (`betaler`).
- Reuse E040 choice banks unchanged for versioned pair-19 tasks. Do not include `p2-5.1` (whole polite-wish frame), E041 contextual scene choices, E042 matching boards, E043 supplied order, or E044 reading facts in the first single-target pool. These remain useful separate operations.
- Keep the scene specific. It neither asks for a generic apology nor tells the learner the answer. New library evidence from one situation follows the same owner-bound saved-introduction rule as work and study. Opening a word card still creates no practice or teaching record.

Proposed typed rules retain raw input. Apply NFC, trim surrounding whitespace, and compare forms without regard to case. The target is one written word. `æ` remains distinct from `ae`. This gives the following initial grading cases for author and teaching review:

| Prompt target and input | Target | Language | Shape | Meaning |
| --- | --- | --- | --- | --- |
| Milk: `mælk` or ` MÆLK ` | Met | Acceptable | Met | Met |
| Milk: `vand`, taught as water in the same lesson | Not met | Acceptable | Met | Not met |
| Milk: `maelk` or a word outside the reviewed forms | Not met | Unknown | Met | Unknown |
| Milk: `en mælk` | Not met | Unknown in this exercise | Not met | Unknown |
| Attention: `undskyld` or ` UNDSKYLD ` | Met | Acceptable | Met | Met |
| Attention: `værsgo`, the introduced handover expression | Not met | Acceptable | Met | Not met |
| Attention: an unreviewed alternative | Not met | Unknown | Met for one word | Unknown |

Feedback should identify the other beverage or handover role for the known alternatives, and state the response-shape request for an added article. The policy makes no judgment about added articles in other Danish contexts. Known mismatches can continue. Unknown latest outcomes allow retry and keep continuation blocked. The Danish character control should support entry of `æ` without an English-only keyboard workaround.

For a fresh owner with only the listed introduction, the expected availability is:

| Food and shopping lesson | Recognition | Written recall |
| --- | --- | --- |
| A small breakfast | `billig` | None |
| Drinks on the table | `mælk` | `mælk` |
| Getting attention and receiving a roll | None | `undskyld` |
| Buying and paying | `køber` | None |
| Marking a polite wish | `køber` | `undskyld` |
| Requesting a supplied quantity | None | None |

Missing the relevant meaning or exact active form excludes that task. Repeated introductions and alternate witnesses must not duplicate an activity. Retain one verified source introduction witness per relevant recorded answer key. The full Food pool has three recognition and two recall tasks. Teaching Food alone must grant no Work and study tasks.

## Places and journeys alternative

`p3-1.1` (en cykel, E045) and `p3-4.3` (et tog, E046) already supply two true single-target choices. L064-S01-F1 has `vocabulary-p3-s1-1`; L072-S01-F1 has `vocabulary-p3-s4-1` or `vocabulary-p3-s6-1`. `p3-5.4` is typed `kører` in a car scene, but its published primary target is G004 present-form recall. Keep that classification. Matching rows are not separate questions. New lexical recall could use exactly taught station phrases in separately supplied destination/static contexts. Broad English “home” makes `hjem`/`hjemme` ambiguous as a cue; rail `skifter` needs the reviewed rail-change scene. Keep `tog` in its transport sense and reserve noun/article recall for a separate Q27 decision.

## Implementation plan after selection

1. Review the two typed tasks, supported normalization and known alternatives. Pin recognition to literal pair nineteen. The new authored recall inputs require a revision that can regenerate those exact tasks. Use the existing text input and grading facets. If advancing the current engine pair, freeze pair twenty before editing under the repository compatibility rules.
2. Proposed focus IDs are `vocabulary-library-food-recognition` and `vocabulary-library-food-recall`, each with the literal plan revision `vocabulary-library-food-practice-1`. Keep `vocabulary-library-recognition` and `vocabulary-library-recall` at `vocabulary-library-practice-1`, with their existing Work and study membership, count bounds and pair-eighteen behavior. Route each focus and revision explicitly in the planner, server and web. Exercise identity remains independent of the catalog selection.
3. Add a finite per-situation availability projection to the existing authenticated teaching read (API-VER-002). Preserve current work-and-study fields and maxima for older clients. An eligible owner can see Food and Work and study together, with independent server-provided counts and starts. Use the existing outbox, owner revalidation and saved-run choice. Keep library practice excluded from cumulative ranking and lesson-introduction eligibility.
4. Verify empty, partial and full pools, owner isolation, expired/unsupported witnesses, stale previews and exact creation retry. Check the new plan’s replay and first grade after restart alongside the retained P1/P2/P3 evidence. Browser coverage should include both starts, a known mismatch, an unknown answer and exact recovery, with enlarged text and both browsers.
5. Track measured variation as semantic task count and presentational order separately. Three fixed choices and two recalled targets remain a small review pool. A later packet can add new scoped propositions when user experience justifies it.
