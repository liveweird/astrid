import AxeBuilder from "@axe-core/playwright";
import { expect, test, type Page } from "@playwright/test";
import { openGenerationControls } from "../support/generation-controls.js";
import { expectFeedbackBadgesUnclipped } from "../support/feedback-layout.js";
import { textLanguage } from "../support/vocabulary.js";

type NewExercise = {
  id: string;
  formatId: string;
  targetSkillId?: string;
  lexicalTarget?: unknown;
  readingTarget?: { facet: string };
  prompt: { cue: string; passage?: string[]; choices: string[]; blocks: unknown[] };
};
type Grade = {
  targetSatisfaction: string;
  meaningSatisfaction?: string;
  linguisticStatus: string;
  linguisticScope?: string;
  responseConstraintStatus: string;
  completedSentence: string;
  hintUsed: boolean;
  retryNumber: number;
};
const WRITING = "Write a Danish sentence";
const READING = "Read a short text";
// These seed/index coordinates use the existing 99-sentence pool, also exercised by E004.
const WRITTEN = "Hun læser et brev.";
const meanings: Record<string, string> = { bog: "book", avis: "newspaper", brev: "letter (written message)" };

async function open(page: Page) {
  await page.goto("/playground");
  await expect(page.getByRole("button", { name: "Check answer", exact: true })).toBeVisible();
}

async function choose(page: Page, title: string, index = 0) {
  await openGenerationControls(page);
  await page.getByRole("textbox", { name: "Next generation seed", exact: true }).fill("47");
  await page.getByRole("textbox", { name: "Next generation index", exact: true }).fill(String(index));
  const selector = page.getByRole("combobox", { name: "Exercise type", exact: true });
  const response = page.waitForResponse((candidate) => candidate.request().method() === "POST" &&
    new URL(candidate.url()).pathname === "/api/v1/exercises" && candidate.status() === 201);
  if (await selector.inputValue() === title) await page.getByRole("button", { name: "Regenerate", exact: true }).click();
  else {
    await selector.click();
    await page.getByRole("option", { name: title, exact: true }).click();
  }
  const exercise = await (await response).json() as NewExercise;
  await expect(page.getByText(exercise.prompt.cue, { exact: true })).toBeVisible();
  return exercise;
}

async function grade(page: Page) {
  const response = page.waitForResponse((candidate) => candidate.request().method() === "POST" &&
    new URL(candidate.url()).pathname === "/api/v1/attempts" && candidate.status() === 200);
  await page.getByRole("button", { name: "Check answer", exact: true }).click();
  const result = await (await response).json() as Grade;
  await expect(page.getByRole("status")).toBeFocused();
  return result;
}

function answerFromText(exercise: NewExercise) {
  const query = exercise.prompt.cue.match(/what does (he|she) (have|read)\?/i);
  expect(query, "English question identifies participant and action").not.toBeNull();
  const subject = query![1]!.toLowerCase() === "she" ? "Hun" : "Han";
  const verb = query![2]!.toLowerCase() === "read" ? "læser" : "har";
  const facts = exercise.prompt.passage!.map((line) => line.match(/^(Han|Hun) (har|læser) (en|et) (bog|avis|brev)\.$/));
  expect(facts.every(Boolean), "Every visible fact uses an authored pilot clause").toBe(true);
  const matching = facts.filter((fact) => fact![1] === subject && fact![2] === verb);
  expect(matching).toHaveLength(1);
  expect(new Set(facts.map((fact) => fact![4])).size).toBe(3);
  return meanings[matching[0]![4]!]!;
}

async function scan(page: Page) {
  const result = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]).analyze();
  expect(result.violations.map(({ id, nodes }) => ({ id, targets: nodes.map((node) => node.target) }))).toEqual([]);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy();
}

test("sentence writing accepts a whole response with keyboard Danish insertion and separate meaning evidence", async ({ page }) => {
  await open(page);
  const exercise = await choose(page, WRITING, 5);
  expect(exercise.targetSkillId).toBe("G005");
  expect(exercise.readingTarget).toBeUndefined();
  expect(exercise.prompt.choices).toEqual([]);
  expect(exercise.prompt.blocks).toEqual([]);
  expect(exercise.prompt.cue).toBe("She reads a letter (written message).");
  await expect(page.getByText(WRITTEN, { exact: true })).toBeHidden();
  const input = page.getByRole("textbox", { name: "Danish sentence", exact: true });
  await expect(input).toHaveAttribute("lang", "da");
  expect(await textLanguage(page.getByText(exercise.prompt.cue, { exact: true }))).toBe("en");
  await input.fill("Hun lXser et brev.");
  await input.press(process.platform === "darwin" ? "Meta+ArrowLeft" : "Home");
  for (let step = 0; step < 5; step += 1) await input.press("ArrowRight");
  await input.press("Shift+ArrowRight");
  const tab = process.platform === "darwin" && page.context().browser()?.browserType().name() === "webkit" ? "Alt+Tab" : "Tab";
  await page.keyboard.press(tab);
  await expect(page.getByRole("button", { name: "Insert æ", exact: true })).toBeFocused();
  await page.keyboard.press("Enter");
  await expect(input).toHaveValue(WRITTEN);
  await expect(input).toBeFocused();
  expect(await input.evaluate((element: HTMLTextAreaElement) => [element.selectionStart, element.selectionEnd])).toEqual([6, 6]);
  const result = await grade(page);
  expect(result).toMatchObject({ targetSatisfaction: "met", meaningSatisfaction: "met", linguisticStatus: "acceptable", linguisticScope: "danish-sentence", responseConstraintStatus: "met", completedSentence: WRITTEN, hintUsed: false });
  await expect(page.getByRole("status").getByText("Meaning matched", { exact: true })).toBeVisible();
  await expect(page.getByRole("status").getByText("Good work", { exact: true })).toBeVisible();
});

test("writing distinguishes preserved meaning, subject-first order and a valid different statement", async ({ page }) => {
  await open(page);
  await choose(page, WRITING, 5);
  const cases = [
    { text: "Han læser et brev.", target: "met", meaning: "not-met" },
    { text: "Hun har et brev.", target: "met", meaning: "not-met" },
    { text: "Et brev læser hun.", target: "not-met", meaning: "met" },
  ];
  for (const item of cases) {
    await page.getByRole("textbox", { name: "Danish sentence", exact: true }).fill(item.text);
    const result = await grade(page);
    expect(result).toMatchObject({ targetSatisfaction: item.target, meaningSatisfaction: item.meaning, linguisticStatus: "acceptable", responseConstraintStatus: "met", completedSentence: item.text });
    await expect(page.getByRole("status").getByText(item.meaning === "met" ? "Meaning matched" : "Meaning to revisit", { exact: true })).toBeVisible();
    await expect(page.getByRole("status").getByText("Good work", { exact: true })).toHaveCount(0);
    await page.getByRole("button", { name: "Try again", exact: true }).click();
  }
});

test("writing diagnoses bounded form issues and reports unsupported or extra writing without accepting a prefix", async ({ page }) => {
  await open(page);
  await choose(page, WRITING, 5);
  const cases = [
    { text: "Hun læser en brev.", target: "met", meaning: "met", language: "known-issue", shape: "met" },
    { text: "Hun læse et brev.", target: "not-met", meaning: "met", language: "known-issue", shape: "met" },
    { text: "Hun studerer et brev.", target: "unevaluable", meaning: "unknown", language: "unknown", shape: "met" },
    { text: "Hun læser et brev. Han sover.", target: "unevaluable", meaning: "unknown", language: "unknown", shape: "not-met" },
    { text: "Hun læser et brev; Han sover", target: "unevaluable", meaning: "unknown", language: "unknown", shape: "not-met" },
  ];
  for (const item of cases) {
    await page.getByRole("textbox", { name: "Danish sentence", exact: true }).fill(item.text);
    const result = await grade(page);
    expect(result).toMatchObject({ targetSatisfaction: item.target, meaningSatisfaction: item.meaning, linguisticStatus: item.language, responseConstraintStatus: item.shape, completedSentence: item.text });
    await expect(page.getByRole("status").getByText("Good work", { exact: true })).toHaveCount(0);
    await expect(page.getByRole("status").getByText("Supplied choice required", { exact: true })).toHaveCount(0);
    if (item.shape === "not-met") await expect(page.getByRole("status").getByText("One statement required", { exact: true })).toBeVisible();
    if (item.language === "unknown") await expect(page.getByRole("status").getByText(/not a general judgment about Danish/)).toBeVisible();
    await page.getByRole("button", { name: "Try again", exact: true }).click();
  }
});

test("optional writing reference records support across retries without preassembling the answer", async ({ page }) => {
  await open(page);
  await choose(page, WRITING, 5);
  await page.getByRole("button", { name: "Learn this first", exact: true }).click();
  const lesson = page.getByRole("region", { name: "Learn this first", exact: true });
  await expect(lesson).toContainText("læser");
  await expect(lesson).not.toContainText(WRITTEN);
  for (let retry = 1; retry <= 2; retry += 1) {
    await page.getByRole("textbox", { name: "Danish sentence", exact: true }).fill(WRITTEN);
    expect(await grade(page)).toMatchObject({ hintUsed: true, retryNumber: retry });
    await expect(page.getByRole("status").getByText(`Try ${retry} · Supported practice`, { exact: true })).toBeVisible();
    if (retry === 1) await page.getByRole("button", { name: "Try again", exact: true }).click();
  }
});

for (const committed of [false, true]) {
  test(`writing replays its exact whole-sentence attempt after ${committed ? "committed" : "uncommitted"} response loss`, async ({ page }) => {
    await open(page);
    await choose(page, WRITING, 5);
    const bodies: string[] = [];
    await page.route("**/api/v1/attempts", async (route) => {
      bodies.push(route.request().postData()!);
      if (bodies.length === 1) {
        if (committed) await route.fetch();
        await route.abort("connectionfailed");
      } else await route.continue();
    });
    const input = page.getByRole("textbox", { name: "Danish sentence", exact: true });
    await input.fill(WRITTEN);
    await page.getByRole("button", { name: "Check answer", exact: true }).click();
    await expect(page.getByText("The result is not confirmed", { exact: true })).toBeVisible();
    await expect(input).toBeDisabled();
    await expect(page.getByRole("combobox", { name: "Exercise type", exact: true })).toBeDisabled();
    for (const letter of ["æ", "ø", "å"]) await expect(page.getByRole("button", { name: `Insert ${letter}`, exact: true })).toBeDisabled();
    await page.getByRole("button", { name: "Check the same attempt", exact: true }).click();
    await expect(page.getByRole("status").getByText("Meaning matched", { exact: true })).toBeVisible();
    expect(bodies).toHaveLength(2);
    expect(bodies[1]).toBe(bodies[0]);
    await expect(page.getByRole("status").getByText("Try 1 · No support opened", { exact: true })).toBeVisible();
  });
}

test("reading binds the asked participant and action with related English choices", async ({ page }) => {
  await open(page);
  for (const index of [0, 1, 2]) {
    const exercise = await choose(page, READING, index);
    expect(exercise.targetSkillId).toBeUndefined();
    expect(exercise.lexicalTarget).toBeUndefined();
    expect(exercise.readingTarget).toEqual({ facet: "participant-action-complement" });
    expect(exercise).not.toHaveProperty("readingSource");
    expect(exercise.prompt.passage).toHaveLength(3);
    expect(new Set(exercise.prompt.choices)).toEqual(new Set(Object.values(meanings)));
    await expect(page.getByRole("group", { name: "Reading passage", exact: true })).toHaveAttribute("lang", "da");
    expect(await textLanguage(page.getByText(exercise.prompt.cue, { exact: true }))).toBe("en");
    const answer = answerFromText(exercise);
    const choices = page.getByRole("group", { name: "Answer choices", exact: true });
    for (const choice of exercise.prompt.choices) expect(await textLanguage(choices.getByRole("button", { name: choice, exact: true }))).toBe("en");
    const wrong = exercise.prompt.choices.find((choice) => choice !== answer)!;
    await choices.getByRole("button", { name: wrong, exact: true }).click();
    expect(await grade(page)).toMatchObject({ targetSatisfaction: "not-met", linguisticStatus: "acceptable", linguisticScope: "english-choice", responseConstraintStatus: "met", completedSentence: "" });
    await page.getByRole("button", { name: "Try again", exact: true }).click();
    const correct = choices.getByRole("button", { name: answer, exact: true });
    await correct.focus();
    await page.keyboard.press("Enter");
    expect(await grade(page)).toMatchObject({ targetSatisfaction: "met", linguisticStatus: "acceptable", linguisticScope: "english-choice", responseConstraintStatus: "met", completedSentence: "" });
    await expect(page.getByRole("status").getByText("Recognized English choice", { exact: true })).toBeVisible();
    await expect(page.getByRole("status").getByText("Good work", { exact: true })).toBeVisible();
  }
});

test("reading preserves supported choice and exact retry when a committed result is lost", async ({ page }) => {
  await open(page);
  const exercise = await choose(page, READING);
  const bodies: string[] = [];
  await page.route("**/api/v1/attempts", async (route) => {
    bodies.push(route.request().postData()!);
    if (bodies.length === 1) { await route.fetch(); await route.abort("connectionfailed"); }
    else await route.continue();
  });
  await page.getByRole("button", { name: "Learn this first", exact: true }).click();
  const choices = page.getByRole("group", { name: "Answer choices", exact: true });
  await choices.getByRole("button", { name: answerFromText(exercise), exact: true }).click();
  await page.getByRole("button", { name: "Check answer", exact: true }).click();
  await expect(page.getByText("The result is not confirmed", { exact: true })).toBeVisible();
  for (const choice of await choices.getByRole("button").all()) await expect(choice).toBeDisabled();
  await page.getByRole("button", { name: "Check the same attempt", exact: true }).click();
  await expect(page.getByRole("status").getByText("Good work", { exact: true })).toBeVisible();
  expect(bodies).toHaveLength(2);
  expect(bodies[1]).toBe(bodies[0]);
  await expect(page.getByRole("status").getByText("Try 1 · Supported practice", { exact: true })).toBeVisible();
});

for (const theme of ["light", "dark"] as const) {
  for (const title of [WRITING, READING]) {
    test(`${title} and feedback reflow accessibly at enlarged text in ${theme}`, async ({ page }) => {
      await page.emulateMedia({ colorScheme: theme });
      await page.setViewportSize({ width: 320, height: 844 });
      await open(page);
      const exercise = await choose(page, title, title === WRITING ? 5 : 0);
      await page.addStyleTag({ content: "html { font-size: 200% !important; }" });
      await scan(page);
      if (title === WRITING) await page.getByRole("textbox", { name: "Danish sentence", exact: true }).fill(`${WRITTEN} Han sover.`);
      else await page.getByRole("group", { name: "Answer choices", exact: true }).getByRole("button", { name: answerFromText(exercise), exact: true }).click();
      await grade(page);
      await expectFeedbackBadgesUnclipped(page);
      await scan(page);
    });
  }
}
