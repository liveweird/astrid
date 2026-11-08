import { Box, Stack, Text, Timeline, Title } from "@mantine/core";
import { useEffect } from "react";
import { CHANGELOG } from "../changelog/entries";
import MarkdownView from "../components/MarkdownView";
import { markChangelogSeen } from "../hooks/useChangelogSeen";

export default function Changelog() {
  useEffect(markChangelogSeen, []);
  return (
    <Stack gap="lg" maw={800}>
      <Box>
        <Title order={1}>Changelog</Title>
        <Text c="dimmed">What changed in each version of Astrid.</Text>
      </Box>
      <Timeline bulletSize={12} lineWidth={2}>
        {CHANGELOG.map((entry) => (
          <Timeline.Item key={entry.version} title={`v${entry.version}`}>
            <Text size="xs" c="dimmed">{entry.date}</Text>
            <MarkdownView>{entry.body}</MarkdownView>
          </Timeline.Item>
        ))}
      </Timeline>
    </Stack>
  );
}
