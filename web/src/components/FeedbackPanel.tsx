import { Alert, Badge, Group, Stack, Text } from "@mantine/core";
import { IconAlertTriangle, IconCircleCheck } from "@tabler/icons-react";
import type { Attempt } from "../api/models";

function targetLabel(value: string) {
  if (value === "met") return "Target met";
  if (value === "unevaluable") return "Target not evaluated";
  return "Target to revisit";
}

function languageLabel(value: string) {
  if (value === "acceptable") return "Acceptable Danish";
  if (value === "known-issue") return "Known language issue";
  return "Language status unknown";
}

export default function FeedbackPanel({ attempt }: { attempt: Attempt }) {
  const met = attempt.targetSatisfaction === "met";
  return (
    <Alert
      color={met ? "teal" : "orange"}
      variant="light"
      icon={met ? <IconCircleCheck size={20} /> : <IconAlertTriangle size={20} />}
      title={met ? "Good work" : "Look at the target once more"}
      role="status"
    >
      <Stack gap="xs">
        <Group gap="xs">
          <Badge color={met ? "teal" : "orange"}>{targetLabel(attempt.targetSatisfaction)}</Badge>
          <Badge color={attempt.linguisticStatus === "acceptable" ? "teal" : "gray"}>
            {languageLabel(attempt.linguisticStatus)}
          </Badge>
        </Group>
        <Text>{attempt.feedback}</Text>
        {attempt.completedSentence && <Text lang="da" fw={650}>{attempt.completedSentence}</Text>}
        {attempt.linguisticStatus === "unknown" && (
          <Text size="sm">This answer is outside the pilot’s licensed patterns. It is not a general judgment about Danish.</Text>
        )}
        {attempt.issues.map((issue) => <Text size="sm" key={issue.code}>{issue.message}</Text>)}
        <Text size="xs" c="dimmed">
          Try {attempt.retryNumber}{attempt.hintUsed ? " · Supported practice" : " · No support opened"}
        </Text>
      </Stack>
    </Alert>
  );
}
