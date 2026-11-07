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
