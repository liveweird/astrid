# Exercise Family Expansion

## Status and rationale

**7 November 2026 — Confirmed user interest:** The user asked whether now is a good time to consider more exercise types, observing that the current variety is small. The initial exchange authorized discussion and proposal development. **Subsequent user selection:** “OK, start with these” authorizes implementation of the recommended first batch: constrained sentence writing and short reading comprehension, initially in the standalone playground. Error diagnosis and transformations remain later proposals.

**Assistant recommendation:** Prioritize new reading/writing operations. The eight catalog entries available before BL-043 largely reused choice, gap filling, ordering and guided single-word repair. Additional topic chooser entries remain optional BL-042 polish. The earlier recommendation to start specifically with question transformations is superseded by the sequence below, which first explores current language material.

## Proposed sequence

| Priority | Family | Learner task and bounded starting point |
| --- | --- | --- |
| First batch | Constrained sentence writing | Write a complete Danish sentence from an English meaning and explicit constraints, using introduced vocabulary and active constructions. Unlike headword recall or arranging supplied blocks, the learner supplies the sentence. Start with one assessed grammar target; provide other needed knowledge. |
| First batch | Short reading comprehension | Read two or three generated, coherent sentences and answer a question about who does/has/reads what. Include competing details so the answer requires binding a participant, action and complement rather than recognizing one isolated noun. Start with related choice answers. |
| Following batch | Error diagnosis and repair | Locate a controlled error before repairing it. Current repair supplies the erroneous token and category. The new task assesses identification separately from correction; start with exactly one planted error and preserve the intended meaning. |
| Later candidate | Sentence transformation | Change one specified property while preserving the rest of the meaning. Statement-to-yes/no-question conversion is a candidate, but requires reviewed activation of G018/C010. Negation additionally needs G017/C009 and vocabulary outside the current seed. |

The user selected the first-batch families; later priorities remain assistant proposals. Prove both selected families in the standalone playground before deciding how to mix them into guided practice and source-linked reviews.

## Design constraints before implementation

- Define the primary target, supplied prerequisites and what each response can demonstrate. Several words in an answer do not automatically justify claims about every grammar or vocabulary skill.
- Generate prompts, source texts, canonical answers and plausible distractors from the same licensed meaning/structure. For reading, make questions answerable from the text; absence of a fact must not be treated as its negation. Pronoun reference and competing details require explicit checks.
- For sentence writing, specify the introduced vocabulary, acceptable variants, punctuation/case policy and response constraints. A licensed alternative can be valid but miss the requested task. An unsupported answer remains unknown rather than being declared invalid Danish.
- Keep error location, correction, grammar acceptability and preserved meaning distinct. A planted corruption is presentation data, not a licensed canonical sentence.
- Check the full teaching unit for answer leakage, and record support, retry and prior exposure. Existing exact-form exposure rules do not automatically cover propositions, whole sentences or diagnosis.
- Start with bounded explicit grading against licensed structures/answers. No unrestricted translation, free composition or new AI-grading dependency is selected.
- Define response/API fields, new revision boundaries and compatibility before code changes. Preserve all published generation/grading and saved replay. The selected first-batch IDs and grading policy are defined in the implementation contract; later families remain unspecified.

## Validation and open decisions

BL-006 owns family specifications; BL-007 covers grading and BL-008 the validation subset. Select worked success, wrong-target, valid-alternative, response-constraint and unknown cases before implementation. Check grammar isolation, ambiguity, real task variety, accessibility and historical compatibility. Language remains draft until appropriately reviewed under BL-022; automated checks do not establish teaching approval or learning efficacy.

The first-batch implementation choices are G005 subject-first writing, an optional unordered language reference, three-fact Danish passages, English reading answers and explicitly bounded accepted sentence variants. The [implementation contract](writing-reading-contract.md) records these choices and their rationale. Diagnosis interaction and guided-practice integration remain open; the implementation stays within the existing lexicon and active beginner constructions.


The selected first batch is governed by the [writing/reading implementation contract](writing-reading-contract.md), including concrete grading cases, new public/private fields, revision boundaries and ownership.


**Observed first-batch delivery:** Both selected families are available in the ten-entry standalone playground at `0.4.0`; see the [verified delivery](writing-reading-contract.md#observed-delivery--7-november-2026). User review should inform subsequent integration or family choices. This does not select diagnosis, transformations or new content.

## User exercise inventory — 8 November 2026

**User-proposed candidates, not an implementation selection:** the user supplied the following list and requested an implementation comparison and recommendations. Numbers preserve the user's ordering. “Pills” means selectable word or phrase tiles. The user clarified that item 8 means Danish-to-English translation with spare tiles (Q25), correcting its initial duplication of item 6.

| No. | Proposed exercise | Current coverage |
| --- | --- | --- |
| 1 | English word → Danish, four choices | E011, four related article+noun bundles in selected reading-material senses. |
| 2 | English word → typed Danish word | E008, introduced singular headword recall for `bog`, `avis`, `brev`. |
| 3 | Danish word → English, four choices | E007 implements the operation with **three**, not four, English choices. |
| 4 | English sentence → typed Danish sentence | E009, bounded introduced meanings and subject-first statements; not unrestricted translation. |
| 5 | English sentence → Danish, all tiles used | E012, individual word tiles with English cue; all required, subject-first target. |
| 6 | English sentence → Danish, spare tiles | E013, one spare infinitive; finite-present target with separate response-order constraint. |
| 7 | Danish sentence → English, all tiles used | Not implemented. |
| 8 | Danish sentence → English, spare tiles | Not implemented; direction confirmed by the user. |
| 9 | One Danish sentence gap, choose the word | E001, finite present versus infinitive of the same verb; two choices. Other word classes are not implemented as choice gaps. |
| 10 | Several Danish sentence gaps, choose words | Not implemented. |
| 11 | English–Danish word matching grid, either orientation | E014, four article+noun pairs with English left and Danish right, per-pair feedback and partial checking. Reverse orientation remains unimplemented. |
| 12 | Produce a question for an answer, typed or tiles | Not implemented; requires question grammar and a constrained intended question. |
| 13 | Reorder a scrambled Danish sentence | E003, all supplied blocks used; article+noun stay together, and the task requests subject-first order. |
| 14 | Match sentence beginnings and continuations | Not implemented. |
| 15 | Supply a verb in the correct tense | E001/E004/E005 cover present-form selection, supply and guided repair; other tenses and tense choice are not activated. |
| 16 | Assign the correct article | E002/E006, supply or repair unstressed singular `en`/`et`; broader article/definiteness work is not activated. |

E005/E006 guided repair and E010 short-text comprehension are additional delivered operations. E009/E010 remain standalone; existing guided practice and reviews remain pinned to revision three.

### Revised recommendation — assistant proposal

For discussion, refine the earlier proposed post-delivery sequence: first add **English-to-Danish word choice (1)** and **English-to-Danish sentence tiles, all required then with distractors (5/6)**; next add **word matching (11)** and **multiple gaps (10)**. Reverse sentence translation (7/8) is a subsequent extension. Error diagnosis before repair remains an additional candidate. These priorities are proposals; no new family is authorized by this comparison, and BL-006 retains its existing status.

Keep direction, response interaction, grammar/vocabulary target and difficulty separate when specifying a family. Four choices need four reviewed, distinct senses with plausible alternatives: the current lexical assessment pool has only three, so do not invent an unrelated fourth distractor. A matching grid may initially use those three pairs; four pairs require another reviewed sense.

**8 November 2026 — Subsequent authoring authorization:** the user selected a concrete Module 1 course-slice plan, including [worked acceptance cases](module-one-worked-exercises.md) for #1 and #5/#6 and an [engine gap/dependency list](module-one-course-slice.md#engine-gaps-and-dependency-order). The candidate fourth reading-material sense is printed `magasin`; `blad` was excluded from this choice pool because its dictionary meaning overlaps `avis`. This selects the authoring pass, not runtime delivery of the new families.

For tile tasks, distinguish individual words from existing constituent blocks, preserve repeated-word identity, accept licensed equivalent orders and avoid capitalization/punctuation revealing the order. Spare tiles should probe the selected target rather than add arbitrary noise. Multiple gaps need explicit target and feedback per slot. Matching halves (14) needs enough context for fair logical matches; accept all licensed combinations or construct an unambiguous set. Questions (12) need an intended question type or highlighted answer information because one answer can satisfy many questions. New tenses (15), questions (12), and richer continuation structures require reviewed content/grammar activation, beyond adding a widget.

**8 November 2026 — Subsequent implementation selection:** the user selected BL-047 and proper versioned release. The [word-choice/tile contract](word-choice-tiles-contract.md) now governs #1 and #5/#6 only, with a bounded choice-only magazine sense and the existing 99 positive propositions. This supersedes the unselected status above for these three formats. Matching, multiple gaps, reverse sentence tiles and later grammar remain proposals.

**Observed subsequent delivery:** Items 1/5/6 are delivered at [0.5.0](https://github.com/liveweird/astrid/releases/tag/v0.5.0) under the [verified contract](word-choice-tiles-contract.md#observed-delivery--8-november-2026), superseding their earlier unselected status. They remain standalone with E009/E010; guided plans stay at pair three. The table's current-coverage cells reflect this delivery. Matching, multiple gaps, reverse sentence tiles and later grammar remain proposals.


**8 November 2026 — Matching implementation selected:** the user's acceptance selects BL-048
for #11, English-left/Danish-right with four existing article+noun senses, partial checking and
per-pair feedback. The [matching contract](vocabulary-matching-contract.md) supersedes its
unselected status above. Opposite orientation, multiple gaps and further families remain proposals.


**Observed matching delivery:** item 11 is delivered at [0.6.0](https://github.com/liveweird/astrid/releases/tag/v0.6.0) under the
[verified matching contract](vocabulary-matching-contract.md#observed-delivery--8-november-2026),
superseding its earlier proposed status. It remains standalone; multiple gaps, reversed
orientation and guided integration remain separate decisions.


**9 November 2026 — Multiple gaps selected:** “Go for it” selects BL-049 / app 0.7.0. The
[multiple-gap contract](multiple-gap-contract.md) defines one licensed Danish sentence with a
primary G004 finite-present gap and explicitly assessed G006 supporting-article gap, separate
feedback, partial answers and safe editable retry. This supersedes the unselected status above
for #10 only; guided integration, reverse translation and further grammar remain proposals.
