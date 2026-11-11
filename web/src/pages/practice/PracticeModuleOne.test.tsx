import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import {
  completedState, context, json, outcomes, owner, practiceId, profiles, resetPracticeBrowser, state,
} from "../../test/practiceFixtures";
import { renderWithProviders } from "../../test/render";
import Practice from "./Practice";
import { listOutbox, saveOutboxEntry, saveRunRecovery } from "./practiceRecovery";

beforeEach(resetPracticeBrowser);

const subjectExercise = {
  ...state().currentExercise,
  id: "77777777-7777-4777-8777-777777777777",
  formatId: "E017",
  targetSkillId: "G003",
  targetScopeId: "human-singular",
  contentRevision: "pilot-8",
  generatorRevision: "generator-8",
  instruction: "Type the introduced subject word.",
  context: "Anna is speaking about herself.",
  prompt: {
    before: "", after: "", choices: [], blocks: [], punctuation: "", cue: "Anna is speaking about herself.",
    cueLanguage: "en", answerLanguage: "da", inputLabel: "Danish subject word",
  },
  introduction: [{ title: "Standalone form", body: "Use lowercase jeg here.", bodyLanguage: "en" }],
  hint: "Review the speaker mapping.",
};

function moduleOneState(overrides: Record<string, unknown> = {}) {
  const item = {
    ...state().items[0]!, exerciseId: subjectExercise.id, formatId: "E017", targetSkillId: "G003",
    targetScopeId: "human-singular", assessmentMode: "recall", canAdvance: false,
  };
  return state({
    focusId: "module-one-singular", title: "Talking about one person", planRevision: "module-one-singular-1",
    contentRevision: "pilot-8", generatorRevision: "generator-8", evaluationRevision: "evaluation-8",
    itemCount: 6, prerequisiteSkillIds: ["G001", "G002"], introductionRequired: true,
    introduction: [
      { title: "Subject words", body: "A subject word identifies who the statement is about.", bodyLanguage: "en" },
      { title: "Worked example", body: "Jeg er Anna.", bodyLanguage: "da" },
    ],
    items: [item], currentExercise: subjectExercise,
    ...overrides,
  });
}

function saveModuleOneRun() {
  saveRunRecovery({
    ownerSessionId: owner, practiceSessionId: practiceId,
    focusId: "module-one-singular", planRevision: "module-one-singular-1", seed: 47,
  });
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
}

it("focuses the required introduction control after replaying a saved creation", async () => {
  saveModuleOneRun();
  const run = {
    ownerSessionId: owner, practiceSessionId: practiceId,
    focusId: "module-one-singular", planRevision: "module-one-singular-1", seed: 47,
  };
  saveOutboxEntry({
    ...run, kind: "creation", identity: practiceId,
    body: JSON.stringify({ practiceSessionId: practiceId, focusId: run.focusId, planRevision: run.planRevision, seed: 47 }),
    queuedAt: 1,
  });
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith("practice-sessions")) return json(moduleOneState(), 201);
    throw new Error(`Unexpected ${path}`);
  }));

  renderWithProviders(<Practice />);
  const introduction = await screen.findByRole("button", { name: "Session introduction" });
  await waitFor(() => expect(introduction).toHaveFocus());
  expect(listOutbox(practiceId)).toEqual([]);
});

it.each([
  ["human-singular", "G003", "One-person subject words"],
  ["human-group", "G003", "Group subject words"],
  ["vaere-present", "G004", "Present verb form"],
  ["inanimate-singular-subject", "G038", "One-thing reference words"],
  ["named-three-verbs-present", "G004", "Present forms of three verbs"],
  ["subject-first-supplied", "G005", "Subject-first statement order"],
  ["have-possession-present", "G004", "Present possession with har"],
] as const)("shows a learning label instead of the %s implementation scope", async (targetScopeId, targetSkillId, label) => {
  saveModuleOneRun();
  const scopedExercise = { ...subjectExercise, targetScopeId, targetSkillId };
  const scopedItem = {
    ...moduleOneState().items[0]!, targetScopeId, targetSkillId, exerciseId: scopedExercise.id,
  };
  const session = moduleOneState({
    introductionOpened: true, currentExercise: scopedExercise, items: [scopedItem],
  });
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.includes("/practice-sessions/")) return json(session);
    throw new Error(`Unexpected ${path}`);
  }));

  renderWithProviders(<Practice />);
  expect(await screen.findByText(label)).toBeVisible();
  expect(screen.queryByText(targetSkillId)).not.toBeInTheDocument();
  expect(screen.queryByText(targetScopeId)).not.toBeInTheDocument();
});

it.each([
  ["Talking about one person", "module-one-singular", "module-one-singular-1"],
  ["Talking to and about groups", "module-one-groups", "module-one-groups-1"],
  ["Using the verb to be", "module-one-er", "module-one-er-1"],
  ["Referring to one thing", "module-one-things", "module-one-things-1"],
  ["Using present verb forms", "module-one-present", "module-one-present-1"],
  ["Building subject-first statements", "module-one-order", "module-one-order-1"],
  ["Saying what someone has", "module-one-possession", "module-one-possession-1"],
] as const)("creates %s only after its exact catalog choice", async (title, focusId, planRevision) => {
  let body = "";
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("development-session")) return json({ mode: "development" });
    if (path.endsWith("practice-profiles")) return json(profiles());
    if (path.endsWith("practice-sessions")) {
      body = String(init?.body);
      const request = JSON.parse(body) as { practiceSessionId: string; seed: number };
      return json(moduleOneState({ id: request.practiceSessionId, focusId, planRevision, title }), 201);
    }
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);

  expect(await screen.findByRole("heading", { name: "Module 1: First steps" })).toBeVisible();
  expect(screen.getByRole("button", { name: "Start practice" })).toBeDisabled();
  await user.click(screen.getByRole("radio", { name: title }));
  await user.click(screen.getByRole("button", { name: "Start practice" }));
  await screen.findByRole("heading", { name: title });
  await waitFor(() => expect(screen.getByRole("button", { name: "Session introduction" })).toHaveFocus());
  const request = JSON.parse(body) as Record<string, unknown>;
  expect(request).toEqual({
    practiceSessionId: expect.any(String), focusId, planRevision, seed: expect.any(Number),
  });
});

it.each([false, true])(
  "keeps all teaching and assessment hidden while a required introduction result is lost (committed: %s)",
  async (committed) => {
  saveModuleOneRun();
  let serverState = moduleOneState();
  const bodies: string[] = [];
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith("/support")) {
      bodies.push(String(init?.body));
      if (bodies.length === 1) {
        if (committed) serverState = moduleOneState({ introductionOpened: true });
        throw new TypeError("request outcome unknown");
      }
      serverState = moduleOneState({ introductionOpened: true });
      return json(serverState);
    }
    if (path.includes("/practice-sessions/")) return json(serverState);
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);

  await screen.findByRole("button", { name: "Session introduction" });
  await waitFor(() => expect(screen.getByRole("button", { name: "Session introduction" })).toHaveFocus());
  expect(screen.queryByRole("heading", { name: "Type the introduced subject word." })).not.toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Learn this first" })).not.toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Check answer" })).not.toBeInTheDocument();
  await user.click(screen.getByRole("button", { name: "Session introduction" }));
  expect(await screen.findByRole("button", { name: "Retry saved operation" })).toBeVisible();
  expect(screen.queryByText("Jeg er Anna.")).not.toBeInTheDocument();
  expect(listOutbox(practiceId)[0]?.body).toBe('{"kind":"introduction"}');

  await user.click(screen.getByRole("button", { name: "Retry saved operation" }));
  expect(await screen.findByText("Jeg er Anna.")).toHaveAttribute("lang", "da");
  expect(await screen.findByRole("heading", { name: "Type the introduced subject word." })).toBeVisible();
  expect(bodies).toEqual(['{"kind":"introduction"}', '{"kind":"introduction"}']);
  expect(listOutbox(practiceId)).toEqual([]);
  },
);

it("uses only the server canAdvance projection and preserves the typed retry", async () => {
  saveModuleOneRun();
  const unknownAttempt = {
    attemptId: "88888888-8888-4888-8888-888888888888", exerciseId: subjectExercise.id,
    targetSatisfaction: "unevaluable", linguisticStatus: "unknown", linguisticScope: "danish-word-form",
    responseConstraintStatus: "met", issues: [], feedback: "This word is outside the introduced alternatives.",
    completedSentence: "", evaluationRevision: "evaluation-8", hintUsed: false, retryNumber: 1, createdAt: 1,
  };
  let serverState = moduleOneState({ introductionOpened: true });
  let attempts = 0;
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith("/attempts")) {
      attempts += 1;
      const answer = JSON.parse(String(init?.body)).answer;
      const canAdvance = attempts === 2;
      const attempt = canAdvance ? {
        ...unknownAttempt, attemptId: "99999999-9999-4999-8999-999999999999", targetSatisfaction: "met",
        linguisticStatus: "acceptable", feedback: "That is the introduced subject word.", retryNumber: 2,
      } : unknownAttempt;
      serverState = moduleOneState({ introductionOpened: true, items: [{
        ...moduleOneState().items[0]!, canAdvance, attemptIds: [attempt.attemptId], attemptCount: attempts,
        firstAttempt: unknownAttempt, lastAttempt: attempt, firstAnswer: answer, lastAnswer: answer,
      }] });
      return json(attempt);
    }
    if (path.includes("/practice-sessions/")) return json(serverState);
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);

  const input = await screen.findByRole("textbox", { name: "Danish subject word" });
  expect(input).toHaveAttribute("autocapitalize", "none");
  await user.type(input, "ukendt");
  await user.click(screen.getByRole("button", { name: "Check answer" }));
  expect(await screen.findByRole("button", { name: "Continue" })).toBeDisabled();
  expect(screen.getByText(/does not have a known outcome yet/i)).toBeVisible();
  expect(screen.getByRole("button", { name: "Show hint" })).toBeEnabled();
  await user.click(screen.getByRole("button", { name: "Try again" }));
  await user.type(screen.getByRole("textbox", { name: "Danish subject word" }), "jeg");
  await user.click(screen.getByRole("button", { name: "Check answer" }));
  expect(await screen.findByRole("button", { name: "Continue" })).toBeEnabled();
});

it("opens repeat and next choices preselected without creating or discarding a run", async () => {
  saveModuleOneRun();
  const rows = [
    ["Singular subject selection", "selection"], ["Singular subject recall", "recall"],
  ].map(([label, assessmentMode]) => ({
    label, targetSkillId: "G003", targetScopeId: "human-singular", assessmentMode,
    plannedItems: 3, attemptedItems: 3, advancedItems: 3, attemptCount: 3, retryCount: 0,
    firstOutcomes: outcomes, latestOutcomes: outcomes, firstWithSupport: 3, firstWithPriorExposure: 3,
    lessonOpenedItems: 0, hintOpenedItems: 0,
  }));
  const completed = moduleOneState({
    state: "completed", cursor: 6, currentExercise: null, introductionOpened: true,
    summary: { ...completedState().summary, plannedItems: 6, completedItems: 6, attemptedItems: 6, rows },
  });
  let creations = 0;
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith("practice-profiles")) return json(profiles());
    if (path.endsWith("practice-sessions")) { creations += 1; return json(completed); }
    if (path.includes("/practice-sessions/")) return json(completed);
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);

  expect(await screen.findByText("Experience from other practice runs is unknown.", { exact: false })).toBeVisible();
  expect(screen.queryByRole("checkbox")).not.toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Start selected review" })).not.toBeInTheDocument();
  await user.click(screen.getByRole("button", { name: "Repeat this session" }));
  const repeatChoice = screen.getByRole("radio", { name: "Talking about one person" });
  expect(repeatChoice).toBeChecked();
  await waitFor(() => expect(repeatChoice).toHaveFocus());
  expect(creations).toBe(0);
  await user.click(screen.getByRole("button", { name: "Cancel" }));
  const summary = await screen.findByRole("heading", { name: "Practice complete" });
  await waitFor(() => expect(summary).toHaveFocus());
  await user.click(screen.getByRole("button", { name: "Next: Talking to and about groups" }));
  const nextChoice = screen.getByRole("radio", { name: "Talking to and about groups" });
  expect(nextChoice).toBeChecked();
  await waitFor(() => expect(nextChoice).toHaveFocus());
  expect(creations).toBe(0);
  expect(localStorage.getItem(`astrid.practice.v1.run.${practiceId}`)).not.toBeNull();
});

it("offers S04 after S03 and preserves the completed S03 run when cancelled", async () => {
  saveRunRecovery({
    ownerSessionId: owner, practiceSessionId: practiceId,
    focusId: "module-one-er", planRevision: "module-one-er-1", seed: 47,
  });
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  const completed = moduleOneState({
    focusId: "module-one-er", planRevision: "module-one-er-1", title: "Using the verb to be",
    state: "completed", cursor: 6, currentExercise: null, introductionOpened: true,
    summary: { ...completedState().summary, plannedItems: 6, completedItems: 6 },
  });
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.includes("/practice-sessions/")) return json(completed);
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);
  expect(await screen.findByRole("button", { name: "Repeat this session" })).toBeVisible();
  await user.click(screen.getByRole("button", { name: "Next: Referring to one thing" }));
  expect(screen.getByRole("radio", { name: "Referring to one thing" })).toBeChecked();
  await user.click(screen.getByRole("button", { name: "Cancel" }));
  expect(await screen.findByRole("heading", { name: "Practice complete" })).toBeVisible();
  expect(localStorage.getItem(`astrid.practice.v1.run.${practiceId}`)).not.toBeNull();
});

it("offers S05 after S04 without claiming a targeted review", async () => {
  saveRunRecovery({
    ownerSessionId: owner, practiceSessionId: practiceId,
    focusId: "module-one-things", planRevision: "module-one-things-1", seed: 47,
  });
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  const completed = moduleOneState({
    focusId: "module-one-things", planRevision: "module-one-things-1", title: "Referring to one thing",
    state: "completed", cursor: 4, itemCount: 4, currentExercise: null, introductionOpened: true,
    summary: {
      ...completedState().summary, plannedItems: 4, completedItems: 4,
      rows: ["selection", "recall"].map((assessmentMode) => ({
        label: assessmentMode === "selection" ? "One-thing reference selection" : "One-thing reference recall",
        targetSkillId: "G038", targetScopeId: "inanimate-singular-subject", assessmentMode,
        plannedItems: 2, attemptedItems: 2, advancedItems: 2, attemptCount: 2, retryCount: 0,
        firstOutcomes: outcomes, latestOutcomes: outcomes, firstWithSupport: 2, firstWithPriorExposure: 2,
        lessonOpenedItems: 0, hintOpenedItems: 0,
      })),
    },
  });
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.includes("/practice-sessions/")) return json(completed);
    throw new Error(`Unexpected ${path}`);
  }));
  renderWithProviders(<Practice />);
  expect(await screen.findByRole("button", { name: "Repeat this session" })).toBeVisible();
  expect(screen.getByText("One-thing reference word selection")).toBeVisible();
  expect(screen.getByText("One-thing reference word recall")).toBeVisible();
  expect(screen.getAllByText("2/2 continued · 2 answers · 0 retries")).toHaveLength(2);
  expect(screen.getByRole("button", { name: "Next: Using present verb forms" })).toBeVisible();
  expect(screen.queryByRole("button", { name: "Start selected review" })).not.toBeInTheDocument();
});

it("offers S07 after S06 and stops the explicit Module 1 sequence after S07", async () => {
  saveRunRecovery({
    ownerSessionId: owner, practiceSessionId: practiceId,
    focusId: "module-one-order", planRevision: "module-one-order-1", seed: 47,
  });
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  let completed = moduleOneState({
    focusId: "module-one-order", planRevision: "module-one-order-1", title: "Building subject-first statements",
    state: "completed", cursor: 6, currentExercise: null, introductionOpened: true,
    summary: { ...completedState().summary, plannedItems: 6, completedItems: 6 },
  });
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.includes("/practice-sessions/")) return json(completed);
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  const view = renderWithProviders(<Practice />);

  await user.click(await screen.findByRole("button", { name: "Next: Saying what someone has" }));
  expect(screen.getByRole("radio", { name: "Saying what someone has" })).toBeChecked();
  await user.click(screen.getByRole("button", { name: "Cancel" }));
  expect(await screen.findByRole("heading", { name: "Practice complete" })).toBeVisible();

  view.unmount();
  completed = moduleOneState({
    focusId: "module-one-possession", planRevision: "module-one-possession-1", title: "Saying what someone has",
    state: "completed", cursor: 4, itemCount: 4, currentExercise: null, introductionOpened: true,
    summary: { ...completedState().summary, plannedItems: 4, completedItems: 4 },
  });
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  renderWithProviders(<Practice />);
  expect(await screen.findByRole("button", { name: "Repeat this session" })).toBeVisible();
  expect(screen.queryByRole("button", { name: /^Next:/ })).not.toBeInTheDocument();
});
