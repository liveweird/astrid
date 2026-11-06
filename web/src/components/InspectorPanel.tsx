import { Alert, Button, Code, PasswordInput, SimpleGrid, Stack, Text } from "@mantine/core";
import { useRef, useState } from "react";
import { inspectExercise } from "../api/client";
import type { InspectorDetails } from "../api/models";

export default function InspectorPanel({ exerciseId }: { exerciseId: string }) {
  const [key, setKey] = useState("");
  const [result, setResult] = useState<{ exerciseId: string; details: InspectorDetails } | null>(null);
  const [failure, setFailure] = useState<{ exerciseId: string; message: string } | null>(null);
  const [loading, setLoading] = useState(false);
  const requestVersion = useRef(0);
  const details = result?.exerciseId === exerciseId ? result.details : null;
  const error = failure?.exerciseId === exerciseId ? failure.message : "";

  const inspect = async () => {
    const version = ++requestVersion.current;
    const requestedExerciseId = exerciseId;
    setLoading(true);
    setFailure(null);
    try {
      const inspected = await inspectExercise(requestedExerciseId, key);
      if (requestVersion.current === version) setResult({ exerciseId: requestedExerciseId, details: inspected });
    } catch {
      if (requestVersion.current === version) {
        setResult(null);
        setFailure({
          exerciseId: requestedExerciseId,
          message: "Inspector access was refused. Check the in-memory key and server configuration.",
        });
      }
    } finally {
      if (requestVersion.current === version) setLoading(false);
    }
  };

  return (
    <Stack gap="sm">
      <Text size="sm">Private generation evidence. The key stays in this tab’s memory.</Text>
      <PasswordInput label="Inspector key" value={key} onChange={(event) => setKey(event.currentTarget.value)} />
      <Button variant="default" loading={loading} disabled={key.length < 32} onClick={() => void inspect()}>
        Inspect this exercise
      </Button>
      {error && <Alert color="red">{error}</Alert>}
      {details && (
        <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="xs">
          <Text size="sm">Canonical sentence<br /><Code lang="da">{details.canonicalSentence}</Code></Text>
          <Text size="sm">Identity<br /><Code>{details.canonicalSentenceId}</Code></Text>
          <Text size="sm">Construction<br /><Code>{details.constructionId}</Code></Text>
          <Text size="sm">Roles<br /><Code>{details.clauseRoles.join(", ")}</Code></Text>
          <Text size="sm">Lexical IDs<br /><Code>{details.lexicalIds.join(", ")}</Code></Text>
          <Text size="sm">Reproduction<br /><Code>seed {details.seed}, index {details.index}</Code></Text>
        </SimpleGrid>
      )}
    </Stack>
  );
}
