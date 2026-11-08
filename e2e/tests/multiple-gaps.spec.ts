import AxeBuilder from "@axe-core/playwright";
import { expect, test, type Page } from "@playwright/test";
import { openGenerationControls } from "../support/generation-controls.js";
import { expectFeedbackBadgesUnclipped } from "../support/feedback-layout.js";
import { textLanguage } from "../support/vocabulary.js";

const TITLE = "Choose a present verb and article";
const BOARD = "Present verb and article gaps";
const VERB = "Present verb — Primary target";
const ARTICLE = "Indefinite article — Supporting grammar";
const ARTICLES: Record<string, string> = { bog: "en", avis: "en", bil: "en", hund: "en", brev: "et", hus: "et", bord: "et", æble: "et" };
const PRESENT = new Set(["har", "er", "læser", "spiser"]);
type Choice = { id: string; text: string };
type Slot = { id: string; targetSkillId: string; assessmentRole: string; choices: Choice[] };
type Selection = { slotId: string; choiceId: string };
type Exercise = { id: string; formatId: string; prompt: { gapPrompt: { segments: string[]; slots: Slot[] } } };
type Grade = {
  targetSatisfaction: string;
  linguisticStatus: string;
  linguisticScope: string;
  responseConstraintStatus: string;
  completedSentence: string;
  retryNumber: number;
  hintUsed: boolean;
  gapResults: { slotId: string; targetSatisfaction: string; assessmentRole: string; feedback: string }[];
};

async function openGaps(page: Page) {
  await page.goto("/");
  await expect(page.getByRole("button", { name: "Check answer", exact: true })).toBeVisible();
  await openGenerationControls(page);
  await page.getByRole("textbox", { name: "Next generation seed", exact: true }).fill("47");
  await page.getByRole("textbox", { name: "Next generation index", exact: true }).fill("5");
  const response = page.waitForResponse((r) => r.request().method() === "POST" &&
    new URL(r.url()).pathname === "/api/v1/exercises" && r.status() === 201);
  await page.getByRole("combobox", { name: "Exercise type", exact: true }).click();
  await page.getByRole("option", { name: TITLE, exact: true }).click();
  const exercise = await (await response).json() as Exercise;
  expect(exercise.formatId).toBe("E015");
  await expect(page.getByRole("button", { name: "Check gaps", exact: true })).toBeDisabled();
  return exercise;
}

function expectedChoices(exercise: Exercise) {
  const { segments, slots } = exercise.prompt.gapPrompt;
  const noun = segments[2]!.trim().replace(/\.$/, "");
  expect(ARTICLES[noun]).toBeDefined();
  return slots.map((slot) => ({
    slotId: slot.id,
    choiceId: slot.choices.find((choice) => slot.assessmentRole === "primary" ? PRESENT.has(choice.text) : choice.text === ARTICLES[noun])!.id,
  }));
}

function alternative(exercise: Exercise, selection: Selection) {
  const slot = exercise.prompt.gapPrompt.slots.find((candidate) => candidate.id === selection.slotId)!;
  return { slotId: slot.id, choiceId: slot.choices.find((choice) => choice.id !== selection.choiceId)!.id };
}

async function select(page: Page, exercise: Exercise, selection: Selection, keyboard = false) {
  const slot = exercise.prompt.gapPrompt.slots.find((candidate) => candidate.id === selection.slotId)!;
  const choice = slot.choices.find((candidate) => candidate.id === selection.choiceId)!;
  const button = page.getByRole("group", { name: slot.assessmentRole === "primary" ? VERB : ARTICLE, exact: true })
    .getByRole("button", { name: choice.text, exact: true });
  if (keyboard) { await button.focus(); await page.keyboard.press("Space"); }
  else await button.click();
}

async function check(page: Page) {
  const response = page.waitForResponse((r) => r.request().method() === "POST" &&
    new URL(r.url()).pathname === "/api/v1/attempts" && r.status() === 200);
  await page.getByRole("button", { name: "Check gaps", exact: true }).click();
  const result = await (await response).json() as Grade;
  await expect(page.getByRole("status")).toBeFocused();
  expect(result.linguisticScope).toBe("danish-gaps");
  expect(result).not.toHaveProperty("meaningSatisfaction");
  expect(result.gapResults.map((row) => row.assessmentRole)).toEqual(["primary", "support"]);
  for (const row of result.gapResults) {
    const name = row.assessmentRole === "primary" ? "Present verb feedback" : "Indefinite article feedback";
    await expect(page.getByRole("group", { name, exact: true }).getByText(row.feedback, { exact: true })).toBeVisible();
  }
  await expect(page.getByRole("status").getByText(/^Meaning (matched|to revisit|not evaluated)$/)).toHaveCount(0);
  return result;
}

async function scan(page: Page) {
  const result = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]).analyze();
  expect(result.violations.map(({ id, nodes }) => ({ id, targets: nodes.map((node) => node.target) }))).toEqual([]);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy();
  const clipped = await page.getByRole("group", { name: /^(Present verb — Primary target|Indefinite article — Supporting grammar|Present verb feedback|Indefinite article feedback)$/ }).evaluateAll((cards) => cards.flatMap((card) => {
    const bounds = card.getBoundingClientRect();
    const walker = document.createTreeWalker(card, NodeFilter.SHOW_TEXT);
    const outside: string[] = [];
    let node = walker.nextNode();
    while (node) {
      if (node.textContent?.trim()) {
        const range = document.createRange();
        range.selectNodeContents(node);
        for (const rect of range.getClientRects()) {
          if (rect.width > 0 && (rect.left < bounds.left - 1 || rect.right > bounds.right + 1)) outside.push(node.textContent);
        }
      }
      node = walker.nextNode();
    }
    return outside;
  }));
  expect(clipped, "Every gap choice label and feedback line stays inside its card").toEqual([]);
}

test("two gaps expose primary and supporting roles and support keyboard changing and clearing", async ({ page }) => {
  const exercise = await openGaps(page);
  const prompt = exercise.prompt.gapPrompt;
  expect(prompt.segments).toHaveLength(3);
  expect(prompt.slots.map(({ id, targetSkillId, assessmentRole }) => ({ id, targetSkillId, assessmentRole }))).toEqual([
    { id: "g0", targetSkillId: "G004", assessmentRole: "primary" },
    { id: "g1", targetSkillId: "G006", assessmentRole: "support" },
  ]);
  for (const [index, name] of [VERB, ARTICLE].entries()) {
    for (const choice of prompt.slots[index]!.choices) {
      expect(await textLanguage(page.getByRole("group", { name, exact: true }).getByRole("button", { name: choice.text, exact: true }))).toBe("da");
    }
  }
  const choices = expectedChoices(exercise);
  await select(page, exercise, choices[0]!, true);
  const clear = page.getByRole("button", { name: "Clear present verb selection", exact: true });
  await clear.focus();
  await page.keyboard.press("Enter");
  await expect(page.getByRole("button", { name: "Check gaps", exact: true })).toBeDisabled();
  await select(page, exercise, alternative(exercise, choices[0]!), true);
  await select(page, exercise, choices[0]!, true);
  await select(page, exercise, choices[1]!, true);
  const result = await check(page);
  expect(result).toMatchObject({ targetSatisfaction: "met", linguisticStatus: "acceptable", responseConstraintStatus: "met", retryNumber: 1 });
  await expect(page.getByRole("status").getByText("Good work", { exact: true })).toBeVisible();
  await expect(page.getByRole("group", { name: "Gap feedback", exact: true })).toBeVisible();
});

test("primary correct with supporting article wrong or missing never reports overall success", async ({ page }) => {
  const exercise = await openGaps(page);
  const choices = expectedChoices(exercise);
  await select(page, exercise, choices[0]!);
  const partial = await check(page);
  expect(partial).toMatchObject({ targetSatisfaction: "met", linguisticStatus: "unknown", responseConstraintStatus: "not-met", completedSentence: "" });
  expect(partial.gapResults.map((row) => row.targetSatisfaction)).toEqual(["met", "unevaluable"]);
  await expect(page.getByRole("status").getByText("Good work", { exact: true })).toHaveCount(0);
  await page.getByRole("button", { name: "Try again", exact: true }).click();
  await expect(page.getByRole("group", { name: BOARD, exact: true })).toBeFocused();
  await expect(page.getByRole("button", { name: "Clear present verb selection", exact: true })).toBeEnabled();
  await select(page, exercise, alternative(exercise, choices[1]!));
  const mixed = await check(page);
  expect(mixed).toMatchObject({ targetSatisfaction: "met", linguisticStatus: "known-issue", responseConstraintStatus: "met", retryNumber: 2 });
  expect(mixed.gapResults.map((row) => row.targetSatisfaction)).toEqual(["met", "not-met"]);
  await expect(page.getByRole("status").getByText("Good work", { exact: true })).toHaveCount(0);
  await expect(page.getByRole("status").getByText("Retry after feedback", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Try again", exact: true }).click();
  await page.getByRole("button", { name: "Learn this first", exact: true }).click();
  await select(page, exercise, choices[1]!);
  expect(await check(page)).toMatchObject({ targetSatisfaction: "met", linguisticStatus: "acceptable", hintUsed: true, retryNumber: 3 });
  await expect(page.getByRole("status").getByText("Try 3 · Supported practice", { exact: true })).toBeVisible();
});

test("wrong primary verb with correct support retains separate outcomes and the selected sentence", async ({ page }) => {
  const exercise = await openGaps(page);
  const choices = expectedChoices(exercise);
  const wrong = alternative(exercise, choices[0]!);
  await select(page, exercise, wrong);
  await select(page, exercise, choices[1]!);
  const result = await check(page);
  expect(result).toMatchObject({ targetSatisfaction: "not-met", linguisticStatus: "known-issue", responseConstraintStatus: "met" });
  expect(result.gapResults.map((row) => row.targetSatisfaction)).toEqual(["not-met", "met"]);
  const { segments, slots } = exercise.prompt.gapPrompt;
  const verb = slots[0]!.choices.find((choice) => choice.id === wrong.choiceId)!.text;
  const article = slots[1]!.choices.find((choice) => choice.id === choices[1]!.choiceId)!.text;
  expect(result.completedSentence).toBe(segments[0]! + verb + segments[1]! + article + segments[2]!);
});

test("regeneration clears both gap selections even when positional ids are reused", async ({ page }) => {
  const previous = await openGaps(page);
  for (const selection of expectedChoices(previous)) await select(page, previous, selection);
  const response = page.waitForResponse((r) => r.request().method() === "POST" && new URL(r.url()).pathname === "/api/v1/exercises" && r.status() === 201);
  await page.getByRole("textbox", { name: "Next generation index", exact: true }).fill("6");
  await page.getByRole("button", { name: "Regenerate", exact: true }).click();
  const next = await (await response).json() as Exercise;
  expect(next.id).not.toBe(previous.id);
  for (const slot of next.prompt.gapPrompt.slots) {
    const group = page.getByRole("group", { name: slot.assessmentRole === "primary" ? VERB : ARTICLE, exact: true });
    for (const choice of slot.choices) await expect(group.getByRole("button", { name: choice.text, exact: true })).toHaveAttribute("aria-pressed", "false");
  }
  await expect(page.getByRole("button", { name: "Check gaps", exact: true })).toBeDisabled();
});

for (const committed of [false, true]) {
  test(`gap answers preserve supported reversed selection order after ${committed ? "committed" : "uncommitted"} response loss`, async ({ page }) => {
    const exercise = await openGaps(page);
    await page.getByRole("button", { name: "Learn this first", exact: true }).click();
    const bodies: string[] = [];
    await page.route("**/api/v1/attempts", async (route) => {
      bodies.push(route.request().postData()!);
      if (bodies.length === 1) {
        if (committed) await route.fetch();
        await route.abort("connectionfailed");
      } else await route.continue();
    });
    const selections = expectedChoices(exercise).reverse();
    for (const selection of selections) await select(page, exercise, selection);
    await page.getByRole("button", { name: "Check gaps", exact: true }).click();
    await expect(page.getByText("The result is not confirmed", { exact: true })).toBeVisible();
    for (const name of [VERB, ARTICLE]) {
      for (const button of await page.getByRole("group", { name, exact: true }).getByRole("button").all()) await expect(button).toBeDisabled();
    }
    for (const name of ["Clear present verb selection", "Clear article selection"]) await expect(page.getByRole("button", { name, exact: true })).toBeDisabled();
    await scan(page);
    await page.getByRole("button", { name: "Check the same attempt", exact: true }).click();
    await expect(page.getByRole("status").getByText("Good work", { exact: true })).toBeVisible();
    expect(bodies).toHaveLength(2);
    expect(bodies[1]).toBe(bodies[0]);
    expect((JSON.parse(bodies[0]!) as { answer: unknown }).answer).toEqual({ kind: "gaps", selections });
    await expect(page.getByRole("status").getByText("Try 1 · Supported practice", { exact: true })).toBeVisible();
  });
}

for (const theme of ["light", "dark"] as const) {
  test(`gap prompt and partial mixed and complete feedback reflow at 200 percent text in ${theme}`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: theme });
    await page.setViewportSize({ width: 320, height: 844 });
    const exercise = await openGaps(page);
    await page.addStyleTag({ content: "html { font-size: 200% !important; }" });
    await scan(page);
    const choices = expectedChoices(exercise);
    await select(page, exercise, choices[0]!);
    await check(page);
    await expectFeedbackBadgesUnclipped(page);
    await scan(page);
    await page.getByRole("button", { name: "Try again", exact: true }).click();
    await select(page, exercise, alternative(exercise, choices[1]!));
    await check(page);
    await expectFeedbackBadgesUnclipped(page);
    await scan(page);
    await page.getByRole("button", { name: "Try again", exact: true }).click();
    await select(page, exercise, choices[1]!);
    await check(page);
    await expectFeedbackBadgesUnclipped(page);
    await scan(page);
  });
}
