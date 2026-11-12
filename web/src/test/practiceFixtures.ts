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
export const gapExercise = {
  id: "55555555-5555-4555-8555-555555555555", formatId: "E015", targetSkillId: "G004",
  supportSkillIds: ["G006"], contentRevision: "pilot-7", generatorRevision: "generator-7",
  instruction: "Complete both gaps.", context: "Choose a present verb and an indefinite article.",
  prompt: {
    before: "", after: "", choices: [], blocks: [], punctuation: ".", cue: "", cueLanguage: "da", answerLanguage: "da",
    gapPrompt: {
      segments: ["Jeg ", " ", " bog."],
      slots: [
        { id: "g0", label: "Present verb — Primary target", targetSkillId: "G004", assessmentRole: "primary",
          choices: [{ id: "c0", text: "læse" }, { id: "c1", text: "læser" }] },
        { id: "g1", label: "Indefinite article — Supporting grammar", targetSkillId: "G006", assessmentRole: "support",
          choices: [{ id: "c0", text: "et" }, { id: "c1", text: "en" }] },
      ],
    },
  },
  introduction: [], hint: "Choose each form for its own gap.",
} as const;
export const gapAttempt = {
  attemptId: "66666666-6666-4666-8666-666666666666", exerciseId: gapExercise.id,
  targetSatisfaction: "not-met", linguisticStatus: "known-issue", linguisticScope: "danish-gaps",
  responseConstraintStatus: "met", issues: [], feedback: "Review both selected forms.", completedSentence: "Jeg læse et bog.",
  evaluationRevision: "evaluation-7", hintUsed: false, retryNumber: 1, createdAt: 1,
  gapResults: [
    { slotId: "g0", choiceId: "c0", targetSkillId: "G004", assessmentRole: "primary", targetSatisfaction: "not-met",
      responseConstraintStatus: "met", linguisticStatus: "known-issue", feedback: "Choose the present form." },
    { slotId: "g1", choiceId: "c0", targetSkillId: "G006", assessmentRole: "support", targetSatisfaction: "not-met",
      responseConstraintStatus: "met", linguisticStatus: "known-issue", feedback: "Choose the article for bog." },
  ],
} as const;
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

export function gapState(overrides: Record<string, unknown> = {}) {
  return state({
    focusId: "verb-article-gaps", title: "Verbs and articles together", planRevision: "verb-article-gaps-1",
    contentRevision: "pilot-7", generatorRevision: "generator-7", evaluationRevision: "evaluation-7",
    itemCount: 4, followUpPlanRevision: "follow-up-3",
    introduction: [
      { title: "Verbs and articles", body: "Choose each form separately.", bodyLanguage: "en" },
      { title: "Worked example", body: "Jeg læser en bog.", bodyLanguage: "da" },
    ],
    items: [{
      position: 0, exerciseId: gapExercise.id, formatId: "E015", targetSkillId: "G004",
      assessments: [
        { slotId: "g0", label: "Present verb selection", targetSkillId: "G004", assessmentMode: "selection", assessmentRole: "primary" },
        { slotId: "g1", label: "Indefinite article selection", targetSkillId: "G006", assessmentMode: "selection", assessmentRole: "support" },
      ],
      lessonOpened: false, hintOpened: false, attemptIds: [], attemptCount: 0,
      firstAttempt: null, lastAttempt: null, firstAttemptContext: null, lastAttemptContext: null,
    }],
    currentExercise: gapExercise,
    ...overrides,
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
      { focusId: "module-one-singular", planRevision: "module-one-singular-1", title: "Talking about one person",
        description: "Subject words for one person.", itemCount: 6, prerequisiteSkillIds: ["G001", "G002"] },
      { focusId: "module-one-groups", planRevision: "module-one-groups-1", title: "Talking to and about groups",
        description: "Subject words for groups.", itemCount: 6, prerequisiteSkillIds: ["G001", "G002", "G003"] },
      { focusId: "module-one-er", planRevision: "module-one-er-1", title: "Using the verb to be",
        description: "The present form er.", itemCount: 6, prerequisiteSkillIds: ["G001", "G002", "G003"] },
      { focusId: "module-one-things", planRevision: "module-one-things-1", title: "Referring to one thing",
        description: "Reference words for one thing.", itemCount: 4, prerequisiteSkillIds: ["G001", "G002", "G003", "G006"] },
      { focusId: "module-one-present", planRevision: "module-one-present-1", title: "Using present verb forms",
        description: "Choose and type present forms of three Danish verbs.", itemCount: 6,
        prerequisiteSkillIds: ["G001", "G002", "G003"] },
      { focusId: "module-one-order", planRevision: "module-one-order-1", title: "Building subject-first statements",
        description: "Put Danish sentence parts and words in order.", itemCount: 6,
        prerequisiteSkillIds: ["G001", "G002", "G003", "G004"] },
      { focusId: "module-one-possession", planRevision: "module-one-possession-1", title: "Saying what someone has",
        description: "Choose and type the Danish present form har for possession.", itemCount: 4,
        prerequisiteSkillIds: ["G001", "G002", "G003", "G004", "G005"] },
      { focusId: "module-one-reading-words", planRevision: "module-one-reading-words-1", title: "Words for things you read",
        description: "Choose Danish words for a magazine, a book, a newspaper and a letter.", itemCount: 4,
        prerequisiteSkillIds: [] },
      { focusId: "module-one-personal", planRevision: "module-one-personal-1", title: "Names, homes and languages",
        description: "Choose and type Danish present forms for names, places and languages.", itemCount: 6,
        prerequisiteSkillIds: ["G001", "G002", "G003", "G004", "G005"] },
      { focusId: "module-one-ability", planRevision: "module-one-ability-1", title: "Using a verb after kan",
        description: "Choose and type the bare infinitive læse after kan.", itemCount: 4,
        prerequisiteSkillIds: ["G001", "G002", "G003", "G004", "G005"] },
      { focusId: "module-one-volition", planRevision: "module-one-volition-1", title: "Expressing an intention",
        description: "Interpret vil as intention and build one supplied vil statement.", itemCount: 2,
        prerequisiteSkillIds: ["G001", "G002", "G003", "G004", "G005"] },
      { focusId: "module-one-obligation", planRevision: "module-one-obligation-1", title: "Understanding a requirement",
        description: "Interpret skal as a requirement and build one supplied skal statement.", itemCount: 2,
        prerequisiteSkillIds: ["G001", "G002", "G003", "G004", "G005"] },
      { focusId: "module-one-permission", planRevision: "module-one-permission-1", title: "Understanding permission",
        description: "Interpret må as permission and build one supplied må statement.", itemCount: 2,
        prerequisiteSkillIds: ["G001", "G002", "G003", "G004", "G005"] },
      { focusId: "module-one-circumstances", planRevision: "module-one-circumstances-1", title: "Adding when and where",
        description: "Keep a supplied clause core in order and add one supplied time or place circumstance at the end.", itemCount: 4,
        prerequisiteSkillIds: ["G001", "G002", "G003", "G004", "G005"] },
      { focusId: "module-one-questions", planRevision: "module-one-questions-1", title: "Asking yes or no questions",
        description: "Arrange supplied Danish constituents into neutral positive yes/no questions.", itemCount: 4,
        prerequisiteSkillIds: ["G001", "G002", "G003", "G004", "G005"] },
      { focusId: "reading-basics", planRevision: "practice-1", title: "Mixed practice",
        description: "Mixed existing skills.", itemCount: 8, prerequisiteSkillIds: [] },
      { focusId: "present-verbs", planRevision: "present-verbs-1", title: "Present verb forms",
        description: "Present-tense verb forms.", itemCount: 6, prerequisiteSkillIds: ["G001", "G002", "G003"] },
      { focusId: "verb-article-gaps", planRevision: "verb-article-gaps-1", title: "Verbs and articles together",
        description: "Practise a present verb and an indefinite article in the same sentence.", itemCount: 4,
        prerequisiteSkillIds: ["G001", "G002", "G003", "G005"] },
      { focusId: "vocabulary-p1-s1", planRevision: "vocabulary-p1-s1-2", title: "Work, reading and today",
        description: "Recognise actions and read a routine.", itemCount: 4, prerequisiteSkillIds: ["G001", "G002"] },
      { focusId: "vocabulary-p1-s2", planRevision: "vocabulary-p1-s2-2", title: "Reading and writing",
        description: "Match reading and writing words.", itemCount: 4, prerequisiteSkillIds: ["G001", "G002", "G003", "G004", "G005"] },
      { focusId: "vocabulary-p1-s3", planRevision: "vocabulary-p1-s3-2", title: "Study, courses and Danish",
        description: "Match meanings and recall present forms.", itemCount: 5, prerequisiteSkillIds: ["G001", "G002", "G003", "G004", "G005"] },
      { focusId: "vocabulary-p1-s4", planRevision: "vocabulary-p1-s4-2", title: "Coffee and water",
        description: "Read beverage facts and choose a present form.", itemCount: 5, prerequisiteSkillIds: ["G001", "G002", "G003", "G004", "G005"] },
      { focusId: "vocabulary-p1-s5", planRevision: "vocabulary-p1-s5-2", title: "Where and when",
        description: "Read about places and times.", itemCount: 5, prerequisiteSkillIds: ["G001", "G002", "G003", "G004", "G005"] },
    ],
  };
}

export async function choosePractice(
  user: ReturnType<typeof userEvent.setup>,
  name: "Talking about one person" | "Talking to and about groups" | "Using the verb to be" | "Referring to one thing" | "Using present verb forms" | "Building subject-first statements" | "Saying what someone has" | "Words for things you read" | "Names, homes and languages" | "Using a verb after kan" | "Expressing an intention" | "Understanding a requirement" | "Understanding permission" | "Adding when and where" | "Asking yes or no questions" | "Mixed practice" | "Present verb forms" | "Verbs and articles together" = "Mixed practice",
) {
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
