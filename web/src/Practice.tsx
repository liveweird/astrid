import {
  Accordion, Alert, Badge, Box, Button, Divider, Group, Loader, Paper, Progress,
  SimpleGrid, Stack, Text, Title,
} from "@mantine/core";
import { IconAlertCircle, IconBook2, IconBulb, IconRefresh } from "@tabler/icons-react";
import { useEffect, useRef, useState } from "react";
import {
  advancePracticeSession, ApiError, createPracticeSession, loadPracticeContext,
  loadPracticeSession, startDevelopmentSession, submitSerializedAttempt,
  updatePracticeSupport,
} from "./api/client";
import type {
  Attempt, AttemptAnswer, AttemptRequest, PracticeCreationRequest, PracticeSession,
} from "./api/models";
import ExercisePrompt from "./components/ExercisePrompt";
import FeedbackPanel from "./components/FeedbackPanel";
import PracticeSummary from "./components/PracticeSummary";
import {
  abandonRun, latestRunRecovery, listOutbox, loadRunRecovery, PracticeOutboxConflictError,
  PracticePersistenceError,
  type PracticeOutboxEntry, type PracticeRunRecovery, removeOutboxEntry, saveOutboxEntry,
  saveRunRecovery,
} from "./practiceRecovery";
import classes from "./theme.module.css";

let automaticStart: Promise<{ run: PracticeRunRecovery; session: PracticeSession }> | null = null;

function freshPracticeSeed() {
  return crypto.getRandomValues(new Uint32Array(1))[0]! & 0x7fffffff;
}

function answerFor(exercise: NonNullable<PracticeSession["currentExercise"]>, text: string, order: string[]): AttemptAnswer {
  return exercise.formatId === "E003" ? { kind: "order", blockIds: order } : { kind: "text", text };
}

function answerValues(answer?: AttemptAnswer) {
  if (answer?.kind === "text") return { text: answer.text, order: [] as string[] };
  if (answer?.kind === "order") return { text: "", order: answer.blockIds };
  return { text: "", order: [] as string[] };
}

function currentRecovery() {
  const boundId = new URLSearchParams(window.location.search).get("session");
  return boundId ? loadRunRecovery(boundId) : latestRunRecovery();
}

function bindUrl(practiceSessionId: string) {
  const url = new URL(window.location.href);
  url.pathname = "/practice";
  url.search = `?session=${encodeURIComponent(practiceSessionId)}`;
  window.history.replaceState(null, "", url);
}

function abandonCurrentPractice(run: PracticeRunRecovery | null) {
  const previous = run ?? currentRecovery();
  if (previous) abandonRun(previous.practiceSessionId);
}

function outboxEntry(run: PracticeRunRecovery, kind: PracticeOutboxEntry["kind"], identity: string, body: unknown) {
  return {
    ownerSessionId: run.ownerSessionId,
    practiceSessionId: run.practiceSessionId,
    kind,
    identity,
    body: JSON.stringify(body),
    queuedAt: performance.timeOrigin + performance.now(),
  } satisfies PracticeOutboxEntry;
}

async function createAutomaticPractice() {
  await startDevelopmentSession();
  const context = await loadPracticeContext();
  const request: PracticeCreationRequest = {
    practiceSessionId: crypto.randomUUID(), focusId: context.focusId,
    planRevision: context.planRevision, seed: freshPracticeSeed(),
  };
  const run: PracticeRunRecovery = { ownerSessionId: context.ownerSessionId, ...request };
  saveRunRecovery(run);
  const creation = outboxEntry(run, "creation", request.practiceSessionId, request);
  saveOutboxEntry(creation);
  bindUrl(run.practiceSessionId);
  const session = await createPracticeSession(run.ownerSessionId, creation.body);
  removeOutboxEntry(creation);
  return { run, session };
}

async function startAutomaticPractice(): Promise<{ run: PracticeRunRecovery; session: PracticeSession }> {
  automaticStart ??= createAutomaticPractice();
  try {
    const result = await automaticStart;
    if (loadRunRecovery(result.run.practiceSessionId)) return result;
    automaticStart = null;
    return startAutomaticPractice();
  } catch (error) {
    automaticStart = null;
    throw error;
  }
}

function practiceError(error: unknown) {
  if (error instanceof PracticePersistenceError) return error.message;
  if (error instanceof ApiError && error.status === 401) return "This practice session expired. Start a new practice to continue.";
  if (error instanceof ApiError && error.status === 403) return "This practice belongs to a different browser session and was not changed.";
  if (error instanceof ApiError && error.status === 404) return "This practice is no longer available for this browser session.";
  if (error instanceof ApiError && error.status === 409) return "This practice changed in another tab. Its latest saved state is shown.";
  if (error instanceof ApiError) return error.message;
  return "Practice could not reach the learning server. Your pending operation is still saved in this browser.";
}

function isDefinitive(error: unknown) {
  return error instanceof ApiError && [400, 401, 403, 404, 409, 422].includes(error.status);
}

function isOwnerUnavailable(error: unknown) {
  return error instanceof ApiError && [401, 403, 404].includes(error.status);
}

async function sendEntry(entry: PracticeOutboxEntry) {
  if (entry.kind === "creation") {
    return createPracticeSession(entry.ownerSessionId, entry.body);
  }
  if (entry.kind === "support") {
    return updatePracticeSupport(entry.practiceSessionId, entry.ownerSessionId, entry.body);
  }
  if (entry.kind === "advance") {
    return advancePracticeSession(entry.practiceSessionId, entry.ownerSessionId, entry.body);
  }
  await submitSerializedAttempt(entry.body);
  return loadPracticeSession(entry.practiceSessionId);
}

function attemptFromEntry(entry: PracticeOutboxEntry): AttemptRequest | null {
  if (entry.kind !== "attempt") return null;
  try { return JSON.parse(entry.body) as AttemptRequest; } catch { return null; }
}

type ReconcileCallbacks = {
  unavailable: (message: string) => void;
  error: (message: string) => void;
  uncertain: (request: AttemptRequest | null) => void;
  pending: (entry: PracticeOutboxEntry) => void;
  definitive: (entry: PracticeOutboxEntry, error: unknown) => void | Promise<void>;
};

type ReconcileResult = { session: PracticeSession | null; pending: PracticeOutboxEntry | null };

async function stateAfterConflict(recovery: PracticeRunRecovery, current: PracticeSession | null) {
  try { return await loadPracticeSession(recovery.practiceSessionId); } catch { return current; }
}

async function finishDefinitiveEntry(
  recovery: PracticeRunRecovery,
  callbacks: ReconcileCallbacks,
  entry: PracticeOutboxEntry,
  caught: unknown,
  latest: PracticeSession | null,
) {
  await callbacks.definitive(entry, caught);
  removeOutboxEntry(entry);
  if (isOwnerUnavailable(caught)) return { session: latest, stop: true };
  const session = caught instanceof ApiError && caught.status === 409
    ? await stateAfterConflict(recovery, latest) : latest;
  return { session, stop: false };
}

async function reconcileOutbox(recovery: PracticeRunRecovery, callbacks: ReconcileCallbacks): Promise<ReconcileResult> {
  const entries = listOutbox(recovery.practiceSessionId);
  let latest: PracticeSession | null = entries.some((entry) => entry.kind === "creation")
    ? null
    : await loadPracticeSession(recovery.practiceSessionId);
  for (const entry of entries) {
    if (entry.ownerSessionId !== recovery.ownerSessionId) {
      callbacks.unavailable("Saved work belongs to an expired browser session and will not be replayed under a new owner.");
      return { session: null, pending: null };
    }
    try {
      latest = await sendEntry(entry);
      removeOutboxEntry(entry);
    } catch (caught) {
      callbacks.error(practiceError(caught));
      if (!isDefinitive(caught)) {
        callbacks.pending(entry);
        callbacks.uncertain(attemptFromEntry(entry));
        return { session: latest, pending: entry };
      }
      const result = await finishDefinitiveEntry(recovery, callbacks, entry, caught, latest);
      latest = result.session;
      if (result.stop) return { session: latest, pending: null };
    }
  }
  return { session: latest ?? await loadPracticeSession(recovery.practiceSessionId), pending: null };
}

async function resolveOperationError(
  caught: unknown,
  run: PracticeRunRecovery | null,
  entry: PracticeOutboxEntry,
) {
  const fallback = { displayed: practiceError(caught), ownerUnavailable: false, latest: null as PracticeSession | null };
  if (!(caught instanceof ApiError) || caught.status !== 409 || !run) return fallback;
  try {
    const context = await loadPracticeContext();
    if (context.ownerSessionId !== run.ownerSessionId) {
      return {
        displayed: "This saved practice belongs to an expired browser session. It was not reassigned or replayed.",
        ownerUnavailable: true,
        latest: null,
      };
    }
    const latest = await loadPracticeSession(run.practiceSessionId);
    return {
      displayed: entry.kind === "attempt"
        ? "Another tab continued this practice. Your new answer was not recorded; the latest saved item is shown."
        : fallback.displayed,
      ownerUnavailable: false,
      latest,
    };
  } catch { return fallback; }
}

type OperationHandlers = {
  applyState: (session: PracticeSession) => void;
  setError: (message: string) => void;
  setOrder: (order: string[]) => void;
  setPending: (entry: PracticeOutboxEntry | null) => void;
  setRecoveryUnavailable: (value: boolean) => void;
  setText: (text: string) => void;
  setUncertain: (request: AttemptRequest | null) => void;
};

function restoreUncertain(request: AttemptRequest | null, handlers: OperationHandlers) {
  handlers.setUncertain(request);
  const restored = answerValues(request?.answer);
  handlers.setText(restored.text);
  handlers.setOrder(restored.order);
}

async function handleDefinitiveOperation(
  rejected: PracticeOutboxEntry,
  caught: unknown,
  run: PracticeRunRecovery,
  handlers: OperationHandlers,
) {
  const resolution = await resolveOperationError(caught, run, rejected);
  if (resolution.ownerUnavailable || isOwnerUnavailable(caught)) handlers.setRecoveryUnavailable(true);
  if (resolution.latest) handlers.applyState(resolution.latest);
  if (rejected.kind === "attempt") handlers.setUncertain(null);
  handlers.setError(resolution.displayed);
}

function operationCallbacks(run: PracticeRunRecovery, handlers: OperationHandlers): ReconcileCallbacks {
  return {
    unavailable: (message) => { handlers.setRecoveryUnavailable(true); handlers.setError(message); },
    error: handlers.setError,
    pending: handlers.setPending,
    uncertain: (request) => restoreUncertain(request, handlers),
    definitive: (entry, caught) => handleDefinitiveOperation(entry, caught, run, handlers),
  };
}

function handleOperationFailure(
  caught: unknown,
  entry: PracticeOutboxEntry,
  run: PracticeRunRecovery | null,
  handlers: OperationHandlers,
) {
  if (isOwnerUnavailable(caught)) {
    handlers.setRecoveryUnavailable(true);
    handlers.setPending(null);
    if (entry.kind === "attempt") handlers.setUncertain(null);
  } else if (caught instanceof PracticeOutboxConflictError && run) {
    const existing = listOutbox(run.practiceSessionId)
      .find((candidate) => candidate.kind === entry.kind && candidate.identity === entry.identity) ?? null;
    handlers.setPending(existing);
    if (existing?.kind === "attempt") restoreUncertain(attemptFromEntry(existing), handlers);
  } else if (caught instanceof PracticePersistenceError) {
    handlers.setPending(null);
    if (entry.kind === "attempt") handlers.setUncertain(null);
  } else {
    handlers.setPending(entry);
    if (entry.kind === "attempt") handlers.setUncertain(attemptFromEntry(entry));
  }
  handlers.setError(practiceError(caught));
}

function SupportSections({ session, disabled, introductionContentRef, lessonContentRef, onSupport }: {
  session: PracticeSession;
  disabled: boolean;
  introductionContentRef: React.RefObject<HTMLParagraphElement | null>;
  lessonContentRef: React.RefObject<HTMLParagraphElement | null>;
  onSupport: (kind: "introduction" | "lesson" | "hint", position?: number) => void;
}) {
  const exercise = session.currentExercise;
  const item = session.items.find((candidate) => candidate.position === session.cursor);
  const [introductionDisclosure, setIntroductionDisclosure] = useState({
    value: session.introductionOpened ? "introduction" : null as string | null,
    userCollapsed: false,
  });
  const [lessonDisclosure, setLessonDisclosure] = useState({
    position: item?.position,
    value: item?.lessonOpened ? "lesson" : null as string | null,
    userCollapsed: false,
  });
  const introductionValue = session.introductionOpened && !introductionDisclosure.userCollapsed
    ? "introduction" : introductionDisclosure.value;
  const lessonValue = lessonDisclosure.position !== item?.position
    ? item?.lessonOpened ? "lesson" : null
    : item?.lessonOpened && !lessonDisclosure.userCollapsed ? "lesson" : lessonDisclosure.value;
  return (
    <Stack gap="sm">
      {session.introduction.length > 0 && (
        <Accordion
          variant="contained"
          transitionDuration={0}
          value={introductionValue}
          onChange={(value) => {
            if (value === "introduction" && !session.introductionOpened) onSupport("introduction");
            else setIntroductionDisclosure({ value, userCollapsed: value === null && session.introductionOpened });
          }}
        >
          <Accordion.Item value="introduction">
            <Accordion.Control disabled={disabled} icon={<IconBook2 size={18} />}>Session introduction</Accordion.Control>
            <Accordion.Panel>
              {session.introductionOpened ? (
                <SimpleGrid minColWidth="min(100%, 18rem)">
                  {session.introduction.map((section, index) => (
                    <Box key={`${section.title}-${section.body}`}>
                      <Title order={3} size="h4">{section.title}</Title>
                      <Text
                        ref={index === 0 ? introductionContentRef : undefined}
                        data-practice-focus={index === 0 ? "introduction" : undefined}
                        tabIndex={index === 0 ? -1 : undefined}
                        size="sm"
                        mt={4}
                      >{section.body}</Text>
                    </Box>
                  ))}
                </SimpleGrid>
              ) : <Text size="sm">Saving support before it is shown…</Text>}
            </Accordion.Panel>
          </Accordion.Item>
        </Accordion>
      )}
      {exercise && exercise.introduction.length > 0 && item && (
        <Accordion
          variant="contained"
          transitionDuration={0}
          value={lessonValue}
          onChange={(value) => {
            if (value === "lesson" && !item.lessonOpened) onSupport("lesson", item.position);
            else setLessonDisclosure({
              position: item.position, value, userCollapsed: value === null && item.lessonOpened,
            });
          }}
        >
          <Accordion.Item value="lesson">
            <Accordion.Control disabled={disabled}>Learn this first</Accordion.Control>
            <Accordion.Panel>
              {item.lessonOpened ? exercise.introduction.map((section, index) => (
                <Box key={`${section.title}-${section.body}`} mb="sm">
                  <Title order={3} size="h4">{section.title}</Title>
                  <Text
                    ref={index === 0 ? lessonContentRef : undefined}
                    data-practice-focus={index === 0 ? "lesson" : undefined}
                    tabIndex={index === 0 ? -1 : undefined}
                    size="sm"
                    mt={4}
                  >{section.body}</Text>
                </Box>
              )) : <Text size="sm">Saving support before it is shown…</Text>}
            </Accordion.Panel>
          </Accordion.Item>
        </Accordion>
      )}
    </Stack>
  );
}

function PracticeError({ message, uncertain, recoveryUnavailable, pending, retryDisabled, focusRef, onStartNew, onRetry }: {
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
        {recoveryUnavailable && <Button variant="light" onClick={onStartNew}>Start new practice</Button>}
      </Stack>
    </Alert>
  );
}

function AttemptContextNote({ item }: { item: PracticeSession["items"][number] }) {
  const context = item.lastAttemptContext;
  if (!item.lastAttempt || !context) return null;
  return (
    <Text size="sm" c="dimmed">
      {context.lessonOpened || context.hintOpened ? "Item support was open for this answer. " : ""}
      {context.priorExposure.length > 0
        ? "This target had possible earlier exposure in released prompts, support, or feedback. " : ""}
      {context.introductionOpened ? "The session introduction had been opened." : ""}
    </Text>
  );
}

function PracticeHint({ focusRef, hint }: { focusRef: React.RefObject<HTMLDivElement | null>; hint: string }) {
  return (
    <Alert ref={focusRef} data-practice-focus="hint" tabIndex={-1} color="gray" icon={<IconBulb size={18} />}>
      {hint}
    </Alert>
  );
}

function itemSupportDescription(attempt: Attempt) {
  return attempt.hintUsed ? "Item lesson or hint opened" : "No item lesson or hint opened";
}

type PracticeFocusTarget = "item" | "feedback" | "summary" | "error" | "introduction" | "lesson" | "hint";

function pendingFocus(entry: PracticeOutboxEntry, session: PracticeSession): PracticeFocusTarget {
  if (entry.kind === "attempt") return "feedback";
  if (entry.kind === "advance") return session.cursor + 1 === session.itemCount ? "summary" : "item";
  if (entry.kind !== "support") return "item";
  try {
    const { kind } = JSON.parse(entry.body) as { kind?: unknown };
    return kind === "introduction" || kind === "lesson" || kind === "hint" ? kind : "item";
  } catch { return "item"; }
}

function reconciledFocus(previousCursor: number | undefined, next: PracticeSession | null, requested: PracticeFocusTarget) {
  if (!next) return "error";
  if (next.state === "completed") return "summary";
  return next.cursor !== previousCursor ? "item" : requested;
}

function usePracticeFocusQueue(error: string) {
  const itemHeadingRef = useRef<HTMLHeadingElement>(null);
  const feedbackRef = useRef<HTMLDivElement>(null);
  const errorRef = useRef<HTMLDivElement>(null);
  const summaryHeadingRef = useRef<HTMLHeadingElement>(null);
  const introductionContentRef = useRef<HTMLParagraphElement>(null);
  const lessonContentRef = useRef<HTMLParagraphElement>(null);
  const hintRef = useRef<HTMLDivElement>(null);
  const [request, setRequest] = useState<{ target: PracticeFocusTarget; sequence: number }>({
    target: "item", sequence: 0,
  });
  const requestFocus = (target: PracticeFocusTarget) => {
    setRequest((current) => ({ target, sequence: current.sequence + 1 }));
  };
  useEffect(() => {
    if (!error && request.sequence === 0) return;
    const refs = {
      item: itemHeadingRef, feedback: feedbackRef, summary: summaryHeadingRef, error: errorRef,
      introduction: introductionContentRef, lesson: lessonContentRef, hint: hintRef,
    };
    const target = refs[error ? "error" : request.target].current;
    target?.focus({ preventScroll: true });
    target?.scrollIntoView({ block: "start", inline: "nearest" });
  }, [error, request]);
  return {
    errorRef, feedbackRef, hintRef, introductionContentRef, itemHeadingRef,
    lessonContentRef, requestFocus, summaryHeadingRef,
  };
}

export default function Practice() {
  const [run, setRun] = useState<PracticeRunRecovery | null>(null);
  const [session, setSession] = useState<PracticeSession | null>(null);
  const [text, setText] = useState("");
  const [order, setOrder] = useState<string[]>([]);
  const [attempt, setAttempt] = useState<Attempt | null>(null);
  const [busy, setBusy] = useState(true);
  const [uncertain, setUncertain] = useState<AttemptRequest | null>(null);
  const [pendingEntry, setPendingEntry] = useState<PracticeOutboxEntry | null>(null);
  const [error, setError] = useState("");
  const [recoveryUnavailable, setRecoveryUnavailable] = useState(false);
  const focusQueue = usePracticeFocusQueue(error);
  const { errorRef, feedbackRef, hintRef, introductionContentRef, itemHeadingRef, lessonContentRef, requestFocus, summaryHeadingRef } = focusQueue;
  const generationRef = useRef(0);

  const applyState = (next: PracticeSession) => {
    setSession(next);
    const item = next.items.find((candidate) => candidate.position === next.cursor);
    setAttempt(item?.lastAttempt ?? null);
    const restored = answerValues(item?.lastAnswer as AttemptAnswer | undefined);
    setText(restored.text);
    setOrder(restored.order);
  };

  useEffect(() => {
    let active = true;
    const startMissing = async () => {
      try {
        const started = await startAutomaticPractice();
        if (active) {
          setRun(started.run);
          bindUrl(started.run.practiceSessionId);
          applyState(started.session);
        }
      } catch (caught) {
        if (active) setError(practiceError(caught));
      } finally {
        if (active) setBusy(false);
      }
    };
    const recoverSaved = async (recovery: PracticeRunRecovery) => {
      setRun(recovery);
      bindUrl(recovery.practiceSessionId);
      try {
        // Owner continuity is checked before any call that can renew the cookie.
        const context = await loadPracticeContext();
        if (context.ownerSessionId !== recovery.ownerSessionId) {
          if (active) {
            setRecoveryUnavailable(true);
            setError("This saved practice belongs to an expired browser session. It was not reassigned or replayed.");
          }
          return;
        }
        const next = await reconcileOutbox(recovery, {
          unavailable: (message) => { setRecoveryUnavailable(true); setError(message); },
          error: setError,
          uncertain: (request) => {
            setUncertain(request);
            const restored = answerValues(request?.answer);
            setText(restored.text);
            setOrder(restored.order);
          },
          pending: setPendingEntry,
          definitive: (_entry, caught) => {
            if (caught instanceof ApiError && [401, 403, 404].includes(caught.status)) {
              setRecoveryUnavailable(true);
            }
          },
        });
        if (active && next.session) {
          applyState(next.session);
          const pendingAttempt = listOutbox(recovery.practiceSessionId)
            .find((entry) => entry.kind === "attempt");
          const request = pendingAttempt ? attemptFromEntry(pendingAttempt) : null;
          if (request) {
            setUncertain(request);
            const restored = answerValues(request.answer);
            setText(restored.text);
            setOrder(restored.order);
          }
        }
      } catch (caught) {
        if (active) {
          setRecoveryUnavailable(caught instanceof ApiError && [401, 403, 404].includes(caught.status));
          setError(practiceError(caught));
        }
      } finally {
        if (active) setBusy(false);
      }
    };
    const recover = async () => {
      const boundId = new URLSearchParams(window.location.search).get("session");
      const recovery = currentRecovery();
      if (boundId && !recovery) {
        if (active) {
          setRecoveryUnavailable(true);
          setError("This tab’s saved practice record is unavailable. Start a new practice when you are ready.");
          setBusy(false);
        }
        return;
      }
      if (recovery) await recoverSaved(recovery);
      else await startMissing();
    };
    void recover();
    return () => { active = false; };
  }, []);

  const operationHandlers: OperationHandlers = {
    applyState, setError,
    setOrder,
    setPending: setPendingEntry,
    setRecoveryUnavailable,
    setText,
    setUncertain,
  };
  const perform = async (entry: PracticeOutboxEntry, focus: PracticeFocusTarget) => {
    const generation = generationRef.current;
    setBusy(true);
    setPendingEntry(entry);
    setError("");
    try {
      saveOutboxEntry(entry);
      if (!run) throw new Error("Practice recovery is unavailable.");
      const result = await reconcileOutbox(run, operationCallbacks(run, operationHandlers));
      if (generation !== generationRef.current) return;
      if (result.session) applyState(result.session);
      setPendingEntry(result.pending);
      if (!result.pending) {
        setUncertain(null);
        requestFocus(reconciledFocus(session?.cursor, result.session, focus));
      }
    } catch (caught) {
      if (generation !== generationRef.current) return;
      handleOperationFailure(caught, entry, run, operationHandlers);
      requestFocus("error");
    } finally {
      if (generation === generationRef.current) setBusy(false);
    }
  };

  const startNewPractice = async () => {
    generationRef.current += 1;
    setBusy(true);
    setError("");
    abandonCurrentPractice(run);
    setRun(null);
    setSession(null);
    setAttempt(null);
    setUncertain(null);
    setPendingEntry(null);
    try {
      await startDevelopmentSession();
      const context = await loadPracticeContext();
      const request: PracticeCreationRequest = {
        practiceSessionId: crypto.randomUUID(), focusId: context.focusId,
        planRevision: context.planRevision, seed: freshPracticeSeed(),
      };
      const recovery: PracticeRunRecovery = { ownerSessionId: context.ownerSessionId, ...request };
      saveRunRecovery(recovery);
      const creation = outboxEntry(recovery, "creation", request.practiceSessionId, request);
      saveOutboxEntry(creation);
      bindUrl(recovery.practiceSessionId);
      setRun(recovery);
      const created = await createPracticeSession(recovery.ownerSessionId, creation.body);
      removeOutboxEntry(creation);
      applyState(created);
      setRecoveryUnavailable(false);
    } catch (caught) {
      setError(practiceError(caught));
    } finally { setBusy(false); }
  };

  if (busy && !session) return <Group justify="center" mih={300}><Loader aria-label="Starting practice" /></Group>;
  if (!session) {
    return (
      <Stack maw={760} className={`${classes.playground} ${classes.practice}`}>
        {error && <Alert ref={errorRef} tabIndex={-1} color="red" icon={<IconAlertCircle />} title="Practice recovery unavailable">{error}</Alert>}
        <Button onClick={() => void startNewPractice()}>Start new practice</Button>
      </Stack>
    );
  }
  const exercise = session.currentExercise;
  const item = session.items.find((candidate) => candidate.position === session.cursor);
  const answerReady = exercise?.formatId === "E003" ? order.length === exercise.prompt.blocks.length : text.trim().length > 0;
  const progress = session.itemCount > 0 ? (session.cursor / session.itemCount) * 100 : 0;
  const blocked = busy || Boolean(pendingEntry) || Boolean(uncertain);
  const submit = () => {
    if (!run || !exercise || !item) return;
    const request: AttemptRequest = {
      attemptId: crypto.randomUUID(), exerciseId: exercise.id,
      answer: answerFor(exercise, text, order), hintUsed: item.lessonOpened || item.hintOpened,
    };
    void perform(outboxEntry(run, "attempt", request.attemptId, request), "feedback");
  };
  const support = (kind: "introduction" | "lesson" | "hint", position?: number) => {
    if (!run) return;
    const body = kind === "introduction" ? { kind } : { kind, position };
    void perform(outboxEntry(run, "support", `${kind}-${position ?? "session"}`, body), kind);
  };
  const advance = () => {
    if (!run || !item) return;
    const completing = session.cursor + 1 === session.itemCount;
    void perform(outboxEntry(run, "advance", `${run.practiceSessionId}-${item.position}`, { position: item.position }), completing ? "summary" : "item");
  };

  return (
    <Stack gap="lg" maw={920} className={`${classes.playground} ${classes.practice}`}>
      <Group justify="space-between" align="start">
        <Box><Title order={1} size="h2">{session.title}</Title><Text c="dimmed">A short practice in eight parts. Continue after any saved answer.</Text></Box>
        <Button variant="subtle" color="gray" disabled={busy} onClick={() => void startNewPractice()}>Start new practice</Button>
      </Group>
      <Box>
        <Group justify="space-between" mb={6}><Text size="sm" fw={650}>{session.state === "completed" ? "Complete" : `Part ${session.cursor + 1} of ${session.itemCount}`}</Text><Text size="sm" c="dimmed">{session.cursor}/{session.itemCount} continued</Text></Group>
        <Progress value={progress} aria-label={`${session.cursor} of ${session.itemCount} practice items continued`} />
      </Box>
      <PracticeError
        message={error}
        uncertain={Boolean(uncertain)}
        recoveryUnavailable={recoveryUnavailable}
        pending={Boolean(pendingEntry)}
        retryDisabled={busy}
        focusRef={errorRef}
        onStartNew={() => void startNewPractice()}
        onRetry={() => {
          if (!pendingEntry) return;
          void perform(pendingEntry, pendingFocus(pendingEntry, session));
        }}
      />
      {session.state === "completed" ? <PracticeSummary session={session} headingRef={summaryHeadingRef} /> : exercise && item ? (
        <>
          <SupportSections session={session} disabled={blocked} introductionContentRef={introductionContentRef} lessonContentRef={lessonContentRef} onSupport={support} />
          <Paper withBorder shadow="xs" className={classes.exerciseCard}>
            <Stack gap="lg">
              <Group justify="space-between"><Text fw={650}>Practice item {item.position + 1}</Text><Badge variant="light" color="astrid">{exercise.targetSkillId ?? "Vocabulary"}</Badge></Group>
              <Divider />
              <Box><Title ref={itemHeadingRef} tabIndex={-1} order={2} size="h4">{exercise.instruction}</Title><Text c="dimmed" mt={4}>{exercise.context}</Text></Box>
              <ExercisePrompt exercise={exercise} text={text} order={order} disabled={blocked || Boolean(attempt)} onText={setText} onOrder={setOrder} />
              {item.hintOpened && <PracticeHint focusRef={hintRef} hint={exercise.hint} />}
              {attempt && <FeedbackPanel
                attempt={attempt}
                focusRef={feedbackRef}
                supportDescription={itemSupportDescription(attempt)}
              />}
              <AttemptContextNote item={item} />
              <Group justify="space-between" className={classes.exerciseActions}>
                <Button variant="subtle" color="gray" disabled={blocked || item.hintOpened || Boolean(attempt)} onClick={() => support("hint", item.position)}>Show hint</Button>
                <Group className={classes.exercisePrimaryActions}>
                  {attempt && <Button variant="default" disabled={blocked} onClick={() => { setAttempt(null); setText(""); setOrder([]); requestFocus("item"); }}>Try again</Button>}
                  {attempt ? <Button loading={busy} disabled={blocked} onClick={advance}>Continue</Button> : <Button loading={busy} disabled={!answerReady || blocked} onClick={submit}>Check answer</Button>}
                </Group>
              </Group>
            </Stack>
          </Paper>
        </>
      ) : null}
    </Stack>
  );
}
