# Topic practice: the chooser and "Present verb forms"

- **Spec**: [tests/topic-practice.spec.ts](../tests/topic-practice.spec.ts)
- **Actors**: an anonymous learner choosing a practice focus (**Mixed practice** or **Present verb
  forms**, six parts: choose the form, recall, repair, each twice); the fixture answers parts through
  the API where a scenario is about something else
- **Owns** (exclusive server-side state): development-session-scoped practice sessions, follow-up
  sessions, exercises and attempts per test; device `localStorage` run records. Source sessions
  must stay byte-identical wherever noted

## Scenario: a fresh chooser creates no run before an explicit focus selection

1. The learner opens `/practice` and watches the creation and session requests.
   - *Expected*: "Choose your practice" shows with nine radios (three existing profiles and six first-step sessions, including the present-form and subject-first sessions), none checked, **Start practice**
     disabled, no creation request and no `session` in the URL.
2. They choose **Present verb forms** and start.
   - *Expected*: "Part 1 of 6"; exactly one creation request with `focusId` `present-verbs` and plan
     revision `present-verbs-1`, and one development-session request. The server state names follow-up
     plan revision `follow-up-2` and formats E001, E004, E005, E001, E004, E005.
3. They reload.
   - *Expected*: "Part 1 of 6" again; no further creation or session request.

## Scenario: selection, recall and repair stay separate through six answers and a frozen summary

1. The learner starts **Present verb forms** and answers each of the six parts wrongly (a choice for
   E001, typed text otherwise), checking and pressing **Continue** each time.
   - *Expected*: a feedback status after every check; "Part N of 6" after parts one to five and
     "Practice complete" after the sixth.
2. They read the summary.
   - *Expected*: three rows in the order selection, recall, repair with the labels
     "Present verbs — choosing the form", "… written recall", "… repairing the form"; each plans 2
     items and has 2 attempts; each row is an article with a checkbox.
3. They inspect the first-attempt context of items at positions 1, 2 and 4.
   - *Expected*: no prior exposure key starts with `verb-present:` — wrong feedback repeats the
     supplied answer and does not invent a correct-form exposure.
4. They reload.
   - *Expected*: the summary shows and the stored session JSON is byte-identical to before.

## Scenario: ${count} verb operation selections produce a bounded source-linked review

Runs for `count` = 1, 2 and 3 selected operations.

1. A **Present verb forms** run is completed through the API; the learner reloads and checks the
   first `count` summary articles' checkboxes, then presses **Start selected review**.
   - *Expected*: "Part 1 of 4" for one or two selections, "Part 1 of 6" for three.
2. They read the child through the API.
   - *Expected*: plan revision `follow-up-2`; the follow-up names the source id and exactly the
     selected keys `grammar:G004:<mode>`; the child's items use only those assessment modes.
3. They reload.
   - *Expected*: the same part count shows and **Source practice** links to
     `/practice?session=<source>`.
4. The child is completed through the API and the learner reloads.
   - *Expected*: "Practice complete" shows; no inherited `feedback` exposure starts with
     `verb-present:`; the source session is byte-identical to before.
5. They follow **Source practice**.
   - *Expected*: the source summary shows and the URL has the source id.

## Scenario: verb creation replays its exact profile after ${committed ? "committed" : "uncommitted"} response loss

Runs with the first creation lost before the server (`uncommitted`) and after the server created
the session (`committed`).

1. The learner starts **Present verb forms**.
   - *Expected*: an alert shows with a session id already in the URL.
2. They press **Start new practice**, see the chooser and press **Cancel**.
   - *Expected*: the alert and **Retry saved operation** are back, the URL still has the same id and
     only one creation request has been sent.
3. They reload.
   - *Expected*: "Part 1 of 6" for the same session id; two creation requests, identical bodies,
     both carrying `present-verbs` / `present-verbs-1`.

## Scenario: cancelling topic selection restores an uncertain answer and its exact retry

1. The first attempt request is committed and then aborted. The learner starts **Present verb
   forms**, answers wrongly and checks.
   - *Expected*: an alert shows.
2. They press **Start new practice** and then **Cancel**.
   - *Expected*: the chooser appears and then the alert returns; the URL still has the old session.
3. They press **Retry saved operation**.
   - *Expected*: the feedback status shows; two attempt requests with identical bodies; the server
     holds one attempt.

## Scenario: opening and cancelling the chooser preserves a draft and the old recovery record

1. In a **Present verb forms** run the first part is answered and advanced through the API; the
   learner reloads and types a draft "læ" into the next part.
2. They press **Start new practice**, select **Mixed practice** and press **Cancel**.
   - *Expected*: the chooser opened without changing the URL; after cancelling the draft "læ" is
     still in the field, the URL is unchanged and no development-session request was made.
3. They press **Start new practice** again and start **Mixed practice**.
   - *Expected*: "Part 1 of 8" on a different session id; the old run's record is still in
     `localStorage`.
4. They navigate back to the old URL.
   - *Expected*: "Part 2 of 6" shows; the server cursor of the old session is 1.

## Scenario: an expired owner is renewed only when the learner actually starts the chosen focus

1. A **Present verb forms** run is open; cookies are cleared.
2. The learner opens the chooser and cancels.
   - *Expected*: the run still shows "Part 1 of 6" and the URL is unchanged; no
     development-session or creation request was sent.
3. They open the chooser again and start **Present verb forms**.
   - *Expected*: a new run "Part 1 of 6" with a different session id and a different owner id; one
     development-session request and one creation request; the old run's `localStorage` record is
     kept, but the old session now answers `404` for the new owner.

## Scenario: a failed catalog check preserves the previous run and cancels without a mutation

1. The practice-profiles request is mocked to answer `503`. The learner opens the chooser,
   selects **Mixed practice** and presses **Start practice**, which runs the catalog check.
   - *Expected*: an alert shows; the URL is unchanged; no creation request was sent.
2. They press **Cancel**.
   - *Expected*: the earlier run shows "Part 1 of 6" and its server cursor is still 0.

## Scenario: topic chooser and verb operations reflow with keyboard access in ${theme}

Runs in light and dark (emulated colour scheme).

1. At 390×844 with text at 200% the learner opens `/practice`.
   - *Expected*: "Choose your practice" shows; zero axe violations and no horizontal overflow.
2. They focus **Present verb forms**, press Space, Tab to **Start practice** (within 30 presses) and
   press Enter.
   - *Expected*: the radio is checked, the button receives focus, and "Part 1 of 6" appears.
3. For the first three parts the part is scanned, answered through the API and advanced, and the
   page reloaded with text enlarged again.
   - *Expected*: each part scans clean and "Part N of 6" follows.
4. The run is completed and the page reloaded.
   - *Expected*: "Practice complete" shows, scanned clean with no overflow.
