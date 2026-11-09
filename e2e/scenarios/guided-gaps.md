# Guided verb and article gaps

- **Spec**: [tests/guided-gaps.spec.ts](../tests/guided-gaps.spec.ts)
- **Actors**: English-speaking learner in an anonymous development session.
- **Owns**: Its session-scoped practice runs, exercises, answers, summaries, reviews and browser recovery storage.

## Scenario: guided gaps keep four exercises and separate primary and supporting summary outcomes

1. Choose Verbs and articles together and verify its explicit plan/revision and four E015 items.
2. Submit correct verbs with wrong or missing articles, a wrong verb with a correct article, and a complete correct response. Open hint support after selecting the first answer, then retry and open lesson support after editing; both selections remain intact. Every response displays the two individual feedback cards.
3. Continue each item; a new exercise clears selections. Complete with five answers and one retry counted once for the session.
4. Verify independent primary/supporting rows, first/latest outcomes and unevaluable missing articles. Reload and compare exact saved summary bytes.

## Scenario: guided gaps create a selected-only ${target} review with explicit article recall

1. Complete an all-wrong four-item source, then select only verbs, only articles, or both.
2. When articles are selected, explain written recall before creating the review.
3. Verify four source-linked E001/E002 items assess only the chosen targets, preserving article recall identity.
4. Complete the child; article review can create another article-only recall review. Original source bytes remain unchanged.

## Scenario: guided gaps replay exact reversed slot answers through reload after ${committed ? "committed" : "uncommitted"} response loss

1. Select the article first and verb second, retaining their interaction order in the answer array.
2. Lose the attempt response, once before commit and once after commit; prevent editing while uncertain.
3. Reload the same owner/run. Replay identical request bytes and identity, recover one saved attempt, then retain selections on editable retry.
4. Clear the article and submit a fresh partial attempt without duplicating the original grade.

## Scenario: guided gaps support lessons and per-slot summaries reflow in ${theme} at ${viewport.width} with enlarged text

1. Start four-item guided gaps in both themes at 320px portrait and 844px short landscape, with 200% text.
2. Open the persisted session introduction by keyboard and verify the Danish worked example's language tag; open item teaching.
3. Scan choice, teaching and partial feedback states for accessibility and horizontal reflow. Check text-range bounds within individual paragraphs and summary columns so internal overlap fails even without document overflow.
4. Finish the run and scan the completed two-row summary at the same enlarged viewport.

## Not covered here (and why)

The engine/server suites cover all nine slot combinations, exact per-assessment exposure filtering,
source exclusion, invalid profile/target matrices and PostgreSQL rollback. Existing practice suites
retain owner-expiry, browser storage-denial and real multi-tab queue tests. These software checks do
not confer Danish teaching approval.

## Scenario: guided gaps replay ${stage} identity after ${committed ? "committed" : "uncommitted"} response loss

1. Start a new guided-gap run, or select article-only review from a completed source.
2. Lose creation/follow-up response before or after commit. Keep the durably saved child/run identity in the URL.
3. Reload and automatically replay identical creation bytes, recovering one four-item run with no answers.
4. Verify exact plan revision and, for reviews, immutable source lineage and article-only E002 content.
