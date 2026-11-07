import { expect, test, type Page } from "@playwright/test";

async function expectNoHorizontalOverflow(page: Page, state: string) {
  const dimensions = await page.evaluate(() => ({
    clientWidth: document.documentElement.clientWidth,
    scrollWidth: document.documentElement.scrollWidth,
  }));
  expect(dimensions.scrollWidth, `${state}: ${JSON.stringify(dimensions)}`).toBeLessThanOrEqual(dimensions.clientWidth + 1);
}

async function chooseFormat(page: Page, title: string) {
  const selector = page.getByRole("combobox", { name: "Exercise type" });
  if (!(await selector.isVisible())) await page.getByRole("button", { name: "Generation controls" }).click();
  await selector.click();
  await page.getByRole("option", { name: title }).click();
  await expect(selector).toHaveValue(title);
}

test("all pilot formats and recovery states reflow at 320px with 200% text", async ({ page }) => {
  await page.setViewportSize({ width: 320, height: 900 });
  await page.addInitScript(() => { document.documentElement.style.fontSize = "200%"; });
  await page.goto("/");
  await expect(page.getByRole("button", { name: "Check answer" })).toBeVisible();
  await expectNoHorizontalOverflow(page, "present-form exercise");

  await page.getByRole("button", { name: "Learn this first" }).click();
  await expectNoHorizontalOverflow(page, "open lesson");
  await page.getByRole("button", { name: "Show hint" }).click();
  await expectNoHorizontalOverflow(page, "hint");
  await page.getByLabel("Answer choices").getByRole("button").first().click();
  await page.getByRole("button", { name: "Check answer" }).click();
  await expect(page.getByRole("status")).toBeVisible();
  await expectNoHorizontalOverflow(page, "feedback");

  await chooseFormat(page, "Supply the indefinite article");
  await expectNoHorizontalOverflow(page, "article exercise");
  await chooseFormat(page, "Arrange a subject-first statement");
  await expectNoHorizontalOverflow(page, "ordering exercise");

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
  await expectNoHorizontalOverflow(page, "revision recovery");
});
