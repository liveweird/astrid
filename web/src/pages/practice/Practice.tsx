import { Box, Group, Loader, Progress, Stack, Text } from "@mantine/core";
import { useEffect, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { loadPracticeContext, loadPracticeProfiles } from "../../api/client";
import type { Attempt, AttemptAnswer, AttemptRequest, GapSelection, PracticeSession } from "../../api/models";
import PracticeSummary from "../../components/PracticeSummary";
import classes from "../../theme.module.css";
import ActivePractice from "./ActivePractice";
import PracticeChooser, { EmptyPractice, PracticeHeading } from "./PracticeChooser";
import { PracticeError } from "./PracticeMessages";
import {
  answerFor, answerValues, attemptFromEntry, handleOperationFailure, type OperationHandlers, operationCallbacks, outboxEntry,
  practiceError, reconcileOutbox,
} from "./practiceOperations";
import {
  followUpRevisionFor, moduleOneProfileKey, nextModuleOneProfile, prepareFollowUpCreation, preparePracticeCreation,
  profileKey, validateProfiles,
} from "./practiceProfiles";
import {
  type PracticeOutboxEntry, type PracticeRunRecovery, saveOutboxEntry, saveRunWithCreation,
} from "./practiceRecovery";
import { useInitialPracticeRecovery } from "./useInitialPracticeRecovery";
import { type PracticeFocusTarget, pendingFocus, reconciledFocus, usePracticeFocusQueue } from "./usePracticeFocusQueue";

function restoreAnswer(
  answer: AttemptAnswer | undefined,
  setText: (value: string) => void,
  setOrder: (value: string[]) => void,
  setGapSelections: (value: GapSelection[]) => void,
) {
  const restored = answerValues(answer);
  setText(restored.text); setOrder(restored.order); setGapSelections(restored.gapSelections);
}

function sameCurrentAnswer(before: PracticeSession | null, next: PracticeSession) {
  if (!before?.currentExercise || before.currentExercise.id !== next.currentExercise?.id) return false;
  const previous = before.items.find((item) => item.position === before.cursor);
  const current = next.items.find((item) => item.position === next.cursor);
  return previous?.lastAttempt?.attemptId === current?.lastAttempt?.attemptId;
}

export default function Practice() {
  const location = useLocation();
  const navigate = useNavigate();
  const [boundId] = useState(() => new URLSearchParams(location.search).get("session"));
  // bindUrl is captured once by the mount-time recovery, so it reads the fragment through a ref.
  const hashRef = useRef(location.hash);
  useEffect(() => { hashRef.current = location.hash; }, [location.hash]);
  const bindUrl = (practiceSessionId: string) => navigate({
    search: new URLSearchParams({ session: practiceSessionId }).toString(), hash: hashRef.current,
  }, { replace: true });
  const [run, setRun] = useState<PracticeRunRecovery | null>(null);
  const [session, setSession] = useState<PracticeSession | null>(null);
  const [text, setText] = useState("");
  const [order, setOrder] = useState<string[]>([]);
  const [gapSelections, setGapSelections] = useState<GapSelection[]>([]);
  const [attempt, setAttempt] = useState<Attempt | null>(null);
  const [busy, setBusy] = useState(true);
  const [uncertain, setUncertain] = useState<AttemptRequest | null>(null);
  const [pendingEntry, setPendingEntry] = useState<PracticeOutboxEntry | null>(null);
  const [error, setError] = useState("");
  const [recoveryUnavailable, setRecoveryUnavailable] = useState(false), [selectedTargets, setSelectedTargets] = useState<Set<string>>(() => new Set());
  const [chooserOpen, setChooserOpen] = useState(false), [selectedProfile, setSelectedProfile] = useState<string | null>(null);
  const { errorRef, feedbackRef, gapFocusRef, hintRef, introductionContentRef, introductionControlRef, itemHeadingRef,
    lessonContentRef, requestFocus, summaryHeadingRef } = usePracticeFocusQueue(error);
  const generationRef = useRef(0), chooserReturnErrorRef = useRef(""); useEffect(() => () => { generationRef.current += 1; }, []);

  const applyState = (next: PracticeSession, preserveDraft = false) => {
    setSession(next);
    if (preserveDraft && sameCurrentAnswer(session, next)) return;
    const item = next.items.find((candidate) => candidate.position === next.cursor);
    setAttempt(item?.lastAttempt ?? null);
    restoreAnswer(item?.lastAnswer as AttemptAnswer | undefined, setText, setOrder, setGapSelections);
    setSelectedTargets(new Set());
  };

  useInitialPracticeRecovery(boundId, {
    applyState, bindUrl, setBusy, setError, setGapSelections, setOrder, setPendingEntry, setRecoveryUnavailable,
    setRun, setText, setUncertain, requestFocus,
    showFreshChooser: (_catalog, message = "") => {
      setSelectedProfile(null);
      setChooserOpen(true);
      setError(message);
    },
  });

  const operationHandlers: OperationHandlers = { applyState, setError, setGapSelections, setOrder,
    setPending: setPendingEntry, setRecoveryUnavailable, setText, setUncertain };
  const perform = async (
    entry: PracticeOutboxEntry,
    focus: PracticeFocusTarget,
    operationRun: PracticeRunRecovery | null = run,
  ) => {
    const generation = generationRef.current;
    setBusy(true);
    setPendingEntry(entry);
    setError("");
    try {
      saveOutboxEntry(entry);
      if (!operationRun) throw new Error("Practice recovery is unavailable.");
      const result = await reconcileOutbox(operationRun, operationCallbacks(operationRun, operationHandlers));
      if (generation !== generationRef.current) return;
      if (result.session) applyState(result.session, entry.kind === "support");
      setPendingEntry(result.pending);
      const pendingRequest = result.pending ? attemptFromEntry(result.pending) : null;
      if (pendingRequest) {
        setUncertain(pendingRequest);
        restoreAnswer(pendingRequest.answer, setText, setOrder, setGapSelections);
      }
      if (!result.pending) {
        setUncertain(null);
        requestFocus(reconciledFocus(session?.cursor, result.session, focus));
      }
    } catch (caught) {
      if (generation !== generationRef.current) return;
      handleOperationFailure(caught, entry, operationRun, operationHandlers);
      requestFocus("error");
    } finally {
      if (generation === generationRef.current) setBusy(false);
    }
  };

  const openPracticeChooser = (preselected: string | null = null) => {
    const generation = generationRef.current + 1;
    generationRef.current = generation;
    chooserReturnErrorRef.current = error;
    setChooserOpen(true);
    setSelectedProfile(preselected);
    setError("");
    if (!run) return;
    void loadPracticeProfiles().then((catalog) => {
      const validated = validateProfiles(catalog);
      if (generation !== generationRef.current || validated.ownerSessionId !== run.ownerSessionId) return;
    }).catch(() => {
      // Present the compiled choices without renewing an expired owner.
    });
  };

  const cancelPracticeChooser = () => {
    generationRef.current += 1;
    setChooserOpen(false);
    setSelectedProfile(null);
    setError(chooserReturnErrorRef.current);
    if (session?.state === "completed") requestFocus("summary");
  };

  const startNewPractice = async () => {
    const chosenKey = selectedProfile;
    if (!chosenKey) return;
    const generation = generationRef.current + 1;
    generationRef.current = generation;
    setBusy(true);
    setError("");
    try {
      const { recovery, creation } = await preparePracticeCreation(chosenKey);
      if (generation !== generationRef.current) return;
      saveRunWithCreation(recovery, creation);
      bindUrl(recovery.practiceSessionId);
      setRun(recovery);
      setSession(null);
      setAttempt(null);
      setGapSelections([]);
      setUncertain(null);
      setPendingEntry(creation);
      setSelectedTargets(new Set());
      setChooserOpen(false);
      setRecoveryUnavailable(false);
      await perform(creation, "item", recovery);
    } catch (caught) {
      if (generation !== generationRef.current) return;
      setError(practiceError(caught));
      requestFocus("error");
    } finally {
      if (generation === generationRef.current) setBusy(false);
    }
  };

  const startFollowUp = async () => {
    if (!run || !session || session.state !== "completed" || selectedTargets.size === 0) return;
    const generation = generationRef.current + 1;
    generationRef.current = generation;
    setBusy(true);
    setError("");
    try {
      const planRevision = followUpRevisionFor(session);
      if (!planRevision) {
        setError("This practice does not advertise a supported follow-up plan.");
        requestFocus("error");
        return;
      }
      // This read cannot renew the development cookie. A review always keeps its source owner.
      const context = await loadPracticeContext();
      if (generation !== generationRef.current) return;
      if (context.ownerSessionId !== run.ownerSessionId) {
        setRecoveryUnavailable(true);
        setError("This source practice belongs to an expired browser session. A review was not created.");
        return;
      }
      const { recovery, creation } = prepareFollowUpCreation(run, session, planRevision, selectedTargets);
      saveRunWithCreation(recovery, creation);
      if (generation !== generationRef.current) return;
      bindUrl(recovery.practiceSessionId);
      setRun(recovery);
      setSession(null);
      setAttempt(null);
      setGapSelections([]);
      setUncertain(null);
      setPendingEntry(creation);
      setSelectedTargets(new Set());
      await perform(creation, "item", recovery);
    } catch (caught) {
      if (generation !== generationRef.current) return;
      setError(practiceError(caught));
      requestFocus("error");
    } finally {
      if (generation === generationRef.current) setBusy(false);
    }
  };

  if (busy && !session && !chooserOpen) return <Group justify="center" mih={300}><Loader aria-label="Starting practice" /></Group>;
  if (chooserOpen) return <PracticeChooser
    selected={selectedProfile}
    busy={busy}
    canCancel={Boolean(run)}
    message={error}
    onSelect={setSelectedProfile}
    onStart={() => void startNewPractice()}
    onCancel={cancelPracticeChooser}
  />;
  if (!session) {
    return <EmptyPractice error={error} unavailable={recoveryUnavailable} pending={Boolean(pendingEntry)}
      busy={busy} focusRef={errorRef} onStartNew={openPracticeChooser}
      onRetry={() => { if (pendingEntry) void perform(pendingEntry, "item"); }} />;
  }
  const exercise = session.currentExercise;
  const item = session.items.find((candidate) => candidate.position === session.cursor);
  const progress = session.itemCount > 0 ? (session.cursor / session.itemCount) * 100 : 0;
  const blocked = busy || Boolean(pendingEntry) || Boolean(uncertain);
  const submit = () => {
    if (!run || !exercise || !item) return;
    const request: AttemptRequest = {
      attemptId: crypto.randomUUID(), exerciseId: exercise.id,
      answer: answerFor(exercise, text, order, gapSelections), hintUsed: item.lessonOpened || item.hintOpened,
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
  const nextProfile = nextModuleOneProfile(session);
  const nextProfileKey = nextProfile ? profileKey(nextProfile) : null;

  return (
    <Stack gap="lg" maw={920} className={`${classes.playground} ${classes.practice}`}>
      <PracticeHeading session={session} busy={busy} onStartNew={() => openPracticeChooser()} />
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
        onStartNew={openPracticeChooser}
        onRetry={() => {
          if (!pendingEntry) return;
          void perform(pendingEntry, pendingFocus(pendingEntry, session));
        }}
      />
      {session.state === "completed" ? <PracticeSummary
        session={session}
        headingRef={summaryHeadingRef}
        selectedTargets={selectedTargets}
        busy={blocked}
        onToggleTarget={(targetKey, selected) => setSelectedTargets((current) => {
          const next = new Set(current);
          if (selected) next.add(targetKey); else next.delete(targetKey);
          return next;
        })}
        onStartReview={() => void startFollowUp()}
        onRepeat={() => {
          const key = moduleOneProfileKey(session);
          if (key) openPracticeChooser(key);
        }}
        nextProfile={nextProfile && nextProfileKey
          ? { title: nextProfile.title, onChoose: () => openPracticeChooser(nextProfileKey) }
          : undefined}
      /> : <ActivePractice
        session={session} blocked={blocked} busy={busy} text={text} order={order}
        gapSelections={gapSelections} attempt={attempt} feedbackRef={feedbackRef} gapFocusRef={gapFocusRef}
        hintRef={hintRef} introductionRef={introductionContentRef} introductionControlRef={introductionControlRef} itemRef={itemHeadingRef} lessonRef={lessonContentRef}
        onText={setText} onOrder={setOrder} onGapSelections={setGapSelections} onSupport={support}
        onSubmit={submit} onAdvance={advance}
        onRetry={() => {
          setAttempt(null);
          if (exercise?.formatId === "E015") requestFocus("gaps");
          else if (["E024", "E025", "E029", "E030"].includes(exercise?.formatId ?? "")) requestFocus("item");
          else { setText(""); setOrder([]); setGapSelections([]); requestFocus("item"); }
        }}
      />}
    </Stack>
  );
}
