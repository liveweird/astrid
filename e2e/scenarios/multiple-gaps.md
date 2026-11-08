# Present verb and supporting article gaps

- **Spec**: [tests/multiple-gaps.spec.ts](../tests/multiple-gaps.spec.ts)
- **Actors**: an anonymous playground learner
- **Owns** (exclusive server-side state): E015 exercises and attempts in each test's development session

The primary target is G004 finite present. G006 indefinite article is explicitly assessed
supporting grammar. Test oracles use existing verb forms and noun gender, not a private answer
endpoint. Passing tests do not establish Danish teaching approval.

## Scenario: two gaps expose primary and supporting roles and support keyboard changing and clearing

1. Generate **Choose a present verb and article** at seed47/index5.
   - *Expected*: three sentence segments, two slots with primary/support roles and Danish choices.
2. Select the primary form with Space, clear it with Enter, then change a wrong choice to the
   correct choice and supply the article by keyboard.
   - *Expected*: checking is disabled with neither slot selected; complete success has focused
     feedback and two per-gap outcomes, with each server explanation in its correct role row and no meaning assessment.

## Scenario: primary correct with supporting article wrong or missing never reports overall success

1. Check only the correct verb.
   - *Expected*: primary target met, language unknown, incomplete response and no completed
     sentence. Missing support never receives whole-exercise success.
2. Retry, retaining the verb; add the wrong article and check.
   - *Expected*: target stays met, supporting outcome is not-met, language has a known issue,
     response is complete, and retry is labeled Retry after feedback. No Good work appears.
3. Retry, open optional support and correct the article.
   - *Expected*: third-attempt supported success, every selection remains editable.

## Scenario: wrong primary verb with correct support retains separate outcomes and the selected sentence

1. Supply the infinitive and correct noun article.
   - *Expected*: primary target not-met, support met, known language issue and complete response.
2. Read completedSentence.
   - *Expected*: it contains the learner's selected infinitive, never a substituted correction.

## Scenario: regeneration clears both gap selections even when positional ids are reused

1. Select both answers, change the index and regenerate.
   - *Expected*: new exercise ID, no pressed choices and disabled checking; previous selections
     never transfer to reused g0/g1/c0/c1 identities.

## Scenario: gap answers preserve supported reversed selection order after ${committed ? "committed" : "uncommitted"} response loss

Runs before and after server commit.

1. Open support and answer article before verb; lose the first response.
   - *Expected*: uncertain message, frozen choices and clear controls, accessible state.
2. Check the same attempt.
   - *Expected*: byte-identical request body and original reversed selections, supported Try1
     success without a new grade or attempt identity.

## Scenario: gap prompt and partial mixed and complete feedback reflow at 200 percent text in ${theme}

Runs at320×844/200% in both themes.

1. Scan empty prompt, primary-only feedback, primary-correct/support-wrong feedback and full success.
   - *Expected*: no Axe A/AA violations, document overflow, clipped feedback badges or text ranges
     outside the individual choice/feedback card.
2. Retry and edit support between each grade.
   - *Expected*: primary selection survives, all controls remain operable and no meaning badge appears.
