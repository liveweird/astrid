import { MantineProvider } from "@mantine/core";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { StrictMode } from "react";
import Practice from "./Practice";
import { listOutbox, saveOutboxEntry, saveRunRecovery } from "./practiceRecovery";

const owner = "11111111-1111-4111-8111-111111111111";
const practiceId = "22222222-2222-4222-8222-222222222222";
const exercise = {
  id: "33333333-3333-4333-8333-333333333333", formatId: "E007", lexicalTarget: {
    entryId: "L014", senseId: "L014-S01", facet: "meaning", direction: "da-en",
  }, supportSkillIds: [], contentRevision: "pilot-3", generatorRevision: "generator-3",
  instruction: "Choose the English meaning.", context: "Read the Danish word.",
  prompt: { before: "", after: "", choices: ["book", "dog", "house"], blocks: [], punctuation: ".", cue: "bog", cueLanguage: "da", answerLanguage: "en" },
  introduction: [{ title: "bog · book", body: "A noun for something you read." }], hint: "Think of something with pages.",
};
const wrongAttempt = {
  attemptId: "44444444-4444-4444-8444-444444444444", exerciseId: exercise.id,
  targetSatisfaction: "not-met", linguisticStatus: "acceptable", linguisticScope: "english-choice",
  issues: [], feedback: "Look at the word once more.", completedSentence: "",
  evaluationRevision: "evaluation-3", hintUsed: true, retryNumber: 1, createdAt: 1,
};
const outcomes = { fullyMet: 0, targetMet: 0, targetNotMet: 1, unevaluable: 0, languageUnknown: 0, languageKnownIssue: 0, responseConstraintNotMet: 0 };

function state(overrides: Record<string, unknown> = {}) {
  return {
    id: practiceId, ownerSessionId: owner, focusId: "reading-basics", title: "Words and simple statements",
    planRevision: "practice-1", seed: 47, contentRevision: "pilot-3", generatorRevision: "generator-3",
    evaluationRevision: "evaluation-3", sessionExpiresAt: 9999999999999, state: "active", cursor: 0,
    itemCount: 1, prerequisiteSkillIds: ["G001"],
    introduction: [{ title: "A short introduction", body: "Use the support when you want it." }],
    introductionOpened: false,
    items: [{ position: 0, exerciseId: exercise.id, formatId: "E007", lexicalTarget: exercise.lexicalTarget,
      lessonOpened: false, hintOpened: false, attemptIds: [], attemptCount: 0,
      firstAttempt: null, lastAttempt: null, firstAttemptContext: null, lastAttemptContext: null }],
    currentExercise: exercise,
    summary: { plannedItems: 1, attemptedItems: 0, completedItems: 0, attemptCount: 0, retryCount: 0,
      introductionOpened: false, rows: [] },
    ...overrides,
  };
}

function completedState() {
  return state({
    state: "completed", cursor: 1, currentExercise: null, introductionOpened: true,
    summary: { plannedItems: 1, attemptedItems: 1, completedItems: 1, attemptCount: 2, retryCount: 1,
      introductionOpened: true, rows: [{ label: "book · meaning", lexicalTarget: {
        entryId: "L014", senseId: "L014-S01", facet: "meaning", direction: "da-en",
      }, plannedItems: 1, attemptedItems: 1, advancedItems: 1,
        attemptCount: 2, retryCount: 1, firstOutcomes: outcomes, latestOutcomes: outcomes,
        firstWithSupport: 1, firstWithPriorExposure: 1, lessonOpenedItems: 1, hintOpenedItems: 1 },
      { label: "Subject-first statements", targetSkillId: "G005", plannedItems: 1, attemptedItems: 1,
        advancedItems: 1, attemptCount: 1, retryCount: 0, firstOutcomes: outcomes, latestOutcomes: outcomes,
        firstWithSupport: 0, firstWithPriorExposure: 0, lessonOpenedItems: 0, hintOpenedItems: 0 }] },
  });
}

function json(body: unknown, status = 200) {
  return Response.json(body, { status, headers: { "Content-Type": "application/json" } });
}

function context(ownerSessionId = owner) {
  return { ownerSessionId, sessionExpiresAt: 9999999999999, focusId: "reading-basics", title: "Words and simple statements", planRevision: "practice-1", itemCount: 1 };
}

beforeEach(() => {
  localStorage.clear();
  window.history.replaceState(null, "", "/practice");
  vi.restoreAllMocks();
});

it("records support before revealing it, permits retries after wrong answers, and completes honestly", async () => {
  let serverState = state();
  const sent: Array<{ path: string; body: string }> = [];
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    const body = String(init?.body ?? "");
    if (body) sent.push({ path, body });
    if (path.endsWith("development-session")) return json({ mode: "development" });
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith("practice-sessions") && init?.method === "POST") return json(serverState, 201);
    if (path.endsWith("/support")) {
      const request = JSON.parse(body) as { kind: string };
      const item = serverState.items[0]!;
      serverState = state({
        introductionOpened: request.kind === "introduction" || serverState.introductionOpened,
        items: [{ ...item, lessonOpened: request.kind === "lesson" || item.lessonOpened, hintOpened: request.kind === "hint" || item.hintOpened }],
      });
      return json(serverState);
    }
    if (path.endsWith("/attempts")) {
      const request = JSON.parse(body) as { attemptId: string; hintUsed: boolean };
      const saved = { ...wrongAttempt, attemptId: request.attemptId, hintUsed: request.hintUsed };
      const old = serverState.items[0]!;
      serverState = state({ introductionOpened: serverState.introductionOpened, items: [{ ...old,
        attemptIds: [...old.attemptIds, request.attemptId], attemptCount: old.attemptCount + 1,
        firstAttempt: old.firstAttempt ?? saved, lastAttempt: saved,
        lastAttemptContext: { introductionOpened: false, lessonOpened: true, hintOpened: false, priorExposure: [] },
      }], summary: serverState.summary });
      return json(saved);
    }
    if (path.endsWith("/advance")) { serverState = completedState(); return json(serverState); }
    if (path.includes("/practice-sessions/")) return json(serverState);
    throw new Error(`Unexpected ${path}`);
  }));

  const user = userEvent.setup();
  render(<MantineProvider env="test"><Practice /></MantineProvider>);
  expect(await screen.findByRole("heading", { name: "Words and simple statements" })).toBeVisible();

  await user.click(screen.getByRole("button", { name: "Session introduction" }));
  expect(await screen.findByText("Use the support when you want it.")).toBeVisible();
  expect(sent.find((request) => request.path.endsWith("/support"))?.body).toBe('{"kind":"introduction"}');
  await user.click(screen.getByRole("button", { name: "Session introduction" }));
  expect(screen.queryByText("Use the support when you want it.")).not.toBeInTheDocument();

  await user.click(screen.getByRole("button", { name: "Learn this first" }));
  expect(await screen.findByText("A noun for something you read.")).toBeVisible();
  await user.click(screen.getByRole("button", { name: "Show hint" }));
  expect(await screen.findByText("Think of something with pages.")).toBeVisible();
  await user.click(screen.getByRole("button", { name: "dog" }));
  await user.click(screen.getByRole("button", { name: "Check answer" }));
  expect(await screen.findByRole("status")).toHaveTextContent("Look at the word once more.");
  const attempts = sent.filter((request) => request.path.endsWith("/attempts"));
  expect(JSON.parse(attempts[0]!.body)).toMatchObject({ answer: { kind: "text", text: "dog" }, hintUsed: true });

  await user.click(screen.getByRole("button", { name: "Try again" }));
  await user.click(screen.getByRole("button", { name: "house" }));
  await user.click(screen.getByRole("button", { name: "Check answer" }));
  await waitFor(() => expect(sent.filter((request) => request.path.endsWith("/attempts"))).toHaveLength(2));
  await user.click(screen.getByRole("button", { name: "Continue" }));

  const summary = await screen.findByRole("heading", { name: "Practice complete" });
  await waitFor(() => expect(summary).toHaveFocus());
  expect(screen.getByText("This records what you attempted, not a mastery judgment.")).toBeVisible();
  expect(screen.getByRole("article", { name: /book · meaning/ })).toHaveTextContent("0 fully met");
  expect(screen.getByRole("article", { name: /book · meaning/ })).toHaveTextContent("1 target not met");
});

it("focuses each support disclosure only after its durable acknowledgement", async () => {
  const run = { ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 };
  saveRunRecovery(run);
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  const sessionIntroduction = [
    { title: "A short introduction", body: "Use the support when you want it." },
    { title: "A second introduction", body: "Keep going at your own pace." },
  ];
  const lessonIntroduction = [
    ...exercise.introduction,
    { title: "A second example", body: "The word can name a printed work." },
  ];
  let serverState = state({
    introduction: sessionIntroduction,
    currentExercise: { ...exercise, introduction: lessonIntroduction },
  });
  let releaseSupport: (() => void) | null = null;
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith("/support")) {
      const request = JSON.parse(String(init?.body)) as { kind: "introduction" | "lesson" | "hint" };
      return new Promise<Response>((resolve) => {
        releaseSupport = () => {
          const item = serverState.items[0]!;
          serverState = state({
            introduction: sessionIntroduction,
            currentExercise: { ...exercise, introduction: lessonIntroduction },
            introductionOpened: request.kind === "introduction" || serverState.introductionOpened,
            items: [{
              ...item,
              lessonOpened: request.kind === "lesson" || item.lessonOpened,
              hintOpened: request.kind === "hint" || item.hintOpened,
            }],
          });
          resolve(json(serverState));
        };
      });
    }
    if (path.includes("/practice-sessions/")) return json(serverState);
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  render(<MantineProvider env="test"><Practice /></MantineProvider>);
  const elsewhere = document.createElement("button");
  document.body.append(elsewhere);

  const introduction = await screen.findByRole("button", { name: "Session introduction" });
  await user.click(introduction);
  await waitFor(() => expect(releaseSupport).not.toBeNull());
  elsewhere.focus();
  releaseSupport!(); releaseSupport = null;
  const introductionContent = await screen.findByText("Use the support when you want it.");
  await waitFor(() => expect(introductionContent).toHaveFocus());

  const lesson = screen.getByRole("button", { name: "Learn this first" });
  await user.click(lesson);
  await waitFor(() => expect(releaseSupport).not.toBeNull());
  elsewhere.focus();
  releaseSupport!(); releaseSupport = null;
  const lessonContent = await screen.findByText("A noun for something you read.");
  await waitFor(() => expect(lessonContent).toHaveFocus());

  await user.click(screen.getByRole("button", { name: "Show hint" }));
  await waitFor(() => expect(releaseSupport).not.toBeNull());
  elsewhere.focus();
  releaseSupport!();
  const hint = await screen.findByText("Think of something with pages.");
  await waitFor(() => expect(hint.closest("[data-practice-focus='hint']")).toHaveFocus());
});

it.each(["introduction", "lesson", "hint"] as const)(
  "restores %s focus after manually retrying a committed support response loss",
  async (kind) => {
    const run = { ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 };
    saveRunRecovery(run);
    window.history.replaceState(null, "", `/practice?session=${practiceId}`);
    let serverState = state();
    const supportBodies: string[] = [];
    vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
      if (path.endsWith("practice-context")) return json(context());
      if (path.endsWith("/support")) {
        supportBodies.push(String(init?.body));
        const item = serverState.items[0]!;
        serverState = state({
          introductionOpened: kind === "introduction" || serverState.introductionOpened,
          items: [{
            ...item,
            lessonOpened: kind === "lesson" || item.lessonOpened,
            hintOpened: kind === "hint" || item.hintOpened,
          }],
        });
        if (supportBodies.length === 1) throw new TypeError("response lost after commit");
        return json(serverState);
      }
      if (path.includes("/practice-sessions/")) return json(serverState);
      throw new Error(`Unexpected ${path}`);
    }));
    const user = userEvent.setup();
    render(<MantineProvider env="test"><Practice /></MantineProvider>);
    const triggerName = kind === "introduction" ? "Session introduction" : kind === "lesson" ? "Learn this first" : "Show hint";
    await user.click(await screen.findByRole("button", { name: triggerName }));
    await user.click(await screen.findByRole("button", { name: "Retry saved operation" }));

    const target = await waitFor(() => {
      const element = document.querySelector<HTMLElement>(`[data-practice-focus='${kind}']`);
      expect(element).toHaveFocus();
      return element;
    });
    expect(target).toBeVisible();
    expect(supportBodies).toHaveLength(2);
    expect(supportBodies[1]).toBe(supportBodies[0]);
  },
);

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
  const first = render(<MantineProvider env="test"><Practice /></MantineProvider>);
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

  const second = render(<MantineProvider env="test"><Practice /></MantineProvider>);
  expect(await screen.findByText("Your answer is not confirmed")).toBeVisible();
  expect(screen.getByRole("button", { name: "dog" })).toHaveAttribute("aria-pressed", "true");
  expect(screen.getByRole("button", { name: "dog" })).toBeDisabled();
  second.unmount();

  failAttempt = false;
  render(<MantineProvider env="test"><Practice /></MantineProvider>);
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

  render(<MantineProvider env="test"><Practice /></MantineProvider>);
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
  render(<MantineProvider env="test"><Practice /></MantineProvider>);
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
  render(<MantineProvider env="test"><Practice /></MantineProvider>);
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

type AttemptRequestLike = { attemptId: string };

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
  render(<MantineProvider env="test"><Practice /></MantineProvider>);
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
  render(<MantineProvider env="test"><Practice /></MantineProvider>);
  await user.click(await screen.findByRole("button", { name: "dog" }));
  await user.click(screen.getByRole("button", { name: "Check answer" }));
  await user.click(await screen.findByRole("button", { name: "Retry saved operation" }));

  expect(await screen.findByText("This practice session expired. Start a new practice to continue.")).toBeVisible();
  expect(screen.queryByRole("button", { name: "Retry saved operation" })).not.toBeInTheDocument();
  expect(screen.getAllByRole("button", { name: "Start new practice" }).every((button) => !button.hasAttribute("disabled"))).toBe(true);
  expect(listOutbox(practiceId)).toHaveLength(1);
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
  render(<MantineProvider env="test"><Practice /></MantineProvider>);
  await user.click(await screen.findByRole("button", { name: "Retry saved operation" }));
  if (complete) await waitFor(() => expect(screen.getByRole("heading", { name: "Practice complete" })).toHaveFocus());
  else expect(await screen.findByRole("button", { name: "Continue" })).toBeEnabled();
  expect(bodies.map((body) => JSON.parse(body).attemptId)).toEqual([first.attemptId, first.attemptId, second.attemptId]);
});

it("allows explicit abandonment of an uncertain operation", async () => {
  const run = { ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 };
  saveRunRecovery(run);
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  let renewed = false;
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("development-session")) { renewed = true; return json({ mode: "development" }); }
    if (path.endsWith("practice-context")) return json(context(renewed ? "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa" : owner));
    if (path.endsWith("/attempts")) throw new TypeError("connection closed");
    if (path.endsWith("practice-sessions")) return json({ ...state(), id: "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb" }, 201);
    if (path.includes("/practice-sessions/")) return json(state());
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  render(<MantineProvider env="test"><Practice /></MantineProvider>);
  await user.click(await screen.findByRole("button", { name: "dog" }));
  await user.click(screen.getByRole("button", { name: "Check answer" }));
  const startNew = await screen.findByRole("button", { name: "Start new practice" });
  expect(startNew).toBeEnabled();
  await user.click(startNew);
  await waitFor(() => expect(new URLSearchParams(window.location.search).get("session")).not.toBe(practiceId));
  expect(localStorage.getItem(`astrid.practice.v1.run.${practiceId}`)).toBeNull();
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
  render(<MantineProvider env="test"><Practice /></MantineProvider>);
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

  render(<MantineProvider env="test"><Practice /></MantineProvider>);
  const ownerAlert = await screen.findByRole("alert");
  expect(ownerAlert).toHaveTextContent("This saved practice belongs to an expired browser session. It was not reassigned or replayed.");
  await waitFor(() => expect(ownerAlert).toHaveFocus());
  expect(screen.getByRole("button", { name: "Start new practice" })).toBeVisible();
  expect(fetchMock).toHaveBeenCalledTimes(1);
});

it("does not send creation when local recovery cannot be persisted", async () => {
  const setItem = vi.spyOn(localStorage, "setItem").mockImplementation(() => { throw new DOMException("full", "QuotaExceededError"); });
  const fetchMock = vi.fn(async (path: string) => {
    if (path.endsWith("development-session")) return json({ mode: "development" });
    if (path.endsWith("practice-context")) return json(context());
    throw new Error(`Creation must not call ${path}`);
  });
  vi.stubGlobal("fetch", fetchMock);
  render(<MantineProvider env="test"><Practice /></MantineProvider>);
  expect(await screen.findByText("This browser could not save recovery data. The operation was not sent.")).toBeVisible();
  expect(fetchMock).toHaveBeenCalledTimes(2);
  setItem.mockRestore();
});

it("explains a missing tab-bound run instead of silently starting another", async () => {
  window.history.replaceState(null, "", "/practice?session=missing-run");
  const fetchMock = vi.fn();
  vi.stubGlobal("fetch", fetchMock);
  render(<MantineProvider env="test"><Practice /></MantineProvider>);
  expect(await screen.findByText("This tab’s saved practice record is unavailable. Start a new practice when you are ready.")).toBeVisible();
  expect(fetchMock).not.toHaveBeenCalled();
  fetchMock.mockImplementation(async (path: string) => {
    if (path.endsWith("development-session")) return json({ mode: "development" });
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith("practice-sessions")) return json(state(), 201);
    throw new Error(`Unexpected ${path}`);
  });
  await userEvent.click(screen.getByRole("button", { name: "Start new practice" }));
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

  render(<MantineProvider env="test"><Practice /></MantineProvider>);
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

  render(<MantineProvider env="test"><Practice /></MantineProvider>);
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
  render(<MantineProvider env="test"><Practice /></MantineProvider>);
  await user.click(await screen.findByRole("button", { name: "dog" }));
  await user.click(screen.getByRole("button", { name: "Check answer" }));
  expect(await screen.findByText("Another tab continued this practice. Your new answer was not recorded; the latest saved item is shown.")).toBeVisible();
  expect(screen.getByRole("heading", { name: "Practice complete" })).toBeVisible();
});

it("keeps expired recovery unavailable on an authenticated read failure", async () => {
  saveRunRecovery({ ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 });
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  vi.stubGlobal("fetch", vi.fn(async () => json({ detail: "expired" }, 401)));
  render(<MantineProvider env="test"><Practice /></MantineProvider>);
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
  render(<MantineProvider env="test"><Practice /></MantineProvider>);
  expect(await screen.findByText(new RegExp(message))).toBeVisible();
  expect(fetchMock).toHaveBeenCalledTimes(1);
});

it("abandons the current browser recovery only after Start new practice is chosen", async () => {
  const run = { ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 };
  saveRunRecovery(run);
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  let renewed = false;
  const randomSeeds = [123, 456];
  vi.spyOn(crypto, "getRandomValues").mockImplementation((array) => {
    (array as Uint32Array)[0] = randomSeeds.shift()!;
    return array;
  });
  const creationSeeds: number[] = [];
  const nextOwner = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa";
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("development-session")) { renewed = true; return json({ mode: "development" }); }
    if (path.endsWith("practice-context")) return json(context(renewed ? nextOwner : owner));
    if (path.endsWith("practice-sessions")) {
      creationSeeds.push(JSON.parse(String(init?.body)).seed);
      return json({ ...state(), ownerSessionId: nextOwner }, 201);
    }
    if (path.includes("/practice-sessions/")) return json(state());
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  render(<MantineProvider env="test"><Practice /></MantineProvider>);
  await user.click(await screen.findByRole("button", { name: "Start new practice" }));
  await waitFor(() => expect(new URLSearchParams(window.location.search).get("session")).not.toBe(practiceId));
  expect(localStorage.getItem(`astrid.practice.v1.run.${practiceId}`)).toBeNull();
  await user.click(screen.getByRole("button", { name: "Start new practice" }));
  await waitFor(() => expect(creationSeeds).toEqual([123, 456]));
});

it("submits an ordering answer and advances without completing early", async () => {
  const ordering = {
    ...exercise, id: "55555555-5555-4555-8555-555555555555", formatId: "E003", lexicalTarget: undefined,
    targetSkillId: "G005", instruction: "Arrange the statement.", introduction: [],
    prompt: { before: "", after: "", choices: [], punctuation: ".", blocks: [{ id: "b0", text: "I" }, { id: "b1", text: "læser" }] },
  };
  const run = { ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 };
  saveRunRecovery(run);
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  let serverState = state({ itemCount: 2, currentExercise: ordering, items: [{ ...state().items[0]!, exerciseId: ordering.id, formatId: "E003", targetSkillId: "G005", lexicalTarget: undefined }] });
  let submitted = "";
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("practice-context")) return json({ ...context(), itemCount: 2 });
    if (path.endsWith("/attempts")) {
      submitted = String(init?.body);
      const saved = { ...wrongAttempt, exerciseId: ordering.id, attemptId: JSON.parse(submitted).attemptId };
      serverState = { ...serverState, items: [{
        ...serverState.items[0]!, attemptCount: 1,
        lastAttempt: saved as never, firstAttempt: saved as never,
        lastAnswer: { kind: "order", blockIds: ["b0", "b1"] } as never,
      } as never] };
      return json(saved);
    }
    if (path.endsWith("/advance")) {
      serverState = state({ itemCount: 2, cursor: 1, items: [serverState.items[0]!, { ...state().items[0]!, position: 1 }], currentExercise: exercise });
      return json(serverState);
    }
    if (path.includes("/practice-sessions/")) return json(serverState);
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  render(<MantineProvider env="test"><Practice /></MantineProvider>);
  await user.click(await screen.findByRole("button", { name: "I" }));
  await user.click(screen.getByRole("button", { name: "læser" }));
  await user.click(screen.getByRole("button", { name: "Check answer" }));
  expect(JSON.parse(submitted)).toMatchObject({ answer: { kind: "order", blockIds: ["b0", "b1"] } });
  await user.click(await screen.findByRole("button", { name: "Continue" }));
  expect(await screen.findByText("Part 2 of 2")).toBeVisible();
  expect(screen.queryByRole("heading", { name: "Practice complete" })).not.toBeInTheDocument();
});

it("deduplicates automatic creation across Strict Mode remounts", async () => {
  const paths: string[] = [];
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    paths.push(path);
    if (path.endsWith("development-session")) return json({ mode: "development" });
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith("practice-sessions")) return json(state(), 201);
    throw new Error(`Unexpected ${path}`);
  }));

  render(<StrictMode><MantineProvider env="test"><Practice /></MantineProvider></StrictMode>);
  expect(await screen.findByRole("heading", { name: "Words and simple statements" })).toBeVisible();
  expect(paths.filter((path) => path.endsWith("development-session"))).toHaveLength(1);
  expect(paths.filter((path) => path.endsWith("practice-sessions"))).toHaveLength(1);
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
  render(<MantineProvider env="test"><Practice /></MantineProvider>);
  await user.click(await screen.findByRole("button", { name: "dog" }));
  saveOutboxEntry({ ...run, ownerSessionId: "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
    kind: "attempt", identity: "foreign-operation", body: "{}", queuedAt: 1 });
  await user.click(screen.getByRole("button", { name: "Check answer" }));
  await waitFor(() => expect(screen.getByRole("alert")).toHaveFocus());
  expect(screen.getByRole("alert")).toHaveTextContent("will not be replayed under a new owner");
});


it("binds an uncertain creation before sending and abandons its original recovery on explicit restart", async () => {
  let creations = 0;
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("development-session")) return json({ mode: "development" });
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith("practice-sessions")) {
      const body = JSON.parse(String(init?.body)) as { practiceSessionId: string };
      expect(new URLSearchParams(window.location.search).get("session")).toBe(body.practiceSessionId);
      creations += 1;
      if (creations === 1) throw new TypeError("creation response lost");
      return json({ ...state(), id: body.practiceSessionId }, 201);
    }
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  render(<MantineProvider env="test"><Practice /></MantineProvider>);
  await screen.findByRole("alert");
  const original = new URLSearchParams(window.location.search).get("session");
  expect(original).not.toBeNull();
  expect(listOutbox(original!)).toHaveLength(1);
  await user.click(screen.getByRole("button", { name: "Start new practice" }));
  await screen.findByRole("button", { name: "Check answer" });
  expect(new URLSearchParams(window.location.search).get("session")).not.toBe(original);
  expect(localStorage.getItem(`astrid.practice.v1.run.${original}`)).toBeNull();
  expect(listOutbox(original!)).toEqual([]);
});
