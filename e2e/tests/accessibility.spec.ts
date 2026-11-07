import AxeBuilder from "@axe-core/playwright";
import { expect, test, type Page } from "@playwright/test";

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

async function chooseFormat(page: Page, title: string) {
  const selector = page.getByRole("combobox", { name: "Exercise type" });
  if (!(await selector.isVisible())) await page.getByRole("button", { name: "Generation controls" }).click();
  await selector.click();
  await page.getByRole("option", { name: title }).click();
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
  await selector.click();
  await page.getByRole("option", { name: "Choose the present form" }).click();
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
