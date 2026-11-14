# Places and journeys vocabulary

- **Spec**: [tests/vocabulary-p3.spec.ts](../tests/vocabulary-p3.spec.ts)
- **Actors**: the anonymous learner and an API client in the same development session
- **Owns** (exclusive server-side state): development-session-scoped P3 exercises, attempts, practice sessions and cumulative reviews

## Scenario: P3 teaches and completes every authored activity in ${profile.focus}

1. Enter each of the six Places and journeys sessions independently.
   - *Expected*: The exact profile, pair-twenty revision and 4/4/4/3/4/4 item counts are returned, and grading is blocked until teaching is saved.
2. Open the direct-entry teaching and inspect its authored forms, English labels and full car or rail explanation and translation.
   - *Expected*: Full authored teaching remains accessible, English labels/scenes remain English, and Danish passages and forms retain Danish language markup.
3. Complete every authored matching, reading, choice, recall and supplied-order activity using its exact bank.
   - *Expected*: All 23 activities use only E045–E051, assessment scenes are exactly the minimal car/train cues, activity 6.2 reveals no English action answer, role-specific reading results remain separate, and reload preserves the frozen summary.

## Scenario: P3 partial matching remains unknown until corrected and replays one saved board

1. Submit one pair from the four-pair station matching board.
   - *Expected*: Unmatched rows are unknown, the complete-board constraint is unmet and continuation remains disabled.
2. Retry, complete the exact board, and lose the response after the server commits.
   - *Expected*: Reload replays the byte-identical saved board once and displays the confirmed result.

## Scenario: P3 recall separates known wrong, unknown and one-word shape before correction

1. Reach car-context kører recall after saving teaching and the first three activities.
   - *Expected*: The exact minimal English car scene stays visible and the Danish input remains separately labelled.
2. Submit taught infinitive køre, unknown bil and multiword kører hjem in separate retries.
   - *Expected*: Known wrong may continue, unknown and invalid one-word shape may not, and the shape feedback is explicit.
3. Correct the response to kører.
   - *Expected*: The saved correct response permits continuation.

## Scenario: P3 whole spatial contrast records one scoped result without lexical credit

1. Reach the home spatial contrast and choose the authored reversed whole mapping.
   - *Expected*: The response is a known complete off-target and may advance.
2. Inspect the completed summary.
   - *Expected*: Exactly one p3-home-spatial-contrast result exists and it has no lexical target credit.

## Scenario: P3 standalone recall separates the English instruction from the Danish cue

1. Select E048 dynamically from the standalone catalogue.
   - *Expected*: The exact minimal English car scene and pure-English instruction remain visible, while `at køre` and the answer field are marked as Danish.

## Scenario: cumulative review three grows from 20 to 26 to 32 families without changing saved one and two

1. Save teaching for the retained twenty Module 1/P1 families, then the six P2 families, then all six P3 families.
   - *Expected*: Current cumulative-three preparation reports 20, 26 and 32 eligible source lessons in sequence.
2. Save cumulative-one at twenty and cumulative-two at twenty-six before adding later teaching.
   - *Expected*: Both saved private selections remain byte-for-byte unchanged after cumulative-three reaches thirty-two.

## Scenario: P3 cumulative rail presentation keeps the minimal scene separate from full teaching

1. Save the complete Session 6 teaching, prepare cumulative review three and select the bounded seed whose saved source is activity 6.2.
   - *Expected*: The teaching record remains the complete authored rail explanation while the selected assessment source is unchanged.
2. Start the exact cumulative run and open its introduction.
   - *Expected*: The presented assessment context is exactly `Rail context: This journey is by train.` and contains neither `changes` nor `connections`.

## Scenario: P3 bilingual contrast and summary reflow at 320px and 200% in ${theme}

1. In light and dark themes, open the home-contrast session at 320px and enlarge text to 200%.
   - *Expected*: Teaching, matching, readings, bilingual mapping buttons, feedback and summary have no horizontal overflow or WCAG A/AA violations.
2. Complete the session entirely by keyboard-aware controls.
   - *Expected*: Focus follows feedback and completion while the final summary remains accessible at enlarged text.

## Not covered here (and why)

The Danish draft still requires specialist teaching review. Browser assertions verify the selected software contract, not linguistic approval.
