import { Box, Button, Checkbox, Divider, Group, Paper, SimpleGrid, Stack, Text, Title } from "@mantine/core";
import type { RefObject } from "react";
import type { PracticeSession, PracticeSummaryRow } from "../api/models";

const OUTCOME_LABELS = [
  ["targetMet", "target met"],
  ["targetNotMet", "target not met"],
  ["unevaluable", "not evaluated"],
  ["languageUnknown", "language unknown"],
  ["languageKnownIssue", "known language issue"],
  ["responseConstraintNotMet", "response constraint not met"],
] as const;

function OutcomeSummary({ row, kind }: { row: PracticeSummaryRow; kind: "firstOutcomes" | "latestOutcomes" }) {
  const values = row[kind];
  const details = OUTCOME_LABELS
    .filter(([key]) => values[key] > 0)
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
  if (row.targetSkillId) {
    const scope = row.targetScopeId ? `${row.targetScopeId}:` : "";
    return row.assessmentMode
      ? `grammar:${row.targetSkillId}:${scope}${row.assessmentMode}`
      : `grammar:${row.targetSkillId}`;
  }
  const target = row.lexicalTarget;
  if (!target) throw new Error("Practice summary target is missing");
  return `lexical:${target.entryId}:${target.senseId}:${target.facet}:${target.direction}`;
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
};

function targetKind(row: PracticeSummaryRow) {
  const scoped = SCOPED_TARGET_KINDS[`${row.targetScopeId ?? ""}:${row.assessmentMode ?? ""}`];
  if (scoped) return scoped;
  if (row.targetSkillId === "G004" && row.assessmentMode === "selection") return "Form selection";
  if (row.targetSkillId === "G004" && row.assessmentMode === "recall") return "Written form recall";
  if (row.targetSkillId === "G004" && row.assessmentMode === "repair") return "Form repair";
  if (row.targetSkillId === "G006" && row.assessmentMode === "selection") return "Article selection";
  if (row.targetSkillId === "G006" && row.assessmentMode === "recall") return "Written article recall";
  if (row.targetSkillId) return "Grammar";
  if (row.lexicalTarget?.facet === "meaning") {
    return row.lexicalTarget.direction === "da-en" ? "Meaning recognition" : "Meaning selection";
  }
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
            key={`${row.targetSkillId ?? "lexical"}-${row.targetScopeId ?? ""}-${row.assessmentMode ?? ""}-${row.assessmentRole ?? ""}-${row.lexicalTarget?.entryId ?? ""}-${row.lexicalTarget?.senseId ?? ""}-${row.lexicalTarget?.facet ?? ""}-${row.lexicalTarget?.direction ?? ""}`}
            withBorder
            p="lg"
          >
            <Stack gap="sm">
              <Box>
                <Title order={3} size="h4">{row.label}</Title>
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
                  ? "after teaching or earlier exposure in this run"
                  : "with possible earlier exposure"}
              </Text>
              {!moduleOne && <Checkbox
                  label={`Review ${row.label}`}
                  checked={selectedTargets.has(practiceTargetKey(row))}
                  disabled={busy}
                  onChange={(event) => onToggleTarget(practiceTargetKey(row), event.currentTarget.checked)}
                />}
            </Stack>
          </Paper>
        ))}
      </SimpleGrid>
      {moduleOne && (
        <Stack gap="xs">
          <Text size="sm" c="dimmed">These activities followed this session’s teaching. Experience from other practice runs is unknown.</Text>
          <Group align="flex-start">
            <Button variant="default" disabled={busy} onClick={onRepeat}>Repeat this session</Button>
            {nextProfile && <Button disabled={busy} onClick={nextProfile.onChoose}>Next: {nextProfile.title}</Button>}
          </Group>
        </Stack>
      )}
      {!moduleOne && <Box>
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
