import { Alert, Badge, Box, Button, Group, Paper, Stack, Text, Title } from "@mantine/core";
import { IconAlertCircle, IconBookmark, IconBook2 } from "@tabler/icons-react";
import { useEffect, useRef, useState } from "react";
import { Link as RouterLink, useNavigate } from "react-router-dom";
import { ApiError } from "../../api/client";
import type { PracticeProfile } from "../../api/models";
import classes from "../../theme.module.css";
import { PracticeCatalogError, practiceError } from "../practice/practiceOperations";
import {
  EXISTING_PRACTICE_PROFILES, MODULE_ONE_PROFILES, preparePracticeCreation, profileKey,
} from "../practice/practiceProfiles";
import {
  latestRunRecoveryEntry, PracticePersistenceError, saveRunWithCreation,
} from "../practice/practiceRecovery";

type LessonKind = "Grammar" | "Vocabulary" | "Combined";

const LESSON_KINDS: Record<string, LessonKind> = {
  "module-one-singular": "Grammar",
  "module-one-groups": "Grammar",
  "module-one-er": "Grammar",
  "module-one-things": "Grammar",
  "module-one-present": "Grammar",
  "module-one-order": "Grammar",
  "module-one-possession": "Grammar",
  "module-one-reading-words": "Vocabulary",
  "module-one-personal": "Combined",
  "module-one-ability": "Grammar",
  "module-one-volition": "Combined",
  "module-one-obligation": "Combined",
  "module-one-permission": "Combined",
  "module-one-circumstances": "Grammar",
  "module-one-questions": "Grammar",
};

function savedPracticeId() {
  try {
    const entry = latestRunRecoveryEntry();
    const run = entry?.run;
    if (!run || typeof run.practiceSessionId !== "string" || typeof run.ownerSessionId !== "string" ||
      typeof run.focusId !== "string" || typeof run.planRevision !== "string" ||
      !Number.isSafeInteger(run.seed) || run.seed < 0 || entry.pointer !== run.practiceSessionId) return null;
    return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(run.practiceSessionId)
      ? run.practiceSessionId : null;
  } catch {
    return null;
  }
}

function startError(error: unknown) {
  if (error instanceof ApiError || error instanceof PracticeCatalogError || error instanceof PracticePersistenceError) {
    return practiceError(error);
  }
  return "Practice could not reach the learning server. No new practice was started.";
}

function ProfileRow({ profile, number, kind, busy, onStart }: {
  profile: PracticeProfile;
  number?: number;
  kind: LessonKind;
  busy: boolean;
  onStart: (profile: PracticeProfile) => void;
}) {
  return (
    <Paper component="li" withBorder p="md" className={classes.lessonRow}>
      {number && <Text className={classes.lessonNumber} aria-hidden="true">{number}</Text>}
      <Box className={classes.lessonBody}>
        <Group gap="xs" align="center">
          <Title order={3} size="h3">{profile.title}</Title>
          <Badge color="gray" variant="light">{kind}</Badge>
        </Group>
        <Text c="dimmed" size="sm" mt={4}>{profile.description}</Text>
        <Text c="dimmed" size="xs" mt="xs">{profile.itemCount} parts</Text>
      </Box>
      <Button variant="light" disabled={busy} onClick={() => onStart(profile)}>
        Start {profile.title}
      </Button>
    </Paper>
  );
}

function ReviewRow({ profile, busy, onStart }: {
  profile: (typeof EXISTING_PRACTICE_PROFILES)[number];
  busy: boolean;
  onStart: (profile: PracticeProfile) => void;
}) {
  const kind: LessonKind = profile.focusId === "reading-basics" ? "Combined" : "Grammar";
  return <ProfileRow profile={profile} kind={kind} busy={busy} onStart={onStart} />;
}

export default function Lessons() {
  const navigate = useNavigate();
  const [resumeId] = useState(savedPracticeId);
  const [pending, setPending] = useState<PracticeProfile | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const errorRef = useRef<HTMLDivElement>(null);
  const generationRef = useRef(0);
  useEffect(() => () => { generationRef.current += 1; }, []);
  useEffect(() => { if (error) errorRef.current?.focus(); }, [error]);

  const choose = (profile: PracticeProfile) => {
    setError("");
    if (resumeId) setPending(profile);
    else void start(profile);
  };
  const start = async (profile: PracticeProfile) => {
    const generation = generationRef.current + 1;
    generationRef.current = generation;
    setPending(null);
    setBusy(true);
    setError("");
    try {
      const { recovery, creation } = await preparePracticeCreation(profileKey(profile));
      if (generation !== generationRef.current) return;
      saveRunWithCreation(recovery, creation);
      navigate(`/practice?session=${encodeURIComponent(recovery.practiceSessionId)}`);
    } catch (caught) {
      if (generation === generationRef.current) setError(startError(caught));
    } finally {
      if (generation === generationRef.current) setBusy(false);
    }
  };

  return (
    <Stack gap="xl" maw={960} className={`${classes.playground} ${classes.lessons}`}>
      <Box className={classes.lessonsIntro}>
        <Group gap="xs" mb="xs"><IconBook2 size={22} aria-hidden="true" /><Text fw={650}>Learn Danish</Text></Group>
        <Title order={1}>Module 1: First steps</Title>
        <Text c="dimmed" mt="xs" maw={660}>
          Read a short explanation, then practise with fifteen focused lessons. Work through them in order or choose what you need today.
        </Text>
      </Box>

      {resumeId && (
        <Paper withBorder p="md" className={classes.resumePanel}>
          <Group justify="space-between" align="center">
            <Group gap="sm"><IconBookmark size={20} aria-hidden="true" /><Box><Text fw={650}>A practice run is saved</Text><Text size="sm" c="dimmed">Continue the exact saved run, including any pending work.</Text></Box></Group>
            <Button component={RouterLink} to={`/practice?session=${encodeURIComponent(resumeId)}`}>Resume saved practice</Button>
          </Group>
        </Paper>
      )}

      {pending && (
        <Alert color="orange" title="Choose what to do with saved practice">
          <Text size="sm">Resume the saved run, or start {pending.title} as a separate new run. The saved run will remain available.</Text>
          <Group mt="sm">
            <Button component={RouterLink} to={`/practice?session=${encodeURIComponent(resumeId!)}`}>Resume saved practice</Button>
            <Button variant="light" loading={busy} onClick={() => void start(pending)}>Start new practice</Button>
            <Button variant="default" disabled={busy} onClick={() => setPending(null)}>Cancel</Button>
          </Group>
        </Alert>
      )}

      {error && <Alert ref={errorRef} tabIndex={-1} icon={<IconAlertCircle size={18} />} color="red" role="alert">{error}</Alert>}

      <Box component="section" aria-labelledby="lesson-list-heading">
        <Title id="lesson-list-heading" order={2} size="h2" mb="md">Lessons</Title>
        <Stack component="ol" className={classes.lessonList} gap="sm">
          {MODULE_ONE_PROFILES.map((profile, index) => (
            <ProfileRow key={profileKey(profile)} profile={profile} number={index + 1}
              kind={LESSON_KINDS[profile.focusId] ?? "Combined"} busy={busy} onStart={choose} />
          ))}
        </Stack>
      </Box>

      <Box component="section" aria-labelledby="review" className={classes.reviewSection}>
        <Title id="review" tabIndex={-1} order={2} size="h2">Review</Title>
        <Text c="dimmed" mt="xs" mb="md" maw={700}>
          Practise familiar words, sentence order, verb forms and articles in these focused sets.
        </Text>
        <Stack component="ul" className={classes.lessonList} gap="sm">
          {EXISTING_PRACTICE_PROFILES.map((profile) => (
            <ReviewRow key={profileKey(profile)} profile={profile} busy={busy} onStart={choose} />
          ))}
        </Stack>
      </Box>
    </Stack>
  );
}
