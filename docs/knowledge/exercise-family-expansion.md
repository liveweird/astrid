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
