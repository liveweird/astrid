# Vocabulary recognition and recall formats

- **Spec**: [tests/vocabulary.spec.ts](../tests/vocabulary.spec.ts)
- **Actors**: an anonymous learner on the engine playground (`/playground`)
- **Owns** (exclusive server-side state): development-session-scoped generated exercises and graded
  attempts in each test's own context

The three headwords are *bog* (book), *avis* (newspaper) and *brev* (letter (written message)).
Exercises use seed 47 and an index. Result labels describe engine output; the Danish content is a
draft awaiting teaching review, and nothing here is linguistic approval.

## Scenario: recognizes all three meanings with related English choices and tracked teaching support

For index 0, 1 and 2 of **Recognize the meaning**:

1. The learner generates the exercise.
   - *Expected*: the lexical target is the meaning facet, direction Danish to English; the Danish cue
     is marked `lang="da"`; exactly three answer buttons show, one per authored meaning, each marked
     `lang="en"`.
2. They choose a wrong meaning and check.
   - *Expected*: "Target to revisit" with "Recognized English choice"; no sentence claims a judgment
     about Danish.
3. They press **Try again**; at index 0 they also open **Learn this first** and choose the right
   meaning with the keyboard (Tab then Enter), otherwise they click it; they check.
   - *Expected*: "Target met", "Recognized English choice", and "Try 2 · Supported practice" at index 0
     or "Try 2 · No support opened" otherwise.

Across the three indices the headwords seen are exactly *avis*, *bog*, *brev*.

## Scenario: recalls all three introduced headwords without a visible answer reveal

For index 0, 1 and 2 of **Recall the Danish headword**:

1. The learner generates the exercise.
   - *Expected*: the lexical target is the singular-headword facet, English to Danish; the English
     cue is marked `lang="en"`; the Danish headword is not visible; the input is marked `lang="da"`.
2. They type another headword and check.
   - *Expected*: "Target to revisit" with "Recognized Danish headword".
3. They press **Try again** and type the right headword in capitals (typed through the keyboard at
   index 0, filled otherwise) and check.
   - *Expected*: "Target met", "Recognized Danish headword" and "Good work".

Across the three indices the headwords seen are exactly *avis*, *bog*, *brev*.

## Scenario: separates a known headword from an extra article, gender and unknown-writing constraints

1. The learner generates **Recall the Danish headword** and types the headword with its article
   ("et" for *brev*, "en" otherwise).
   - *Expected*: "Target met" and "Recognized Danish headword", with "One-word response required"
     and a note that they identified the introduced headword; no "Good work".
2. They try again with the other-gender article.
   - *Expected*: "Target met", "Known language issue" and "One-word response required".
3. They try "unfamiliar".
   - *Expected*: "Target to revisit", "Language status unknown" and a note about alternative
     translations.
4. They open **Learn this first** and type the bare headword.
   - *Expected*: "Target met", "Recognized Danish headword" and "Try 4 · Supported practice".

## Scenario: replays an uncertain lexical submission with its original target and support evidence

1. The first attempt request is committed by the server and then aborted. The learner generates
   **Recall the Danish headword**, types the right headword and checks.
   - *Expected*: "The result is not confirmed"; the text field and **Insert æ/ø/å** buttons are
     disabled.
2. They press **Check the same attempt**.
   - *Expected*: the feedback status holds focus and shows "Recognized Danish headword" and
     "Try 1 · No support opened"; two attempt requests with identical bodies were sent.
