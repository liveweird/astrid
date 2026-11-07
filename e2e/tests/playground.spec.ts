import { expect, test, type Locator, type Page } from "@playwright/test";
import { openGenerationControls, waitForGenerationControls } from "../support/generation-controls.js";

function channelToLinear(value: number) {
  const normalized = value / 255;
  return normalized <= 0.04045 ? normalized / 12.92 : ((normalized + 0.055) / 1.055) ** 2.4;
}

function luminance(color: string) {
  const channels = color.match(/[\d.]+/g)?.slice(0, 3).map(Number) ?? [0, 0, 0];
  return 0.2126 * channelToLinear(channels[0]!) + 0.7152 * channelToLinear(channels[1]!) +
    0.0722 * channelToLinear(channels[2]!);
}

function contrast(foreground: string, background: string) {
  const [lighter, darker] = [luminance(foreground), luminance(background)].sort((a, b) => b - a);
  return (lighter! + 0.05) / (darker! + 0.05);
}

async function openPlayground(page: Page) {
  await page.goto("/");
  await expect(page.getByRole("heading", { name: "Danish engine playground" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Check answer" })).toBeVisible();
}

async function chooseFormat(page: Page, title: string) {
  const selector = page.getByRole("combobox", { name: "Exercise type" });
  await openGenerationControls(page);
  await selector.click();
  await page.getByRole("option", { name: title }).click();
  await expect(selector).toHaveValue(title);
}

async function tabTo(page: Page, target: Locator, reverse = false) {
  // Safari's default macOS focus policy uses Option-Tab for all clickable controls.
  const macWebKit = process.platform === "darwin" && page.context().browser()?.browserType().name() === "webkit";
  const tab = macWebKit ? "Alt+Tab" : "Tab";
  for (let step = 0; step < 40; step += 1) {
    await page.keyboard.press(reverse ? `Shift+${tab}` : tab);
    if (await target.evaluate((element) => element === document.activeElement)) return;
  }
  throw new Error(`Keyboard focus did not reach ${await target.getAttribute("aria-label") ?? "the expected control"}`);
}

async function enterOrderingAnswer(page: Page) {
  const instruction = page.getByRole("heading", { name: "Arrange the blocks to make a statement beginning with the subject." });
  await expect(instruction).toBeFocused();
  const available = page.getByLabel("Available sentence parts");
  const subject = available.getByRole("button", { name: "I", exact: true });
  await tabTo(page, subject);
  await page.keyboard.press("Enter");
  await expect(available.getByRole("button", { name: "har", exact: true })).toBeFocused();
  await page.keyboard.press("Enter");
  await expect(available.getByRole("button", { name: "en hund", exact: true })).toBeFocused();
  await page.keyboard.press("Enter");
  const last = page.getByRole("button", { name: "Remove en hund from position 3" });
  await expect(last).toBeFocused();
  await expect(page.getByRole("button", { name: "Remove I from position 1" })).toHaveText("I");
  await page.keyboard.press("Enter");
  await expect(available.getByRole("button", { name: "en hund", exact: true })).toBeFocused();
  await page.keyboard.press("Enter");
  await expect(last).toBeFocused();
}

test("completes each pilot exercise format", async ({ page }) => {
  await openPlayground(page);

  await page.getByRole("button", { name: "Learn this first" }).click();
  await expect(page.getByRole("region", { name: "Learn this first" }).getByText(/Its finite present form/)).toBeVisible();
  await page.getByLabel("Answer choices").getByRole("button").first().click();
  await page.getByRole("button", { name: "Check answer" }).click();
  await expect(page.getByRole("status")).toBeVisible();
  await expect(page.getByRole("status").getByText("Try 1 · Supported practice", { exact: true })).toBeVisible();

  await chooseFormat(page, "Supply the indefinite article");
  await page.getByLabel("Missing word").fill("en");
  await page.getByRole("button", { name: "Check answer" }).click();
  await expect(page.getByRole("status")).toBeVisible();

  await chooseFormat(page, "Arrange a subject-first statement");
  const available = page.getByLabel("Available sentence parts").getByRole("button");
  while (await available.count()) await available.first().click();
  await page.getByRole("button", { name: "Check answer" }).click();
  await expect(page.getByRole("status")).toBeVisible();
});

test("replays the exact attempt after an uncertain connection result", async ({ page }) => {
  const bodies: string[] = [];
  await page.route("**/api/v1/attempts", async (route) => {
    bodies.push(route.request().postData() ?? "");
    if (bodies.length <= 2) {
      // Let the server commit, then simulate the response connection disappearing.
      await route.fetch();
      await route.abort("connectionfailed");
    }
    else await route.continue();
  });
  await openPlayground(page);
  await page.getByLabel("Answer choices").getByRole("button").first().click();
  await page.getByRole("button", { name: "Check answer" }).click();
  await expect(page.getByText("The result is not confirmed")).toBeVisible();
  await expect(page.getByLabel("Answer choices").getByRole("button").first()).toBeDisabled();
  await page.getByRole("button", { name: "Check the same attempt" }).click();
  await expect(page.getByText("The result is not confirmed")).toBeVisible();
  await page.getByRole("button", { name: "Check the same attempt" }).click();
  await expect(page.getByRole("status")).toBeVisible();
  expect(bodies).toHaveLength(3);
  expect(bodies[1]).toBe(bodies[0]);
  expect(bodies[2]).toBe(bodies[0]);
});

test("completes, retries and advances an ordering exercise with the keyboard", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await openPlayground(page);
  const controls = page.getByRole("button", { name: "Generation controls" });
  await tabTo(page, controls);
  await page.keyboard.press("Enter");
  await waitForGenerationControls(page);
  const index = page.getByRole("textbox", { name: "Next generation index" });
  await tabTo(page, index);
  await page.keyboard.press("ControlOrMeta+A");
  await page.keyboard.type("16");
  const selector = page.getByRole("combobox", { name: "Exercise type" });
  await tabTo(page, selector, true);
  await page.keyboard.press("ControlOrMeta+A");
  await page.keyboard.type("Arrange a subject-first statement");
  const orderingOption = page.getByRole("option", { name: "Arrange a subject-first statement" });
  await expect(orderingOption).toBeVisible();
  await page.keyboard.press("ArrowDown");
  await expect(orderingOption).toHaveAttribute("data-combobox-selected", "true");
  await expect(selector).toBeFocused();
  await page.keyboard.press("Enter");
  await expect(selector).toHaveValue("Arrange a subject-first statement");

  await enterOrderingAnswer(page);
  const check = page.getByRole("button", { name: "Check answer" });
  await tabTo(page, check);
  await page.keyboard.press("Enter");
  await expect(page.getByRole("status")).toBeVisible();

  const retry = page.getByRole("button", { name: "Try again" });
  await tabTo(page, retry);
  await page.keyboard.press("Enter");
  await enterOrderingAnswer(page);
  await tabTo(page, check);
  await page.keyboard.press("Enter");
  await expect(page.getByRole("status")).toBeVisible();

  const next = page.getByRole("button", { name: "Next exercise" });
  await tabTo(page, next);
  await page.keyboard.press("Enter");
  await expect(page.getByRole("heading", { name: "Arrange the blocks to make a statement beginning with the subject." })).toBeFocused();
});

test("explicit light theme overrides a dark operating-system preference", async ({ page }) => {
  await page.emulateMedia({ colorScheme: "dark" });
  await openPlayground(page);
  await page.getByRole("button", { name: "Use light theme" }).click();
  await expect(page.locator("html")).toHaveAttribute("data-mantine-color-scheme", "light");
  await expect(page.getByRole("button", { name: "Use dark theme" })).toBeVisible();

  const styles = await page.evaluate(() => {
    const style = (selector: string) => {
      const computed = getComputedStyle(document.querySelector(selector)!);
      return { background: computed.backgroundColor, color: computed.color };
    };
    return {
      header: style("header"),
      navbar: style("nav"),
      canvas: style("#main-content"),
      card: style(".mantine-Paper-root"),
      heading: style("h2"),
    };
  });
  expect(styles.header.background).toBe("rgb(255, 255, 255)");
  expect(styles.navbar.background).toBe("rgb(255, 255, 255)");
  expect(styles.canvas.background).toBe("rgb(248, 249, 251)");
  expect(styles.card.background).toBe("rgb(255, 255, 255)");
  expect(contrast(styles.heading.color, styles.canvas.background)).toBeGreaterThanOrEqual(4.5);
});
