import type { ComponentType } from "react";
import { renderWithProviders } from "./render";

// Shared fixtures for the pages/playground/*.test.tsx suites.
export const session = {
  mode: "development", inspectorEnabled: false, contentRevision: "pilot-2",
  generatorRevision: "generator-2", evaluationRevision: "evaluation-2",
};
export const catalog = {
  contentRevision: "pilot-2", generatorRevision: "generator-2", evaluationRevision: "evaluation-2",
  sentenceCount: 99,
  skills: [
    { id: "G004", title: "Present form", explanation: "Choose a present verb.", prerequisiteIds: [] },
    { id: "G005", title: "Subject-first statements", explanation: "Arrange a statement.", prerequisiteIds: [] },
    { id: "G006", title: "Indefinite articles", explanation: "Choose an article.", prerequisiteIds: [] },
  ],
  formats: [
    { id: "E001", title: "Choose the present form", targetSkillId: "G004" },
    { id: "E002", title: "Supply the indefinite article", targetSkillId: "G006" },
    { id: "E003", title: "Arrange a subject-first statement", targetSkillId: "G005" },
    { id: "E004", title: "Supply the present form", targetSkillId: "G004" },
    { id: "E005", title: "Repair the verb form", targetSkillId: "G004" },
    { id: "E006", title: "Repair the indefinite article", targetSkillId: "G006" },
  ],
};
export const exercise = {
  id: "011835c0-bb42-47ef-b21e-ae21411b24a1",
  formatId: "E001", targetSkillId: "G004", supportSkillIds: [],
  contentRevision: "pilot-2", generatorRevision: "generator-2",
  instruction: "Choose the present-tense verb.", context: "You are describing what happens now.",
  prompt: { before: "Jeg", after: "en bog", choices: ["læse", "læser"], blocks: [], punctuation: "." },
  introduction: [{ title: "læse · to read", body: "Present form: læser" }],
  hint: "The present form usually ends in -r.",
};
export const attempt = {
  attemptId: "28d52d72-7a04-4a42-8e87-bf780f821510",
  exerciseId: exercise.id,
  targetSatisfaction: "met", linguisticStatus: "acceptable", issues: [],
  feedback: "You selected the present form.", completedSentence: "Jeg læser en bog.",
  evaluationRevision: "evaluation-2", hintUsed: false, retryNumber: 1, createdAt: 1,
};

export function json(body: unknown) {
  return Response.json(body, { headers: { "Content-Type": "application/json" } });
}


/** Renders a freshly imported Playground, so its module-level bootstrap state starts empty. */
export async function renderPlayground() {
  vi.resetModules();
  const Playground = (await import("../pages/playground/Playground")).default as ComponentType;
  renderWithProviders(<Playground />);
}
