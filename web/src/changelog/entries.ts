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
    version: "0.14.0",
    date: "2026-11-11",
    body: `**Talk about names, homes and languages.**

- **Names, homes and languages** adds six activities for choosing and typing hedder, bor and taler in supplied personal statements.
- Read the required introduction to connect each Danish dictionary form with its present form, meaning and worked example.
- Practise one choice and one written response for each verb while names, city phrases and dansk remain supplied.
- See separate selection and written-recall results, reload saved progress, repeat the session, or enter it directly.
- Continue from **Words for things you read**. The material remains a draft awaiting Danish teaching review. Earlier exercises keep their original behavior.`,
  },
  {
    version: "0.13.0",
    date: "2026-11-11",
    body: `**Learn Danish words for things you read.**

- **Words for things you read** adds four meaning activities for a printed magazine, a book, a newspaper and a written letter.
- Read the required introduction to learn et magasin and review en bog, en avis and et brev as complete Danish article-and-noun phrases.
- Choose among the same four related phrases. The supplied article is part of each choice; these activities assess the requested meaning, not article recall.
- See a separate result for each meaning, retry a known wrong choice, reload the saved summary, or repeat the session.
- Continue from **Saying what someone has** or enter the session directly. The material remains a draft awaiting Danish teaching review.`,
  },
  {
    version: "0.12.0",
    date: "2026-11-11",
    body: `**Say what someone has with har.**

- **Saying what someone has** adds four activities for choosing and typing the Danish present form har in supplied statements.
- Read the required introduction to connect at have with har and see that the present form stays the same with han, vi, du and de.
- Distinguish the introduced present form from the infinitive, then see separate selection and written-recall results with saved retries and recovery.
- Continue from **Building subject-first statements**, enter the session directly, or repeat it from its summary.
- The material remains a draft awaiting Danish teaching review. Earlier exercises and saved practice keep their original behavior.`,
  },
  {
    version: "0.11.0",
    date: "2026-11-10",
    body: `**Use present verbs and build Danish statements.**

- **Using present verb forms** adds six activities for choosing and typing the present forms of arbejde, læse and sove in supplied situations.
- **Building subject-first statements** adds six ordering activities with supplied Danish sentence parts, then individual word tiles.
- Read each required introduction before practice. The statements explain the subject as the first constituent and the finite verb as the second constituent in this bounded pattern.
- Continue from **Referring to one thing** through both sessions, enter either session directly, or repeat it from its summary.
- The material remains a draft awaiting Danish teaching review. Earlier exercises and saved practice keep their original behavior.`,
  },
  {
    version: "0.10.0",
    date: "2026-11-10",
    body: `**Refer to one thing with den and det.**

- **Referring to one thing** adds a four-part session to Module 1: First steps. Learn to refer back to a particular book or letter using den or det.
- Each situation supplies the Danish noun with its article, meaning and gender. Read the introduction, then choose and type the introduced reference words.
- See separate selection and written-recall results, with support, editable retries and saved-session recovery.
- Continue from **Using the verb to be**, enter the session directly, or repeat it from its summary.
- The material remains a draft awaiting Danish teaching review. Earlier exercises and saved practice keep their original behavior.`,
  },
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
