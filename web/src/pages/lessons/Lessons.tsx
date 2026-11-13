import { Alert, Badge, Box, Button, Group, Paper, Stack, Text, Title } from "@mantine/core";
import { IconAlertCircle, IconBookmark, IconBook2 } from "@tabler/icons-react";
import { useEffect, useRef, useState } from "react";
import { Link as RouterLink, useNavigate } from "react-router-dom";
import {
  ApiError, loadPracticeContext, loadPracticeReviewContext, startDevelopmentSession,
} from "../../api/client";
import type { PracticeProfile, PracticeReviewContext } from "../../api/models";
import classes from "../../theme.module.css";
import { outboxEntry, PracticeCatalogError, practiceError } from "../practice/practiceOperations";
import {
  EXISTING_PRACTICE_PROFILES, MODULE_ONE_PROFILES, VOCABULARY_P1_PROFILES, VOCABULARY_P2_PROFILES,
  preparePracticeCreation, profileKey,
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

function validateReviewContext(context: PracticeReviewContext) {
  if (!context.ownerSessionId || !Number.isSafeInteger(context.sessionExpiresAt) || context.sessionExpiresAt <= 0 ||
    context.focusId !== "cumulative-review" ||
    !["cumulative-review-1", "cumulative-review-2"].includes(context.planRevision) || !Number.isSafeInteger(context.itemCount) ||
    context.itemCount < 0 || context.itemCount > 8 || !Number.isSafeInteger(context.sourceLessonCount) ||
    context.sourceLessonCount < 0) {
    throw new PracticeCatalogError("The mixed review preparation is unavailable or unsupported.");
  }
  return context;
}

function freshSeed() {
  return crypto.getRandomValues(new Uint32Array(1))[0]! & 0x7fffffff;
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
  const [pendingReview, setPendingReview] = useState(false);
  const [reviewContext, setReviewContext] = useState<PracticeReviewContext | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const errorRef = useRef<HTMLDivElement>(null);
  const reviewStatusRef = useRef<HTMLDivElement>(null);
  const generationRef = useRef(0);
  useEffect(() => () => { generationRef.current += 1; }, []);
  useEffect(() => { if (reviewContext) reviewStatusRef.current?.focus(); }, [reviewContext]);
  useEffect(() => { if (error) errorRef.current?.focus(); }, [error]);

  const choose = (profile: PracticeProfile) => {
    setError("");
    setPendingReview(false);
    if (resumeId) setPending(profile);
    else void start(profile);
  };
  const prepareReview = async () => {
    const generation = generationRef.current + 1;
    generationRef.current = generation;
    setPending(null);
    setPendingReview(false);
    setBusy(true);
    setError("");
    try {
      await startDevelopmentSession();
      const prepared = validateReviewContext(await loadPracticeReviewContext());
      if (generation === generationRef.current) setReviewContext(prepared);
    } catch (caught) {
      if (generation === generationRef.current) {
        setReviewContext(null);
        setError(startError(caught));
      }
    } finally {
      if (generation === generationRef.current) setBusy(false);
    }
  };
  const chooseReview = () => {
    setError("");
    setPending(null);
    if (resumeId) setPendingReview(true);
    else void startReview();
  };
  const startReview = async () => {
    const prepared = reviewContext;
    if (!prepared || prepared.itemCount === 0) return;
    const generation = generationRef.current + 1;
    generationRef.current = generation;
    setPending(null);
    setPendingReview(false);
    setBusy(true);
    setError("");
    try {
      // Both reads are deliberately fresh. The review may have changed after preparation, and
      // creation must remain bound to the same current browser owner.
      const [ownerContext, currentReview] = await Promise.all([
        loadPracticeContext(), loadPracticeReviewContext(),
      ]);
      if (generation !== generationRef.current) return;
      const current = validateReviewContext(currentReview);
      if (ownerContext.ownerSessionId !== prepared.ownerSessionId || current.ownerSessionId !== prepared.ownerSessionId) {
        setReviewContext(null);
        throw new PracticeCatalogError("This mixed review preparation expired. Prepare it again for this browser session.");
      }
      if (current.itemCount === 0) {
        setReviewContext(current);
        throw new PracticeCatalogError("There is no introduced material to review yet. Start a lesson first.");
      }
      const practiceSessionId = crypto.randomUUID();
      const request = {
        practiceSessionId,
        focusId: "cumulative-review" as const,
        planRevision: "cumulative-review-2" as const,
        seed: freshSeed(),
      };
      const recovery = { ownerSessionId: current.ownerSessionId, ...request };
      const creation = outboxEntry(recovery, "creation", practiceSessionId, request);
      saveRunWithCreation(recovery, creation);
      if (generation !== generationRef.current) return;
      navigate(`/practice?session=${encodeURIComponent(practiceSessionId)}`);
    } catch (caught) {
      if (generation === generationRef.current) setError(startError(caught));
    } finally {
      if (generation === generationRef.current) setBusy(false);
    }
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
        <Group gap="xs" mb="xs"><IconBook2 size={22} aria-hidden="true" /><Text fw={650}>Reading and writing</Text></Group>
        <Title order={1}>Learn Danish</Title>
        <Text c="dimmed" mt="xs" maw={660}>
          Explore focused grammar lessons and a vocabulary unit for everyday situations. Each session begins with teaching, so you can start where you need today.
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

      {pendingReview && (
        <Alert color="orange" title="Choose what to do with saved practice">
          <Text size="sm">Resume the saved run, or start the prepared mixed review as a separate new run. The saved run will remain available.</Text>
          <Group mt="sm">
            <Button component={RouterLink} to={`/practice?session=${encodeURIComponent(resumeId!)}`}>Resume saved practice</Button>
            <Button variant="light" loading={busy} onClick={() => void startReview()}>Start new mixed review</Button>
            <Button variant="default" disabled={busy} onClick={() => setPendingReview(false)}>Cancel</Button>
          </Group>
        </Alert>
      )}

      {error && <Alert ref={errorRef} tabIndex={-1} icon={<IconAlertCircle size={18} />} color="red" role="alert">{error}</Alert>}

      <Box component="section" aria-labelledby="lesson-list-heading">
        <Title id="lesson-list-heading" order={2} size="h2" mb="md">Module 1: First steps</Title>
        <Stack component="ol" className={classes.lessonList} gap="sm">
          {MODULE_ONE_PROFILES.map((profile, index) => (
            <ProfileRow key={profileKey(profile)} profile={profile} number={index + 1}
              kind={LESSON_KINDS[profile.focusId] ?? "Combined"} busy={busy} onStart={choose} />
          ))}
        </Stack>
      </Box>

      <Box component="section" aria-labelledby="vocabulary-p1-heading">
        <Title id="vocabulary-p1-heading" order={2} size="h2">A day of work and study</Title>
        <Text c="dimmed" mt="xs" mb="md" maw={700}>
          Learn words in small batches, then match meanings, read short routines and build supplied statements.
        </Text>
        <Stack component="ol" className={classes.lessonList} gap="sm">
          {VOCABULARY_P1_PROFILES.map((profile, index) => (
            <ProfileRow key={profileKey(profile)} profile={profile} number={index + 1}
              kind="Vocabulary" busy={busy} onStart={choose} />
          ))}
        </Stack>
      </Box>

      <Box component="section" aria-labelledby="vocabulary-p2-heading">
        <Title id="vocabulary-p2-heading" order={2} size="h2">Food, cafés and shopping</Title>
        <Text c="dimmed" mt="xs" mb="md" maw={700}>
          Learn useful food, drink and shopping language through short readings, matching and supplied sentences.
        </Text>
        <Stack component="ol" className={classes.lessonList} gap="sm">
          {VOCABULARY_P2_PROFILES.map((profile, index) => (
            <ProfileRow key={profileKey(profile)} profile={profile} number={index + 1}
              kind="Vocabulary" busy={busy} onStart={choose} />
          ))}
        </Stack>
      </Box>

      <Box component="section" aria-labelledby="review" className={classes.reviewSection}>
        <Title id="review" tabIndex={-1} order={2} size="h2" className={classes.reviewHeading}>Review</Title>
        <Text c="dimmed" mt="xs" mb="md" maw={700}>
          Practise familiar words, sentence order, verb forms and articles in these focused sets.
        </Text>
        <Paper component="article" withBorder p="md" mb="sm">
          <Stack gap="sm">
            <Box>
              <Group gap="xs" align="center">
                <Title order={3} size="h3">Mixed review from your lessons</Title>
                <Badge color="red" variant="light">Combined</Badge>
              </Group>
              <Text c="dimmed" size="sm" mt={4}>
                Prepare a short mix from material introduced in lessons in this browser’s current development session.
              </Text>
            </Box>
            {!reviewContext && <Box><Button loading={busy} onClick={() => void prepareReview()}>
              Prepare mixed review
            </Button></Box>}
            {reviewContext && reviewContext.itemCount === 0 && (
              <Stack gap="xs">
                <Alert ref={reviewStatusRef} tabIndex={-1} color="blue" title="No introduced material yet">
                  Open and save a lesson’s teaching first. Introduced material from this browser session will then become available here.
                </Alert>
                <Box><Button variant="default" disabled={busy} onClick={() => void prepareReview()}>Refresh preparation</Button></Box>
              </Stack>
            )}
            {reviewContext && reviewContext.itemCount > 0 && (
              <Stack ref={reviewStatusRef} tabIndex={-1} role="status" gap="xs">
                <Text fw={650}>{reviewContext.itemCount} {reviewContext.itemCount === 1 ? "part" : "parts"} from {reviewContext.sourceLessonCount} {reviewContext.sourceLessonCount === 1 ? "lesson" : "lessons"}</Text>
                {reviewContext.itemCount < 8 && (
                  <Text size="sm" c="dimmed">This review is shorter because only {reviewContext.itemCount} eligible {reviewContext.itemCount === 1 ? "item is" : "items are"} available.</Text>
                )}
                <Text size="xs" c="dimmed">
                  Prepared for this browser’s development session until <time dateTime={new Date(reviewContext.sessionExpiresAt).toISOString()}>{new Date(reviewContext.sessionExpiresAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}</time>. This is not a mastery score or cross-device progress.
                </Text>
                <Group>
                  <Button loading={busy} onClick={chooseReview}>Start mixed review</Button>
                  <Button variant="default" disabled={busy} onClick={() => void prepareReview()}>Refresh preparation</Button>
                </Group>
              </Stack>
            )}
          </Stack>
        </Paper>
        <Stack component="ul" className={classes.lessonList} gap="sm">
          {EXISTING_PRACTICE_PROFILES.map((profile) => (
            <ReviewRow key={profileKey(profile)} profile={profile} busy={busy} onStart={choose} />
          ))}
        </Stack>
      </Box>
    </Stack>
  );
}
