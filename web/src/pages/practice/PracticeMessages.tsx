import { Alert, Button, Stack, Text } from "@mantine/core";
import { IconAlertCircle, IconBulb, IconRefresh } from "@tabler/icons-react";
import type { PracticeSession } from "../../api/models";

export function PracticeError({ message, uncertain, recoveryUnavailable, pending, retryDisabled, focusRef, onStartNew, onRetry }: {
  message: string;
  uncertain: boolean;
  recoveryUnavailable: boolean;
  pending: boolean;
  retryDisabled: boolean;
  focusRef: React.RefObject<HTMLDivElement | null>;
  onStartNew: () => void;
  onRetry: () => void;
}) {
  if (!message) return null;
  return (
    <Alert ref={focusRef} tabIndex={-1} color={uncertain ? "orange" : "red"} icon={uncertain ? <IconRefresh /> : <IconAlertCircle />} title={uncertain ? "Your answer is not confirmed" : "Practice needs attention"}>
      <Stack gap="sm">
        <Text>{message}</Text>
        {pending && <Button color="orange" variant="light" disabled={retryDisabled} onClick={onRetry}>Retry saved operation</Button>}
        {recoveryUnavailable && <Button variant="light" disabled={retryDisabled} onClick={onStartNew}>Start new practice</Button>}
      </Stack>
    </Alert>
  );
}

export function AttemptContextNote({ item }: { item: PracticeSession["items"][number] }) {
  const context = item.lastAttemptContext;
  if (!item.lastAttempt || !context) return null;
  const sourceExposure = context.sourceExposure ?? [];
  return (
    <Text size="sm" c="dimmed">
      {context.lessonOpened || context.hintOpened ? "Item support was open for this answer. " : ""}
      {context.priorExposure.length > 0
        ? "This target had possible earlier exposure in this run’s prompts, support, or feedback. " : ""}
      {sourceExposure.length > 0
        ? "This target had possible exposure in the source practice. " : ""}
      {context.introductionOpened ? "The session introduction had been opened." : ""}
    </Text>
  );
}

export function PracticeHint({ focusRef, hint }: { focusRef: React.RefObject<HTMLDivElement | null>; hint: string }) {
  return (
    <Alert ref={focusRef} data-practice-focus="hint" tabIndex={-1} color="gray" icon={<IconBulb size={18} />}>
      {hint}
    </Alert>
  );
}
