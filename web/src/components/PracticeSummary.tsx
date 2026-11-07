import { Box, Divider, Paper, SimpleGrid, Stack, Text, Title } from "@mantine/core";
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

export default function PracticeSummary({ session, headingRef }: {
  session: PracticeSession;
  headingRef: RefObject<HTMLHeadingElement | null>;
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
                <Text size="sm" c="dimmed">{row.advancedItems}/{row.plannedItems} continued · {row.attemptCount} answers · {row.retryCount} retries</Text>
              </Box>
              <Divider />
              <SimpleGrid cols={{ base: 1, xs: 2 }}>
                <OutcomeSummary row={row} kind="firstOutcomes" />
                <OutcomeSummary row={row} kind="latestOutcomes" />
              </SimpleGrid>
              <Text size="sm" c="dimmed">{row.firstWithSupport} first with support · {row.firstWithPriorExposure} with possible earlier exposure</Text>
            </Stack>
          </Paper>
        ))}
      </SimpleGrid>
    </Stack>
  );
}
