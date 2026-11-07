export type PracticeOperationKind = "creation" | "support" | "attempt" | "advance";

export type PracticeRunRecovery = {
  ownerSessionId: string;
  practiceSessionId: string;
  focusId: string;
  planRevision: string;
  seed: number;
  sourcePracticeSessionId?: string;
  selectedTargetKeys?: string[];
};

export type PracticeOutboxEntry = {
  ownerSessionId: string;
  practiceSessionId: string;
  kind: PracticeOperationKind;
  identity: string;
  body: string;
  sourcePracticeSessionId?: string;
  queuedAt?: number;
};

const PREFIX = "astrid.practice.v1";
const LATEST_KEY = `${PREFIX}.latest`;
const RUN_PREFIX = `${PREFIX}.run.`;
const OUTBOX_PREFIX = `${PREFIX}.outbox.`;

export class PracticePersistenceError extends Error {
  constructor(message = "This browser could not save recovery data. The operation was not sent.") {
    super(message);
  }
}

export class PracticeOutboxConflictError extends PracticePersistenceError {
  constructor() {
    super("Saved recovery data conflicts with this operation. Nothing was sent.");
  }
}

function parse<T>(value: string | null): T | null {
  if (!value) return null;
  try { return JSON.parse(value) as T; } catch { return null; }
}

function safeSet(key: string, value: string) {
  try { localStorage.setItem(key, value); } catch { throw new PracticePersistenceError(); }
}

function restoreIfUnchanged(key: string, expected: string, previous: string | null) {
  if (localStorage.getItem(key) !== expected) return;
  try {
    if (previous === null) localStorage.removeItem(key);
    else localStorage.setItem(key, previous);
  } catch {
    // Keep the original persistence error. The guarded restore never replaces newer tab state.
  }
}

export function runKey(practiceSessionId: string) {
  return `${RUN_PREFIX}${practiceSessionId}`;
}

export function outboxKey(entry: Pick<PracticeOutboxEntry, "practiceSessionId" | "kind" | "identity">) {
  return `${OUTBOX_PREFIX}${entry.practiceSessionId}.${entry.kind}.${entry.identity}`;
}

export function saveRunRecovery(run: PracticeRunRecovery) {
  safeSet(runKey(run.practiceSessionId), JSON.stringify(run));
  safeSet(LATEST_KEY, run.practiceSessionId);
}

export function saveRunWithCreation(run: PracticeRunRecovery, entry: PracticeOutboxEntry) {
  if (entry.kind !== "creation" || entry.practiceSessionId !== run.practiceSessionId ||
    entry.ownerSessionId !== run.ownerSessionId) {
    throw new PracticeOutboxConflictError();
  }
  const savedRunKey = runKey(run.practiceSessionId);
  const savedOutboxKey = outboxKey(entry);
  const runValue = JSON.stringify(run);
  const durableEntry = { ...entry, queuedAt: entry.queuedAt ?? performance.timeOrigin + performance.now() };
  const outboxValue = JSON.stringify(durableEntry);
  const previousRun = localStorage.getItem(savedRunKey);
  const previousOutbox = localStorage.getItem(savedOutboxKey);
  const previousLatest = localStorage.getItem(LATEST_KEY);
  try {
    safeSet(savedRunKey, runValue);
    saveOutboxEntry(durableEntry);
    // Another tab may have selected a newer run between these writes. Preserve that choice.
    if (localStorage.getItem(LATEST_KEY) === previousLatest) safeSet(LATEST_KEY, run.practiceSessionId);
  } catch (error) {
    restoreIfUnchanged(savedOutboxKey, outboxValue, previousOutbox);
    restoreIfUnchanged(savedRunKey, runValue, previousRun);
    restoreIfUnchanged(LATEST_KEY, run.practiceSessionId, previousLatest);
    throw error;
  }
}

export function loadRunRecovery(practiceSessionId: string): PracticeRunRecovery | null {
  return parse<PracticeRunRecovery>(localStorage.getItem(runKey(practiceSessionId)));
}

export function latestRunRecovery(): PracticeRunRecovery | null {
  const id = localStorage.getItem(LATEST_KEY);
  return id ? loadRunRecovery(id) : null;
}

export function saveOutboxEntry(entry: PracticeOutboxEntry) {
  const key = outboxKey(entry);
  const existing = parse<PracticeOutboxEntry>(localStorage.getItem(key));
  if (existing) {
    const unchanged = existing.ownerSessionId === entry.ownerSessionId &&
      existing.practiceSessionId === entry.practiceSessionId && existing.kind === entry.kind &&
      existing.identity === entry.identity && existing.body === entry.body &&
      existing.sourcePracticeSessionId === entry.sourcePracticeSessionId;
    if (!unchanged) throw new PracticeOutboxConflictError();
    return;
  }
  const queuedAt = entry.queuedAt ?? performance.timeOrigin + performance.now();
  safeSet(key, JSON.stringify({ ...entry, queuedAt }));
}

export function removeOutboxEntry(entry: PracticeOutboxEntry) {
  localStorage.removeItem(outboxKey(entry));
}

export function listOutbox(practiceSessionId: string): PracticeOutboxEntry[] {
  const prefix = `${OUTBOX_PREFIX}${practiceSessionId}.`;
  const entries: PracticeOutboxEntry[] = [];
  for (let index = 0; index < localStorage.length; index += 1) {
    const key = localStorage.key(index);
    if (!key?.startsWith(prefix)) continue;
    const entry = parse<PracticeOutboxEntry>(localStorage.getItem(key));
    if (entry) entries.push(entry);
  }
  const order: Record<PracticeOperationKind, number> = { creation: 0, support: 1, attempt: 2, advance: 3 };
  return entries.sort((left, right) =>
    order[left.kind] - order[right.kind] ||
    (left.queuedAt ?? 0) - (right.queuedAt ?? 0) ||
    left.identity.localeCompare(right.identity));
}

export function abandonRun(practiceSessionId: string) {
  for (const entry of listOutbox(practiceSessionId)) removeOutboxEntry(entry);
  localStorage.removeItem(runKey(practiceSessionId));
  if (localStorage.getItem(LATEST_KEY) === practiceSessionId) localStorage.removeItem(LATEST_KEY);
}
