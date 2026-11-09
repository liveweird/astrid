export interface ChangelogEntry {
  version: string;
  /** Release date in YYYY-MM-DD form. */
  date: string;
  /** Authored English Markdown shown to learners and reviewers. */
  body: string;
}

// Build-time release content, newest first. A deploy changes this artifact; the
// application does not fetch or reconstruct release notes at runtime.
export const CHANGELOG: readonly ChangelogEntry[] = [
  {
    version: "0.9.0",
    date: "2026-11-10",
    body: `**Take your first steps in Danish.**

- **Module 1: First steps** offers three short sessions: talking about one person, talking to and about groups, and using the present form of to be.
- Read each session's introduction before six practice activities. Choose introduced subject words and verb forms, then supply them in writing with vocabulary support.
- See separate recognition and written-recall results. Unknown answers stay open for support or retry; confirmed mistakes and clear written-form corrections can continue.
- Repeat a session or choose the suggested next one. Earlier exercises, practice runs and saved answers keep their original behavior.
- The beginner material remains a draft awaiting Danish teaching review. These results describe practice after teaching and support.`,
  },
  {
    version: "0.8.0",
    date: "2026-11-09",
    body: `**Practise verbs and articles together.**

- Choose **Verbs and articles together** for four guided two-gap exercises, with an optional introduction and worked example.
- See separate results for the primary present verb and supporting article, including first answers and editable retries.
- Select either target for a focused review. Verb reviews use choices; article reviews ask you to supply the article in writing.
- Saved selections and uncertain answers recover safely after reload. Earlier practice runs and exercise results keep their original revisions.`,
  },
  {
    version: "0.7.0",
    date: "2026-11-09",
    body: `**Two gaps with clear grammar feedback.**

- **Choose a present verb and article** completes two gaps in a Danish sentence using related choices for each gap.
- The present verb is the primary target; the noun's article is supporting grammar. Feedback shows each result separately.
- Check one or both answers, then edit either selection on retry. An uncertain result safely reuses the exact saved attempt.
- Earlier exercises, saved results and guided practice keep their original revisions.`,
  },
  {
    version: "0.6.0",
    date: "2026-11-08",
    body: `**Match English and Danish words.**

- Match four English meanings with Danish nouns, each shown with its article, in independently shuffled columns.
- Check a complete or partial board and see feedback for each pair. Use touch, mouse or keyboard to make and remove matches.
- Try again keeps your selections editable. After an uncertain connection, checking the same attempt safely retrieves its saved result.
- Existing exercises, saved results and guided practice keep their original revisions.`,
  },
  {
    version: "0.5.0",
    date: "2026-11-08",
    body: `**More ways to practise Danish.**

- **Choose the Danish word** matches an English meaning to one of four related Danish nouns, each shown with its article.
- **Build a Danish sentence** translates an English sentence using every individual word tile.
- **Choose the present form with word tiles** adds a spare infinitive, so you practise choosing the present verb while building the sentence.
- Feedback separates the grammar target, meaning, language and requested response shape. Saved exercises and existing guided practice retain their original revisions.`,
  },
  {
    version: "0.4.1",
    date: "2026-11-08",
    body: `**A new Astrid logo.**

- Astrid's mark is now a girl with plaited hair, drawn in shades of Danish red. It appears in the navigation and as the browser tab icon, with a lighter variant for the dark theme.`,
  },
  {
    version: "0.4.0",
    date: "2026-11-07",
    body: `**Sentence writing and short reading tasks.**

- **Write a Danish sentence** asks you to express an English meaning using introduced vocabulary. Feedback separates word order, meaning, language issues and the requested response shape.
- **Read a short text** asks you to connect a participant and action to the right object across three Danish sentences.
- Both exercise types are available in **Engine playground**, with optional teaching support and safe retry after an uncertain result.`,
  },
  {
    version: "0.3.0",
    date: "2026-11-07",
    body: `**Choose your practice focus.**

- Start fresh with **Mixed practice** or **Present verb forms**. Saved runs resume directly, and cancelling a new selection keeps your current practice.
- Practise present verbs in six parts: choose the form, recall it in writing, and repair one wrong word.
- Verb summaries keep these three operations separate. Select the ones to revisit in a new, source-linked review.`,
  },
  {
    version: "0.2.0",
    date: "2026-11-07",
    body: `**Application version and changelog.**

- Astrid now shows its application version together with the exact source commit and commit time.
- Open **Changelog** from the navigation to read release notes. Astrid marks the current version as read on this device.`,
  },
  {
    version: "0.1.0",
    date: "2026-11-07",
    body: `**Initial private Danish learning engine.**

- Eight reading and writing exercise formats cover focused grammar choices, ordering, repair, meaning recognition, and written headword recall.
- Guided practice records support, first and latest outcomes, and possible earlier exposure without claiming mastery.
- Completed summaries can start source-linked reviews for learner-selected grammar and vocabulary targets.`,
  },
];
