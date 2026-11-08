import { screen } from "@testing-library/react";
import type userEvent from "@testing-library/user-event";

// Shared fixtures for the pages/practice/*.test.tsx suites.
export const owner = "11111111-1111-4111-8111-111111111111";
export const practiceId = "22222222-2222-4222-8222-222222222222";
export const exercise = {
  id: "33333333-3333-4333-8333-333333333333", formatId: "E007", lexicalTarget: {
    entryId: "L014", senseId: "L014-S01", facet: "meaning", direction: "da-en",
  }, supportSkillIds: [], contentRevision: "pilot-3", generatorRevision: "generator-3",
  instruction: "Choose the English meaning.", context: "Read the Danish word.",
  prompt: { before: "", after: "", choices: ["book", "dog", "house"], blocks: [], punctuation: ".", cue: "bog", cueLanguage: "da", answerLanguage: "en" },
  introduction: [{ title: "bog · book", body: "A noun for something you read." }], hint: "Think of something with pages.",
};
export const wrongAttempt = {
  attemptId: "44444444-4444-4444-8444-444444444444", exerciseId: exercise.id,
  targetSatisfaction: "not-met", linguisticStatus: "acceptable", linguisticScope: "english-choice",
  issues: [], feedback: "Look at the word once more.", completedSentence: "",
  evaluationRevision: "evaluation-3", hintUsed: true, retryNumber: 1, createdAt: 1,
};
export const outcomes = { fullyMet: 0, targetMet: 0, targetNotMet: 1, unevaluable: 0, languageUnknown: 0, languageKnownIssue: 0, responseConstraintNotMet: 0 };

export function state(overrides: Record<string, unknown> = {}) {
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

export function completedState() {
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

export function json(body: unknown, status = 200) {
  const type = status >= 400 ? "application/problem+json" : "application/json";
  return Response.json(body, { status, headers: { "Content-Type": type } });
}

export function context(ownerSessionId = owner) {
  return { ownerSessionId, sessionExpiresAt: 9999999999999, focusId: "reading-basics", title: "Words and simple statements", planRevision: "practice-1", itemCount: 1 };
}

export function profiles(ownerSessionId = owner) {
  return {
    ownerSessionId, sessionExpiresAt: 9999999999999, profiles: [
      { focusId: "reading-basics", planRevision: "practice-1", title: "Mixed practice",
        description: "Mixed existing skills.", itemCount: 8, prerequisiteSkillIds: [] },
      { focusId: "present-verbs", planRevision: "present-verbs-1", title: "Present verb forms",
        description: "Present-tense verb forms.", itemCount: 6, prerequisiteSkillIds: ["G001", "G002", "G003"] },
    ],
  };
}

export async function choosePractice(user: ReturnType<typeof userEvent.setup>, name: "Mixed practice" | "Present verb forms" = "Mixed practice") {
  await screen.findByRole("heading", { name: "Choose your practice" });
  await user.click(screen.getByRole("radio", { name }));
  await user.click(screen.getByRole("button", { name: "Start practice" }));
}


export type AttemptRequestLike = { attemptId: string };

export function resetPracticeBrowser() {
  localStorage.clear();
  window.history.replaceState(null, "", "/practice");
  vi.restoreAllMocks();
}
