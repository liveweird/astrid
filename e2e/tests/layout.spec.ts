import { expect, test, type Page } from "@playwright/test";
import { openGenerationControls } from "../support/generation-controls.js";
import { expectHighlightedFormatVisible } from "../support/format-select.js";

async function expectNoHorizontalOverflow(page: Page, state: string) {
  const dimensions = await page.evaluate(() => ({
    clientWidth: document.documentElement.clientWidth,
    scrollWidth: document.documentElement.scrollWidth,
  }));
  expect(dimensions.scrollWidth, `${state}: ${JSON.stringify(dimensions)}`).toBeLessThanOrEqual(dimensions.clientWidth + 1);
}

async function expectInsideViewport(page: Page, accessibleName: string) {
  const control = page.getByRole("button", { name: accessibleName });
  const box = await control.boundingBox();
  const headerBox = await page.locator("header").boundingBox();
  expect(box, `${accessibleName} must have a rendered box`).not.toBeNull();
  expect(headerBox, "header must have a rendered box").not.toBeNull();
  expect(box!.x, `${accessibleName} left edge`).toBeGreaterThanOrEqual(0);
  expect(box!.x + box!.width, `${accessibleName} right edge`).toBeLessThanOrEqual(320);
  expect(box!.y, `${accessibleName} top edge`).toBeGreaterThanOrEqual(headerBox!.y);
  expect(box!.y + box!.height, `${accessibleName} bottom edge`).toBeLessThanOrEqual(headerBox!.y + headerBox!.height);
}

async function expectUnclippedLabels(page: Page) {
  const labels = page.locator(".mantine-Badge-label, .mantine-Accordion-label, .mantine-Button-label");
  const clipped = await labels.evaluateAll((elements) => elements
    .filter((element) => element.getClientRects().length > 0 &&
      (element.scrollWidth > element.clientWidth + 1 || element.scrollHeight > element.clientHeight + 1))
    .map((element) => element.textContent));
  expect(clipped).toEqual([]);
}

async function chooseFormat(page: Page, title: string) {
  const selector = page.getByRole("combobox", { name: "Exercise type" });
  await openGenerationControls(page);
  await selector.click();
  await page.getByRole("option", { name: title }).click();
  await expect(selector).toHaveValue(title);
}

test("all pilot formats and recovery states reflow at 320px with 200% text", async ({ page }) => {
  const pageErrors: Error[] = [];
  page.on("pageerror", (error) => pageErrors.push(error));
  await page.setViewportSize({ width: 320, height: 900 });
  await page.addInitScript(() => {
    const enlargeText = () => {
      const style = document.createElement("style");
      style.textContent = "html { font-size: 200% !important; }";
      document.documentElement.append(style);
    };
    if (document.documentElement) enlargeText();
    else document.addEventListener("DOMContentLoaded", enlargeText, { once: true });
  });
  await page.goto("/");
  await expect(page.getByRole("button", { name: "Check answer" })).toBeVisible();
  expect(await page.evaluate(() => getComputedStyle(document.documentElement).fontSize)).toBe("32px");
  expect(pageErrors).toEqual([]);
  await expectInsideViewport(page, "Toggle navigation");
  await expectInsideViewport(page, "Use dark theme");

  const skipLink = page.getByRole("link", { name: "Skip to learning exercise" });
  await expect(skipLink).not.toBeInViewport();
  const macWebKit = process.platform === "darwin" && page.context().browser()?.browserType().name() === "webkit";
  const tab = macWebKit ? "Alt+Tab" : "Tab";
  await page.keyboard.press(tab);
  await expect(skipLink).toBeFocused();
  await expect(skipLink).toBeInViewport();
  await page.keyboard.press(tab);

  await page.getByRole("button", { name: "Toggle navigation" }).click();
  await expect(page.getByRole("navigation")).toBeVisible();
  await page.getByRole("button", { name: "Toggle navigation" }).click();
  await expectNoHorizontalOverflow(page, "present-form exercise");
  await expectUnclippedLabels(page);

  await page.getByRole("button", { name: "Learn this first" }).click();
  await expectNoHorizontalOverflow(page, "open lesson");
  await page.getByRole("button", { name: "Show hint" }).click();
  await expectNoHorizontalOverflow(page, "hint");
  await page.getByLabel("Answer choices").getByRole("button").first().click();
  await page.getByRole("button", { name: "Check answer" }).click();
  await expect(page.getByRole("status")).toBeVisible();
  await expectNoHorizontalOverflow(page, "feedback");
  await expectUnclippedLabels(page);

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
  await expectUnclippedLabels(page);
  expect(pageErrors).toEqual([]);
});

for (const viewport of [{ width: 844, height: 390 }, { width: 320, height: 400 }]) {
  for (const fontSize of [16, 32]) {
    test(`format navigation stays inside short ${viewport.width}x${viewport.height} windows at ${fontSize}px`, async ({ page }) => {
      await page.setViewportSize(viewport);
      await page.goto("/");
      await expect(page.getByRole("button", { name: "Check answer" })).toBeVisible();
      if (fontSize === 32) await page.addStyleTag({ content: "html { font-size: 200% !important; }" });
      expect(await page.evaluate(() => getComputedStyle(document.documentElement).fontSize)).toBe(`${fontSize}px`);
      await openGenerationControls(page);
      const selector = page.getByRole("combobox", { name: "Exercise type" });

      for (const title of ["Choose the present form", "Supply the indefinite article", "Arrange a subject-first statement"]) {
        await test.step(`reopen ${title}`, async () => {
          if ((await selector.inputValue()) !== title) await chooseFormat(page, title);
          await selector.click();
          const current = page.getByRole("option", { name: title });
          await expect(current).toHaveAttribute("data-combobox-selected", "true");
          await expectHighlightedFormatVisible(page);
          const documentScroll = await page.evaluate(() => document.scrollingElement?.scrollTop);
          for (let step = 0; step < 3; step += 1) {
            await page.keyboard.press("ArrowDown");
            await expectHighlightedFormatVisible(page);
          }
          expect(await page.evaluate(() => document.scrollingElement?.scrollTop)).toBe(documentScroll);
          await expectNoHorizontalOverflow(page, `${viewport.width}x${viewport.height} at ${fontSize}px`);
          await page.keyboard.press("Escape");
          await expect(selector).toHaveAttribute("aria-expanded", "false");
        });
      }
    });
  }
}
