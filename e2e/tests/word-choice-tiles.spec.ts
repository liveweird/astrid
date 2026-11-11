import AxeBuilder from "@axe-core/playwright";
import { expect, test, type Page } from "@playwright/test";
import { openGenerationControls } from "../support/generation-controls.js";
import { expectFeedbackBadgesUnclipped } from "../support/feedback-layout.js";
import { textLanguage } from "../support/vocabulary.js";

const WORD = "Choose the Danish word";
const ALL = "Build a Danish sentence";
const SPARE = "Choose the present form with word tiles";
const WORDS = ["hun", "læser", "et", "brev"];
const MEANINGS: Record<string, string> = {
  book: "en bog", newspaper: "en avis", "letter (written message)": "et brev", "magazine (printed publication)": "et magasin",
};
type Exercise = {
  id: string;
  formatId: string;
  prompt: {
    cue: string;
    choices: string[];
    blocks: { id: string; text: string }[];
    tileMode?: string;
    requiredTileCount?: number;
  };
};
type Grade = {
  targetSatisfaction: string;
  meaningSatisfaction: string;
  responseConstraintStatus: string;
  linguisticStatus: string;
  linguisticScope: string;
  hintUsed: boolean;
};

async function choose(page: Page, title: string, index = 5) {
  await openGenerationControls(page);
  await page.getByRole("textbox", { name: "Next generation seed", exact: true }).fill("47");
  await page.getByRole("textbox", { name: "Next generation index", exact: true }).fill(String(index));
  const selector = page.getByRole("combobox", { name: "Exercise type", exact: true });
  const pending = page.waitForResponse((r) => r.request().method() === "POST" &&
    new URL(r.url()).pathname === "/api/v1/exercises" && r.status() === 201);
  if (await selector.inputValue() === title) await page.getByRole("button", { name: "Regenerate", exact: true }).click();
  else { await selector.click(); await page.getByRole("option", { name: title, exact: true }).click(); }
  return await (await pending).json() as Exercise;
}

async function open(page: Page) {
  await page.goto("/playground");
  await expect(page.getByRole("button", { name: "Check answer", exact: true })).toBeVisible();
}

async function tiles(page: Page, words: string[]) {
  const bank = page.getByLabel("Available Danish word tiles", { exact: true });
  for (const word of words) await bank.getByRole("button", { name: new RegExp(`^${word}$`, "i") }).click();
}

async function grade(page: Page) {
  const pending = page.waitForResponse((r) => r.request().method() === "POST" &&
    new URL(r.url()).pathname === "/api/v1/attempts" && r.status() === 200);
  await page.getByRole("button", { name: "Check answer", exact: true }).click();
  const result = await (await pending).json() as Grade;
  await expect(page.getByRole("status")).toBeFocused();
  return result;
}

async function scan(page: Page) {
  const result = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]).analyze();
  expect(result.violations.map(({ id, nodes }) => ({ id, targets: nodes.map((node) => node.target) }))).toEqual([]);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy();
}

test("English word recognition supplies four related Danish article bundles and grades meaning separately", async ({ page }) => {
  await open(page);
  for (const index of [0, 1, 2, 3]) {
    const exercise = await choose(page, WORD, index);
    expect(exercise.formatId).toBe("E011");
    expect(new Set(exercise.prompt.choices)).toEqual(new Set(Object.values(MEANINGS)));
    expect(exercise).not.toHaveProperty("tileSource");
    const answer = MEANINGS[exercise.prompt.cue];
    expect(answer).toBeDefined();
    const choices = page.getByRole("group", { name: "Answer choices", exact: true });
    for (const value of exercise.prompt.choices) expect(await textLanguage(choices.getByRole("button", { name: value, exact: true }))).toBe("da");
    expect(await textLanguage(page.getByText(exercise.prompt.cue, { exact: true }))).toBe("en");
    await choices.getByRole("button", { name: exercise.prompt.choices.find((v) => v !== answer)!, exact: true }).click();
    expect(await grade(page)).toMatchObject({ targetSatisfaction: "not-met", meaningSatisfaction: "not-met", responseConstraintStatus: "met", linguisticStatus: "acceptable", linguisticScope: "danish-choice" });
    await expect(page.getByRole("status").getByText("Good work", { exact: true })).toHaveCount(0);
    await page.getByRole("button", { name: "Try again", exact: true }).click();
    await choices.getByRole("button", { name: answer!, exact: true }).focus();
    await page.keyboard.press("Enter");
    expect(await grade(page)).toMatchObject({ targetSatisfaction: "met", meaningSatisfaction: "met", responseConstraintStatus: "met", linguisticStatus: "acceptable" });
  }
});

test("all-word translation supports keyboard removal and checks a complete sentence", async ({ page }) => {
  await open(page);
  const exercise = await choose(page, ALL);
  expect(exercise.prompt).toMatchObject({ cue: "She reads a letter (written message).", tileMode: "use-all", requiredTileCount: 4 });
  expect(exercise.prompt.blocks).toHaveLength(4);
  const bank = page.getByLabel("Available Danish word tiles", { exact: true });
  const subject = bank.getByRole("button", { name: /^hun$/i });
  await subject.focus();
  await page.keyboard.press("Enter");
  const remove = page.getByRole("button", { name: /Remove hun from position 1/i });
  await remove.focus();
  await page.keyboard.press("Enter");
  await expect(subject).toBeFocused();
  await tiles(page, WORDS);
  expect(await grade(page)).toMatchObject({ targetSatisfaction: "met", meaningSatisfaction: "met", responseConstraintStatus: "met", linguisticStatus: "acceptable" });
  await expect(page.getByRole("status").getByText("Good work", { exact: true })).toBeVisible();
});

test("tile translation grades incomplete selections and licensed fronting without accepting a prefix", async ({ page }) => {
  await open(page);
  await choose(page, ALL);
  await tiles(page, ["hun", "læser", "brev"]);
  expect(await grade(page)).toMatchObject({ targetSatisfaction: "unevaluable", meaningSatisfaction: "unknown", responseConstraintStatus: "not-met", linguisticStatus: "unknown" });
  await page.getByRole("button", { name: "Try again", exact: true }).click();
  await choose(page, ALL);
  await tiles(page, ["et", "brev", "læser", "hun"]);
  expect(await grade(page)).toMatchObject({ targetSatisfaction: "not-met", meaningSatisfaction: "met", responseConstraintStatus: "met", linguisticStatus: "acceptable" });
});

test("spare-word translation distinguishes infinitive choice from finite subject-first order", async ({ page }) => {
  await open(page);
  const exercise = await choose(page, SPARE);
  expect(exercise.prompt).toMatchObject({ tileMode: "with-spares", requiredTileCount: 4 });
  expect(exercise.prompt.blocks).toHaveLength(5);
  await tiles(page, ["hun", "læse", "et", "brev"]);
  expect(await grade(page)).toMatchObject({ targetSatisfaction: "not-met", meaningSatisfaction: "met", responseConstraintStatus: "met", linguisticStatus: "known-issue" });
  await page.getByRole("button", { name: "Try again", exact: true }).click();
  await choose(page, SPARE);
  await tiles(page, WORDS);
  await expect(page.getByLabel("Available Danish word tiles", { exact: true }).getByRole("button", { name: "læse", exact: true })).toBeVisible();
  expect(await grade(page)).toMatchObject({ targetSatisfaction: "met", meaningSatisfaction: "met", responseConstraintStatus: "met", linguisticStatus: "acceptable" });
});

for (const title of [WORD, SPARE]) {
  for (const committed of [false, true]) {
    test(`${title} preserves supported exact attempt replay after ${committed ? "committed" : "uncommitted"} response loss`, async ({ page }) => {
      await open(page);
      const exercise = await choose(page, title);
      await page.getByRole("button", { name: "Learn this first", exact: true }).click();
      const bodies: string[] = [];
      await page.route("**/api/v1/attempts", async (route) => {
        bodies.push(route.request().postData()!);
        if (bodies.length === 1) {
          if (committed) await route.fetch();
          await route.abort("connectionfailed");
        }
        else await route.continue();
      });
      if (title === WORD) await page.getByRole("group", { name: "Answer choices", exact: true }).getByRole("button", { name: MEANINGS[exercise.prompt.cue]!, exact: true }).click();
      else await tiles(page, WORDS);
      await page.getByRole("button", { name: "Check answer", exact: true }).click();
      await expect(page.getByText("The result is not confirmed", { exact: true })).toBeVisible();
      const inputs = title === WORD ? page.getByRole("group", { name: "Answer choices", exact: true }).getByRole("button") : page.getByLabel("Available Danish word tiles", { exact: true }).getByRole("button");
      for (const input of await inputs.all()) await expect(input).toBeDisabled();
      await page.getByRole("button", { name: "Check the same attempt", exact: true }).click();
      await expect(page.getByRole("status").getByText("Good work", { exact: true })).toBeVisible();
      expect(bodies).toHaveLength(2);
      expect(bodies[1]).toBe(bodies[0]);
      await expect(page.getByRole("status").getByText("Try 1 · Supported practice", { exact: true })).toBeVisible();
    });
  }
}

for (const theme of ["light", "dark"] as const) {
  for (const title of [WORD, ALL, SPARE]) {
    test(`${title} reflows with accessible feedback at 200 percent text in ${theme}`, async ({ page }) => {
      await page.emulateMedia({ colorScheme: theme });
      await page.setViewportSize({ width: 320, height: 844 });
      await open(page);
      const exercise = await choose(page, title);
      await page.addStyleTag({ content: "html { font-size: 200% !important; }" });
      await scan(page);
      if (title === WORD) await page.getByRole("group", { name: "Answer choices", exact: true }).getByRole("button", { name: MEANINGS[exercise.prompt.cue]!, exact: true }).click();
      else await tiles(page, WORDS);
      await grade(page);
      await expectFeedbackBadgesUnclipped(page);
      await scan(page);
    });
  }
}
