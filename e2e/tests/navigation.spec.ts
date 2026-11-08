import { expect, test, type Locator, type Page } from "@playwright/test";

function tabKey(page: Page) {
  const macWebKit = process.platform === "darwin" && page.context().browser()?.browserType().name() === "webkit";
  return macWebKit ? "Alt+Tab" : "Tab";
}

async function tabTo(page: Page, target: Locator) {
  const tab = tabKey(page);
  for (let step = 0; step < 12; step += 1) {
    await page.keyboard.press(tab);
    if (await target.evaluate((element) => element === document.activeElement)) return;
  }
  throw new Error("Keyboard focus did not reach the navigation destination");
}

async function openPlayground(page: Page) {
  await page.goto("/");
  await expect(page.getByRole("heading", { name: "Danish engine playground" })).toBeVisible();
}

test("mobile navigation opens and closes with native keyboard behavior", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await openPlayground(page);

  const burger = page.getByRole("button", { name: "Toggle navigation" });
  const navigation = page.getByRole("navigation");
  const destination = navigation.getByRole("link", { name: "Engine playground" });
  await expect(burger).toHaveAttribute("aria-controls", "primary-navigation");
  await expect(burger).toHaveAttribute("aria-expanded", "false");
  await expect(navigation).toBeHidden();
  await expect(destination).toBeHidden();

  await tabTo(page, burger);
  await page.keyboard.press("Enter");

  await expect(burger).toHaveAttribute("aria-expanded", "true");
  await expect(navigation).toBeVisible();
  await tabTo(page, destination);
  await expect(destination).toBeFocused();
  await page.keyboard.press("Enter");

  await expect(burger).toHaveAttribute("aria-expanded", "false");
  await expect(page.locator("#main-content")).toBeFocused();
  await expect(navigation).toBeHidden();
  await expect(destination).toBeHidden();
});

test("desktop navigation remains in native keyboard order", async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 900 });
  await openPlayground(page);

  const navigation = page.getByRole("navigation");
  const destination = navigation.getByRole("link", { name: "Engine playground" });
  await expect(navigation).toBeVisible();
  await tabTo(page, destination);
  await expect(destination).toBeFocused();
});

test("persisted rail preference survives a mobile drawer and viewport changes", async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem("astrid.viewSettings.appShell.navCollapsed", "true");
  });
  await page.setViewportSize({ width: 390, height: 844 });
  await openPlayground(page);

  const burger = page.getByRole("button", { name: "Toggle navigation" });
  const navigation = page.getByRole("navigation");
  await tabTo(page, burger);
  await page.keyboard.press("Enter");
  await expect(burger).toHaveAttribute("aria-expanded", "true");
  await expect(navigation.getByRole("link", { name: "Engine playground" })).toBeVisible();

  await page.setViewportSize({ width: 1280, height: 900 });

  await expect.poll(async () => (await navigation.boundingBox())?.width).toBe(64);
  const railToggle = page.getByRole("button", { name: "Expand navigation" });
  await tabTo(page, railToggle);
  await expect(railToggle).toBeFocused();
  await expect(railToggle).toHaveAttribute("aria-expanded", "false");
  await expect(railToggle).toHaveAttribute("aria-controls", "primary-navigation");
  expect(await page.evaluate(() => localStorage.getItem("astrid.viewSettings.appShell.navCollapsed"))).toBe("true");

  await page.setViewportSize({ width: 390, height: 844 });

  await expect(burger).toHaveAttribute("aria-expanded", "false");
  await expect(navigation).toBeHidden();
  await expect(navigation.getByRole("link", { name: "Engine playground" })).toBeHidden();
});
