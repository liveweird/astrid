import AxeBuilder from "@axe-core/playwright";
import { expect, test, type Locator, type Page } from "@playwright/test";
import { openGenerationControls } from "../support/generation-controls.js";
import { expectHighlightedFormatVisible } from "../support/format-select.js";
import { PILOT_FORMAT_TITLES } from "../support/formats.js";
import { chooseVocabulary, VOCABULARY } from "../support/vocabulary.js";

const WCAG_TAGS = ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"];

async function scan(page: Page) {
  const result = await new AxeBuilder({ page }).withTags(WCAG_TAGS).analyze();
  expect(result.violations.map((violation) => ({
    id: violation.id,
    impact: violation.impact,
    help: violation.help,
    nodes: violation.nodes.map((node) => node.target.join(" ")),
  }))).toEqual([]);
}

async function openPlayground(page: Page) {
  await page.goto("/");
  await expect(page.getByRole("heading", { name: "Danish engine playground" })).toBeVisible();
}

async function selectFormatOption(page: Page, selector: Locator, title: string) {
  await selector.click();
  await selector.fill(title);
  const option = page.getByRole("option", { name: title, exact: true });
  await expect(option).toBeVisible();
  // Typing already highlights the first match; ArrowDown would move to a prefix-sharing title.
  await expect(option).toHaveAttribute("data-combobox-selected", "true");
  await selector.press("Enter");
}

async function chooseFormat(page: Page, title: string) {
  const selector = page.getByRole("combobox", { name: "Exercise type" });
  await openGenerationControls(page);
  await selectFormatOption(page, selector, title);
  await expect(selector).toHaveValue(title);
}

async function scanLearningStates(page: Page) {
  await openPlayground(page);
  await scan(page);

  await page.getByLabel("Answer choices").getByRole("button").first().click();
  await page.getByRole("button", { name: "Check answer" }).click();
  await expect(page.getByRole("status")).toBeVisible();
  await scan(page);

  await chooseFormat(page, "Supply the indefinite article");
  await scan(page);
  await chooseFormat(page, "Arrange a subject-first statement");
  await scan(page);
  for (const title of PILOT_FORMAT_TITLES.slice(3, 6)) {
    await chooseFormat(page, title);
    await scan(page);
    const label = title === "Supply the present form" ? "Present form" :
      title === "Repair the verb form" ? "Replacement verb" : "Replacement article";
    await page.getByRole("textbox", { name: label, exact: true }).fill("unknown");
    await page.getByRole("button", { name: "Check answer" }).click();
    await expect(page.getByRole("status")).toBeVisible();
    await scan(page);
  }

  let reject = true;
  await page.route("**/api/v1/exercises", async (route) => {
    if (reject) {
      reject = false;
      await route.fulfill({
        status: 409,
        contentType: "application/problem+json",
        body: JSON.stringify({ detail: "The engine revision changed.", code: "revision_conflict" }),
      });
    } else await route.continue();
  });
  const selector = page.getByRole("combobox", { name: "Exercise type" });
  await selectFormatOption(page, selector, "Choose the present form");
  await expect(page.getByRole("button", { name: "Reload engine" })).toBeVisible();
  await scan(page);

  await page.getByRole("button", { name: "Reload engine" }).click();
  await expect(page.getByRole("button", { name: "Check answer" })).toBeVisible();
  await page.route("**/api/v1/attempts", (route) => route.abort("connectionfailed"));
  await page.getByLabel("Answer choices").getByRole("button").first().click();
  await page.getByRole("button", { name: "Check answer" }).click();
  await expect(page.getByText("The result is not confirmed")).toBeVisible();
  await scan(page);
}

test("pilot formats, feedback and recovery states have no WCAG A/AA violations", async ({ page }) => {
  await scanLearningStates(page);
});

test("pilot formats, feedback and recovery states have no WCAG A/AA violations in dark mode", async ({ page }) => {
  await page.emulateMedia({ colorScheme: "dark" });
  await scanLearningStates(page);
  await expect(page.locator("html")).toHaveAttribute("data-mantine-color-scheme", "dark");
});

test("open format lists remain accessible with enlarged text", async ({ page }) => {
  await page.setViewportSize({ width: 320, height: 900 });
  await openPlayground(page);
  await page.addStyleTag({ content: "html { font-size: 200% !important; }" });
  expect(await page.evaluate(() => getComputedStyle(document.documentElement).fontSize)).toBe("32px");
  const selector = page.getByRole("combobox", { name: "Exercise type" });
  await openGenerationControls(page);

  for (const title of PILOT_FORMAT_TITLES) {
    if ((await selector.inputValue()) !== title) await chooseFormat(page, title);
    await selector.click();
    const list = page.getByRole("listbox", { name: "Exercise type" });
    await expect(list).toBeVisible();
    await expect(selector).toHaveAttribute("aria-expanded", "true");
    const current = list.getByRole("option", { name: title, exact: true });
    await expect(current).toHaveAttribute("data-combobox-selected", "true");
    await expect(selector).toHaveAttribute("aria-activedescendant", (await current.getAttribute("id"))!);
    const optionBounds = await list.getByRole("option").evaluateAll((options) => options.map((option) => {
      const box = option.getBoundingClientRect();
      return { title: option.textContent, left: box.left, right: box.right };
    }));
    for (const bounds of optionBounds) {
      expect(bounds.left, `${bounds.title} left edge`).toBeGreaterThanOrEqual(0);
      expect(bounds.right, `${bounds.title} right edge`).toBeLessThanOrEqual(320);
    }
    const documentScroll = await page.evaluate(() => document.scrollingElement?.scrollTop);
    for (let step = 0; step < PILOT_FORMAT_TITLES.length; step += 1) {
      await page.keyboard.press("ArrowDown");
      await expectHighlightedFormatVisible(page);
    }
    expect(await page.evaluate(() => document.scrollingElement?.scrollTop)).toBe(documentScroll);
    await scan(page);
    await page.keyboard.press("Escape");
    await expect(list).toBeHidden();
  }
});

for (const theme of ["light", "dark"] as const) {
  test(`vocabulary prompts and constrained feedback have no WCAG A/AA violations in ${theme} mode`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: theme });
    await openPlayground(page);
    await chooseVocabulary(page, "Recognize the meaning");
    await scan(page);
    await page.getByLabel("Answer choices").getByRole("button").first().click();
    await page.getByRole("button", { name: "Check answer" }).click();
    await expect(page.getByRole("status")).toBeVisible();
    await scan(page);

    const exercise = await chooseVocabulary(page, "Recall the Danish headword");
    const target = VOCABULARY.find((entry) => entry.meaning === exercise.prompt.cue)!;
    const input = page.getByRole("textbox", { name: "Danish singular headword", exact: true });
    await scan(page);
    await input.fill(`${target.headword === "brev" ? "et" : "en"} ${target.headword}`);
    await page.getByRole("button", { name: "Check answer" }).click();
    await expect(page.getByRole("status").getByText("One-word response required", { exact: true })).toBeVisible();
    await scan(page);
    await page.getByRole("button", { name: "Try again" }).click();
    await input.fill("unfamiliar");
    await page.getByRole("button", { name: "Check answer" }).click();
    await expect(page.getByRole("status")).toBeVisible();
    await scan(page);
  });
}
