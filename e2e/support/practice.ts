import { expect, type APIRequestContext, type Page } from "@playwright/test";

export const PRACTICE_HEADERS = { "X-Astrid-Client": "playground" };
export type PracticeExercise = {
  id: string;
  formatId: string;
  prompt: { cue?: string; choices: string[]; blocks: { id: string; text: string }[] };
};
export type PracticeContext = {
  introductionOpened: boolean;
  lessonOpened: boolean;
  hintOpened: boolean;
  priorExposure: { position: number; source: string; keys: string[] }[];
};
export type PracticeState = {
  id: string;
  ownerSessionId: string;
  state: string;
  cursor: number;
  itemCount: number;
  currentExercise: PracticeExercise | null;
  items: {
    position: number;
    exerciseId: string;
    attemptCount: number;
    attemptIds: string[];
    firstAttemptContext: PracticeContext | null;
    lastAttemptContext: PracticeContext | null;
    [key: string]: unknown;
  }[];
  summary: { attemptedItems: number; completedItems: number; attemptCount: number; retryCount: number; rows: unknown[] };
};

export async function practiceOwner(request: APIRequestContext) {
  const session = await request.post("/api/v1/development-session", { headers: PRACTICE_HEADERS, data: {} });
  expect(session.ok()).toBeTruthy();
  const response = await request.get("/api/v1/practice-context");
  expect(response.status()).toBe(200);
  return (await response.json() as { ownerSessionId: string }).ownerSessionId;
}

export function ownerHeaders(owner: string) {
  return { ...PRACTICE_HEADERS, "X-Astrid-Practice-Owner": owner };
}

export function creationBody() {
  return { practiceSessionId: crypto.randomUUID(), focusId: "reading-basics", planRevision: "practice-1", seed: 47 };
}

export async function createPractice(request: APIRequestContext) {
  const owner = await practiceOwner(request);
  const body = creationBody();
  const response = await request.post("/api/v1/practice-sessions", { headers: ownerHeaders(owner), data: body });
  expect(response.status()).toBe(201);
  return { owner, body, state: await response.json() as PracticeState };
}

export async function readPractice(request: APIRequestContext, id: string) {
  const response = await request.get(`/api/v1/practice-sessions/${id}`);
  expect(response.status()).toBe(200);
  return await response.json() as PracticeState;
}

const meanings: Record<string, string> = { bog: "book", avis: "newspaper", brev: "letter (written message)" };
export function wrongAnswer(exercise: PracticeExercise) {
  if (exercise.formatId === "E001") {
    const infinitives = new Set(["sove", "have", "læse", "spise", "være"]);
    return { kind: "text", text: exercise.prompt.choices.find((choice) => infinitives.has(choice))! };
  }
  if (exercise.formatId === "E007") {
    return { kind: "text", text: exercise.prompt.choices.find((choice) => choice !== meanings[exercise.prompt.cue!])! };
  }
  if (exercise.formatId === "E003") {
    const verb = exercise.prompt.blocks.find((block) => block.text === "læser")!;
    return { kind: "order", blockIds: [verb.id, ...exercise.prompt.blocks.filter((block) => block !== verb).map((block) => block.id)] };
  }
  return { kind: "text", text: "unfamiliar" };
}

export async function saveWrongAttempt(request: APIRequestContext, exercise: PracticeExercise) {
  const body = { attemptId: crypto.randomUUID(), exerciseId: exercise.id, answer: wrongAnswer(exercise), hintUsed: false };
  const response = await request.post("/api/v1/attempts", { headers: PRACTICE_HEADERS, data: body });
  expect(response.status()).toBe(200);
  expect((await response.json() as { targetSatisfaction: string }).targetSatisfaction).toBe("not-met");
  return body;
}

export async function choosePractice(page: Page, focus = "Mixed practice") {
  await expect(page.getByRole("heading", { name: "Choose your practice", exact: true })).toBeVisible();
  await page.getByRole("radio", { name: focus, exact: true }).check();
  await page.getByRole("button", { name: "Start practice", exact: true }).click();
}
