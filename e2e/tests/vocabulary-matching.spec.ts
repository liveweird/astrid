import AxeBuilder from "@axe-core/playwright";
import { expect, test, type Page } from "@playwright/test";
import { openGenerationControls } from "../support/generation-controls.js";
import { expectFeedbackBadgesUnclipped } from "../support/feedback-layout.js";
import { textLanguage } from "../support/vocabulary.js";

const TITLE = "Match English and Danish words";
const MEANINGS: Record<string, string> = {
  book: "en bog", newspaper: "en avis", "letter (written message)": "et brev", "magazine (printed publication)": "et magasin",
};
type Pair = { leftId: string; rightId: string };
type Item = { id: string; text: string };
type Exercise = { id: string; formatId: string; prompt: { matchingColumns: { left: Item[]; right: Item[] } }; lexicalTargets: unknown[] };
type PairResult = { leftId: string; rightId?: string; feedback: string; targetSatisfaction: string; meaningSatisfaction: string; responseConstraintStatus: string; linguisticStatus: string };
type Grade = {
  targetSatisfaction: string;
  meaningSatisfaction: string;
  responseConstraintStatus: string;
  linguisticStatus: string;
  linguisticScope: string;
  hintUsed: boolean;
  retryNumber: number;
  matchingResults: PairResult[];
};

async function openMatching(page: Page) {
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
  expect(exercise.formatId).toBe("E014");
  await expect(page.getByRole("button", { name: "Check matches", exact: true })).toBeDisabled();
  return exercise;
}

function correctPairs(exercise: Exercise): Pair[] {
  const { left, right } = exercise.prompt.matchingColumns;
  return left.map((item) => ({ leftId: item.id, rightId: right.find((candidate) => candidate.text === MEANINGS[item.text])!.id }));
}

async function selectPairs(page: Page, exercise: Exercise, pairs: Pair[], keyboard = false) {
  const { left, right } = exercise.prompt.matchingColumns;
  for (const pair of pairs) {
    const english = page.getByRole("group", { name: "English words", exact: true })
      .getByRole("button", { name: left.find((item) => item.id === pair.leftId)!.text, exact: true });
    const danish = page.getByRole("group", { name: "Danish words", exact: true })
      .getByRole("button", { name: right.find((item) => item.id === pair.rightId)!.text, exact: true });
    if (keyboard) {
      await english.focus();
      await page.keyboard.press("Enter");
      await danish.focus();
      await page.keyboard.press("Space");
    } else { await english.click(); await danish.click(); }
  }
}

async function check(page: Page) {
  const response = page.waitForResponse((r) => r.request().method() === "POST" &&
    new URL(r.url()).pathname === "/api/v1/attempts" && r.status() === 200);
  await page.getByRole("button", { name: "Check matches", exact: true }).click();
  const result = await (await response).json() as Grade;
  await expect(page.getByRole("status")).toBeFocused();
  expect(result.linguisticScope).toBe("danish-matching");
  expect(result.matchingResults).toHaveLength(4);
  for (const row of result.matchingResults) {
    await expect(page.getByRole("group", { name: "Matching feedback", exact: true }).getByText(row.feedback, { exact: true }).first()).toBeVisible();
  }
  return result;
}

async function expectMatchingTextUnclipped(page: Page) {
  const clipped = await page.getByRole("group", { name: /^(Match|Chosen match) for / }).evaluateAll((groups) => groups.flatMap((group) => {
    const bounds = group.getBoundingClientRect();
    const walker = document.createTreeWalker(group, NodeFilter.SHOW_TEXT);
    const failures: { group: string | null; text: string; left: number; right: number }[] = [];
    let node = walker.nextNode();
    while (node) {
      if (node.textContent?.trim()) {
        const range = document.createRange();
        range.selectNodeContents(node);
        for (const rect of range.getClientRects()) {
          if (rect.width > 0 && (rect.left < bounds.left - 1 || rect.right > bounds.right + 1)) {
            failures.push({ group: group.getAttribute("aria-label"), text: node.textContent, left: rect.left - bounds.left, right: rect.right - bounds.right });
          }
        }
      }
      node = walker.nextNode();
    }
    return failures;
  }));
  expect(clipped, "Every matching label and feedback line stays inside its own card").toEqual([]);
}

async function scan(page: Page) {
  const result = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]).analyze();
  expect(result.violations.map(({ id, nodes }) => ({ id, targets: nodes.map((node) => node.target) }))).toEqual([]);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy();
  await expectMatchingTextUnclipped(page);
}

test("matching supplies four bilingual article bundles and supports keyboard pairing removal and grading", async ({ page }) => {
  const exercise = await openMatching(page);
  const { left, right } = exercise.prompt.matchingColumns;
  expect(exercise.lexicalTargets).toHaveLength(4);
  expect(new Set(left.map((item) => item.text))).toEqual(new Set(Object.keys(MEANINGS)));
  expect(new Set(right.map((item) => item.text))).toEqual(new Set(Object.values(MEANINGS)));
  expect(left.map((item) => item.id)).toEqual(["l0", "l1", "l2", "l3"]);
  expect(right.map((item) => item.id)).toEqual(["r0", "r1", "r2", "r3"]);
  for (const [name, items, language] of [["English words", left, "en"], ["Danish words", right, "da"]] as const) {
    for (const item of items) expect(await textLanguage(page.getByRole("group", { name, exact: true }).getByRole("button", { name: item.text, exact: true }))).toBe(language);
  }
  const pairs = correctPairs(exercise);
  await selectPairs(page, exercise, pairs.slice(0, 1), true);
  const remove = page.getByRole("button", { name: `Remove match for ${left[0]!.text}`, exact: true });
  await remove.focus();
  await page.keyboard.press("Enter");
  await expect(page.getByRole("button", { name: "Check matches", exact: true })).toBeDisabled();
  await selectPairs(page, exercise, pairs, true);
  expect(await check(page)).toMatchObject({ targetSatisfaction: "met", meaningSatisfaction: "met", responseConstraintStatus: "met", linguisticStatus: "acceptable", retryNumber: 1 });
  await expect(page.getByRole("status").getByText("Good work", { exact: true })).toBeVisible();
  await expect(page.getByRole("group", { name: "Matching feedback", exact: true })).toBeVisible();
});

test("regeneration clears an unfinished English selection before pairing on the new board", async ({ page }) => {
  const old = await openMatching(page);
  await page.getByRole("group", { name: "English words", exact: true }).getByRole("button").first().click();
  await page.getByRole("textbox", { name: "Next generation index", exact: true }).fill("6");
  const response = page.waitForResponse((r) => r.request().method() === "POST" &&
    new URL(r.url()).pathname === "/api/v1/exercises" && r.status() === 201);
  await page.getByRole("button", { name: "Regenerate", exact: true }).click();
  const exercise = await (await response).json() as Exercise;
  expect(exercise.id).not.toBe(old.id);
  for (const button of await page.getByRole("group", { name: "English words", exact: true }).getByRole("button").all()) {
    await expect(button).toHaveAttribute("aria-pressed", "false");
  }
  for (const button of await page.getByRole("group", { name: "Danish words", exact: true }).getByRole("button").all()) await expect(button).toBeDisabled();
  await expect(page.getByRole("button", { name: "Check matches", exact: true })).toBeDisabled();
  await expect(page.getByRole("group", { name: "Chosen matches", exact: true })).toHaveCount(0);
  await selectPairs(page, exercise, correctPairs(exercise));
  expect(await check(page)).toMatchObject({ targetSatisfaction: "met", retryNumber: 1 });
});

test("matching partial and mixed feedback permits editing every pair on a new supported retry", async ({ page }) => {
  const exercise = await openMatching(page);
  const pairs = correctPairs(exercise);
  await selectPairs(page, exercise, pairs.slice(0, 1));
  const partial = await check(page);
  expect(partial).toMatchObject({ targetSatisfaction: "unevaluable", meaningSatisfaction: "unknown", responseConstraintStatus: "not-met", linguisticStatus: "acceptable" });
  expect(partial.matchingResults.filter((row) => row.targetSatisfaction === "met")).toHaveLength(1);
  expect(partial.matchingResults.filter((row) => row.targetSatisfaction === "unevaluable")).toHaveLength(3);
  await expect(page.getByRole("status").getByText("Good work", { exact: true })).toHaveCount(0);
  await page.getByRole("button", { name: "Try again", exact: true }).click();
  await expect(page.getByRole("group", { name: TITLE, exact: true })).toBeFocused();
  await expect(page.getByRole("button", { name: `Remove match for ${exercise.prompt.matchingColumns.left[0]!.text}`, exact: true })).toBeEnabled();
  const wrong = [
    { leftId: pairs[1]!.leftId, rightId: pairs[2]!.rightId },
    { leftId: pairs[2]!.leftId, rightId: pairs[1]!.rightId },
    pairs[3]!,
  ];
  await selectPairs(page, exercise, wrong);
  const mixed = await check(page);
  expect(mixed).toMatchObject({ targetSatisfaction: "not-met", meaningSatisfaction: "not-met", responseConstraintStatus: "met", linguisticStatus: "acceptable", retryNumber: 2 });
  expect(mixed.matchingResults.filter((row) => row.targetSatisfaction === "met")).toHaveLength(2);
  expect(mixed.matchingResults.filter((row) => row.targetSatisfaction === "not-met")).toHaveLength(2);
  await expect(page.getByRole("status").getByText("Retry after feedback", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Try again", exact: true }).click();
  await page.getByRole("button", { name: "Learn this first", exact: true }).click();
  for (const left of exercise.prompt.matchingColumns.left) await page.getByRole("button", { name: `Remove match for ${left.text}`, exact: true }).click();
  await selectPairs(page, exercise, pairs);
  expect(await check(page)).toMatchObject({ targetSatisfaction: "met", hintUsed: true, retryNumber: 3 });
  await expect(page.getByRole("status").getByText("Try 3 · Supported practice", { exact: true })).toBeVisible();
});

for (const committed of [false, true]) {
  test(`matching replays the exact saved board after ${committed ? "committed" : "uncommitted"} response loss`, async ({ page }) => {
    const exercise = await openMatching(page);
    await page.getByRole("button", { name: "Learn this first", exact: true }).click();
    const bodies: string[] = [];
    await page.route("**/api/v1/attempts", async (route) => {
      bodies.push(route.request().postData()!);
      if (bodies.length === 1) {
        if (committed) await route.fetch();
        await route.abort("connectionfailed");
      } else await route.continue();
    });
    await selectPairs(page, exercise, correctPairs(exercise).reverse());
    await page.getByRole("button", { name: "Check matches", exact: true }).click();
    await expect(page.getByText("The result is not confirmed", { exact: true })).toBeVisible();
    for (const name of ["English words", "Danish words", "Chosen matches"]) {
      for (const control of await page.getByRole("group", { name, exact: true }).getByRole("button").all()) await expect(control).toBeDisabled();
    }
    await scan(page);
    await page.getByRole("button", { name: "Check the same attempt", exact: true }).click();
    await expect(page.getByRole("status").getByText("Good work", { exact: true })).toBeVisible();
    expect(bodies).toHaveLength(2);
    expect(bodies[1]).toBe(bodies[0]);
    const saved = JSON.parse(bodies[0]!) as { answer: { kind: string; pairs: Pair[] } };
    expect(saved.answer).toEqual({ kind: "matching", pairs: correctPairs(exercise).reverse() });
    await expect(page.getByRole("status").getByText("Try 1 · Supported practice", { exact: true })).toBeVisible();
  });
}

for (const theme of ["light", "dark"] as const) {
  test(`matching board and partial mixed and complete feedback reflow at 200 percent text in ${theme}`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: theme });
    await page.setViewportSize({ width: 320, height: 844 });
    const exercise = await openMatching(page);
    await page.addStyleTag({ content: "html { font-size: 200% !important; }" });
    await scan(page);
    const pairs = correctPairs(exercise);
    await selectPairs(page, exercise, pairs.slice(0, 1));
    await check(page);
    await expectFeedbackBadgesUnclipped(page);
    await scan(page);
    await page.getByRole("button", { name: "Try again", exact: true }).click();
    await selectPairs(page, exercise, [{ leftId: pairs[1]!.leftId, rightId: pairs[2]!.rightId }]);
    await check(page);
    await scan(page);
    await page.getByRole("button", { name: "Try again", exact: true }).click();
    for (const left of exercise.prompt.matchingColumns.left.slice(0, 2)) await page.getByRole("button", { name: `Remove match for ${left.text}`, exact: true }).click();
    await selectPairs(page, exercise, pairs);
    await check(page);
    await expectFeedbackBadgesUnclipped(page);
    await scan(page);
  });
}
