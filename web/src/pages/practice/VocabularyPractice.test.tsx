import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import {
  completedState, context, json, outcomes, owner, practiceId, profiles, resetPracticeBrowser, state,
} from "../../test/practiceFixtures";
import { renderWithProviders } from "../../test/render";
import Practice from "./Practice";
import { listOutbox, saveRunRecovery } from "./practiceRecovery";

beforeEach(resetPracticeBrowser);

const matchingExercise = {
  id: "71717171-7171-4717-8717-717171717171",
  formatId: "E033",
  lexicalTargets: [
    { entryId: "L015", senseId: "L015-S01", facet: "meaning", direction: "en-da" },
    { entryId: "L018", senseId: "L018-S01", facet: "meaning", direction: "en-da" },
    { entryId: "L041", senseId: "L041-S01", facet: "meaning", direction: "en-da" },
    { entryId: "L042", senseId: "L042-S01", facet: "meaning", direction: "en-da" },
  ],
  supportSkillIds: ["G001", "G002", "G003", "G004", "G005"],
  contentRevision: "pilot-17",
  generatorRevision: "generator-17",
  instruction: "Match each English meaning to its introduced Danish form.",
  context: "Use only the reviewed P1 material.",
  prompt: {
    before: "", after: "", choices: [], blocks: [], punctuation: "", cueLanguage: "en", answerLanguage: "da",
    matchingColumns: {
      left: [
        { id: "l0", text: "a newspaper" }, { id: "l1", text: "a written letter" },
        { id: "l2", text: "writes" }, { id: "l3", text: "a computer" },
      ],
      right: [
        { id: "r2", text: "skriver" }, { id: "r0", text: "en avis" },
        { id: "r3", text: "en computer" }, { id: "r1", text: "et brev" },
      ],
    },
  },
  introduction: [{ title: "A newspaper", body: "en avis", bodyLanguage: "da" }],
  hint: "Use each supplied item at most once.",
};

const correctPairs = [
  { leftId: "l0", rightId: "r0" }, { leftId: "l1", rightId: "r1" },
  { leftId: "l2", rightId: "r2" }, { leftId: "l3", rightId: "r3" },
];

const partialAttempt = {
  attemptId: "81818181-8181-4818-8818-818181818181",
  exerciseId: matchingExercise.id,
  targetSatisfaction: "unevaluable",
  linguisticStatus: "acceptable",
  linguisticScope: "danish-matching",
  responseConstraintStatus: "not-met",
  meaningSatisfaction: "unknown",
  issues: [],
  feedback: "Review the pair feedback.",
  completedSentence: "",
  evaluationRevision: "evaluation-17",
  hintUsed: true,
  retryNumber: 1,
  createdAt: 1,
  matchingResults: [
    { leftId: "l0", rightId: "r0", lexicalTarget: matchingExercise.lexicalTargets[0], targetSatisfaction: "met",
      meaningSatisfaction: "met", responseConstraintStatus: "met", linguisticStatus: "acceptable", feedback: "Correct. This pair matches." },
    ...matchingExercise.lexicalTargets.slice(1).map((lexicalTarget, index) => ({
      leftId: `l${index + 1}`, lexicalTarget, targetSatisfaction: "unevaluable", meaningSatisfaction: "unknown",
      responseConstraintStatus: "not-met", linguisticStatus: "unknown", feedback: "Choose a supplied Danish form for this meaning.",
    })),
  ],
};

function p1Run(focusId = "vocabulary-p1-s2", planRevision = "vocabulary-p1-s2-1") {
  return { ownerSessionId: owner, practiceSessionId: practiceId, focusId, planRevision, seed: 47 };
}

function matchingState(overrides: Record<string, unknown> = {}) {
  const item = {
    ...state().items[0]!, exerciseId: matchingExercise.id, formatId: "E033", lexicalTarget: undefined,
    matchingTarget: { facet: "meaning", direction: "en-da" },
    assessments: matchingExercise.lexicalTargets.map((lexicalTarget, index) => ({
      slotId: `l${index}`, label: ["a newspaper", "a written letter", "writes", "a computer"][index],
      lexicalTarget, assessmentMode: "matching", assessmentRole: "primary",
    })),
    canAdvance: false,
  };
  return state({
    focusId: "vocabulary-p1-s2", title: "Reading and writing", planRevision: "vocabulary-p1-s2-1",
    contentRevision: "pilot-17", generatorRevision: "generator-17", evaluationRevision: "evaluation-17",
    itemCount: 4, prerequisiteSkillIds: ["G001", "G002", "G003", "G004", "G005"],
    introductionRequired: true,
    introduction: [
      { title: "A newspaper", body: "en avis", bodyLanguage: "da" },
      { title: "A written letter", body: "et brev", bodyLanguage: "da" },
      { title: "Required support review", body: "Subjects, present forms, and subject-first order are supplied support." },
    ],
    currentExercise: matchingExercise,
    items: [item],
    ...overrides,
  });
}

function saveP1Run(focusId = "vocabulary-p1-s2", planRevision = "vocabulary-p1-s2-1") {
  saveRunRecovery(p1Run(focusId, planRevision));
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
}

function savedMatchingState(attemptId: string, pairs = correctPairs) {
  const saved = { ...partialAttempt, attemptId };
  const answer = { kind: "matching", pairs };
  return matchingState({
    introductionOpened: true,
    items: [{
      ...matchingState().items[0]!, lessonOpened: true, attemptIds: [attemptId], attemptCount: 1,
      firstAttempt: saved, lastAttempt: saved, firstAnswer: answer, lastAnswer: answer, canAdvance: false,
    }],
  });
}

it("requires direct-entry P1 teaching and keeps a partial matching draft through support and retry editing", async () => {
  saveP1Run();
  let serverState = matchingState();
  let attemptBody = "";
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith("/support")) {
      const request = JSON.parse(String(init?.body)) as { kind: string };
      const item = serverState.items[0]!;
      serverState = matchingState({
        introductionOpened: request.kind === "introduction" || serverState.introductionOpened,
        items: [{ ...item, lessonOpened: request.kind === "lesson" || item.lessonOpened }],
      });
      return json(serverState);
    }
    if (path.endsWith("/attempts")) {
      attemptBody = String(init?.body);
      const request = JSON.parse(attemptBody) as { attemptId: string; answer: { pairs: typeof correctPairs } };
      expect(listOutbox(practiceId)).toEqual([expect.objectContaining({ kind: "attempt", body: attemptBody })]);
      serverState = savedMatchingState(request.attemptId, request.answer.pairs);
      return json({ ...partialAttempt, attemptId: request.attemptId });
    }
    if (path.includes("/practice-sessions/")) return json(serverState);
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);

  expect(await screen.findByRole("button", { name: "Session introduction" })).toHaveFocus();
  expect(screen.queryByRole("group", { name: "Match English and Danish words" })).not.toBeInTheDocument();
  await user.click(screen.getByRole("button", { name: "Session introduction" }));
  expect(within(await screen.findByRole("region", { name: "Session introduction" })).getByText("en avis"))
    .toHaveAttribute("lang", "da");
  await user.click(screen.getByRole("button", { name: "a newspaper" }));
  await user.click(screen.getByRole("button", { name: "en avis" }));
  await user.click(screen.getByRole("button", { name: "a written letter" }));
  await user.click(screen.getByRole("button", { name: "Learn this first" }));
  expect(await screen.findByRole("button", { name: "Remove match for a newspaper" })).toBeEnabled();
  expect(screen.getByRole("button", { name: "a written letter" })).toHaveAttribute("aria-pressed", "true");
  await user.click(screen.getByRole("button", { name: "Check matches" }));

  expect(JSON.parse(attemptBody)).toMatchObject({
    exerciseId: matchingExercise.id, answer: { kind: "matching", pairs: [correctPairs[0]] }, hintUsed: true,
  });
  expect(listOutbox(practiceId)).toEqual([]);
  expect(await screen.findByRole("button", { name: "Continue" })).toBeDisabled();
  expect(screen.getByRole("button", { name: "a written letter" })).toHaveAttribute("aria-pressed", "false");
  await user.click(screen.getByRole("button", { name: "Try again" }));
  await waitFor(() => expect(screen.getByRole("group", { name: "Match English and Danish words" })).toHaveFocus());
  expect(screen.getByRole("button", { name: "a written letter" })).toHaveAttribute("aria-pressed", "false");
  const remove = screen.getByRole("button", { name: "Remove match for a newspaper" });
  expect(remove).toBeEnabled();
  await user.click(remove);
  expect(screen.getByRole("button", { name: "a newspaper" })).toBeEnabled();
  await user.click(screen.getByRole("button", { name: "a newspaper" }));
  await user.click(screen.getByRole("button", { name: "en avis" }));
  expect(screen.getByRole("button", { name: "Remove match for a newspaper" })).toBeEnabled();
});

it("restores and replays a lost four-pair P1 matching body byte-for-byte after reload", async () => {
  saveP1Run();
  let failAttempt = true;
  let serverState = matchingState({ introductionOpened: true });
  const bodies: string[] = [];
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith("/attempts")) {
      const body = String(init?.body);
      bodies.push(body);
      if (failAttempt) throw new TypeError("response lost after commit");
      const request = JSON.parse(body) as { attemptId: string };
      serverState = savedMatchingState(request.attemptId);
      return json({ ...partialAttempt, attemptId: request.attemptId });
    }
    if (path.includes("/practice-sessions/")) return json(serverState);
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  const first = renderWithProviders(<Practice />);
  for (const [index, pair] of correctPairs.entries()) {
    await user.click(await screen.findByRole("button", { name: matchingExercise.prompt.matchingColumns.left[index]!.text }));
    await user.click(screen.getByRole("button", { name: matchingExercise.prompt.matchingColumns.right.find(({ id }) => id === pair.rightId)!.text }));
  }
  await user.click(screen.getByRole("button", { name: "Check matches" }));
  expect(await screen.findByText("Your answer is not confirmed")).toBeVisible();
  const original = bodies[0]!;
  expect(JSON.parse(original)).toMatchObject({ answer: { kind: "matching", pairs: correctPairs } });
  first.unmount();

  const second = renderWithProviders(<Practice />);
  expect(await screen.findByText("Your answer is not confirmed")).toBeVisible();
  for (const { text } of matchingExercise.prompt.matchingColumns.left) {
    expect(screen.getByRole("button", { name: `Remove match for ${text}` })).toBeDisabled();
  }
  expect(bodies[1]).toBe(original);
  second.unmount();

  failAttempt = false;
  renderWithProviders(<Practice />);
  expect(await screen.findByRole("status")).toHaveTextContent("Review the pair feedback.");
  expect(bodies[2]).toBe(original);
  expect(listOutbox(practiceId)).toEqual([]);
});

function summaryRow(overrides: Record<string, unknown>) {
  return {
    label: "P1 result", plannedItems: 1, attemptedItems: 1, advancedItems: 1, attemptCount: 1, retryCount: 0,
    firstOutcomes: { ...outcomes, meaningMet: 1, meaningNotMet: 0, meaningUnknown: 0 },
    latestOutcomes: { ...outcomes, fullyMet: 1, targetNotMet: 0, meaningMet: 1, meaningNotMet: 0, meaningUnknown: 0 },
    firstWithSupport: 1, firstWithPriorExposure: 1, lessonOpenedItems: 0, hintOpenedItems: 0,
    ...overrides,
  };
}

function completedP1(focusId: string, planRevision: string, title: string, rows: unknown[]) {
  return matchingState({
    focusId, planRevision, title, state: "completed", cursor: focusId.endsWith("s5") ? 5 : 4,
    itemCount: focusId.endsWith("s5") ? 5 : 4, currentExercise: null, introductionOpened: true,
    summary: { ...completedState().summary, plannedItems: rows.length, completedItems: rows.length,
      attemptedItems: rows.length, introductionOpened: true, rows },
  });
}

it("renders P1 lexical and reading meaning summaries and opens next or repeat choices without discarding the run", async () => {
  saveP1Run("vocabulary-p1-s1", "vocabulary-p1-s1-1");
  const rows = [
    summaryRow({ label: "Works", lexicalTarget: { entryId: "L027", senseId: "L027-S01", facet: "meaning", direction: "da-en" } }),
    summaryRow({ label: "Work meaning match", assessmentMode: "matching",
      lexicalTarget: { entryId: "L027", senseId: "L027-S01", facet: "meaning", direction: "en-da" } }),
    summaryRow({ label: "Explicit object fact", readingTarget: { facet: "explicit-fact" }, readingRole: "object" }),
  ];
  let completed = completedP1("vocabulary-p1-s1", "vocabulary-p1-s1-1", "Work, reading and today", rows);
  let creations = 0;
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith("practice-profiles")) return json(profiles());
    if (path.endsWith("practice-sessions")) { creations += 1; return json(completed); }
    if (path.includes("/practice-sessions/")) return json(completed);
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  const first = renderWithProviders(<Practice />);

  expect(await screen.findByRole("article", { name: "Works" })).toHaveTextContent("Meaning recognition");
  expect(screen.getByRole("article", { name: "Work meaning match" })).toHaveTextContent("Bilingual matching");
  expect(screen.getByRole("article", { name: "Explicit object fact" })).toHaveTextContent("Explicit object fact");
  expect(screen.getAllByText(/1 meaning met/)).toHaveLength(6);
  await user.click(screen.getByRole("button", { name: "Next: Reading and writing" }));
  expect(screen.getByRole("radio", { name: "Reading and writing" })).toBeChecked();
  await user.click(screen.getByRole("button", { name: "Cancel" }));
  expect(await screen.findByRole("heading", { name: "Practice complete" })).toHaveFocus();
  expect(creations).toBe(0);
  expect(localStorage.getItem(`astrid.practice.v1.run.${practiceId}`)).not.toBeNull();
  first.unmount();

  saveP1Run("vocabulary-p1-s5", "vocabulary-p1-s5-1");
  completed = completedP1("vocabulary-p1-s5", "vocabulary-p1-s5-1", "Where and when", rows);
  renderWithProviders(<Practice />);
  await user.click(await screen.findByRole("button", { name: "Repeat this session" }));
  expect(screen.getByRole("radio", { name: "Where and when" })).toBeChecked();
  expect(screen.queryByRole("button", { name: /^Next:/ })).not.toBeInTheDocument();
  await user.click(screen.getByRole("button", { name: "Cancel" }));
  expect(await screen.findByRole("heading", { name: "Practice complete" })).toBeVisible();
  expect(creations).toBe(0);
});

it("lists all five independent P1 profiles in their own chooser group", async () => {
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("development-session")) return json({ mode: "development" });
    if (path.endsWith("practice-profiles")) return json(profiles());
    if (path.endsWith("practice-context")) return json(context());
    throw new Error(`Unexpected ${path}`);
  }));
  renderWithProviders(<Practice />);

  expect(await screen.findByRole("heading", { name: "A day of work and study" })).toBeVisible();
  for (const title of ["Work, reading and today", "Reading and writing", "Study, courses and Danish", "Coffee and water", "Where and when"]) {
    expect(screen.getByRole("radio", { name: title })).toBeVisible();
  }
});
