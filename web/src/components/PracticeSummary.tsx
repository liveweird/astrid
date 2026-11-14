import { Box, Button, Checkbox, Divider, Group, Paper, SimpleGrid, Stack, Text, Title } from "@mantine/core";
import type { RefObject } from "react";
import type { PracticeSession, PracticeSummaryRow } from "../api/models";
import classes from "../theme.module.css";

const OUTCOME_LABELS = [
  ["targetMet", "target met"],
  ["targetNotMet", "target not met"],
  ["unevaluable", "not evaluated"],
  ["languageUnknown", "language unknown"],
  ["languageKnownIssue", "known language issue"],
  ["responseConstraintNotMet", "response constraint not met"],
] as const;

const MEANING_LABELS = [
  ["meaningMet", "meaning met"],
  ["meaningNotMet", "different meaning"],
  ["meaningUnknown", "meaning unknown"],
] as const;

function OutcomeSummary({ row, kind }: { row: PracticeSummaryRow; kind: "firstOutcomes" | "latestOutcomes" }) {
  const values = row[kind];
  const details = [...OUTCOME_LABELS, ...MEANING_LABELS]
    .filter(([key]) => (values[key] ?? 0) > 0)
    .map(([key, label]) => `${values[key]} ${label}`);
  return (
    <Box>
      <Text size="sm" fw={650}>{kind === "firstOutcomes" ? "First answers" : "Latest answers"}</Text>
      <Text size="sm">{values.fullyMet} fully met</Text>
      <Text size="sm">{details.join(" · ")}</Text>
    </Box>
  );
}

function practiceTargetKey(row: PracticeSummaryRow) {
  if (row.readingTarget) return `reading:${row.readingTarget.facet}:${row.readingRole ?? ""}`;
  if (row.targetSkillId) {
    const scope = row.targetScopeId ? `${row.targetScopeId}:` : "";
    return row.assessmentMode
      ? `grammar:${row.targetSkillId}:${scope}${row.assessmentMode}`
      : `grammar:${row.targetSkillId}`;
  }
  const target = row.lexicalTarget;
  if (!target) throw new Error("Practice summary target is missing");
  const mode = row.assessmentMode ? `:${row.assessmentMode}` : "";
  return `lexical:${target.entryId}:${target.senseId}:${target.facet}:${target.direction}${mode}`;
}

const SCOPED_TARGET_KINDS: Record<string, string> = {
  "human-singular:selection": "One-person subject word selection",
  "human-singular:recall": "One-person subject word recall",
  "human-group:selection": "Group subject word selection",
  "human-group:recall": "Group subject word recall",
  "vaere-present:selection": "Present er selection",
  "vaere-present:recall": "Present er recall",
  "inanimate-singular-subject:selection": "One-thing reference word selection",
  "inanimate-singular-subject:recall": "One-thing reference word recall",
  "modal-volition-meaning:selection": "Meaning selection",
  "modal-obligation-meaning:selection": "Meaning selection",
  "modal-permission-meaning:selection": "Meaning selection",
  "supplied-modal-order:word-order": "Word order",
  "one-final-circumstance:constituent-order": "Constituent order",
  "neutral-positive-yes-no:constituent-order": "Question order",
  "p1-subject-first-supplied:word-order": "Word order",
  "p2-subject-first-supplied:word-order": "Word order",
  "p2-polite-wish:word-order": "Word order",
  "p2-polite-request:word-order": "Word order",
  "p3-kore-present:recall": "Written present-form recall",
  "p3-subject-first-supplied:word-order": "Word order",
  "p3-home-direction:selection": "Homeward meaning selection",
  "p3-home-spatial-contrast:selection": "Whole home spatial contrast",
  "p3-station-spatial-contrast:selection": "Whole station spatial contrast",
};

const GRAMMAR_TARGET_KINDS: Record<string, string> = {
  "G004:selection": "Form selection",
  "G004:recall": "Written form recall",
  "G004:repair": "Form repair",
  "G006:selection": "Article selection",
  "G006:recall": "Written article recall",
};

function targetKind(row: PracticeSummaryRow) {
  if (row.readingTarget) return `Explicit ${row.readingRole ?? "reading"} facts`;
  if (row.assessmentMode === "matching") return "Bilingual matching";
  const scoped = SCOPED_TARGET_KINDS[`${row.targetScopeId ?? ""}:${row.assessmentMode ?? ""}`];
  if (scoped) return scoped;
  if (row.targetSkillId) return GRAMMAR_TARGET_KINDS[`${row.targetSkillId}:${row.assessmentMode ?? ""}`] ?? "Grammar";
  if (row.lexicalTarget?.facet === "meaning") {
    return row.lexicalTarget.direction === "da-en" ? "Meaning recognition" : "Meaning selection";
  }
  if (row.lexicalTarget?.facet === "fixed-chunk") return "Written expression recall";
  if (row.lexicalTarget?.facet === "mass-headword") return "Written mass-word recall";
  return "Written headword recall";
}

export default function PracticeSummary({
  session, headingRef, selectedTargets, busy, onToggleTarget, onStartReview, onRepeat, nextProfile,
}: {
  session: PracticeSession;
  headingRef: RefObject<HTMLHeadingElement | null>;
  selectedTargets: Set<string>;
  busy: boolean;
  onToggleTarget: (targetKey: string, selected: boolean) => void;
  onStartReview: () => void;
  onRepeat?: () => void;
  nextProfile?: { title: string; onChoose: () => void };
}) {
  const moduleOne = session.introductionRequired === true;
  const sessionFocusId: string = session.focusId;
  const sessionPlanRevision: string = session.planRevision;
  const cumulativeReview = sessionFocusId === "cumulative-review"
    && ["cumulative-review-1", "cumulative-review-2", "cumulative-review-3"].includes(sessionPlanRevision);
  return (
    <Stack gap="lg">
      <Box>
        <Title ref={headingRef} tabIndex={-1} order={2}>Practice complete</Title>
        <Text c="dimmed">This records what you attempted, not a mastery judgment.</Text>
      </Box>
      <Paper withBorder p="lg">
        <SimpleGrid minColWidth="min(100%, 9rem)">
          <Box><Text fw={700} size="xl">{session.summary.completedItems}</Text><Text size="sm" c="dimmed">continued</Text></Box>
          <Box><Text fw={700} size="xl">{session.summary.attemptedItems}</Text><Text size="sm" c="dimmed">attempted</Text></Box>
          <Box><Text fw={700} size="xl">{session.summary.attemptCount}</Text><Text size="sm" c="dimmed">answers</Text></Box>
          <Box><Text fw={700} size="xl">{session.summary.retryCount}</Text><Text size="sm" c="dimmed">retries</Text></Box>
        </SimpleGrid>
      </Paper>
      <SimpleGrid minColWidth="min(100%, 22rem)">
        {session.summary.rows.map((row) => (
          <Paper
            component="article"
            aria-label={row.label}
            className={classes.summaryCard}
            key={`${practiceTargetKey(row)}-${row.assessmentRole ?? ""}`}
            withBorder
            p="lg"
          >
            <Stack gap="sm">
              <Box>
                <Title className={classes.summaryTitle} order={3} size="h4">{row.label}</Title>
                <Text size="sm" fw={650}>{targetKind(row)}</Text>
                {row.assessmentRole && (
                  <Text size="sm" c="dimmed">
                    {row.assessmentRole === "primary" ? "Primary assessment" : "Supporting assessment"}
                  </Text>
                )}
                <Text size="sm" c="dimmed">{row.advancedItems}/{row.plannedItems} continued · {row.attemptCount} answers · {row.retryCount} retries</Text>
              </Box>
              <Divider />
              <SimpleGrid minColWidth="min(100%, 12rem)">
                <OutcomeSummary row={row} kind="firstOutcomes" />
                <OutcomeSummary row={row} kind="latestOutcomes" />
              </SimpleGrid>
              <Text size="sm" c="dimmed">
                {row.firstWithSupport} first with support · {row.firstWithPriorExposure} {moduleOne
                  ? cumulativeReview
                    ? "with possible prior exposure in the saved cross-session snapshot"
                    : "after teaching or earlier exposure in this run"
                  : "with possible earlier exposure"}
              </Text>
              {!moduleOne && !cumulativeReview && <Checkbox
                  label={`Review ${row.label}`}
                  checked={selectedTargets.has(practiceTargetKey(row))}
                  disabled={busy}
                  onChange={(event) => onToggleTarget(practiceTargetKey(row), event.currentTarget.checked)}
                />}
            </Stack>
          </Paper>
        ))}
      </SimpleGrid>
      {moduleOne && !cumulativeReview && (
        <Stack gap="xs">
          <Text size="sm" c="dimmed">These activities followed this session’s teaching. Experience from other practice runs is unknown.</Text>
          <Group align="flex-start">
            <Button variant="default" disabled={busy} onClick={onRepeat}>Repeat this session</Button>
            {nextProfile && <Button disabled={busy} onClick={nextProfile.onChoose}>Next: {nextProfile.title}</Button>}
          </Group>
        </Stack>
      )}
      {cumulativeReview && (
        <Stack gap="xs">
          <Text size="sm" c="dimmed">This run keeps the cross-session exposure snapshot used when it was created. Prepare again to use material currently introduced in this browser session.</Text>
          <Box><Button variant="default" disabled={busy} onClick={onRepeat}>Prepare another mixed review</Button></Box>
        </Stack>
      )}
      {!moduleOne && !cumulativeReview && <Box>
        <Button disabled={selectedTargets.size === 0 || busy} loading={busy} onClick={onStartReview}>
          Start selected review
        </Button>
        <Text size="sm" c="dimmed" mt="xs">
          Choose one or more targets. Vocabulary targets currently add one meaningful item each; grammar can vary across sentences.
        </Text>
        {selectedTargets.has("grammar:G006:selection") && (
          <Text size="sm" fw={650} mt="xs">Articles will be supplied in writing</Text>
        )}
      </Box>}
    </Stack>
  );
}
