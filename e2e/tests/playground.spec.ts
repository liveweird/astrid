import { expect, test, type Page } from "@playwright/test";

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
  if (!(await selector.isVisible())) await page.getByRole("button", { name: "Generation controls" }).click();
  await selector.click();
  await page.getByRole("option", { name: title }).click();
  await expect(selector).toHaveValue(title);
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
    if (bodies.length === 1) {
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
  await page.getByRole("button", { name: "Check the same attempt" }).click();
  await expect(page.getByRole("status")).toBeVisible();
  expect(bodies).toHaveLength(2);
  expect(bodies[1]).toBe(bodies[0]);
});

test("supports a narrow-screen keyboard ordering journey", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await openPlayground(page);
  await chooseFormat(page, "Arrange a subject-first statement");
  const firstBlock = page.getByLabel("Available sentence parts").getByRole("button").first();
  const blockText = await firstBlock.textContent();
  await firstBlock.focus();
  await page.keyboard.press("Enter");
  await expect(page.getByLabel(new RegExp(`Remove ${blockText ?? ""} from position 1`))).toBeVisible();
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
