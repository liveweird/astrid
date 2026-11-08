# Vocabulary matching

- **Spec**: [tests/vocabulary-matching.spec.ts](../tests/vocabulary-matching.spec.ts)
- **Actors**: an anonymous playground learner
- **Owns** (exclusive server-side state): E014 exercises and attempts in each test's development session

These tests use the same four draft senses as Danish word recognition. Passing tests
establish reproducible behavior, not teaching approval.

## Scenario: matching supplies four bilingual article bundles and supports keyboard pairing removal and grading

1. Generate **Match English and Danish words**, seed 47/index 5.
   - *Expected*: four English meanings, four Danish article+noun phrases, opaque positional IDs,
     four unlinked targets, correct language tags and disabled checking before any match.
2. Use Enter/Space to select a pair; remove it using Enter, then fill the whole board by keyboard.
   - *Expected*: all four facets met, four per-pair feedback rows and focused Good work feedback.

## Scenario: regeneration clears an unfinished English selection before pairing on the new board

1. Select an English item without its Danish counterpart, change the index and regenerate.
   - *Expected*: a new exercise identity; no English item pressed, all Danish items disabled,
     no chosen pairs and disabled checking. Previous local selection never transfers.
2. Build the new board correctly and check.
   - *Expected*: all targets met on the first attempt for this exercise.

## Scenario: matching partial and mixed feedback permits editing every pair on a new supported retry

1. Check one correct pair.
   - *Expected*: one met row, three unmatched rows; incomplete constraint and no whole-board success.
2. Try again, retain the correct pair and add two wrong pairs plus another correct pair.
   - *Expected*: board receives focus; saved selection can be removed; complete mixed feedback has
     two correct/two incorrect rows and retry number two, explicitly labeled Retry after feedback.
3. Retry, open teaching support, remove every match and submit the correct board.
   - *Expected*: every pair remains editable, support sticks and the third attempt is supported.

## Scenario: matching replays the exact saved board after ${committed ? "committed" : "uncommitted"} response loss

Runs before and after server commit.

1. Open support, build the correct board in reverse displayed-left order and lose the response.
   - *Expected*: uncertain result; all pairing and removal controls frozen, accessible state.
2. Check the same attempt.
   - *Expected*: byte-identical serialized request, unchanged pair order, supported Try 1 success.

## Scenario: matching board and partial mixed and complete feedback reflow at 200 percent text in ${theme}

Runs in light and dark at 320×844 with 200% text.

1. Scan the empty board, one-correct-pair feedback, partial mixed feedback and complete success.
   - *Expected*: no horizontal overflow or Axe WCAG A/AA violations; badges remain unclipped.
     Text-range bounds for each chosen pair and feedback card stay within that card, catching
     local clipping that document overflow cannot detect.
2. Use retry to edit earlier pairs before completing the board.
   - *Expected*: all controls remain usable with enlarged text.
