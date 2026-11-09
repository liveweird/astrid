import AxeBuilder from "@axe-core/playwright";
import { expect, test, type Page } from "@playwright/test";
import { choosePractice, ownerHeaders, readPractice, type PracticeState } from "../support/practice.js";

const FOCUS = "Verbs and articles together";
const VERB = "Present verb — Primary target";
const ARTICLE = "Indefinite article — Supporting grammar";
const VERB_ROW = "Present verb selection";
const ARTICLE_ROW = "Indefinite article selection";
const ARTICLES: Record<string, string> = { bog: "en", avis: "en", bil: "en", hund: "en", brev: "et", hus: "et", bord: "et", æble: "et" };
const PRESENT = new Set(["har", "er", "læser", "spiser"]);
type Selection = { slotId: string; choiceId: string };
type GapExercise = {
  id: string;
  formatId: string;
  prompt: { gapPrompt: { segments: string[]; slots: { id: string; assessmentRole: string; choices: { id: string; text: string }[] }[] } };
};
type Outcome = { fullyMet: number; targetMet: number; targetNotMet: number; unevaluable: number };
type SummaryRow = {
  label: string; targetSkillId: string; assessmentMode: string; assessmentRole?: string;
  plannedItems: number; attemptCount: number; retryCount: number; firstOutcomes: Outcome; latestOutcomes: Outcome;
};
type GuidedState = PracticeState & {
  planRevision: string; contentRevision: string; generatorRevision: string; followUpPlanRevision?: string;
  followUp?: { sourcePracticeSessionId: string; selectedTargetKeys: string[] };
};

async function openGuided(page: Page) {
  await page.goto("/practice");
  await choosePractice(page, FOCUS);
  await expect(page.getByText("Part 1 of 4", { exact: true })).toBeVisible();
  return await readPractice(page.request, new URL(page.url()).searchParams.get("session")!) as GuidedState;
}

function gapExercise(state: PracticeState) {
  expect(state.currentExercise?.formatId).toBe("E015");
  return state.currentExercise as unknown as GapExercise;
}

function selections(exercise: GapExercise, verbCorrect = true, articleCorrect = true) {
  const { segments, slots } = exercise.prompt.gapPrompt;
  const article = ARTICLES[segments[2]!.trim().replace(/\.$/, "")];
  expect(article).toBeDefined();
  return slots.map((slot) => {
    const correct = slot.assessmentRole === "primary" ? verbCorrect : articleCorrect;
    return { slotId: slot.id, choiceId: slot.choices.find((choice) =>
      (slot.assessmentRole === "primary" ? PRESENT.has(choice.text) : choice.text === article) === correct)!.id };
  });
}

async function select(page: Page, exercise: GapExercise, selection: Selection) {
  const slot = exercise.prompt.gapPrompt.slots.find((candidate) => candidate.id === selection.slotId)!;
  const choice = slot.choices.find((candidate) => candidate.id === selection.choiceId)!;
  await page.getByRole("group", { name: slot.assessmentRole === "primary" ? VERB : ARTICLE, exact: true })
    .getByRole("button", { name: choice.text, exact: true }).click();
}

async function expectSelections(page: Page, exercise: GapExercise, answers: Selection[]) {
  for (const answer of answers) {
    const slot = exercise.prompt.gapPrompt.slots.find((candidate) => candidate.id === answer.slotId)!;
    const choice = slot.choices.find((candidate) => candidate.id === answer.choiceId)!;
    await expect(page.getByRole("group", { name: slot.assessmentRole === "primary" ? VERB : ARTICLE, exact: true })
      .getByRole("button", { name: choice.text, exact: true })).toHaveAttribute("aria-pressed", "true");
  }
}

async function check(page: Page) {
  await page.getByRole("button", { name: "Check gaps", exact: true }).click();
  await expect(page.getByRole("status")).toBeFocused();
  for (const name of ["Present verb feedback", "Indefinite article feedback"]) {
    await expect(page.getByRole("group", { name, exact: true })).toBeVisible();
  }
}

async function continueItem(page: Page, position: number) {
  await page.getByRole("button", { name: "Continue", exact: true }).click();
  if (position === 3) await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeFocused();
  else await expect(page.getByText(`Part ${position + 2} of 4`, { exact: true })).toBeVisible();
}

async function finishWrong(page: Page, state: GuidedState) {
  while (state.state !== "completed") {
    const exercise = state.currentExercise!;
    const answer = exercise.formatId === "E015"
      ? { kind: "gaps", selections: selections(gapExercise(state), false, false) }
      : { kind: "text", text: exercise.formatId === "E001" ? exercise.prompt.choices.find((choice) => !PRESENT.has(choice))! : "unfamiliar" };
    const response = await page.request.post("/api/v1/attempts", {
      headers: { "X-Astrid-Client": "playground" },
      data: { attemptId: crypto.randomUUID(), exerciseId: exercise.id, answer, hintUsed: false },
    });
    expect(response.status()).toBe(200);
    const advanced = await page.request.post(`/api/v1/practice-sessions/${state.id}/advance`, {
      headers: ownerHeaders(state.ownerSessionId), data: { position: state.cursor },
    });
    expect(advanced.status()).toBe(200);
    state = await advanced.json() as GuidedState;
  }
  return state;
}

async function scan(page: Page) {
  const result = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]).analyze();
  expect(result.violations.map(({ id, nodes }) => ({ id, targets: nodes.map((node) => node.target) }))).toEqual([]);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy();
  const overflow = await page.locator("#main-content p, #main-content h2, #main-content h3, #main-content h4").evaluateAll((elements) => elements.flatMap((element) => {
    const bounds = element.getBoundingClientRect();
    if (bounds.width === 0 || bounds.height === 0) return [];
    const range = document.createRange();
    range.selectNodeContents(element);
    return [...range.getClientRects()].some((rect) => rect.width > 0 &&
      (rect.left < bounds.left - 1 || rect.right > bounds.right + 1)) ? [element.textContent] : [];
  }));
  expect(overflow, "Text stays within its own summary column or teaching paragraph").toEqual([]);
}

test("guided gaps keep four exercises and separate primary and supporting summary outcomes", async ({ page }) => {
  let state = await openGuided(page);
  expect(state).toMatchObject({ planRevision: "verb-article-gaps-1", contentRevision: "pilot-7", generatorRevision: "generator-7", itemCount: 4, followUpPlanRevision: "follow-up-3" });
  expect(state.items.map((item) => item.formatId)).toEqual(["E015", "E015", "E015", "E015"]);
  for (let position = 0; position < 4; position += 1) {
    const exercise = gapExercise(state);
    const answers = selections(exercise, position !== 2, position !== 0);
    await select(page, exercise, answers[0]!);
    if (position !== 1) await select(page, exercise, answers[1]!);
    if (position === 0) {
      await page.getByRole("button", { name: "Show hint", exact: true }).click();
      await expect(page.getByRole("button", { name: "Check gaps", exact: true })).toBeEnabled();
      await expectSelections(page, exercise, answers);
    }
    await check(page);
    if (position === 0) {
      await expect(page.getByText("Verb target met; review the article.", { exact: true })).toBeVisible();
      await expect(page.getByRole("status").getByText("Good work", { exact: true })).toHaveCount(0);
      await page.getByRole("button", { name: "Try again", exact: true }).click();
      await expect(page.getByRole("group", { name: "Present verb and article gaps", exact: true })).toBeFocused();
      const retained = exercise.prompt.gapPrompt.slots[0]!.choices.find((choice) => choice.id === answers[0]!.choiceId)!;
      await expect(page.getByRole("group", { name: VERB, exact: true }).getByRole("button", { name: retained.text, exact: true })).toHaveAttribute("aria-pressed", "true");
      await select(page, exercise, selections(exercise)[1]!);
      await page.getByRole("button", { name: "Learn this first", exact: true }).click();
      await expect(page.getByRole("button", { name: "Check gaps", exact: true })).toBeEnabled();
      await check(page);
    }
    await continueItem(page, position);
    state = await readPractice(page.request, state.id) as GuidedState;
    if (position < 3) await expect(page.getByRole("button", { name: "Check gaps", exact: true })).toBeDisabled();
  }
  expect(state.summary).toMatchObject({ plannedItems: 4, attemptedItems: 4, completedItems: 4, attemptCount: 5, retryCount: 1 });
  const rows = state.summary.rows as SummaryRow[];
  expect(rows.map((row) => [row.label, row.assessmentMode, row.assessmentRole])).toEqual([
    [VERB_ROW, "selection", "primary"], [ARTICLE_ROW, "selection", "support"],
  ]);
  expect(rows[0]).toMatchObject({ plannedItems: 4, attemptCount: 5, retryCount: 1,
    firstOutcomes: { fullyMet: 3, targetMet: 3, targetNotMet: 1, unevaluable: 0 },
    latestOutcomes: { fullyMet: 3, targetMet: 3, targetNotMet: 1, unevaluable: 0 } });
  expect(rows[1]).toMatchObject({ firstOutcomes: { fullyMet: 2, targetMet: 2, targetNotMet: 1, unevaluable: 1 },
    latestOutcomes: { fullyMet: 3, targetMet: 3, targetNotMet: 0, unevaluable: 1 } });
  for (const row of rows) await expect(page.getByRole("article", { name: row.label, exact: true }).getByRole("checkbox")).toBeVisible();
  const raw = await (await page.request.get(`/api/v1/practice-sessions/${state.id}`)).text();
  await page.reload();
  await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
  expect(await (await page.request.get(`/api/v1/practice-sessions/${state.id}`)).text()).toBe(raw);
});

for (const target of ["verb", "article", "both"] as const) {
  test(`guided gaps create a selected-only ${target} review with explicit article recall`, async ({ page }) => {
    const source = await finishWrong(page, await openGuided(page));
    const frozen = await (await page.request.get(`/api/v1/practice-sessions/${source.id}`)).text();
    await page.reload();
    await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
    if (target !== "article") await page.getByRole("article", { name: VERB_ROW, exact: true }).getByRole("checkbox").check();
    if (target !== "verb") {
      await page.getByRole("article", { name: ARTICLE_ROW, exact: true }).getByRole("checkbox").check();
      await expect(page.getByText(/Articles will be supplied in writing/)).toBeVisible();
    }
    await page.getByRole("button", { name: "Start selected review", exact: true }).click();
    await expect(page.getByText("Part 1 of 4", { exact: true })).toBeVisible();
    const child = await readPractice(page.request, new URL(page.url()).searchParams.get("session")!) as GuidedState;
    expect(child.planRevision).toBe("follow-up-3");
    const formats = new Set(child.items.map((item) => item.formatId));
    expect(formats).toEqual(new Set(target === "both" ? ["E001", "E002"] : [target === "verb" ? "E001" : "E002"]));
    expect(child.items.filter((item) => item.formatId === "E002").every((item) => item.assessmentMode === "recall")).toBe(true);
    expect(child.followUp?.sourcePracticeSessionId).toBe(source.id);
    await expect(page.getByRole("link", { name: "Source practice", exact: true })).toHaveAttribute("href", `/practice?session=${source.id}`);
    const completed = await finishWrong(page, child);
    await page.reload();
    await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
    if (target !== "verb") {
      const article = (completed.summary.rows as SummaryRow[]).find((row) => row.targetSkillId === "G006")!;
      expect(article.assessmentMode).toBe("recall");
      await page.getByRole("article", { name: article.label, exact: true }).getByRole("checkbox").check();
      await page.getByRole("button", { name: "Start selected review", exact: true }).click();
      await expect(page.getByText("Part 1 of 4", { exact: true })).toBeVisible();
      const grandchild = await readPractice(page.request, new URL(page.url()).searchParams.get("session")!) as GuidedState;
      expect(grandchild.items.every((item) => item.formatId === "E002")).toBe(true);
      expect(grandchild.followUp?.selectedTargetKeys).toEqual(["grammar:G006:recall"]);
    }
    expect(await (await page.request.get(`/api/v1/practice-sessions/${source.id}`)).text()).toBe(frozen);
  });
}

for (const committed of [false, true]) {
  test(`guided gaps replay exact reversed slot answers through reload after ${committed ? "committed" : "uncommitted"} response loss`, async ({ page }) => {
    const state = await openGuided(page), exercise = gapExercise(state);
    const bodies: string[] = [];
    await page.route("**/api/v1/attempts", async (route) => {
      bodies.push(route.request().postData()!);
      if (bodies.length === 1) {
        if (committed) await route.fetch();
        await route.abort("connectionfailed");
      } else await route.continue();
    });
    const answers = selections(exercise).reverse();
    for (const answer of answers) await select(page, exercise, answer);
    await page.getByRole("button", { name: "Check gaps", exact: true }).click();
    await expect(page.getByRole("button", { name: "Retry saved operation", exact: true })).toBeVisible();
    for (const name of ["Clear present verb selection", "Clear article selection"]) await expect(page.getByRole("button", { name, exact: true })).toBeDisabled();
    await page.reload();
    await expect(page.getByRole("status").getByText("Good work", { exact: true })).toBeVisible();
    expect(bodies).toHaveLength(2);
    expect(bodies[1]).toBe(bodies[0]);
    expect(JSON.parse(bodies[0]!) as unknown).toMatchObject({ answer: { kind: "gaps", selections: answers } });
    expect((await readPractice(page.request, state.id)).items[0]!.attemptCount).toBe(1);
    await page.getByRole("button", { name: "Try again", exact: true }).click();
    await expect(page.getByRole("group", { name: "Present verb and article gaps", exact: true })).toBeFocused();
    await page.getByRole("button", { name: "Clear article selection", exact: true }).click();
    await check(page);
    expect((await readPractice(page.request, state.id)).items[0]!.attemptCount).toBe(2);
  });
}

for (const theme of ["light", "dark"] as const) {
  for (const viewport of [{ width: 320, height: 844 }, { width: 844, height: 390 }]) {
    test(`guided gaps support lessons and per-slot summaries reflow in ${theme} at ${viewport.width} with enlarged text`, async ({ page }) => {
      await page.emulateMedia({ colorScheme: theme });
      await page.setViewportSize(viewport);
      let state = await openGuided(page);
      await page.addStyleTag({ content: "html { font-size: 200% !important; }" });
      await scan(page);
      const intro = page.getByRole("button", { name: "Session introduction", exact: true });
      await intro.focus();
      await page.keyboard.press("Enter");
      const example = page.getByText("Jeg læser en bog.", { exact: true });
      await expect(example).toBeVisible();
      expect(await example.evaluate((element) => element.closest("[lang]")?.getAttribute("lang"))).toBe("da");
      await scan(page);
      await page.getByRole("button", { name: "Learn this first", exact: true }).click();
      await scan(page);
      const exercise = gapExercise(state);
      await select(page, exercise, selections(exercise)[0]!);
      await check(page);
      await scan(page);
      await continueItem(page, 0);
      state = await readPractice(page.request, state.id) as GuidedState;
      await finishWrong(page, state);
      await page.reload();
      await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
      await page.addStyleTag({ content: "html { font-size: 200% !important; }" });
      await scan(page);
    });
  }
}

for (const stage of ["creation", "follow-up"] as const) {
  for (const committed of [false, true]) {
    test(`guided gaps replay ${stage} identity after ${committed ? "committed" : "uncommitted"} response loss`, async ({ page }) => {
      let source: GuidedState | null = null;
      if (stage === "follow-up") {
        source = await finishWrong(page, await openGuided(page));
        await page.reload();
        await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
      }
      const bodies: string[] = [];
      await page.route(stage === "creation" ? "**/api/v1/practice-sessions" : "**/api/v1/practice-sessions/*/follow-ups", async (route) => {
        bodies.push(route.request().postData()!);
        if (bodies.length === 1) {
          if (committed) await route.fetch();
          await route.abort("connectionfailed");
        } else await route.continue();
      });
      if (stage === "creation") {
        await page.goto("/practice");
        await choosePractice(page, FOCUS);
      } else {
        await page.getByRole("article", { name: ARTICLE_ROW, exact: true }).getByRole("checkbox").check();
        await page.getByRole("button", { name: "Start selected review", exact: true }).click();
      }
      await expect(page.getByRole("button", { name: "Retry saved operation", exact: true })).toBeVisible();
      const id = new URL(page.url()).searchParams.get("session");
      await page.reload();
      await expect(page.getByText("Part 1 of 4", { exact: true })).toBeVisible();
      expect(new URL(page.url()).searchParams.get("session")).toBe(id);
      expect(bodies).toHaveLength(2);
      expect(bodies[1]).toBe(bodies[0]);
      const state = await readPractice(page.request, id!) as GuidedState;
      expect(state.planRevision).toBe(stage === "creation" ? "verb-article-gaps-1" : "follow-up-3");
      expect(state.summary.attemptCount).toBe(0);
      if (source) {
        expect(state.followUp).toEqual({ sourcePracticeSessionId: source.id, selectedTargetKeys: ["grammar:G006:selection"] });
        expect(state.items.every((item) => item.formatId === "E002")).toBe(true);
      }
    });
  }
}
