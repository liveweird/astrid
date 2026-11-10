import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import {
  choosePractice, completedState, context, exercise, gapAttempt, gapExercise, gapState, json, owner, practiceId, profiles,
  resetPracticeBrowser, state, wrongAttempt,
} from "../../test/practiceFixtures";
import { renderWithProviders } from "../../test/render";
import Practice from "./Practice";
import { saveRunRecovery } from "./practiceRecovery";

beforeEach(resetPracticeBrowser);

it("submits one selected gap in supplied order and labels the bounded action", async () => {
  saveRunRecovery({ ownerSessionId: owner, practiceSessionId: practiceId, focusId: "verb-article-gaps", planRevision: "verb-article-gaps-1", seed: 47 });
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  let submitted = "";
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith("/attempts")) { submitted = String(init?.body); return json(gapAttempt); }
    if (path.includes("/practice-sessions/")) return json(gapState());
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);

  const article = within(await screen.findByRole("group", { name: "Indefinite article — Supporting grammar" }));
  await user.click(article.getByRole("button", { name: "en" }));
  const check = screen.getByRole("button", { name: "Check gaps" });
  expect(check).toBeEnabled();
  await user.click(check);

  expect(JSON.parse(submitted)).toMatchObject({
    answer: { kind: "gaps", selections: [{ slotId: "g1", choiceId: "c1" }] },
  });
});

it("retains both gap selections for retry, focuses the board, and clears them on the next item", async () => {
  saveRunRecovery({ ownerSessionId: owner, practiceSessionId: practiceId, focusId: "verb-article-gaps", planRevision: "verb-article-gaps-1", seed: 47 });
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  const selections = [{ slotId: "g1", choiceId: "c1" }, { slotId: "g0", choiceId: "c0" }];
  const checkedItem = {
    ...gapState().items[0]!, attemptIds: [gapAttempt.attemptId], attemptCount: 1,
    firstAttempt: gapAttempt, lastAttempt: gapAttempt,
    firstAnswer: { kind: "gaps", selections }, lastAnswer: { kind: "gaps", selections },
  };
  let serverState = gapState({ items: [checkedItem] });
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith("/attempts")) {
      const request = JSON.parse(String(init?.body)) as { answer: { kind: "gaps"; selections: typeof selections } };
      const retried = { ...gapAttempt, retryNumber: 2 };
      const retriedItem = { ...checkedItem, attemptCount: 2, lastAttempt: retried, lastAnswer: request.answer };
      serverState = gapState({ items: [retriedItem] });
      return json(retried);
    }
    if (path.endsWith("/advance")) {
      serverState = gapState({
        cursor: 1,
        items: [checkedItem, { ...gapState().items[0]!, position: 1, exerciseId: gapExercise.id }],
      });
      return json(serverState);
    }
    if (path.includes("/practice-sessions/")) return json(serverState);
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);

  expect(await screen.findByRole("button", { name: "en" })).toHaveAttribute("aria-pressed", "true");
  expect(screen.getByRole("button", { name: "læse" })).toHaveAttribute("aria-pressed", "true");
  expect(screen.getByRole("group", { name: "Present verb feedback" })).toHaveTextContent("Choose the present form.");
  expect(screen.getByRole("group", { name: "Indefinite article feedback" })).toHaveTextContent("Choose the article for bog.");
  await user.click(screen.getByRole("button", { name: "Try again" }));
  const board = screen.getByRole("group", { name: "Present verb and article gaps" });
  await waitFor(() => expect(board).toHaveFocus());
  expect(screen.getByRole("button", { name: "en" })).toHaveAttribute("aria-pressed", "true");
  expect(screen.getByRole("button", { name: "læse" })).toHaveAttribute("aria-pressed", "true");
  await user.click(screen.getByRole("button", { name: "Check gaps" }));
  await user.click(await screen.findByRole("button", { name: "Continue" }));
  expect(await screen.findByText("Part 2 of 4")).toBeVisible();
  expect(screen.getByRole("button", { name: "en" })).toHaveAttribute("aria-pressed", "false");
  expect(screen.getByRole("button", { name: "læse" })).toHaveAttribute("aria-pressed", "false");
});

it("preserves fresh gap choices and edited retries while saving introduction lesson and hint support", async () => {
  saveRunRecovery({ ownerSessionId: owner, practiceSessionId: practiceId, focusId: "verb-article-gaps", planRevision: "verb-article-gaps-1", seed: 47 });
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  const currentExercise = { ...gapExercise, introduction: [{ title: "Noun article", body: "en bog", bodyLanguage: "da" }] };
  let serverState = gapState({ currentExercise });
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith("/support")) {
      const request = JSON.parse(String(init?.body)) as { kind: string };
      serverState = { ...serverState,
        introductionOpened: serverState.introductionOpened || request.kind === "introduction",
        items: serverState.items.map((item) => ({ ...item,
          lessonOpened: item.lessonOpened || request.kind === "lesson", hintOpened: item.hintOpened || request.kind === "hint" })),
      };
      return json(serverState);
    }
    if (path.endsWith("/attempts")) {
      const request = JSON.parse(String(init?.body)) as { answer: { kind: "gaps"; selections: { slotId: string; choiceId: string }[] } };
      serverState = gapState({ ...serverState, items: [{ ...serverState.items[0]!,
        attemptIds: [gapAttempt.attemptId], attemptCount: 1, lastAttempt: gapAttempt, lastAnswer: request.answer,
      }] });
      return json(gapAttempt);
    }
    if (path.includes("/practice-sessions/")) return json(serverState);
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);
  await user.click(await screen.findByRole("button", { name: "læser" }));
  await user.click(screen.getByRole("button", { name: "Session introduction" }));
  expect(await screen.findByText("Choose each form separately.")).toBeVisible();
  expect(screen.getByRole("button", { name: "læser" })).toHaveAttribute("aria-pressed", "true");
  await user.click(screen.getByRole("button", { name: "Check gaps" }));
  await user.click(await screen.findByRole("button", { name: "Try again" }));
  await user.click(screen.getByRole("button", { name: "en" }));
  await user.click(screen.getByRole("button", { name: "Learn this first" }));
  expect(await screen.findByText("en bog", { exact: true })).toBeVisible();
  expect(screen.getByRole("button", { name: "en" })).toHaveAttribute("aria-pressed", "true");
  await user.click(screen.getByRole("button", { name: "Show hint" }));
  expect(await screen.findByText(currentExercise.hint)).toBeVisible();
  expect(screen.getByRole("button", { name: "læser" })).toHaveAttribute("aria-pressed", "true");
  expect(screen.getByRole("button", { name: "en" })).toHaveAttribute("aria-pressed", "true");
  expect(screen.getByRole("button", { name: "Check gaps" })).toBeEnabled();
  expect(screen.queryByRole("button", { name: "Continue" })).not.toBeInTheDocument();
});

it.each(["new grade", "next item"])("restores authoritative state when support readback includes a %s", async (change) => {
  saveRunRecovery({ ownerSessionId: owner, practiceSessionId: practiceId, focusId: "verb-article-gaps", planRevision: "verb-article-gaps-1", seed: 47 });
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  let serverState = gapState();
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith("/support")) {
      const checked = { ...serverState.items[0]!, attemptCount: 1, attemptIds: [gapAttempt.attemptId], lastAttempt: gapAttempt,
        lastAnswer: { kind: "gaps", selections: [{ slotId: "g0", choiceId: "c0" }, { slotId: "g1", choiceId: "c0" }] } };
      const nextExercise = { ...gapExercise, id: "77777777-7777-4777-8777-777777777777" };
      serverState = gapState({ introductionOpened: true, items: change === "new grade" ? [checked] : [checked, {
        ...gapState().items[0]!, position: 1, exerciseId: nextExercise.id,
      }], ...(change === "next item" ? { cursor: 1, currentExercise: nextExercise } : {}) });
      return json(serverState);
    }
    if (path.includes("/practice-sessions/")) return json(serverState);
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);
  await user.click(await screen.findByRole("button", { name: "læser" }));
  await user.click(screen.getByRole("button", { name: "Session introduction" }));
  expect(await screen.findByText("Choose each form separately.")).toBeVisible();
  expect(screen.getByRole("button", { name: "læser" })).toHaveAttribute("aria-pressed", "false");
  if (change === "new grade") {
    expect(screen.getByRole("button", { name: "læse" })).toHaveAttribute("aria-pressed", "true");
    expect(screen.getByRole("group", { name: "Present verb feedback" })).toBeVisible();
    expect(screen.getByRole("button", { name: "Continue" })).toBeEnabled();
  } else {
    expect(screen.getByText("Part 2 of 4")).toBeVisible();
    expect(screen.getByRole("button", { name: "læse" })).toHaveAttribute("aria-pressed", "false");
    expect(screen.getByRole("button", { name: "Check gaps" })).toBeDisabled();
  }
});

it("marks each introduction section with its supplied content language", async () => {
  saveRunRecovery({ ownerSessionId: owner, practiceSessionId: practiceId, focusId: "verb-article-gaps", planRevision: "verb-article-gaps-1", seed: 47 });
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  let serverState = gapState();
  let supportBody = "";
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith("/support")) {
      supportBody = String(init?.body);
      serverState = gapState({ introductionOpened: true });
      return json(serverState);
    }
    if (path.includes("/practice-sessions/")) return json(serverState);
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);
  await user.click(await screen.findByRole("button", { name: "Session introduction" }));
  expect(await screen.findByText("Choose each form separately.")).toHaveAttribute("lang", "en");
  expect(screen.getByText("Jeg læser en bog.")).toHaveAttribute("lang", "da");
  expect(supportBody).toBe('{"kind":"introduction"}');
});

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

it("submits a partial guided word-tile answer and preserves it through support", async () => {
  const ordering = {
    ...exercise, id: "77777777-7777-4777-8777-777777777777", formatId: "E025", lexicalTarget: undefined,
    targetSkillId: "G005", targetScopeId: "subject-first-supplied", instruction: "Build the supplied statement.",
    context: "Put the supplied Danish words in subject-first order.",
    introduction: [{ title: "Statement pattern", body: "subject | finite verb | supplied object" }],
    prompt: {
      before: "", after: "", choices: [], punctuation: ".", cue: "I read a newspaper.",
      cueLanguage: "en", answerLanguage: "da", tileMode: "use-all", requiredTileCount: 4,
      blocks: [
        { id: "s", text: "jeg" }, { id: "v", text: "læser" },
        { id: "a", text: "en" }, { id: "n", text: "avis" },
      ],
    },
  };
  const run = {
    ownerSessionId: owner, practiceSessionId: practiceId,
    focusId: "module-one-order", planRevision: "module-one-order-1", seed: 47,
  };
  saveRunRecovery(run);
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  const item = {
    ...state().items[0]!, exerciseId: ordering.id, formatId: "E025", lexicalTarget: undefined,
    targetSkillId: "G005", targetScopeId: "subject-first-supplied", assessmentMode: "word-order",
  };
  let serverState = state({
    focusId: run.focusId, planRevision: run.planRevision, title: "Building subject-first statements",
    contentRevision: "pilot-10", generatorRevision: "generator-10", evaluationRevision: "evaluation-10",
    itemCount: 6, introductionOpened: true, currentExercise: ordering, items: [item],
  });
  let submitted = "";
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith("/support")) {
      serverState = { ...serverState, items: [{ ...serverState.items[0]!, lessonOpened: true }] };
      return json(serverState);
    }
    if (path.endsWith("/attempts")) {
      submitted = String(init?.body);
      const request = JSON.parse(submitted);
      const partialAttempt = {
        ...wrongAttempt, exerciseId: ordering.id, attemptId: request.attemptId,
        targetSatisfaction: "unevaluable", linguisticStatus: "unknown", linguisticScope: "danish-sentence",
        responseConstraintStatus: "not-met", meaningSatisfaction: "unknown",
        issues: [{ code: "REQUIRED_WORDS_MISSING", message: "Use every supplied word." }],
      };
      serverState = state({ ...serverState, items: [{ ...serverState.items[0]!,
        lastAttempt: partialAttempt, lastAnswer: request.answer, canAdvance: true,
      }] });
      return json(partialAttempt);
    }
    if (path.includes("/practice-sessions/")) return json(serverState);
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);

  await user.click(await screen.findByRole("button", { name: "jeg" }));
  expect(screen.getByRole("button", { name: "Check answer" })).toBeEnabled();
  await user.click(screen.getByRole("button", { name: "Learn this first" }));
  expect(await screen.findByRole("button", { name: "Remove jeg from position 1" })).toBeVisible();
  await user.click(screen.getByRole("button", { name: "Check answer" }));
  expect(JSON.parse(submitted)).toMatchObject({ answer: { kind: "order", blockIds: ["s"] } });
  await user.click(await screen.findByRole("button", { name: "Try again" }));
  expect(screen.getByRole("button", { name: "Remove jeg from position 1" })).toBeEnabled();
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
