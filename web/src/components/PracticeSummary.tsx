import { Box, Button, Checkbox, Divider, Paper, SimpleGrid, Stack, Text, Title } from "@mantine/core";
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
  if (row.targetSkillId) return `grammar:${row.targetSkillId}`;
  const target = row.lexicalTarget;
  if (!target) throw new Error("Practice summary target is missing");
  return `lexical:${target.entryId}:${target.senseId}:${target.facet}:${target.direction}`;
}

function targetKind(row: PracticeSummaryRow) {
  if (row.targetSkillId) return "Grammar";
  return row.lexicalTarget?.direction === "da-en" ? "Meaning recognition" : "Written headword recall";
}

export default function PracticeSummary({ session, headingRef, selectedTargets, busy, onToggleTarget, onStartReview }: {
  session: PracticeSession;
  headingRef: RefObject<HTMLHeadingElement | null>;
  selectedTargets: Set<string>;
  busy: boolean;
  onToggleTarget: (targetKey: string, selected: boolean) => void;
  onStartReview: () => void;
}) {
  return (
    <Stack gap="lg">
      <Box>
        <Title ref={headingRef} tabIndex={-1} order={2}>Practice complete</Title>
        <Text c="dimmed">This records what you attempted, not a mastery judgment.</Text>
      </Box>
      <Paper withBorder p="lg">
        <SimpleGrid cols={{ base: 2, sm: 4 }}>
          <Box><Text fw={700} size="xl">{session.summary.completedItems}</Text><Text size="sm" c="dimmed">continued</Text></Box>
          <Box><Text fw={700} size="xl">{session.summary.attemptedItems}</Text><Text size="sm" c="dimmed">attempted</Text></Box>
          <Box><Text fw={700} size="xl">{session.summary.attemptCount}</Text><Text size="sm" c="dimmed">answers</Text></Box>
          <Box><Text fw={700} size="xl">{session.summary.retryCount}</Text><Text size="sm" c="dimmed">retries</Text></Box>
        </SimpleGrid>
      </Paper>
      <SimpleGrid cols={{ base: 1, md: 2 }}>
        {session.summary.rows.map((row) => (
          <Paper
            component="article"
            aria-label={row.label}
            key={`${row.targetSkillId ?? "lexical"}-${row.lexicalTarget?.entryId ?? ""}-${row.lexicalTarget?.senseId ?? ""}-${row.lexicalTarget?.facet ?? ""}-${row.lexicalTarget?.direction ?? ""}`}
            withBorder
            p="lg"
          >
            <Stack gap="sm">
              <Box>
                <Title order={3} size="h4">{row.label}</Title>
                <Text size="sm" fw={650}>{targetKind(row)}</Text>
                <Text size="sm" c="dimmed">{row.advancedItems}/{row.plannedItems} continued · {row.attemptCount} answers · {row.retryCount} retries</Text>
              </Box>
              <Divider />
              <SimpleGrid cols={{ base: 1, xs: 2 }}>
                <OutcomeSummary row={row} kind="firstOutcomes" />
                <OutcomeSummary row={row} kind="latestOutcomes" />
              </SimpleGrid>
              <Text size="sm" c="dimmed">{row.firstWithSupport} first with support · {row.firstWithPriorExposure} with possible earlier exposure</Text>
              <Checkbox
                label={`Review ${row.label}`}
                checked={selectedTargets.has(practiceTargetKey(row))}
                disabled={busy}
                onChange={(event) => onToggleTarget(practiceTargetKey(row), event.currentTarget.checked)}
              />
            </Stack>
          </Paper>
        ))}
      </SimpleGrid>
      <Box>
        <Button disabled={selectedTargets.size === 0 || busy} loading={busy} onClick={onStartReview}>
          Start selected review
        </Button>
        <Text size="sm" c="dimmed" mt="xs">
          Choose one or more targets. Vocabulary targets currently add one meaningful item each; grammar can vary across sentences.
        </Text>
      </Box>
    </Stack>
  );
}
