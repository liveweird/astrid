import { Badge, Box, Group, Text, Title } from "@mantine/core";

export default function PlaygroundIntroduction({ sentenceCount }: { sentenceCount: number }) {
  return (
    <Box>
      <Group justify="space-between" align="start">
        <Box>
          <Title order={2}>Danish engine playground</Title>
          <Text c="dimmed">Practise one language target at a time with generated exercises.</Text>
        </Box>
        <Badge variant="light" color="gray">{sentenceCount} sentence combinations</Badge>
      </Group>
    </Box>
  );
}
