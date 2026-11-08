import {
  Accordion, Alert, Box, Button, Divider, Group, Loader, NumberInput,
  Paper, SimpleGrid, Stack, Text, Title,
} from "@mantine/core";
import { IconAlertCircle, IconBulb, IconRefresh } from "@tabler/icons-react";
import { type QueryClient, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import {
  ApiError, bootstrapPlayground, createExercise, submitAttempt, UncertainResultError,
} from "../../api/client";
import type { Attempt, AttemptAnswer, AttemptRequest, Catalog, DevelopmentSession, Exercise } from "../../api/models";
import ExercisePrompt from "../../components/ExercisePrompt";
import FeedbackPanel from "../../components/FeedbackPanel";
import FormatSelect from "../../components/FormatSelect";
import InspectorPanel from "../../components/InspectorPanel";
import classes from "../../theme.module.css";
import ErrorRecoveryActions, { type RecoveryRequired } from "./ErrorRecoveryActions";
import ExerciseIdentity from "./ExerciseIdentity";
import PlaygroundIntroduction from "./PlaygroundIntroduction";
import { catalogQuery, useCatalogQuery } from "./useCatalogQuery";
import { useExerciseFocus, useRequestedFocus } from "./usePlaygroundFocus";

const INITIAL_SEED = 20261107;
const INITIAL_INDEX = 0;
let bootstrapPromise: Promise<[DevelopmentSession, Catalog, Exercise]> | null = null;

// Deduplicates the in-flight start (Strict Mode effect replays); each later mount starts afresh,
// as a page load did, while the catalog itself comes from the query cache.
function bootstrap(queryClient: QueryClient) {
  bootstrapPromise ??= bootstrapPlayground(INITIAL_SEED, INITIAL_INDEX, () => queryClient.ensureQueryData(catalogQuery))
    .then(({ session, catalog, exercise }) => [session, catalog, exercise] as [DevelopmentSession, Catalog, Exercise])
    .finally(() => {
      bootstrapPromise = null;
    });
  return bootstrapPromise;
}

function answerFor(exercise: Exercise, text: string, order: string[]): AttemptAnswer {
  return exercise.formatId === "E003" ? { kind: "order", blockIds: order } : { kind: "text", text };
}

function messageFor(error: unknown) {
  if (error instanceof ApiError && error.status === 401) return "Your learning session expired. Reconnect to continue.";
  if (error instanceof ApiError && error.status === 409) return "The engine revision changed. Generate a new exercise.";
  if (error instanceof ApiError) return error.message;
  return "The learning engine could not be reached. Check that the server is running.";
}

function recoveryFor(error: unknown): RecoveryRequired {
  if (!(error instanceof ApiError)) return null;
  if (error.status === 401) return "session";
  if (error.status === 409) return "engine";
  return null;
}

function activeSelection(catalog: Catalog | null, exercise: Exercise | null, requestedFormatId: string) {
  const id = exercise?.formatId ?? requestedFormatId;
  return { id, format: catalog?.formats.find((candidate) => candidate.id === id) };
}

export default function Playground() {
  const queryClient = useQueryClient();
  const [session, setSession] = useState<DevelopmentSession | null>(null);
  const { data: catalog = null } = useCatalogQuery(session !== null);
  const [exercise, setExercise] = useState<Exercise | null>(null);
  const [formatId, setFormatId] = useState("");
  const [seed, setSeed] = useState(INITIAL_SEED);
  const [index, setIndex] = useState(INITIAL_INDEX);
  const [activeReproduction, setActiveReproduction] = useState({ seed: INITIAL_SEED, index: INITIAL_INDEX });
  const [text, setText] = useState("");
  const [order, setOrder] = useState<string[]>([]);
  const [hintUsed, setHintUsed] = useState(false);
  const [hintVisible, setHintVisible] = useState(false);
  const [attempt, setAttempt] = useState<Attempt | null>(null);
  const [pending, setPending] = useState<AttemptRequest | null>(null);
  const [uncertain, setUncertain] = useState(false);
  const [busy, setBusy] = useState(true);
  const [error, setError] = useState("");
  const [recoveryRequired, setRecoveryRequired] = useState<RecoveryRequired>(null);
  const [exerciseHeadingRef, requestExerciseFocus] = useExerciseFocus(attempt, exercise);
  const [errorAlertRef, requestErrorFocus] = useRequestedFocus<HTMLDivElement>();
  const [uncertainAlertRef, requestUncertainFocus] = useRequestedFocus<HTMLDivElement>();
  const [feedbackRef, requestFeedbackFocus] = useRequestedFocus<HTMLDivElement>();
  const generate = async (nextFormat: string, nextSeed: number, nextIndex: number, knownCatalog = catalog) => {
    if (!knownCatalog) return;
    const format = knownCatalog.formats.find((candidate) => candidate.id === nextFormat);
    if (!format) return;
    setBusy(true);
    setError("");
    setRecoveryRequired(null);
    try {
      const created = await createExercise({
        formatId: format.id,
        ...(format.targetSkillId ? { targetSkillId: format.targetSkillId } : {}),
        seed: nextSeed,
        index: nextIndex,
        contentRevision: knownCatalog.contentRevision,
        generatorRevision: knownCatalog.generatorRevision,
      });
      requestExerciseFocus();
      setExercise(created);
      setFormatId(created.formatId);
      setSeed(nextSeed);
      setIndex(nextIndex);
      setActiveReproduction({ seed: nextSeed, index: nextIndex });
      setText("");
      setOrder([]);
      setHintUsed(false);
      setHintVisible(false);
      setAttempt(null);
      setPending(null);
      setUncertain(false);
    } catch (caught) {
      requestErrorFocus();
      setError(messageFor(caught));
      setRecoveryRequired(recoveryFor(caught));
    } finally {
      setBusy(false);
    }
  };
  useEffect(() => {
    let active = true;
    void (async () => {
      try {
        const [started, loaded, created] = await bootstrap(queryClient);
        if (!active) return;
        const first = loaded.formats[0]?.id ?? "";
        setSession(started);
        setFormatId(first);
        setExercise(created);
        setBusy(false);
      } catch (caught) {
        if (active) { setError(messageFor(caught)); setBusy(false); }
      }
    })();
    return () => { active = false; };
  }, [queryClient]);
  const deliverAttempt = async (request: AttemptRequest) => {
    setPending(request);
    setBusy(true);
    setError("");
    setRecoveryRequired(null);
    setUncertain(false);
    try {
      const result = await submitAttempt(request);
      requestFeedbackFocus();
      setAttempt(result);
      setPending(null);
    } catch (caught) {
      if (caught instanceof UncertainResultError) {
        requestUncertainFocus();
        setUncertain(true);
      } else {
        setPending(null);
        requestErrorFocus();
        setError(messageFor(caught));
        setRecoveryRequired(recoveryFor(caught));
      }
    } finally {
      setBusy(false);
    }
  };
  const checkAnswer = () => {
    if (!exercise) return;
    void deliverAttempt({
      attemptId: crypto.randomUUID(),
      exerciseId: exercise.id,
      answer: answerFor(exercise, text, order),
      hintUsed,
    });
  };
  const retryKnownAttempt = () => {
    requestExerciseFocus();
    setAttempt(null);
    setText("");
    setOrder([]);
    // Support evidence is sticky for this exercise: the learner has already seen it.
    setHintVisible(false);
    setError("");
  };
  if (busy && !exercise) {
    return <Group justify="center" mih={300}><Loader aria-label="Starting learning engine" /></Group>;
  }
  const { id: activeFormatId, format } = activeSelection(catalog, exercise, formatId);
  const answerReady = exercise?.formatId === "E003" ? order.length === exercise.prompt.blocks.length : text.trim().length > 0;

  return (
    <Stack gap="lg" maw={920} className={classes.playground}>
      <PlaygroundIntroduction sentenceCount={catalog?.sentenceCount ?? 0} />
      {error && (
        <Alert ref={errorAlertRef} tabIndex={-1} color="red" icon={<IconAlertCircle size={20} />} title="The exercise could not continue">
          <Stack gap="sm">
            <Text>{error}</Text>
            <ErrorRecoveryActions recoveryRequired={recoveryRequired} hasExercise={Boolean(exercise)} />
          </Stack>
        </Alert>
      )}
      {uncertain && pending && (
        <Alert ref={uncertainAlertRef} tabIndex={-1} color="orange" icon={<IconRefresh size={20} />} title="The result is not confirmed">
          <Stack gap="sm">
            <Text>The server may have saved your answer. Keep this page open and retry the exact same attempt.</Text>
            <Button color="orange" variant="light" onClick={() => void deliverAttempt(pending)}>
              Check the same attempt
            </Button>
          </Stack>
        </Alert>
      )}
      <Paper withBorder shadow="xs" className={classes.exerciseCard}>
        <Stack gap="lg">
          <ExerciseIdentity catalog={catalog} exercise={exercise} format={format} />
          <Divider />
          {exercise && (
            <>
              {exercise.introduction.length > 0 && (
                <Box>
                  <Text size="sm" c="dimmed" mb="xs">
                    Open the lesson to learn these words. Using it records supported practice.
                  </Text>
                  <Accordion
                    key={exercise.id}
                    variant="contained"
                    onChange={(value) => { if (value === "lesson") setHintUsed(true); }}
                  >
                  <Accordion.Item value="lesson">
                    <Accordion.Control>Learn this first</Accordion.Control>
                    <Accordion.Panel>
                      <SimpleGrid minColWidth={exercise.introduction.length > 1 ? "min(100%, 18rem)" : undefined} autoFlow="auto-fit">
                        {exercise.introduction.map((section) => (
                          <Box key={section.title}>
                            <Title order={3} size="h4">{section.title}</Title>
                            <Text lang={section.bodyLanguage ?? undefined} size="sm" mt={4}>{section.body}</Text>
                          </Box>
                        ))}
                      </SimpleGrid>
                    </Accordion.Panel>
                  </Accordion.Item>
                  </Accordion>
                </Box>
              )}
              <Box>
                <Title ref={exerciseHeadingRef} tabIndex={-1} order={3} size="h4">{exercise.instruction}</Title>
                <Text c="dimmed" mt={4}>{exercise.context}</Text>
              </Box>
              <ExercisePrompt
                exercise={exercise}
                text={text}
                order={order}
                disabled={busy || uncertain || Boolean(attempt)}
                onText={setText}
                onOrder={setOrder}
              />
              {hintVisible && <Alert color="gray" icon={<IconBulb size={18} />}>{exercise.hint}</Alert>}
              {attempt && <FeedbackPanel attempt={attempt} focusRef={feedbackRef} />}
              <Group justify="space-between" className={classes.exerciseActions}>
                <Button
                  variant="subtle"
                  color="gray"
                  disabled={busy || uncertain || hintVisible || Boolean(attempt)}
                  onClick={() => { setHintVisible(true); setHintUsed(true); }}
                >
                  Show hint
                </Button>
                <Group className={classes.exercisePrimaryActions}>
                  {attempt && (
                    <Button variant="default" disabled={busy || uncertain} onClick={retryKnownAttempt}>Try again</Button>
                  )}
                  {attempt ? (
                    <Button
                      loading={busy}
                      disabled={busy || uncertain}
                      onClick={() => void generate(
                        activeFormatId,
                        activeReproduction.seed,
                        activeReproduction.index + 1,
                      )}
                    >
                      Next exercise
                    </Button>
                  ) : (
                    <Button loading={busy} disabled={!answerReady || uncertain} onClick={checkAnswer}>Check answer</Button>
                  )}
                </Group>
              </Group>
            </>
          )}
        </Stack>
      </Paper>

      <Accordion variant="contained">
        <Accordion.Item value="controls">
          <Accordion.Control>Generation controls</Accordion.Control>
          <Accordion.Panel>
            <SimpleGrid minColWidth="min(100%, 12rem)" autoFlow="auto-fit">
              <FormatSelect
                data={catalog?.formats.map((item) => ({ value: item.id, label: item.title })) ?? []}
                value={activeFormatId}
                disabled={busy || uncertain}
                onChange={(value) => void generate(value, seed, index)}
              />
              <NumberInput disabled={busy || uncertain} label="Next generation seed" min={0} max={2147483647} value={seed} onChange={(value) => setSeed(Number(value))} />
              <NumberInput disabled={busy || uncertain} label="Next generation index" min={0} max={1000000} value={index} onChange={(value) => setIndex(Number(value))} />
            </SimpleGrid>
            <Button mt="md" variant="default" disabled={busy || uncertain} onClick={() => void generate(activeFormatId, seed, index)}>Regenerate</Button>
          </Accordion.Panel>
        </Accordion.Item>
        {session?.inspectorEnabled && exercise && (
          <Accordion.Item value="inspector">
            <Accordion.Control>Private engine inspector</Accordion.Control>
            <Accordion.Panel><InspectorPanel exerciseId={exercise.id} /></Accordion.Panel>
          </Accordion.Item>
        )}
      </Accordion>
    </Stack>
  );
}
