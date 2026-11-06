import {
  Accordion, Alert, Badge, Box, Button, Divider, Group, Loader, NumberInput,
  Paper, Select, SimpleGrid, Stack, Text, Title,
} from "@mantine/core";
import { IconAlertCircle, IconBulb, IconRefresh } from "@tabler/icons-react";
import { useEffect, useState } from "react";
import {
  ApiError, bootstrapPlayground, createExercise, submitAttempt, UncertainResultError,
} from "./api/client";
import type { Attempt, AttemptAnswer, AttemptRequest, Catalog, DevelopmentSession, Exercise } from "./api/models";
import ExercisePrompt from "./components/ExercisePrompt";
import FeedbackPanel from "./components/FeedbackPanel";
import InspectorPanel from "./components/InspectorPanel";

const INITIAL_SEED = 20261107;
const INITIAL_INDEX = 0;
let bootstrapPromise: Promise<[DevelopmentSession, Catalog, Exercise]> | null = null;

function bootstrap() {
  bootstrapPromise ??= bootstrapPlayground(INITIAL_SEED, INITIAL_INDEX)
    .then(({ session, catalog, exercise }) => [session, catalog, exercise] as [DevelopmentSession, Catalog, Exercise])
    .catch((error: unknown) => {
      bootstrapPromise = null;
      throw error;
    });
  return bootstrapPromise;
}

function answerFor(exercise: Exercise, text: string, order: string[]): AttemptAnswer {
  return exercise.formatId === "E003" ? { kind: "order", blockIds: order } : { kind: "text", text };
}

function messageFor(error: unknown) {
  if (error instanceof ApiError && error.status === 409) return "The engine revision changed. Generate a new exercise.";
  if (error instanceof ApiError) return error.message;
  return "The learning engine could not be reached. Check that the server is running.";
}

function activeSelection(catalog: Catalog | null, exercise: Exercise | null, requestedFormatId: string) {
  const id = exercise?.formatId ?? requestedFormatId;
  return { id, format: catalog?.formats.find((candidate) => candidate.id === id) };
}

function ErrorRecoveryActions({ reloadRequired, hasExercise }: { reloadRequired: boolean; hasExercise: boolean }) {
  if (reloadRequired) {
    return <Button color="red" variant="light" onClick={() => window.location.reload()}>Reload engine</Button>;
  }
  if (!hasExercise) {
    return <Button color="red" variant="light" onClick={() => window.location.reload()}>Try connection again</Button>;
  }
  return null;
}

export default function Playground() {
  const [session, setSession] = useState<DevelopmentSession | null>(null);
  const [catalog, setCatalog] = useState<Catalog | null>(null);
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
  const [reloadRequired, setReloadRequired] = useState(false);

  const generate = async (nextFormat: string, nextSeed: number, nextIndex: number, knownCatalog = catalog) => {
    if (!knownCatalog) return;
    const format = knownCatalog.formats.find((candidate) => candidate.id === nextFormat);
    if (!format) return;
    setBusy(true);
    setError("");
    setReloadRequired(false);
    try {
      const created = await createExercise({
        formatId: format.id,
        targetSkillId: format.targetSkillId,
        seed: nextSeed,
        index: nextIndex,
        contentRevision: knownCatalog.contentRevision,
        generatorRevision: knownCatalog.generatorRevision,
      });
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
      setError(messageFor(caught));
      setReloadRequired(caught instanceof ApiError && caught.status === 409);
    } finally {
      setBusy(false);
    }
  };

  useEffect(() => {
    let active = true;
    void (async () => {
      try {
        const [started, loaded, created] = await bootstrap();
        if (!active) return;
        const first = loaded.formats[0]?.id ?? "";
        setSession(started);
        setCatalog(loaded);
        setFormatId(first);
        setExercise(created);
        setBusy(false);
      } catch (caught) {
        if (active) { setError(messageFor(caught)); setBusy(false); }
      }
    })();
    return () => { active = false; };
  }, []);

  const deliverAttempt = async (request: AttemptRequest) => {
    setPending(request);
    setBusy(true);
    setError("");
    setReloadRequired(false);
    setUncertain(false);
    try {
      setAttempt(await submitAttempt(request));
      setPending(null);
    } catch (caught) {
      if (caught instanceof UncertainResultError) setUncertain(true);
      else {
        setPending(null);
        setError(messageFor(caught));
        setReloadRequired(caught instanceof ApiError && caught.status === 409);
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
  const skill = catalog?.skills.find((candidate) => candidate.id === format?.targetSkillId);
  const answerReady = exercise?.formatId === "E003" ? order.length === exercise.prompt.blocks.length : text.trim().length > 0;

  return (
    <Stack gap="lg" maw={920}>
      <Box>
        <Group justify="space-between" align="start">
          <Box>
            <Title order={2}>Danish engine playground</Title>
            <Text c="dimmed">Practise one grammar target at a time with generated everyday sentences.</Text>
          </Box>
          <Badge variant="light" color="gray">{catalog?.sentenceCount ?? 0} sentence combinations</Badge>
        </Group>
      </Box>

      {error && (
        <Alert color="red" icon={<IconAlertCircle size={20} />} title="The exercise could not continue">
          <Stack gap="sm">
            <Text>{error}</Text>
            <ErrorRecoveryActions reloadRequired={reloadRequired} hasExercise={Boolean(exercise)} />
          </Stack>
        </Alert>
      )}
      {uncertain && pending && (
        <Alert color="orange" icon={<IconRefresh size={20} />} title="The result is not confirmed">
          <Stack gap="sm">
            <Text>The server may have saved your answer. Keep this page open and retry the exact same attempt.</Text>
            <Button color="orange" variant="light" onClick={() => void deliverAttempt(pending)}>
              Check the same attempt
            </Button>
          </Stack>
        </Alert>
      )}

      <Paper withBorder shadow="xs" p={{ base: "md", sm: "xl" }}>
        <Stack gap="lg">
          <Group justify="space-between" align="start">
            <Box>
              <Text fw={650}>{format?.title ?? "Exercise"}</Text>
              {skill && <Text size="sm" c="dimmed">Target: {skill.title}</Text>}
            </Box>
            <Badge color="astrid" variant="light">{exercise?.targetSkillId}</Badge>
          </Group>
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
                      <SimpleGrid cols={{ base: 1, sm: exercise.introduction.length > 1 ? 2 : 1 }}>
                        {exercise.introduction.map((section) => (
                          <Box key={section.title}>
                            <Title order={3} size="h4">{section.title}</Title>
                            <Text size="sm" mt={4}>{section.body}</Text>
                          </Box>
                        ))}
                      </SimpleGrid>
                    </Accordion.Panel>
                  </Accordion.Item>
                  </Accordion>
                </Box>
              )}
              <Box>
                <Title order={3} size="h4">{exercise.instruction}</Title>
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
              {attempt && <FeedbackPanel attempt={attempt} />}
              <Group justify="space-between">
                <Button
                  variant="subtle"
                  color="gray"
                  disabled={busy || uncertain || hintVisible || Boolean(attempt)}
                  onClick={() => { setHintVisible(true); setHintUsed(true); }}
                >
                  Show hint
                </Button>
                <Group>
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
            <SimpleGrid cols={{ base: 1, sm: 3 }}>
              <Select
                label="Exercise type"
                data={catalog?.formats.map((item) => ({ value: item.id, label: item.title })) ?? []}
                value={activeFormatId}
                disabled={busy || uncertain}
                onChange={(value) => { if (value) void generate(value, seed, index); }}
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
