import { expect, test, type Page } from "@playwright/test";
import { openGenerationControls } from "../support/generation-controls.js";

async function choose(page: Page, title: string, index: number) {
  await openGenerationControls(page);
  await page.getByRole("textbox", { name: "Next generation seed" }).fill("47");
  await page.getByRole("textbox", { name: "Next generation index" }).fill(String(index));
  const selector = page.getByRole("combobox", { name: "Exercise type" });
  if (await selector.inputValue() === title) {
    await page.getByRole("button", { name: "Regenerate", exact: true }).click();
  } else {
    await selector.click();
    await page.getByRole("option", { name: title, exact: true }).click();
  }
  await expect(selector).toHaveValue(title);
}

async function open(page: Page) {
  await page.goto("/");
  await expect(page.getByRole("button", { name: "Check answer" })).toBeVisible();
}

async function check(page: Page, target: string, language: string) {
  await page.getByRole("button", { name: "Check answer" }).click();
  const status = page.getByRole("status");
  await expect(status).toBeFocused();
  await expect(status.getByText(target, { exact: true })).toBeVisible();
  await expect(status.getByText(language, { exact: true })).toBeVisible();
}

function nativeTab(page: Page) {
  return process.platform === "darwin" && page.context().browser()?.browserType().name() === "webkit" ? "Alt+Tab" : "Tab";
}

test("recalls a named verb with keyboard Danish entry and explicit support evidence", async ({ page }) => {
  await open(page);
  // Published legacy sampling coordinates retain the same canonical pool on revision 2.
  await choose(page, "Supply the present form", 5);
  await expect(page.getByRole("heading", { name: /Type the finite present form of læse/ })).toBeVisible();
  const input = page.getByRole("textbox", { name: "Present form", exact: true });
  await input.fill("lXser");
  await input.press(process.platform === "darwin" ? "Meta+ArrowLeft" : "Home");
  await input.press("ArrowRight");
  await input.press("Shift+ArrowRight");
  expect(await input.evaluate((element: HTMLInputElement) => [element.selectionStart, element.selectionEnd])).toEqual([1, 2]);
  await page.keyboard.press(nativeTab(page));
  await expect(page.getByRole("button", { name: "Insert æ", exact: true })).toBeFocused();
  await page.keyboard.press("Enter");
  await expect(input).toHaveValue("læser");
  await expect(input).toBeFocused();
  expect(await input.evaluate((element: HTMLInputElement) => [element.selectionStart, element.selectionEnd])).toEqual([2, 2]);
  await check(page, "Target met", "Acceptable Danish");
  await expect(page.getByRole("status").getByText("Hun læser et brev.", { exact: true })).toBeVisible();
  await expect(page.getByRole("status").getByText("Try 1 · No support opened", { exact: true })).toBeVisible();

  await page.getByRole("button", { name: "Try again" }).click();
  await page.getByRole("button", { name: "Learn this first" }).click();
  await expect(page.getByRole("region", { name: "Learn this first" }).getByText(/Its finite present form is læser/)).toBeVisible();
  await input.fill("læser");
  await check(page, "Target met", "Acceptable Danish");
  await expect(page.getByRole("status").getByText("Try 2 · Supported practice", { exact: true })).toBeVisible();
});

test("repairs only the named verb and distinguishes licensed alternatives from unknown writing", async ({ page }) => {
  await open(page);
  await choose(page, "Repair the verb form", 5);
  await expect(page.getByText("Word to replace", { exact: true })).toBeVisible();
  const input = page.getByRole("textbox", { name: "Replacement verb", exact: true });
  await input.fill("har");
  await check(page, "Target to revisit", "Acceptable Danish");
  await expect(page.getByRole("status").getByText(/task asks for the named verb/)).toBeVisible();
  await page.getByRole("button", { name: "Try again" }).click();
  await input.fill("unfamiliar");
  await check(page, "Target to revisit", "Language status unknown");
  await page.getByRole("button", { name: "Try again" }).click();
  await input.fill("læse");
  await check(page, "Target to revisit", "Known language issue");
  await page.getByRole("button", { name: "Try again" }).click();
  await input.fill("læser");
  await check(page, "Target met", "Acceptable Danish");
  await expect(page.getByRole("status").getByText("Hun læser et brev.", { exact: true })).toBeVisible();
  await choose(page, "Repair the verb form", 1);
  await expect(page.getByRole("group", { name: "Danish sentence to repair" })).toContainText("sove.");
  await input.fill("sover");
  await check(page, "Target met", "Acceptable Danish");
  await expect(page.getByRole("status").getByText("Du sover.", { exact: true })).toBeVisible();
});

test("repairs an article while distinguishing a numeral from the unstressed target", async ({ page }) => {
  await open(page);
  await choose(page, "Repair the indefinite article", 0);
  const input = page.getByRole("textbox", { name: "Replacement article", exact: true });
  await input.fill("et");
  await check(page, "Target to revisit", "Known language issue");
  await page.getByRole("button", { name: "Try again" }).click();
  await input.fill("én");
  await check(page, "Target to revisit", "Acceptable Danish");
  await expect(page.getByRole("status").getByText(/task asks for the unstressed indefinite article/)).toBeVisible();
  await page.getByRole("button", { name: "Try again" }).click();
  await input.fill("en");
  await check(page, "Target met", "Acceptable Danish");
  await expect(page.getByRole("status").getByText("Vi læser en bog.", { exact: true })).toBeVisible();
});

test("freezes typed recall and character entry until an uncertain attempt is replayed", async ({ page }) => {
  const payloads: string[] = [];
  await page.route("**/api/v1/attempts", async (route) => {
    payloads.push(route.request().postData() ?? "");
    if (payloads.length === 1) {
      await route.fetch();
      await route.abort("connectionfailed");
    } else await route.continue();
  });
  await open(page);
  await choose(page, "Supply the present form", 5);
  const input = page.getByRole("textbox", { name: "Present form", exact: true });
  await input.fill("læser");
  await page.getByRole("button", { name: "Check answer" }).click();
  await expect(page.getByText("The result is not confirmed")).toBeVisible();
  await expect(input).toBeDisabled();
  for (const letter of ["æ", "ø", "å"]) await expect(page.getByRole("button", { name: `Insert ${letter}`, exact: true })).toBeDisabled();
  await page.getByRole("button", { name: "Check the same attempt" }).click();
  await expect(page.getByRole("status")).toBeFocused();
  expect(payloads).toHaveLength(2);
  expect(payloads[1]).toBe(payloads[0]);
  await expect(page.getByRole("status").getByText("Try 1 · No support opened", { exact: true })).toBeVisible();
});
