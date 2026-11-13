# Vocabulary P2 — Food, cafés and shopping

- **Spec**: [tests/vocabulary-p2.spec.ts](../tests/vocabulary-p2.spec.ts)
- **Purpose**: protect the six authored P2 sessions, their teaching gate, five exercise operations,
  cumulative admission and compatibility behavior.

## Scenario: P2 teaches and completes every authored activity in ${profile.focus}

1. Open each session from **Food, cafés and shopping**.
   - *Expected*: the run is pinned to `vocabulary-p2-s1`…`s6`, revision `-1`, `pilot-19` /
     `generator-19`, and the authored 3/4/4/3/3/4 activity counts.
   - *Expected*: attempts are rejected until the required introduction is saved.
2. Open teaching and complete all 21 authored activities.
   - *Expected*: Danish teaching is visibly marked, every E040–E044 response uses its advertised
     text, matching or order transport, and the completed summary survives reload without overcredit.
3. Exercise the corrected Session 6 board when that session is selected.
   - *Expected*: only `a cup ↔ en kop` and `a bottle ↔ en flaske` appear; one partial pair cannot
     continue, retry preserves the pair, and the complete two-pair board can continue.
4. Complete both supplied requests in Session 6.
   - *Expected*: the fixed comma appears outside the `tak` tile only after preceding content, and
     server feedback preserves the complete punctuated sentence.
5. Complete the run and reload the summary.
   - *Expected*: the actual activity and summary counts are saved without turning supplied words into
     independent recall or grammar credit; the family becomes eligible for revision-two cumulative review.

## Scenario: P2 two-pair matching preserves a partial retry and then completes

1. Open Session 6, save teaching, and pair only one English head with its Danish head.
   - *Expected*: feedback reports one retained result and requires exactly two matches; Continue is disabled.
2. Retry, keep the existing pair and add the other pair with the keyboard.
   - *Expected*: the honest two-pair board completes without filler quantities or beverage mappings.

## Scenario: P2 matching replays the exact saved response after a lost response and reload

1. Complete the Session 6 matching board and let the server commit while the browser loses the response.
2. Reload the URL-bound run.
   - *Expected*: the exact serialized outbox body and attempt identity replay, producing one saved attempt.

## Scenario: P2 blocks an unknown complete order until the learner retries

1. Reach Session 4's purchase order, submit every supplied block in reverse order, then retry.
   - *Expected*: the UI uses the bounded unknown-language explanation and blocks continuation until
     a reviewed response is saved.
2. Remove the chosen blocks and submit the exact taught order.
   - *Expected*: the corrected response can continue without generalizing the first answer as bad Danish.

## Scenario: cumulative review admits all six taught P2 families while a saved revision-one plan stays unchanged

1. Introduce P1 and inspect revision-two preparation before any P2 teaching is saved.
   - *Expected*: untaught P2 families are absent and only one source lesson is reported.
2. Save a revision-one cumulative plan, then introduce all six P2 families.
   - *Expected*: new preparation advertises `cumulative-review-2` with all seven taught source
     families, while the saved revision-one plan remains byte-for-byte unchanged and resumable.

## Scenario: P2 matching requests fixed punctuation and summary reflow at 320px and 200% in ${theme}

1. Repeat Session 6 in light and dark themes at 320px with actual 200% text.
   - *Expected*: teaching, matching, fixed punctuation, feedback and summary remain keyboard-usable,
     axe-clean and free of horizontal overflow.

## Scenario: P2 long functional matching and meaning choices reflow at 320px and 200% in ${theme}

1. In each theme at 320px and actual 200% text, open Session 3's four-pair board with its full
   attention/handover labels, then save its complete matching feedback.
2. Open Session 5 and inspect the full `gerne` meaning alternatives.
   - *Expected*: the long bilingual controls and feedback remain axe-clean, keyboard-operable and
     within the viewport without shortening their authored meanings.

## Scenario: P2 standalone playground submits the advertised operation ${operation.id}

1. Open the developer playground and select each advertised E040–E044 operation using an authored
   seed/index that covers the two-head matching board and fixed-comma request.
2. Answer from an independent cue, board or order oracle.
   - *Expected*: catalog selection generates the chosen format, the UI submits its actual text,
     matching or order transport, the two special operations retain two pairs/fixed punctuation,
     and the server saves a fully met result without creating a lesson run.
