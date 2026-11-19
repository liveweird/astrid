# Learner Flow Review

## Scope — 19 November 2026

Overnight BL-035 review of [v0.25.0](https://github.com/liveweird/astrid/releases/tag/v0.25.0), source `57548fb`. Walkthroughs used the released Docker image, a disposable database and fresh browser owners. The following are observed screen and engine behavior. Learner comprehension, classroom pacing, and Danish teaching approval still need human review under BL-035 and BL-022.

## First steps

Both native Chromium and WebKit completed the 22 activities in S01–S04 through the Lessons directory and the actual next-session chooser. Each lesson required teaching before assessment. Repeat opened a chooser with the same lesson selected, and cancelling returned to the unchanged result. A known wrong subject choice stayed advanceable in S01. In the first typed S04 slot, `måske` yielded “That response is outside the introduced alternatives for this task,” blocked continuation, survived reload, and allowed a retry with the requested form.

| Session and revision | Activities | Teaching cards | Cards repeated verbatim from earlier in this sequence |
| --- | ---: | ---: | ---: |
| S01 `module-one-singular-1` | 6 | 10 | 0 |
| S02 `module-one-groups-1` | 6 | 12 | 7 |
| S03 `module-one-er-1` | 6 | 15 | 7 |
| S04 `module-one-things-1` | 4 | 7 | 0 |

The rendered card counts match the saved introduction. Repeated cards help direct entry, but currently precede the lesson-specific material even in a sequential run. A teaching review should compare leading with today's target and keeping the foundation available as labelled support. A change to what opens or counts as teaching would need its own exposure policy and compatibility review.

### Reproducible corrections

1. **S03 occupation example needs its promised explanation.** Start **Using the verb to be**, open **Session introduction**, and read **Noun bundles**, **Worked examples**, and **Worked meanings** consecutively. The first shows `en lærer`, the second `Jeg er lærer`; the third translates the sentence and says the profession and article are supplied. The [activation contract](module-one-activation.md#language-ledger-and-licensed-frames) calls for an explanation of this particular bare occupational frame. The teaching copy should explain the relationship while retaining `er` as the only assessed form. Avoid generalizing article use to all professions.
2. **S02 should show the specified dictionary-to-present mapping.** Open **Talking to and about groups**. Its worked example is `Vi sover`, glossed “We sleep.” The required teaching lacks the `at sove → sover` mapping specified in the [session cards](module-one-activation.md#session-cards-and-exact-assessment-budget). The requested Danish answers remain subject forms. A future teaching revision can show that pair as supplied support without scoring the verb.
3. **The result counts describe overlapping parts of the same answers.** Complete the two S03 selection items correctly. Its first-answer result shows “2 fully met” and “2 target met,” then “2 first with support · 2 after teaching or earlier exposure in this run.” Both answer counts describe the same two answers. The required teaching itself counts as support in the existing model. Proposed acceptance: a learner can identify which results to revisit from a primary plain-language result, with separate accessible details for first/latest, language, target, response constraints and possible exposure. Keep stored grades and evidence definitions intact.

S04 clearly separates noun gender, reference and identification and assesses only the standalone subject word in the supplied context. It demonstrates `en bog → den` and `et brev → det`, alongside the distinct identifying prompt `Det er …`. A teaching reviewer could test whether a single, licensed complete reference example clarifies the use without expanding the assessment. The Lessons sequence exposed list and list-item roles in both browser accessibility snapshots. Actual screen-reader speech, including whether ordinal positions are announced, was not tested.

The [seed audit](practice-variation-audit.md#work-and-study-library-and-first-step-choice-positions--19-november-2026) found a practice shortcut in S03. Both selection items put the correct answer on the same side in all 1,024 sampled runs. Seed 0 gives first/first and seed 1 gives second/second. A revised activity can vary the bank position independently of the frame while retaining the related `er`/`være` alternatives.

### S05 through S13

Both native browsers completed the remaining 44 grammar activities at 390 × 844, through the visible Next choices. The eleven session profiles include three separate modal rounds. Entering `have` in the possession recall requested the present form. Entering `at læse` after supplied `kan` requested the bare infinitive. Each could be retried. A front-position tile order in S12 and a subject-first tile order in S13 received task-specific order feedback. Their target mismatch was known, language status remained unknown, and continuation was available. Both incorrect orders returned as editable selections on retry.

**S09 gives conflicting instructions about what to supply.** In **Names, homes and languages**, the first teaching card says “After the subject and verb, supply a name, i with a city, or a language.” The exercise frame already supplies the name, place or language, and the input requests a single present verb. Describe the complement as supplied in a future teaching revision. Keep the actual assessed form and published plan intact.

**Review the balance of teaching and practice in S10–S13.** All four S10 activities request `læse`, and the introduction gives the exact answer. Each S11 modal round states the intended interpretation before offering it as a choice. These are deliberately teaching-supported uses of a narrow pattern. A human review should assess when a new licensed frame or a separate later review would add value. The S13 introduction displays 21 cards for four questions, with the first activity heading 2,001 CSS px from the document top. The short answers `ja` and `nej` are supplied support. Question scoring remains limited to ordering the supplied constituents.

## Work and study, food and shopping, and places and journeys

Checked all 17 thematic plans at seed 47 against the released server and isolated PostgreSQL. Authored answers completed 23 distinct work-and-study, 21 food-and-shopping and 23 places-and-journeys activities. This checks the saved path and intended scope. It does not establish that a learner inferred an answer independently from the teaching.

At 390 × 844, both Chromium and WebKit completed **Requesting a supplied quantity**, **Waiting at the station**, and **A change and the pharmacy** through the visible controls. The cup/bottle board contained two separately assessed heads. The request tile exercises supplied the quantity together, rendered the comma outside the `tak` button, and recorded supplied order as the primary target. Both rows of the station mapping had separate language markup. In the rail reading item, the English context stated travel by train, while the Danish statement supplied the action of changing connections.

In `p3-5.4` (E048), the prompt asks for the present form in a car journey. `køre` gets the infinitive correction, and `cykler` gets the car-specific correction; both are known target mismatches. `svømmer` gets “That form is outside the reviewed material” and blocks continuation pending a retry. The assessed target remains the named present form. The matching and spatial items carry their own evidence.

### Teaching reaches beyond the first screen

Measure immediately after the introduction opens at 390 × 844 with default text, using the top of the first exercise heading relative to the viewport. Both browsers produced the same rounded measurements. The page stayed within the horizontal viewport.

| Entry | Rendered teaching cards | First exercise heading below top of viewport |
| --- | ---: | ---: |
| Work, reading and today | 7 | 717 CSS px |
| Requesting a supplied quantity | 15 | 1,238 CSS px |
| Waiting at the station | 19 | 1,862 CSS px |
| A change and the pharmacy | 25 | 2,286 CSS px |

The final Places introduction includes 169 space-separated words in the card bodies. The measurement is a scroll distance, not a reading-time estimate. A prominent jump to the first activity after saved teaching would help people who already know where they want to go and keep the full introduction available on direct entry.

### Some summary headings expose internal scope IDs

Complete **Requesting a supplied quantity** and **Waiting at the station**. Summary headings include `p2-polite-request`, `p3-station-spatial-contrast` and `p3-kore-present`. The lower description may explain the operation, but the heading and article's accessible name still show the ID. Replace those display titles with short labels such as “Polite request word order” and “To the station or at the station.” Retain internal IDs, answer keys, recorded outcomes and old plan snapshots.

The sampled P1 recognition cue was marked Danish and its choices were marked English, with the group named **Answer choices**. The finding about summary labels is distinct from pronunciation of Danish terms.

## Focused review and follow-up

At 320 CSS px, both browsers completed **Mixed practice** (eight activities), **Present verb forms** (six), and **Verbs and articles together** (four) from the **Review** list. Selection, written recall and repair stayed distinct. An intentionally wrong English meaning stayed advanceable, and retry retained both the first and latest results. With a correct verb and wrong supporting article, the two-gap feedback said “Verb target met; review the article.” The support error remained separate after a correct retry.

In each summary, follow-up required an explicit target. Selecting one headword direction started one written item; selecting present-form recall or the article-support row started four written items in their respective scopes. This exercises the article selection-to-recall change. Reloading each new run restored it exactly. **Source practice** returned to the same completed summary with identical parsed response data. These checks sampled the three visible paths. They did not test whether a learner would confidently choose the next target without help.

## Vocabulary library

Both browsers followed the [review guide](pilot-review-guide.md#try-vocabulary-library-practice) from a fresh owner. Opening and saving **Work, reading and today** unlocked three meaning choices. Saving **Study, courses and Danish** added the single written `nu` activity. A wrong selected meaning stayed advanceable and survived reload. An unreviewed written form blocked continuation until retry. Completing both modes and returning through **Back to vocabulary** left the saved teaching response unchanged.

## Cumulative mixed review

In fresh Chromium and WebKit sessions, opening **Review** made no learning API calls. **Prepare mixed review** focused the empty-pool message. After saving only S01 teaching, both browsers reported “6 parts from 1 lesson.” Reloading a started review restored the same introduction, item identities and position. Saving other lesson introductions afterward left that short run unchanged. A second browser owner still had an empty pool.

### Preparation describes the available pool as the selected run

After saving introductions through the isolated API for all fifteen First steps sessions and all seventeen thematic sessions, browser preparation showed “8 parts from 32 lessons” in both browsers. The eight items were distinct. The displayed 32 is the count of eligible lesson focuses with usable candidates, as specified by the [review contract](cumulative-review.md#eligibility-and-ownership). Each of the eight items has a single source, so the selected run cannot contain material from 32 lessons. A copy-only correction can say “8 activities available for this review” and show “32 eligible lessons” separately. Keep the count meaning and the selection policy intact.

### A short review can open a long introduction

At 390 × 844, the two full runs rendered 84 and 63 deduplicated teaching cards from their selected sources. The first activity heading was 6,931 and 5,869 CSS px from the document top, respectively. These runs had different saved seeds and selected sources. The measurements are examples of content length and scroll distance, rather than a browser comparison. An explicit **Go to first activity** action after teaching is saved would address the distance before changing any exposure policy. Test focus movement to the activity heading, keyboard access, reload and narrow or enlarged text. Any later change to which cards count as opened needs a separate exposure design.
