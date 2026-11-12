import AxeBuilder from "@axe-core/playwright";
import { expect, test, type Page } from "@playwright/test";
import {
  ownerHeaders, PRACTICE_HEADERS, readPractice, type PracticeExercise, type PracticeState,
} from "../support/practice.js";

type ReviewContext = {
  ownerSessionId: string; sessionExpiresAt: number; focusId: string; planRevision: string;
  title: string; itemCount: number; sourceLessonCount: number;
};
type ReviewExercise = Omit<PracticeExercise, "prompt"> & {
  targetSkillId?: string;
  targetScopeId?: string;
  lexicalTarget?: { entryId: string; senseId: string; facet: string; direction: string };
  prompt: PracticeExercise["prompt"] & { before?: string; after?: string };
};

const LESSONS = [
  ["module-one-singular", "module-one-singular-1"],
  ["module-one-groups", "module-one-groups-1"],
  ["module-one-er", "module-one-er-1"],
  ["module-one-present", "module-one-present-1"],
] as const;
const SUBJECT_FORMS: Record<string, string> = {
  I: "jeg", "you (one person)": "du", he: "han", she: "hun", we: "vi",
  "you (several people)": "I", "they (several people)": "de",
};
const PRESENT_FORMS: Record<string, string> = {
  "Jeg |.": "arbejder", "Hun | en avis.": "læser", "Vi |.": "sover",
  "De |.": "arbejder", "I | en bog.": "læser", "Han |.": "sover",
};

async function reviewContext(page: Page) {
  const response = await page.request.get("/api/v1/practice-review-context");
  expect(response.status()).toBe(200);
  return await response.json() as ReviewContext;
}

async function introduceLesson(page: Page, owner: string, focusId: string, planRevision: string, seed: number) {
  const practiceSessionId = crypto.randomUUID();
  const created = await page.request.post("/api/v1/practice-sessions", {
    headers: ownerHeaders(owner), data: { practiceSessionId, focusId, planRevision, seed },
  });
  expect(created.status()).toBe(201);
  const introduced = await page.request.post(`/api/v1/practice-sessions/${practiceSessionId}/support`, {
    headers: ownerHeaders(owner), data: { kind: "introduction" },
  });
  expect(introduced.status()).toBe(200);
}

function answerFor(exercise: ReviewExercise) {
  if (exercise.targetScopeId === "vaere-present") return { kind: "text", text: "er" };
  if (exercise.targetScopeId === "named-three-verbs-present") {
    const answer = PRESENT_FORMS[`${exercise.prompt.before ?? ""}|${exercise.prompt.after ?? ""}`];
    expect(answer, "The supplied present-tense frame has a bounded authored oracle").toBeDefined();
    return { kind: "text", text: answer! };
  }
  expect(exercise.targetScopeId, "Only the four seeded lesson scopes enter this journey")
    .toMatch(/^(human-singular|human-group)$/);
  const answer = SUBJECT_FORMS[exercise.prompt.cue ?? ""];
  expect(answer, "The authored subject cue has a bounded Danish-form oracle").toBeDefined();
  return { kind: "text", text: answer! };
}

async function finish(page: Page, state: PracticeState) {
  if (!(state as PracticeState & { introductionOpened?: boolean }).introductionOpened) {
    await page.getByRole("button", { name: "Session introduction", exact: true }).click();
    await expect(page.getByRole("button", { name: "Check answer", exact: true })).toBeVisible();
    state = await readPractice(page.request, state.id);
  }
  while (state.state !== "completed") {
    const exercise = state.currentExercise as ReviewExercise;
    const attempt = await page.request.post("/api/v1/attempts", {
      headers: PRACTICE_HEADERS,
      data: { attemptId: crypto.randomUUID(), exerciseId: exercise.id, answer: answerFor(exercise), hintUsed: false },
    });
    expect(attempt.status()).toBe(200);
    const advanced = await page.request.post(`/api/v1/practice-sessions/${state.id}/advance`, {
      headers: ownerHeaders(state.ownerSessionId), data: { position: state.cursor },
    });
    expect(advanced.status()).toBe(200);
    state = await advanced.json() as PracticeState;
  }
  return state;
}

async function scan(page: Page) {
  const result = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"])
    .analyze();
  expect(result.violations.map(({ id, nodes }) => ({ id, targets: nodes.map((node) => node.target) }))).toEqual([]);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true);
}

test("cumulative review stays passive until preparation and explains an empty pool", async ({ page }) => {
  const learningRequests: string[] = [];
  page.on("request", (request) => {
    if (new URL(request.url()).pathname.startsWith("/api/")) learningRequests.push(request.url());
  });
  await page.goto("/#review");
  await expect(page.getByRole("heading", { name: "Review", exact: true })).toBeVisible();
  expect(learningRequests).toEqual([]);

  await page.getByRole("button", { name: "Prepare mixed review", exact: true }).click();
  await expect(page.getByText("No introduced material yet", { exact: true })).toBeVisible();
  await expect(page.getByRole("alert").filter({ hasText: "No introduced material yet" })).toBeFocused();
  await expect(page.getByText(/Open and save a lesson’s teaching first/)).toBeVisible();
  await expect(page.getByRole("button", { name: "Start mixed review", exact: true })).toHaveCount(0);
  expect(learningRequests.map((url) => new URL(url).pathname)).toEqual([
    "/api/v1/development-session", "/api/v1/practice-review-context",
  ]);
  await scan(page);
});

test("cumulative review reports short and full eligible pools and mixes introduced targets", async ({ page }) => {
  await page.goto("/#review");
  await page.getByRole("button", { name: "Prepare mixed review", exact: true }).click();
  await expect(page.getByText("No introduced material yet", { exact: true })).toBeVisible();
  const empty = await reviewContext(page);

  await introduceLesson(page, empty.ownerSessionId, ...LESSONS[0], 101);
  await page.getByRole("button", { name: "Refresh preparation", exact: true }).click();
  const short = await reviewContext(page);
  expect(short.itemCount).toBeGreaterThan(0);
  expect(short.itemCount).toBeLessThan(8);
  await expect(page.getByText(new RegExp(`shorter because only ${short.itemCount} eligible`))).toBeVisible();

  for (const [index, lesson] of LESSONS.slice(1).entries()) {
    await introduceLesson(page, empty.ownerSessionId, lesson[0], lesson[1], 102 + index);
  }
  await page.getByRole("button", { name: "Refresh preparation", exact: true }).click();
  const full = await reviewContext(page);
  expect(full).toMatchObject({ focusId: "cumulative-review", planRevision: "cumulative-review-1", itemCount: 8 });
  expect(full.sourceLessonCount).toBeGreaterThanOrEqual(2);
  await expect(page.getByText(`8 parts from ${full.sourceLessonCount} lessons`, { exact: true })).toBeVisible();
  await expect(page.getByRole("status")).toBeFocused();

  await page.getByRole("button", { name: "Start mixed review", exact: true }).click();
  await expect(page).toHaveURL(/\/practice\?session=/);
  const id = new URL(page.url()).searchParams.get("session")!;
  const state = await readPractice(page.request, id);
  const targets = new Set(state.items.map((item) =>
    String(item.targetScopeId ?? (item.lexicalTarget as { entryId?: string } | undefined)?.entryId ?? item.formatId)));
  expect(targets.size).toBeGreaterThan(1);
  expect(state.itemCount).toBe(8);
});

test("cumulative review replays an exact saved creation and resumes the URL-bound run", async ({ page }) => {
  await page.goto("/#review");
  await page.getByRole("button", { name: "Prepare mixed review", exact: true }).click();
  await expect(page.getByText("No introduced material yet", { exact: true })).toBeVisible();
  const empty = await reviewContext(page);
  for (const [index, lesson] of LESSONS.entries()) {
    await introduceLesson(page, empty.ownerSessionId, lesson[0], lesson[1], 201 + index);
  }
  await page.getByRole("button", { name: "Refresh preparation", exact: true }).click();
  await expect(page.getByRole("button", { name: "Start mixed review", exact: true })).toBeVisible();

  const bodies: string[] = [];
  await page.route("**/api/v1/practice-sessions", async (route) => {
    bodies.push(route.request().postData()!);
    if (bodies.length === 1) { await route.fetch(); await route.abort("connectionfailed"); }
    else await route.continue();
  });
  await page.getByRole("button", { name: "Start mixed review", exact: true }).click();
  await expect(page.getByRole("alert")).toBeVisible();
  const id = new URL(page.url()).searchParams.get("session")!;
  await page.reload();

  await page.getByRole("button", { name: "Session introduction", exact: true }).click();
  await expect(page.getByRole("button", { name: "Check answer", exact: true })).toBeVisible();
  expect(new URL(page.url()).searchParams.get("session")).toBe(id);
  expect(bodies).toHaveLength(2);
  expect(bodies[1]).toBe(bodies[0]);
  expect(JSON.parse(bodies[0]!)).toMatchObject({
    practiceSessionId: id, focusId: "cumulative-review", planRevision: "cumulative-review-1",
  });
});

for (const theme of ["light", "dark"] as const) {
  test(`cumulative prepared and completed views reflow accessibly in ${theme} mode`, async ({ page }) => {
    const pageErrors: Error[] = [];
    page.on("pageerror", (error) => pageErrors.push(error));
    await page.emulateMedia({ colorScheme: theme });
    await page.setViewportSize({ width: 320, height: 720 });
    await page.addInitScript(() => {
      const enlargeText = () => {
        const style = document.createElement("style");
        style.textContent = "html { font-size: 200% !important; }";
        document.documentElement.append(style);
      };
      if (document.documentElement) enlargeText();
      else document.addEventListener("DOMContentLoaded", enlargeText, { once: true });
    });
    await page.goto("/#review");
    expect(await page.evaluate(() => getComputedStyle(document.documentElement).fontSize)).toBe("32px");
    expect(pageErrors).toEqual([]);
    await page.getByRole("button", { name: "Prepare mixed review", exact: true }).click();
    await expect(page.getByText("No introduced material yet", { exact: true })).toBeVisible();
    const empty = await reviewContext(page);
    for (const [index, lesson] of LESSONS.entries()) {
      await introduceLesson(page, empty.ownerSessionId, lesson[0], lesson[1], 301 + index);
    }
    await page.getByRole("button", { name: "Refresh preparation", exact: true }).click();
    await expect(page.getByRole("button", { name: "Start mixed review", exact: true })).toBeVisible();
    await scan(page);
    await page.getByRole("button", { name: "Start mixed review", exact: true }).click();
    await expect(page).toHaveURL(/\/practice\?session=/);
    await expect(page.getByRole("button", { name: "Session introduction", exact: true })).toBeVisible();
    const id = new URL(page.url()).searchParams.get("session")!;
    await finish(page, await readPractice(page.request, id));
    await page.reload();

    expect(await page.evaluate(() => getComputedStyle(document.documentElement).fontSize)).toBe("32px");
    expect(pageErrors).toEqual([]);
    await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
    await expect(page.getByRole("button", { name: "Prepare another mixed review", exact: true })).toBeVisible();
    await expect(page.getByRole("button", { name: "Start selected review", exact: true })).toHaveCount(0);
    await scan(page);
    await page.getByRole("button", { name: "Prepare another mixed review", exact: true }).click();
    await expect(page).toHaveURL(/\/#review$/);
    await expect(page.getByRole("button", { name: "Prepare mixed review", exact: true })).toBeVisible();
  });
}
