import { Alert, Box, Button, Group, Paper, Radio, Stack, Text, Title } from "@mantine/core";
import { IconAlertCircle } from "@tabler/icons-react";
import type { PracticeSession } from "../../api/models";
import classes from "../../theme.module.css";
import { PracticeError } from "./PracticeMessages";
import { PRESENTATION_PROFILES, profileKey } from "./practiceProfiles";

export default function PracticeChooser({ selected, busy, canCancel, message, onSelect, onStart, onCancel }: {
  selected: string | null;
  busy: boolean;
  canCancel: boolean;
  message: string;
  onSelect: (key: string) => void;
  onStart: () => void;
  onCancel: () => void;
}) {
  return (
    <Stack maw={760} gap="lg" className={`${classes.playground} ${classes.practice}`}>
      <Box>
        <Title order={1} size="h2">Choose your practice</Title>
        <Text c="dimmed" mt="xs">Choose one focus for this run. You can start another without removing saved practice.</Text>
      </Box>
      {message && <Alert icon={<IconAlertCircle size={18} />} color="red" role="alert">{message}</Alert>}
      <Radio.Group value={selected} onChange={onSelect} aria-label="Practice focus">
        <Stack gap="sm">
          {PRESENTATION_PROFILES.map((profile) => (
            <Paper key={profileKey(profile)} withBorder p="md">
              <Radio
                value={profileKey(profile)}
                label={profile.title}
                description={profile.description}
                disabled={busy}
              />
            </Paper>
          ))}
        </Stack>
      </Radio.Group>
      <Group>
        <Button disabled={!selected || busy} loading={busy} onClick={onStart}>Start practice</Button>
        {canCancel && <Button variant="default" disabled={busy} onClick={onCancel}>Cancel</Button>}
      </Group>
    </Stack>
  );
}

export function PracticeHeading({ session, busy, onStartNew }: {
  session: PracticeSession;
  busy: boolean;
  onStartNew: () => void;
}) {
  return (
    <Group justify="space-between" align="start">
      <Box>
        <Title order={1} size="h2">{session.title}</Title>
        <Text c="dimmed">{session.followUp ? "A focused review" : "A short practice"} in {session.itemCount} {session.itemCount === 1 ? "part" : "parts"}. Continue after any saved answer.</Text>
        {session.followUp && <Button component="a" variant="subtle" px={0} href={`/practice?session=${encodeURIComponent(session.followUp.sourcePracticeSessionId)}`}>Source practice</Button>}
      </Box>
      <Button variant="subtle" color="gray" disabled={busy} onClick={onStartNew}>Start new practice</Button>
    </Group>
  );
}

export function EmptyPractice({ error, unavailable, pending, busy, focusRef, onStartNew, onRetry }: {
  error: string;
  unavailable: boolean;
  pending: boolean;
  busy: boolean;
  focusRef: React.RefObject<HTMLDivElement | null>;
  onStartNew: () => void;
  onRetry: () => void;
}) {
  return (
    <Stack maw={760} className={`${classes.playground} ${classes.practice}`}>
      <PracticeError message={error} uncertain={false} recoveryUnavailable={unavailable}
        pending={pending} retryDisabled={busy} focusRef={focusRef} onStartNew={onStartNew} onRetry={onRetry} />
      {!unavailable && <Button disabled={busy} onClick={onStartNew}>Start new practice</Button>}
    </Stack>
  );
}
