import {
  advancePracticeSession, ApiError, createPracticeFollowUp, createPracticeSession, loadPracticeContext,
  loadPracticeSession, submitSerializedAttempt, updatePracticeSupport,
} from "../../api/client";
import type { AttemptAnswer, AttemptRequest, GapSelection, PracticeSession } from "../../api/models";
import {
  listOutbox, PracticeOutboxConflictError, PracticePersistenceError,
  type PracticeOutboxEntry, type PracticeRunRecovery, removeOutboxEntry,
} from "./practiceRecovery";

export class PracticeCatalogError extends Error {}

export function answerFor(
  exercise: NonNullable<PracticeSession["currentExercise"]>,
  text: string,
  order: string[],
  gapSelections: GapSelection[],
): AttemptAnswer {
  if (["E003", "E024", "E025", "E029", "E030"].includes(exercise.formatId)) return { kind: "order", blockIds: order };
  if (exercise.formatId === "E015") return { kind: "gaps", selections: gapSelections };
  return { kind: "text", text };
}

export function answerValues(answer?: AttemptAnswer) {
  if (answer?.kind === "text") return { text: answer.text, order: [] as string[], gapSelections: [] as GapSelection[] };
  if (answer?.kind === "order") return { text: "", order: answer.blockIds, gapSelections: [] as GapSelection[] };
  if (answer?.kind === "gaps") return { text: "", order: [] as string[], gapSelections: answer.selections };
  return { text: "", order: [] as string[], gapSelections: [] as GapSelection[] };
}

export function outboxEntry(run: PracticeRunRecovery, kind: PracticeOutboxEntry["kind"], identity: string, body: unknown) {
  return {
    ownerSessionId: run.ownerSessionId,
    practiceSessionId: run.practiceSessionId,
    kind,
    identity,
    body: JSON.stringify(body),
    ...(run.sourcePracticeSessionId ? { sourcePracticeSessionId: run.sourcePracticeSessionId } : {}),
    queuedAt: performance.timeOrigin + performance.now(),
  } satisfies PracticeOutboxEntry;
}

export function practiceError(error: unknown) {
  if (error instanceof PracticePersistenceError) return error.message;
  if (error instanceof PracticeCatalogError) return error.message;
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

export function isOwnerUnavailable(error: unknown) {
  return error instanceof ApiError && [401, 403, 404].includes(error.status);
}

async function sendEntry(entry: PracticeOutboxEntry) {
  if (entry.kind === "creation") {
    if (entry.sourcePracticeSessionId) {
      return createPracticeFollowUp(entry.sourcePracticeSessionId, entry.ownerSessionId, entry.body);
    }
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

export function attemptFromEntry(entry: PracticeOutboxEntry): AttemptRequest | null {
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

export async function reconcileOutbox(recovery: PracticeRunRecovery, callbacks: ReconcileCallbacks): Promise<ReconcileResult> {
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

export type OperationHandlers = {
  applyState: (session: PracticeSession) => void;
  setError: (message: string) => void;
  setGapSelections: (selections: GapSelection[]) => void;
  setOrder: (order: string[]) => void;
  setPending: (entry: PracticeOutboxEntry | null) => void;
  setRecoveryUnavailable: (value: boolean) => void;
  setText: (text: string) => void;
  setUncertain: (request: AttemptRequest | null) => void;
};

function restoreUncertain(request: AttemptRequest | null, handlers: OperationHandlers) {
  handlers.setUncertain(request);
  const restored = answerValues(request?.answer);
  handlers.setGapSelections(restored.gapSelections);
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

export function operationCallbacks(run: PracticeRunRecovery, handlers: OperationHandlers): ReconcileCallbacks {
  return {
    unavailable: (message) => { handlers.setRecoveryUnavailable(true); handlers.setError(message); },
    error: handlers.setError,
    pending: handlers.setPending,
    uncertain: (request) => restoreUncertain(request, handlers),
    definitive: (entry, caught) => handleDefinitiveOperation(entry, caught, run, handlers),
  };
}

export function handleOperationFailure(
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
