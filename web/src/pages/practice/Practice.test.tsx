import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import {
  choosePractice, completedState, context, exercise, json, owner, practiceId, profiles, resetPracticeBrowser, state, wrongAttempt,
} from "../../test/practiceFixtures";
import { renderWithProviders } from "../../test/render";
import Practice from "./Practice";
import { saveRunRecovery } from "./practiceRecovery";

beforeEach(resetPracticeBrowser);

it("records support before revealing it, permits retries after wrong answers, and completes honestly", async () => {
  let serverState = state();
  const sent: Array<{ path: string; body: string }> = [];
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    const body = String(init?.body ?? "");
    if (body) sent.push({ path, body });
    if (path.endsWith("development-session")) return json({ mode: "development" });
    if (path.endsWith("practice-profiles")) return json(profiles());
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
  renderWithProviders(<Practice />);
  await choosePractice(user);
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
  renderWithProviders(<Practice />);
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
    renderWithProviders(<Practice />);
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
  renderWithProviders(<Practice />);
  await user.click(await screen.findByRole("button", { name: "I" }));
  await user.click(screen.getByRole("button", { name: "læser" }));
  await user.click(screen.getByRole("button", { name: "Check answer" }));
  expect(JSON.parse(submitted)).toMatchObject({ answer: { kind: "order", blockIds: ["b0", "b1"] } });
  await user.click(await screen.findByRole("button", { name: "Continue" }));
  expect(await screen.findByText("Part 2 of 2")).toBeVisible();
  expect(screen.queryByRole("heading", { name: "Practice complete" })).not.toBeInTheDocument();
});

it("reports source-practice exposure separately from exposure inside the current run", async () => {
  saveRunRecovery({ ownerSessionId: owner, practiceSessionId: practiceId, focusId: "targeted-review", planRevision: "follow-up-1", seed: 47,
    sourcePracticeSessionId: "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa", selectedTargetKeys: ["lexical:L014:L014-S01:meaning:da-en"] });
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  const contextWithSource = {
    introductionOpened: false, lessonOpened: false, hintOpened: false, priorExposure: [],
    sourceExposure: [{ practiceSessionId: "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa", position: 0, source: "feedback", keys: ["gloss:L014-S01"] }],
  };
  const active = state({
    focusId: "targeted-review", planRevision: "follow-up-1",
    followUp: { sourcePracticeSessionId: "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa", selectedTargetKeys: ["lexical:L014:L014-S01:meaning:da-en"] },
    items: [{ ...state().items[0]!, attemptCount: 1, attemptIds: [wrongAttempt.attemptId], firstAttempt: wrongAttempt,
      lastAttempt: wrongAttempt, firstAttemptContext: contextWithSource, lastAttemptContext: contextWithSource }],
  });
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.includes("/practice-sessions/")) return json(active);
    throw new Error(`Unexpected ${path}`);
  }));
  renderWithProviders(<Practice />);
  expect(await screen.findByText("This target had possible exposure in the source practice.")).toBeVisible();
  expect(screen.queryByText(/this run’s prompts/)).not.toBeInTheDocument();
});

it("keeps the URL fragment when it binds a created practice to the address", async () => {
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("development-session")) return json({ mode: "development" });
    if (path.endsWith("practice-profiles")) return json(profiles());
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith("practice-sessions") && init?.method === "POST") return json(state(), 201);
    return json(state());
  }));
  window.history.replaceState(null, "", "/practice#main-content");
  const user = userEvent.setup();
  renderWithProviders(<Practice />);

  await choosePractice(user);

  await waitFor(() => expect(new URLSearchParams(window.location.search).get("session")).not.toBeNull());
  expect(window.location.pathname).toBe("/practice");
  expect(window.location.hash).toBe("#main-content");
});
