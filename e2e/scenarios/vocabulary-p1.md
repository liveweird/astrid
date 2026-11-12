# P1 vocabulary: a day of work and study

- **Spec**: [tests/vocabulary-p1.spec.ts](../tests/vocabulary-p1.spec.ts)
- **Actors**: an anonymous learner entering each P1 session independently
- **Owns** (exclusive server-side state): each generated test owns its development-session-scoped
  P1 practice session, exercises, support confirmations and attempts

## Scenario: P1 teaches direct entry and exercises every authored slot for ${profile.focus}

1. Enter each of the five P1 sessions directly, before completing any other lesson.
   - *Expected*: the run is independently pinned to its `vocabulary-p1-s1`…`s5` focus,
     revision-two plan, pair eighteen and the literal 4/4/5/5/5 item count. Assessment before the saved session
     introduction is rejected and persists no attempt.
2. Open the required introduction and work through its exact packet slots using a literal answer
   oracle independent of the inspector and grader.
   - *Expected*: each profile matches its literal authored format/cue sequence, so the five journeys cover exactly all 23 slots and E031–E039.
     Danish/English reading metadata, passages, choices, matching columns, typed fields and occurrence
     tile banks retain their selected response shapes.
3. On every reading, save a wrong supplied fact before correcting it. On every three- or four-pair
   board, save one partial pair, retry the editable retained pair, save a complete mixed board, then
   remove and correct all pairs.
   - *Expected*: wrong facts remain known supplied meanings; incomplete boards cannot continue;
     complete mixed boards can continue without implying success; per-pair outcomes distinguish met,
     not-met and missing rows; Try again restores keyboard-editable pair controls.
4. For typed present forms, submit the introduced infinitive before the case-insensitive correct
   present. For the spare-form tile task submit `Nora drikke vand` before `Nora drikker vand`.
   For the two reviewed fronting banks submit the grammatical fronted order before subject-first order.
   - *Expected*: isolated infinitives are acceptable Danish but miss the present-form target; the spare task leaves `drikke`, and reviewed
     fronting preserves meaning/Danish acceptability while missing the explicit subject-first target.
     Whole-bank answers use each occurrence once.
5. Complete each run, inspect its summary, reload, then open Next or Repeat and cancel the chooser.
   - *Expected*: every item is attempted/completed, summary rows group repeated reading roles/order scopes while keeping each matching meaning separate, remain saved after refresh, and the
     suggested destination is checked without auto-starting it, and cancel returns to the preserved run.

## Scenario: P1 latest unknown blocks continuation after an eligible typed answer and preserves both saved results

1. Enter Coffee and water directly, save its teaching, and reach present-form recall after completing
   the preceding authored slots.
2. Save correct `drikker`, retry with unsupported `svømmer`, then reload and read back the unknown attempt.
   - *Expected*: the latest unknown supersedes the eligible correct answer, disables Continue, preserves
     raw stored response bytes across reload and does not invent a Danish correction.
3. Retry with introduced infinitive `drikke`.
   - *Expected*: it is a known wrong present-form outcome and enables continuation without full success.

## Scenario: P1 matching replays the original serialized board after a committed response is lost and refreshes

1. Enter Reading and writing directly, save its teaching and pair all four rows in reverse answer-array order.
2. Let the server commit the matching attempt, abort its response and refresh.
   - *Expected*: recovery replays the same attempt identity and byte-identical serialized matching body.
     The original pair array is retained and the server stores one attempt, not a duplicate.

## Scenario: P1 teaching matching feedback tiles typed input and summary reflow at 320 and 200 percent in ${theme}

1. In each colour scheme, open Study, courses and Danish at 320 px and 200% text, then save its teaching.
   - *Expected*: direct-entry teaching has no horizontal overflow or configured Axe violations.
2. Exercise partial and complete matching, explicit-fact reading, reviewed fronting tiles and both typed
   recall fields, scanning each feedback state.
   - *Expected*: the four new runner families remain keyboard-operable and accessible without clipped
     horizontal content at the bounded viewport.
3. Inspect the completed five-item summary.
   - *Expected*: matching pair evidence and the reading, order and recall rows reflow accessibly in both themes.

## Scenario: P1 standalone playground submits the advertised operation ${operation.id}

1. Open the developer playground and select each of the nine advertised P1 operations at literal
   seed 47/index 5 through the format control.
2. Use the independent packet answer oracle to choose meanings, match visible words, arrange
   occurrence tiles or type the introduced answer.
   - *Expected*: the selected format renders and submits its correct text/order/matching answer
     family, saves a fully met result and offers an editable retry. No inspector answer key is used.

## Not covered here

Danish teaching approval and learning efficacy remain outside browser verification. Revision dispatch,
private answer absence, fingerprint mutation, exposure projection and database owner locking are covered by
engine/server tests; retained E014 matching and old profiles stay covered by their existing journeys.
