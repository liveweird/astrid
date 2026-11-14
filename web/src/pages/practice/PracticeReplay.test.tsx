import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import {
  type AttemptRequestLike, choosePractice, completedState, context, exercise, gapAttempt, gapState, json, owner, practiceId,
  profiles, resetPracticeBrowser, state, wrongAttempt,
} from "../../test/practiceFixtures";
import { renderWithProviders } from "../../test/render";
import Practice from "./Practice";
import { listOutbox, saveOutboxEntry, saveRunRecovery } from "./practiceRecovery";

beforeEach(resetPracticeBrowser);

it.each(["cumulative-review-1", "cumulative-review-2", "cumulative-review-3"] as const)(
  "routes completed %s back to a fresh preparation instead of offering unsupported follow-ups",
  async (planRevision) => {
  const run = {
    ownerSessionId: owner, practiceSessionId: practiceId,
    focusId: "cumulative-review", planRevision, seed: 47,
  };
  saveRunRecovery(run);
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.includes(`/practice-sessions/${practiceId}`)) return json(state({
      ...completedState(), focusId: "cumulative-review", planRevision,
      title: "Mixed review from your lessons", introductionRequired: true,
    }));
    throw new Error(`Unexpected ${path}`);
  }));

  renderWithProviders(<Practice />);

  expect(await screen.findByRole("heading", { name: "Practice complete" })).toBeVisible();
  expect(screen.queryByRole("checkbox")).not.toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Start selected review" })).not.toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Repeat this session" })).not.toBeInTheDocument();
  expect(screen.getByText(/keeps the cross-session exposure snapshot/)).toBeVisible();
  expect(screen.getAllByText(/with possible prior exposure in the saved cross-session snapshot/)).not.toHaveLength(0);
  await userEvent.click(screen.getByRole("button", { name: "Prepare another mixed review" }));
  expect(window.location.pathname).toBe("/");
  expect(window.location.hash).toBe("#review");
});

it("restores an uncertain gap answer in its exact saved order and freezes the board", async () => {
  const run = { ownerSessionId: owner, practiceSessionId: practiceId, focusId: "verb-article-gaps", planRevision: "verb-article-gaps-1", seed: 47 };
  saveRunRecovery(run);
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  const pending = {
    attemptId: "77777777-7777-4777-8777-777777777777",
    exerciseId: gapAttempt.exerciseId,
    answer: { kind: "gaps", selections: [{ slotId: "g1", choiceId: "c1" }, { slotId: "g0", choiceId: "c0" }] },
    hintUsed: false,
  };
  const body = JSON.stringify(pending);
  saveOutboxEntry({ ...run, kind: "attempt", identity: pending.attemptId, body, queuedAt: 1 });
  const bodies: string[] = [];
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith("/attempts")) {
      bodies.push(String(init?.body));
      throw new TypeError("response lost");
    }
    if (path.includes("/practice-sessions/")) return json(gapState());
    throw new Error(`Unexpected ${path}`);
  }));

  renderWithProviders(<Practice />);
  expect(await screen.findByText("Your answer is not confirmed")).toBeVisible();
  expect(screen.getByRole("button", { name: "en" })).toHaveAttribute("aria-pressed", "true");
  expect(screen.getByRole("button", { name: "læse" })).toHaveAttribute("aria-pressed", "true");
  expect(screen.getByRole("button", { name: "en" })).toBeDisabled();
  expect(screen.getByRole("button", { name: "Check gaps" })).toBeDisabled();
  await userEvent.click(screen.getByRole("button", { name: "Retry saved operation" }));
  expect(await screen.findByRole("button", { name: "en" })).toHaveAttribute("aria-pressed", "true");
  expect(screen.getByRole("button", { name: "læse" })).toHaveAttribute("aria-pressed", "true");
  expect(bodies).toEqual([body, body]);
  expect(JSON.parse(bodies[0]!).answer.selections).toEqual(pending.answer.selections);
});

it("unlocks a reloaded gap item after its pending attempt replays successfully", async () => {
  const run = { ownerSessionId: owner, practiceSessionId: practiceId, focusId: "verb-article-gaps", planRevision: "verb-article-gaps-1", seed: 47 };
  saveRunRecovery(run);
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  const selections = [{ slotId: "g1", choiceId: "c1" }, { slotId: "g0", choiceId: "c0" }];
  const pending = {
    attemptId: "77777777-7777-4777-8777-777777777777", exerciseId: gapAttempt.exerciseId,
    answer: { kind: "gaps", selections }, hintUsed: false,
  };
  saveOutboxEntry({ ...run, kind: "attempt", identity: pending.attemptId, body: JSON.stringify(pending), queuedAt: 1 });
  let serverState = gapState();
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith("/attempts")) {
      const saved = { ...gapAttempt, attemptId: pending.attemptId };
      serverState = gapState({ items: [{ ...gapState().items[0]!, attemptIds: [pending.attemptId], attemptCount: 1,
        firstAttempt: saved, lastAttempt: saved, firstAnswer: pending.answer, lastAnswer: pending.answer }] });
      return json(saved);
    }
    if (path.includes("/practice-sessions/")) return json(serverState);
    throw new Error(`Unexpected ${path}`);
  }));

  renderWithProviders(<Practice />);
  expect(await screen.findByRole("status")).toHaveTextContent("Review both selected forms.");
  expect(screen.getByRole("button", { name: "Try again" })).toBeEnabled();
  expect(screen.getByRole("button", { name: "Continue" })).toBeEnabled();
  expect(screen.queryByText("Your answer is not confirmed")).not.toBeInTheDocument();
});

it("replays an uncertain answer byte-for-byte after refresh and blocks editing meanwhile", async () => {
  const run = { ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 };
  saveRunRecovery(run);
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  let failAttempt = true;
  const attemptBodies: string[] = [];
  let serverState = state();
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith("/attempts")) {
      attemptBodies.push(String(init?.body));
      if (failAttempt) throw new TypeError("connection closed");
      const request = JSON.parse(String(init?.body)) as AttemptRequestLike;
      const saved = { ...wrongAttempt, attemptId: request.attemptId };
      serverState = state({ items: [{ ...serverState.items[0]!, attemptIds: [request.attemptId], attemptCount: 1, firstAttempt: saved, lastAttempt: saved }] });
      return json(saved);
    }
    if (path.includes(`/practice-sessions/${practiceId}`)) return json(serverState);
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  const first = renderWithProviders(<Practice />);
  await user.click(await screen.findByRole("button", { name: "dog" }));
  await user.click(screen.getByRole("button", { name: "Check answer" }));
  expect(await screen.findByText("Your answer is not confirmed")).toBeVisible();
  expect(screen.getByRole("button", { name: "dog" })).toBeDisabled();
  expect(screen.getByRole("button", { name: "Check answer" })).toBeDisabled();
  expect(screen.getByRole("button", { name: "Session introduction" })).toBeDisabled();
  expect(screen.getByRole("button", { name: "Learn this first" })).toBeDisabled();
  expect(screen.getByRole("button", { name: "Show hint" })).toBeDisabled();
  expect(screen.getByRole("button", { name: "Start new practice" })).toBeEnabled();
  expect(screen.getByRole("button", { name: "Retry saved operation" })).toBeVisible();
  first.unmount();

  const second = renderWithProviders(<Practice />);
  expect(await screen.findByText("Your answer is not confirmed")).toBeVisible();
  expect(screen.getByRole("button", { name: "dog" })).toHaveAttribute("aria-pressed", "true");
  expect(screen.getByRole("button", { name: "dog" })).toBeDisabled();
  second.unmount();

  failAttempt = false;
  renderWithProviders(<Practice />);
  expect(await screen.findByRole("status")).toHaveTextContent("Look at the word once more.");
  expect(attemptBodies).toHaveLength(3);
  expect(attemptBodies[1]).toBe(attemptBodies[0]);
  expect(attemptBodies[2]).toBe(attemptBodies[0]);
});

it("restores a confirmed checked answer and its frozen exposure context on refresh", async () => {
  const run = { ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 };
  saveRunRecovery(run);
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  const item = {
    ...state().items[0]!, attemptIds: [wrongAttempt.attemptId], attemptCount: 1,
    firstAttempt: wrongAttempt, lastAttempt: wrongAttempt,
    firstAnswer: { kind: "text", text: "dog" }, lastAnswer: { kind: "text", text: "dog" },
    lastAttemptContext: {
      introductionOpened: false, lessonOpened: false, hintOpened: false,
      priorExposure: [{ position: 0, source: "prompt", keys: ["gloss:L014-S01"] }],
    },
  };
  const checked = state({ introductionOpened: true, items: [item] });
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.includes("/practice-sessions/")) return json(checked);
    throw new Error(`Unexpected ${path}`);
  }));

  renderWithProviders(<Practice />);
  const selected = await screen.findByRole("button", { name: "dog" });
  expect(selected).toHaveAttribute("aria-pressed", "true");
  expect(selected).toBeDisabled();
  expect(screen.getByRole("status")).toHaveTextContent("Look at the word once more.");
  expect(screen.getByText(/possible earlier exposure/)).toBeVisible();
});

it("keeps a checked answer visible and retries an uncertain advance exactly", async () => {
  const run = { ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 };
  saveRunRecovery(run);
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  const item = {
    ...state().items[0]!, attemptIds: [wrongAttempt.attemptId], attemptCount: 1,
    firstAttempt: wrongAttempt, lastAttempt: wrongAttempt,
    firstAnswer: { kind: "text", text: "dog" }, lastAnswer: { kind: "text", text: "dog" },
  };
  const checked = state({ items: [item] });
  const advanceBodies: string[] = [];
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith("/advance")) {
      advanceBodies.push(String(init?.body));
      if (advanceBodies.length === 1) throw new TypeError("connection closed");
      return json(completedState());
    }
    if (path.includes("/practice-sessions/")) return json(checked);
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);
  await user.click(await screen.findByRole("button", { name: "Continue" }));

  expect(await screen.findByRole("button", { name: "Retry saved operation" })).toBeVisible();
  expect(screen.getByRole("button", { name: "dog" })).toHaveAttribute("aria-pressed", "true");
  expect(screen.getByRole("button", { name: "Continue" })).toBeDisabled();
  expect(screen.getByRole("button", { name: "Start new practice" })).toBeEnabled();
  await user.click(screen.getByRole("button", { name: "Retry saved operation" }));

  expect(await screen.findByRole("heading", { name: "Practice complete" })).toBeVisible();
  expect(advanceBodies).toHaveLength(2);
  expect(advanceBodies[1]).toBe(advanceBodies[0]);
});

it("retains checked state when advance persistence fails before send", async () => {
  const run = { ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 };
  saveRunRecovery(run);
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  const checked = state({ items: [{
    ...state().items[0]!, firstAttempt: wrongAttempt, lastAttempt: wrongAttempt,
    firstAnswer: { kind: "text", text: "dog" }, lastAnswer: { kind: "text", text: "dog" },
  }] });
  const fetchMock = vi.fn(async (path: string) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.includes("/practice-sessions/")) return json(checked);
    throw new Error(`No mutation expected: ${path}`);
  });
  vi.stubGlobal("fetch", fetchMock);
  const user = userEvent.setup();
  renderWithProviders(<Practice />);
  const continueButton = await screen.findByRole("button", { name: "Continue" });
  const setItem = vi.spyOn(localStorage, "setItem").mockImplementation(() => { throw new DOMException("full", "QuotaExceededError"); });
  await user.click(continueButton);

  expect(await screen.findByText("This browser could not save recovery data. The operation was not sent.")).toBeVisible();
  expect(screen.getByRole("button", { name: "dog" })).toHaveAttribute("aria-pressed", "true");
  expect(screen.getByRole("button", { name: "Continue" })).toBeEnabled();
  expect(screen.queryByRole("button", { name: "Retry saved operation" })).not.toBeInTheDocument();
  expect(fetchMock).toHaveBeenCalledTimes(2);
  setItem.mockRestore();
});

it.each([409, 401])("unlocks an uncertain attempt after definitive HTTP %i", async (status) => {
  const run = { ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 };
  saveRunRecovery(run);
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  let attempts = 0;
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith("/attempts")) {
      attempts += 1;
      if (attempts === 1) throw new TypeError("connection closed");
      return json({ detail: status === 409 ? "cursor changed" : "expired" }, status);
    }
    if (path.includes("/practice-sessions/")) return json(state());
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);
  await user.click(await screen.findByRole("button", { name: "dog" }));
  await user.click(screen.getByRole("button", { name: "Check answer" }));
  await user.click(await screen.findByRole("button", { name: "Retry saved operation" }));
  await waitFor(() => expect(screen.queryByRole("button", { name: "Retry saved operation" })).not.toBeInTheDocument());
  expect(screen.getByRole("button", { name: "dog" })).toBeEnabled();
  expect(screen.getAllByRole("button", { name: "Start new practice" }).some((button) => !button.hasAttribute("disabled"))).toBe(true);
});

it("marks recovery unavailable when the pre-retry state read is definitively expired", async () => {
  const run = { ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 };
  saveRunRecovery(run);
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  let expired = false;
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith("/attempts")) { expired = true; throw new TypeError("connection closed"); }
    if (path.includes("/practice-sessions/")) return expired ? json({ detail: "expired" }, 401) : json(state());
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);
  await user.click(await screen.findByRole("button", { name: "dog" }));
  await user.click(screen.getByRole("button", { name: "Check answer" }));
  await user.click(await screen.findByRole("button", { name: "Retry saved operation" }));

  expect(await screen.findByText("This practice session expired. Start a new practice to continue.")).toBeVisible();
  expect(screen.queryByRole("button", { name: "Retry saved operation" })).not.toBeInTheDocument();
  expect(screen.getAllByRole("button", { name: "Start new practice" }).every((button) => !button.hasAttribute("disabled"))).toBe(true);
  expect(listOutbox(practiceId)).toHaveLength(1);
});

it("disables every new-practice action while an unavailable run resolves an in-flight conflict", async () => {
  const run = { ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 };
  saveRunRecovery(run);
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  let attempts = 0;
  let contextReads = 0;
  let resolveConflict: ((response: Response) => void) | undefined;
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("practice-context")) {
      contextReads += 1;
      if (contextReads === 2) return new Promise<Response>((resolve) => { resolveConflict = resolve; });
      return json(context());
    }
    if (path.endsWith("/attempts")) {
      attempts += 1;
      return attempts === 1 ? json({ detail: "expired" }, 401) : json({ detail: "cursor changed" }, 409);
    }
    if (path.includes("/practice-sessions/")) return json(state());
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);
  await user.click(await screen.findByRole("button", { name: "dog" }));
  await user.click(screen.getByRole("button", { name: "Check answer" }));
  await screen.findByText("This practice session expired. Start a new practice to continue.");
  await user.click(screen.getByRole("button", { name: "dog" }));
  await user.click(screen.getByRole("button", { name: "Check answer" }));
  await screen.findByText("This practice changed in another tab. Its latest saved state is shown.");
  expect(screen.getAllByRole("button", { name: "Start new practice" }).every((button) => button.hasAttribute("disabled"))).toBe(true);
  resolveConflict?.(json(context()));
  await waitFor(() => expect(screen.getAllByRole("button", { name: "Start new practice" }).every((button) => !button.hasAttribute("disabled"))).toBe(true));
  expect(new URLSearchParams(window.location.search).get("session")).toBe(practiceId);
});

it.each([false, true])("drains later operations after a pending attempt succeeds (complete: %s)", async (complete) => {
  const run = { ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 };
  saveRunRecovery(run);
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  const first = { attemptId: "ffffffff-ffff-4fff-8fff-ffffffffffff", exerciseId: exercise.id, answer: { kind: "text", text: "dog" }, hintUsed: false };
  const second = { attemptId: "00000000-0000-4000-8000-000000000000", exerciseId: exercise.id, answer: { kind: "text", text: "house" }, hintUsed: false };
  saveOutboxEntry({ ...run, kind: "attempt", identity: first.attemptId, body: JSON.stringify(first), queuedAt: 1 });
  saveOutboxEntry({ ...run, kind: "attempt", identity: second.attemptId, body: JSON.stringify(second), queuedAt: 2 });
  if (complete) saveOutboxEntry({ ...run, kind: "advance", identity: `${practiceId}-0`, body: '{"position":0}', queuedAt: 3 });
  const bodies: string[] = [];
  let fail = true;
  let serverState = state();
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith("/attempts")) {
      bodies.push(String(init?.body));
      if (fail) { fail = false; throw new TypeError("connection closed"); }
      const request = JSON.parse(String(init?.body)) as typeof first;
      const saved = { ...wrongAttempt, attemptId: request.attemptId };
      serverState = state({ items: [{ ...state().items[0]!, firstAttempt: saved, lastAttempt: saved,
        firstAnswer: request.answer, lastAnswer: request.answer }] });
      return json(saved);
    }
    if (path.endsWith("/advance")) { serverState = completedState(); return json(serverState); }
    if (path.includes("/practice-sessions/")) return json(serverState);
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);
  await user.click(await screen.findByRole("button", { name: "Retry saved operation" }));
  if (complete) await waitFor(() => expect(screen.getByRole("heading", { name: "Practice complete" })).toHaveFocus());
  else expect(await screen.findByRole("button", { name: "Continue" })).toBeEnabled();
  expect(bodies.map((body) => JSON.parse(body).attemptId)).toEqual([first.attemptId, first.attemptId, second.attemptId]);
});

it("keeps the immutable saved attempt when an identity is reused with another body", async () => {
  const run = { ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 };
  saveRunRecovery(run);
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  const fixedId = "77777777-7777-4777-8777-777777777777";
  const fetchMock = vi.fn(async (path: string) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.includes("/practice-sessions/")) return json(state());
    throw new Error(`No attempt should be sent: ${path}`);
  });
  vi.stubGlobal("fetch", fetchMock);
  const user = userEvent.setup();
  renderWithProviders(<Practice />);
  await screen.findByRole("button", { name: "dog" });
  saveOutboxEntry({ ...run, kind: "attempt", identity: fixedId, body: JSON.stringify({
    attemptId: fixedId, exerciseId: exercise.id, answer: { kind: "text", text: "house" }, hintUsed: false,
  }) });
  vi.spyOn(crypto, "randomUUID").mockReturnValue(fixedId);
  await user.click(screen.getByRole("button", { name: "dog" }));
  await user.click(screen.getByRole("button", { name: "Check answer" }));

  expect(await screen.findByText("Saved recovery data conflicts with this operation. Nothing was sent.")).toBeVisible();
  expect(screen.getByRole("button", { name: "house" })).toHaveAttribute("aria-pressed", "true");
  expect(screen.getByRole("button", { name: "Retry saved operation" })).toBeVisible();
  expect(fetchMock).toHaveBeenCalledTimes(2);
});

it("never reassigns saved recovery when the authenticated owner rotates", async () => {
  saveRunRecovery({ ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 });
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  const fetchMock = vi.fn(async (path: string) => {
    if (path.endsWith("practice-context")) return json(context("99999999-9999-4999-8999-999999999999"));
    throw new Error(`Recovery must not call ${path}`);
  });
  vi.stubGlobal("fetch", fetchMock);

  renderWithProviders(<Practice />);
  const ownerAlert = await screen.findByRole("alert");
  expect(ownerAlert).toHaveTextContent("This saved practice belongs to an expired browser session. It was not reassigned or replayed.");
  await waitFor(() => expect(ownerAlert).toHaveFocus());
  expect(screen.getByRole("button", { name: "Start new practice" })).toBeVisible();
  expect(fetchMock).toHaveBeenCalledTimes(1);
});

it("does not send creation when local recovery cannot be persisted", async () => {
  const fetchMock = vi.fn(async (path: string) => {
    if (path.endsWith("development-session")) return json({ mode: "development" });
    if (path.endsWith("practice-profiles")) return json(profiles());
    throw new Error(`Creation must not call ${path}`);
  });
  vi.stubGlobal("fetch", fetchMock);
  const user = userEvent.setup();
  renderWithProviders(<Practice />);
  await screen.findByRole("heading", { name: "Choose your practice" });
  const setItem = vi.spyOn(localStorage, "setItem").mockImplementation(() => { throw new DOMException("full", "QuotaExceededError"); });
  await choosePractice(user);
  expect(await screen.findByText("This browser could not save recovery data. The operation was not sent.")).toBeVisible();
  expect(fetchMock).toHaveBeenCalledTimes(3);
  setItem.mockRestore();
});

it("explains a missing tab-bound run instead of silently starting another", async () => {
  window.history.replaceState(null, "", "/practice?session=missing-run");
  const fetchMock = vi.fn();
  vi.stubGlobal("fetch", fetchMock);
  renderWithProviders(<Practice />);
  expect(await screen.findByText("This tab’s saved practice record is unavailable. Start a new practice when you are ready.")).toBeVisible();
  expect(fetchMock).not.toHaveBeenCalled();
  fetchMock.mockImplementation(async (path: string) => {
    if (path.endsWith("development-session")) return json({ mode: "development" });
    if (path.endsWith("practice-profiles")) return json(profiles());
    if (path.endsWith("practice-sessions")) return json(state(), 201);
    throw new Error(`Unexpected ${path}`);
  });
  await userEvent.click(screen.getByRole("button", { name: "Start new practice" }));
  const user = userEvent.setup();
  await choosePractice(user);
  expect(await screen.findByRole("heading", { name: "Words and simple statements" })).toBeVisible();
});

it("reconciles a pending creation exactly without renewing its owner", async () => {
  const run = { ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 };
  saveRunRecovery(run);
  const body = JSON.stringify({ practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 });
  saveOutboxEntry({ ...run, kind: "creation", identity: practiceId, body });
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  const calls: Array<{ path: string; body?: string }> = [];
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    calls.push({ path, body: init?.body?.toString() });
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith("practice-sessions")) return json(state());
    throw new Error(`Unexpected ${path}`);
  }));

  renderWithProviders(<Practice />);
  expect(await screen.findByRole("heading", { name: "Words and simple statements" })).toBeVisible();
  expect(calls.map((call) => call.path)).toEqual(["/api/v1/practice-context", "/api/v1/practice-sessions"]);
  expect(calls[1]?.body).toBe(body);
});

it("reconciles saved support, attempt, and advance in dependency order", async () => {
  const run = { ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 };
  saveRunRecovery(run);
  const support = { ...run, kind: "support" as const, identity: "hint-0", body: '{"kind":"hint","position":0}' };
  const attemptEntry = { ...run, kind: "attempt" as const, identity: wrongAttempt.attemptId,
    body: JSON.stringify({ attemptId: wrongAttempt.attemptId, exerciseId: exercise.id, answer: { kind: "text", text: "dog" }, hintUsed: true }) };
  const advance = { ...run, kind: "advance" as const, identity: `${practiceId}-0`, body: '{"position":0}' };
  saveOutboxEntry(advance); saveOutboxEntry(attemptEntry); saveOutboxEntry(support);
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  const mutations: string[] = [];
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith("/support")) { mutations.push("support"); return json(state()); }
    if (path.endsWith("/attempts")) { mutations.push("attempt"); return json(wrongAttempt); }
    if (path.endsWith("/advance")) { mutations.push("advance"); return json(completedState()); }
    if (path.includes("/practice-sessions/")) return json(state());
    throw new Error(`Unexpected ${path} ${String(init?.body)}`);
  }));

  renderWithProviders(<Practice />);
  expect(await screen.findByRole("heading", { name: "Practice complete" })).toBeVisible();
  expect(mutations).toEqual(["support", "attempt", "advance"]);
});

it("refreshes the current item after a same-owner stale answer conflict", async () => {
  const run = { ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 };
  saveRunRecovery(run);
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  let reads = 0;
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith("/attempts")) return json({ detail: "cursor changed" }, 409);
    if (path.includes("/practice-sessions/")) { reads += 1; return json(reads === 1 ? state() : completedState()); }
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);
  await user.click(await screen.findByRole("button", { name: "dog" }));
  await user.click(screen.getByRole("button", { name: "Check answer" }));
  expect(await screen.findByText("Another tab continued this practice. Your new answer was not recorded; the latest saved item is shown.")).toBeVisible();
  expect(screen.getByRole("heading", { name: "Practice complete" })).toBeVisible();
});

it("keeps expired recovery unavailable on an authenticated read failure", async () => {
  saveRunRecovery({ ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 });
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  vi.stubGlobal("fetch", vi.fn(async () => json({ detail: "expired" }, 401)));
  renderWithProviders(<Practice />);
  expect(await screen.findByText("This practice session expired. Start a new practice to continue.")).toBeVisible();
  expect(screen.getByRole("button", { name: "Start new practice" })).toBeVisible();
});

it.each([
  [403, "different browser session"],
  [404, "no longer available"],
  [418, "teapot"],
] as const)("explains recovery HTTP %i without replaying mutations", async (status, message) => {
  saveRunRecovery({ ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 });
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  const fetchMock = vi.fn(async () => json({ detail: message }, status));
  vi.stubGlobal("fetch", fetchMock);
  renderWithProviders(<Practice />);
  expect(await screen.findByText(new RegExp(message))).toBeVisible();
  expect(fetchMock).toHaveBeenCalledTimes(1);
});

it("focuses unavailable recovery instead of replaying a foreign-owner queued operation", async () => {
  const run = { ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 };
  saveRunRecovery(run);
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.includes("/practice-sessions/")) return json(state());
    throw new Error(`No mutation should be sent: ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);
  await user.click(await screen.findByRole("button", { name: "dog" }));
  saveOutboxEntry({ ...run, ownerSessionId: "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
    kind: "attempt", identity: "foreign-operation", body: "{}", queuedAt: 1 });
  await user.click(screen.getByRole("button", { name: "Check answer" }));
  await waitFor(() => expect(screen.getByRole("alert")).toHaveFocus());
  expect(screen.getByRole("alert")).toHaveTextContent("will not be replayed under a new owner");
});
