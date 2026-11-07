import {
  abandonRun, latestRunRecovery, listOutbox, loadRunRecovery, PracticePersistenceError,
  PracticeOutboxConflictError, removeOutboxEntry, saveOutboxEntry, saveRunRecovery,
  saveRunWithCreation,
} from "./practiceRecovery";

const run = {
  ownerSessionId: "11111111-1111-4111-8111-111111111111",
  practiceSessionId: "22222222-2222-4222-8222-222222222222",
  focusId: "reading-basics",
  planRevision: "practice-1",
  seed: 47,
};

beforeEach(() => localStorage.clear());

it("stores immutable operations under distinct identities and replays them in dependency order", () => {
  saveRunRecovery(run);
  const advance = { ...run, kind: "advance" as const, identity: `${run.practiceSessionId}-0`, body: '{"position":0}' };
  const attempt = { ...run, kind: "attempt" as const, identity: "attempt-1", body: '{"attemptId":"attempt-1"}' };
  const laterAttempt = { ...run, kind: "attempt" as const, identity: "attempt-2", body: '{"attemptId":"attempt-2"}' };
  const support = { ...run, kind: "support" as const, identity: "hint-0", body: '{"kind":"hint","position":0}' };
  saveOutboxEntry(advance);
  saveOutboxEntry(attempt);
  saveOutboxEntry(laterAttempt);
  saveOutboxEntry(support);

  expect(latestRunRecovery()).toEqual(run);
  expect(loadRunRecovery(run.practiceSessionId)).toEqual(run);
  expect(listOutbox(run.practiceSessionId).map((entry) => entry.kind)).toEqual(["support", "attempt", "attempt", "advance"]);

  removeOutboxEntry(attempt);
  expect(listOutbox(run.practiceSessionId).map((entry) => entry.kind)).toEqual(["support", "attempt", "advance"]);
  abandonRun(run.practiceSessionId);
  expect(latestRunRecovery()).toBeNull();
  expect(listOutbox(run.practiceSessionId)).toEqual([]);
});

it("does not allow a write to be sent when durable persistence fails", () => {
  const setItem = vi.spyOn(localStorage, "setItem").mockImplementation(() => { throw new DOMException("full", "QuotaExceededError"); });
  expect(() => saveRunRecovery(run)).toThrow(PracticePersistenceError);
  setItem.mockRestore();
});

it("ignores malformed recovery records without changing them", () => {
  localStorage.setItem(`astrid.practice.v1.run.${run.practiceSessionId}`, "{");
  localStorage.setItem("astrid.practice.v1.latest", run.practiceSessionId);
  expect(latestRunRecovery()).toBeNull();
});

it("skips unrelated and malformed outbox values and preserves another latest run", () => {
  const other = { ...run, practiceSessionId: "33333333-3333-4333-8333-333333333333" };
  saveRunRecovery(run);
  saveRunRecovery(other);
  localStorage.setItem("unrelated", "value");
  localStorage.setItem(`astrid.practice.v1.outbox.${run.practiceSessionId}.attempt.bad`, "{");
  expect(listOutbox(run.practiceSessionId)).toEqual([]);
  abandonRun(run.practiceSessionId);
  expect(latestRunRecovery()).toEqual(other);
});

it("orders same-kind attempts by persisted enqueue time instead of UUID order", () => {
  const chronologicallyFirst = {
    ...run, kind: "attempt" as const, identity: "ffffffff-ffff-4fff-8fff-ffffffffffff",
    body: '{"attemptId":"first"}', queuedAt: 100.25,
  };
  const chronologicallySecond = {
    ...run, kind: "attempt" as const, identity: "00000000-0000-4000-8000-000000000000",
    body: '{"attemptId":"second"}', queuedAt: 100.5,
  };
  const simultaneousTie = {
    ...run, kind: "attempt" as const, identity: "11111111-1111-4111-8111-111111111111",
    body: '{"attemptId":"same-time"}', queuedAt: 100.5,
  };
  saveOutboxEntry(simultaneousTie);
  saveOutboxEntry(chronologicallySecond);
  saveOutboxEntry(chronologicallyFirst);

  expect(listOutbox(run.practiceSessionId).map((entry) => entry.body)).toEqual([
    chronologicallyFirst.body,
    chronologicallySecond.body,
    simultaneousTie.body,
  ]);
});

it("preserves the first enqueue timestamp on exact retry and refuses identity mutation", () => {
  const entry = { ...run, kind: "attempt" as const, identity: "attempt-fixed", body: '{"attemptId":"fixed"}' };
  saveOutboxEntry(entry);
  const queuedAt = listOutbox(run.practiceSessionId)[0]?.queuedAt;

  saveOutboxEntry({ ...entry, queuedAt: 999_999 });
  expect(listOutbox(run.practiceSessionId)[0]?.queuedAt).toBe(queuedAt);
  expect(() => saveOutboxEntry({ ...entry, body: '{"attemptId":"changed"}' }))
    .toThrow(PracticeOutboxConflictError);
  expect(listOutbox(run.practiceSessionId)[0]?.body).toBe(entry.body);
});

it("treats the follow-up source endpoint as part of an immutable creation", () => {
  const entry = {
    ...run,
    kind: "creation" as const,
    identity: run.practiceSessionId,
    body: '{"practiceSessionId":"fixed","targetKeys":["grammar:G005"]}',
    sourcePracticeSessionId: "33333333-3333-4333-8333-333333333333",
  };
  saveOutboxEntry(entry);
  expect(() => saveOutboxEntry({
    ...entry,
    sourcePracticeSessionId: "44444444-4444-4444-8444-444444444444",
  })).toThrow(PracticeOutboxConflictError);
  expect(listOutbox(run.practiceSessionId)[0]?.sourcePracticeSessionId).toBe(entry.sourcePracticeSessionId);
});

it("saves a new run and its creation before making it latest", () => {
  const source = { ...run, practiceSessionId: "33333333-3333-4333-8333-333333333333" };
  const child = { ...run, sourcePracticeSessionId: source.practiceSessionId };
  saveRunRecovery(source);
  const creation = {
    ...child, kind: "creation" as const, identity: child.practiceSessionId,
    body: '{"practiceSessionId":"child"}',
  };
  saveRunWithCreation(child, creation);
  expect(latestRunRecovery()).toEqual(child);
  expect(listOutbox(child.practiceSessionId)).toHaveLength(1);
  expect(loadRunRecovery(source.practiceSessionId)).toEqual(source);
});

it("does not replace a newer latest run selected while a creation is saved", () => {
  const source = { ...run, practiceSessionId: "33333333-3333-4333-8333-333333333333" };
  const child = { ...run, sourcePracticeSessionId: source.practiceSessionId };
  const other = { ...run, practiceSessionId: "44444444-4444-4444-8444-444444444444" };
  saveRunRecovery(source);
  const originalSetItem = localStorage.setItem.bind(localStorage);
  const setItem = vi.spyOn(localStorage, "setItem").mockImplementation((key, value) => {
    originalSetItem(key, value);
    if (key.includes(`outbox.${child.practiceSessionId}.creation.`)) {
      originalSetItem("astrid.practice.v1.latest", other.practiceSessionId);
    }
  });
  saveRunWithCreation(child, {
    ...child, kind: "creation", identity: child.practiceSessionId, body: '{"practiceSessionId":"child"}',
  });
  expect(localStorage.getItem("astrid.practice.v1.latest")).toBe(other.practiceSessionId);
  expect(loadRunRecovery(child.practiceSessionId)).toEqual(child);
  setItem.mockRestore();
});
