import { Alert, Anchor, Button, Divider, Group, Paper, Stack, Text, Title } from "@mantine/core";
import { useEffect, useRef, useState } from "react";
import { Link as RouterLink, useNavigate } from "react-router-dom";
import { ApiError, loadVocabularyContext } from "../../api/client";
import type { PracticeCreationRequest, VocabularyContext } from "../../api/models";
import classes from "../../theme.module.css";
import { outboxEntry, practiceError } from "../practice/practiceOperations";
import { PracticePersistenceError, savedPracticeId, saveRunWithCreation } from "../practice/practiceRecovery";
import { VOCABULARY_PRACTICE_OPTIONS, VOCABULARY_PRACTICE_REVISION } from "../practice/vocabularyPracticeRun";

type Option = (typeof VOCABULARY_PRACTICE_OPTIONS)[number];

function sameCurrentOwner(previous: VocabularyContext, current: VocabularyContext) {
  return current.ownerSessionId === previous.ownerSessionId && current.sessionExpiresAt > Date.now()
    && current.libraryRevision === previous.libraryRevision;
}

function focusNotice(element: HTMLDivElement | null) {
  element?.focus({ preventScroll: true });
  element?.scrollIntoView({ block: "start" });
}

export default function VocabularyPractice({ context, onInvalidate }: {
  context: VocabularyContext | null;
  onInvalidate: () => void;
}) {
  const navigate = useNavigate();
  const [busy, setBusy] = useState<Option["focusId"] | null>(null);
  const [pending, setPending] = useState<{ option: Option; resumeId: string } | null>(null);
  const [error, setError] = useState("");
  const generationRef = useRef(0);
  const errorRef = useRef<HTMLDivElement>(null);
  const choiceRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  useEffect(() => () => { generationRef.current += 1; }, []);
  useEffect(() => { if (error) focusNotice(errorRef.current); }, [error]);
  useEffect(() => { if (pending) focusNotice(choiceRef.current); }, [pending]);
  const practice = context?.practice;
  const ready = practice?.planRevision === VOCABULARY_PRACTICE_REVISION;

  const start = async (option: Option) => {
    if (!context || busy || !ready || practice[option.count] === 0) return;
    const generation = ++generationRef.current;
    setPending(null);
    setBusy(option.focusId);
    setError("");
    try {
      const current = await loadVocabularyContext();
      if (generation !== generationRef.current) return;
      if (!sameCurrentOwner(context, current)) {
        onInvalidate();
        setError("Saved lesson teaching has changed. Load it again for this browser session.");
        return;
      }
      if (current.practice?.planRevision !== VOCABULARY_PRACTICE_REVISION || current.practice[option.count] < 1) {
        onInvalidate();
        setError("These activities are no longer available. Load saved lesson teaching again.");
        return;
      }
      const practiceSessionId = crypto.randomUUID();
      const request: PracticeCreationRequest = {
        practiceSessionId,
        focusId: option.focusId,
        planRevision: VOCABULARY_PRACTICE_REVISION,
        seed: crypto.getRandomValues(new Uint32Array(1))[0]! & 0x7fffffff,
      };
      const recovery = { ownerSessionId: current.ownerSessionId, ...request };
      saveRunWithCreation(recovery, outboxEntry(recovery, "creation", practiceSessionId, request));
      navigate(`/practice?session=${encodeURIComponent(practiceSessionId)}`);
    } catch (caught) {
      if (generation !== generationRef.current) return;
      if (caught instanceof ApiError && caught.status === 401) {
        onInvalidate();
        setError("Saved lesson teaching expired. Start a lesson, then load saved teaching again.");
      } else {
        setError(caught instanceof PracticePersistenceError
          ? practiceError(caught) : "Vocabulary practice could not be prepared. Try again.");
      }
    } finally {
      if (generation === generationRef.current) setBusy(null);
    }
  };

  const choose = (option: Option, trigger: HTMLButtonElement) => {
    setError("");
    const resumeId = savedPracticeId();
    if (resumeId) {
      triggerRef.current = trigger;
      setPending({ option, resumeId });
    } else void start(option);
  };

  return (
    <Paper component="section" aria-labelledby="vocabulary-practice" withBorder p="md">
      <Stack gap="sm">
        <Title id="vocabulary-practice" order={2} size="h3">Practice work and study vocabulary</Title>
        {!ready && !error && <Text size="sm" c="dimmed">Load saved lesson teaching to see which activities are ready.</Text>}
        {ready && (practice.recognitionCount > 0 || practice.recallCount > 0) && (
          <Group align="start">
            {VOCABULARY_PRACTICE_OPTIONS.map((option) => {
              const count = practice[option.count];
              return count > 0 && (
                <Button key={option.focusId} variant="light" disabled={busy !== null}
                  loading={busy === option.focusId} onClick={(event) => choose(option, event.currentTarget)}>
                  {option.label} ({count})
                </Button>
              );
            })}
          </Group>
        )}
        {ready && practice.recognitionCount === 0 && practice.recallCount === 0 && (
          <Text size="sm" c="dimmed">
            No work and study activities are ready yet.{" "}
            <Anchor component={RouterLink} to="/#lesson-vocabulary-p1-s1">Open the first work and study lesson</Anchor>
            {" "}and open its teaching.
          </Text>
        )}
        {pending && (
          <Stack ref={choiceRef} tabIndex={-1} gap="xs" className={classes.vocabularyPracticeNotice}
            component="section" aria-labelledby="vocabulary-saved-choice">
            <Divider />
            <Title id="vocabulary-saved-choice" order={3} size="h4">A practice run is saved</Title>
            <Text size="sm">Resume your saved run or start a separate vocabulary practice.</Text>
            <Group>
              <Button component={RouterLink} to={`/practice?session=${encodeURIComponent(pending.resumeId)}`}>
                Resume saved run
              </Button>
              <Button variant="light" disabled={!ready || practice[pending.option.count] === 0}
                onClick={() => void start(pending.option)}>Start new run</Button>
              <Button variant="default" onClick={() => { setPending(null); triggerRef.current?.focus(); }}>Cancel</Button>
            </Group>
          </Stack>
        )}
        {error && (
          <Alert ref={errorRef} tabIndex={-1} p="xs" className={classes.vocabularyPracticeNotice} color="red" role="alert">
            {error}
          </Alert>
        )}
      </Stack>
    </Paper>
  );
}
