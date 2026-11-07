import { expect, test, type Locator, type Page } from "@playwright/test";
import { chooseVocabulary, textLanguage, VOCABULARY } from "../support/vocabulary.js";

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

async function chooseWithKeyboard(page: Page, choice: Locator) {
  const tab = process.platform === "darwin" && page.context().browser()?.browserType().name() === "webkit" ? "Alt+Tab" : "Tab";
  for (let step = 0; step < 40; step += 1) {
    if (await choice.evaluate((element) => element === document.activeElement)) {
      await expect(choice).toBeFocused();
      await page.keyboard.press("Enter");
      return;
    }
    await page.keyboard.press(tab);
  }
  throw new Error("Native keyboard traversal did not reach the English meaning choice");
}

function recallTarget(cue: string) {
  const target = VOCABULARY.find((entry) => entry.meaning === cue);
  expect(target, `Authored meaning for ${cue}`).toBeDefined();
  return target!;
}

test("recognizes all three meanings with related English choices and tracked teaching support", async ({ page }) => {
  await open(page);
  const seen = new Set<string>();
  for (let index = 0; index < 3; index += 1) {
    const exercise = await chooseVocabulary(page, "Recognize the meaning", index);
    const target = VOCABULARY.find((entry) => entry.headword === exercise.prompt.cue)!;
    seen.add(target.headword);
    expect(exercise.lexicalTarget.facet).toBe("meaning");
    expect(exercise.lexicalTarget.direction).toBe("da-en");
    expect(await textLanguage(page.getByText(target.headword, { exact: true }))).toBe("da");
    const choices = page.getByLabel("Answer choices");
    await expect(choices.getByRole("button")).toHaveCount(3);
    for (const entry of VOCABULARY) {
      const choice = choices.getByRole("button", { name: entry.meaning, exact: true });
      await expect(choice).toBeVisible();
      expect(await textLanguage(choice)).toBe("en");
    }
    const wrong = VOCABULARY.find((entry) => entry.headword !== target.headword)!;
    await choices.getByRole("button", { name: wrong.meaning, exact: true }).click();
    await check(page, "Target to revisit", "Recognized English choice");
    await expect(page.getByRole("status").getByText(/judgment about Danish/)).toHaveCount(0);
    await page.getByRole("button", { name: "Try again" }).click();
    if (index === 0) await page.getByRole("button", { name: "Learn this first" }).click();
    const correctChoice = choices.getByRole("button", { name: target.meaning, exact: true });
    if (index === 0) await chooseWithKeyboard(page, correctChoice);
    else await correctChoice.click();
    await check(page, "Target met", "Recognized English choice");
    await expect(page.getByRole("status").getByText(index === 0 ? "Try 2 · Supported practice" : "Try 2 · No support opened", { exact: true })).toBeVisible();
  }
  expect([...seen].sort()).toEqual(["avis", "bog", "brev"]);
});

test("recalls all three introduced headwords without a visible answer reveal", async ({ page }) => {
  await open(page);
  const seen = new Set<string>();
  for (let index = 0; index < 3; index += 1) {
    const exercise = await chooseVocabulary(page, "Recall the Danish headword", index);
    const target = recallTarget(exercise.prompt.cue);
    seen.add(target.headword);
    expect(exercise.lexicalTarget.facet).toBe("singular-headword");
    expect(exercise.lexicalTarget.direction).toBe("en-da");
    expect(await textLanguage(page.getByText(target.meaning, { exact: true }))).toBe("en");
    await expect(page.getByText(target.headword, { exact: true })).toBeHidden();
    const input = page.getByRole("textbox", { name: "Danish singular headword", exact: true });
    await expect(input).toHaveAttribute("lang", "da");
    const wrong = VOCABULARY.find((entry) => entry.headword !== target.headword)!;
    await input.fill(wrong.headword);
    await check(page, "Target to revisit", "Recognized Danish headword");
    await page.getByRole("button", { name: "Try again" }).click();
    if (index === 0) {
      await input.click();
      await page.keyboard.type(target.headword.toUpperCase());
    } else await input.fill(target.headword.toUpperCase());
    await check(page, "Target met", "Recognized Danish headword");
    await expect(page.getByRole("status").getByText("Good work", { exact: true })).toBeVisible();
  }
  expect([...seen].sort()).toEqual(["avis", "bog", "brev"]);
});

test("separates a known headword from an extra article, gender and unknown-writing constraints", async ({ page }) => {
  await open(page);
  const exercise = await chooseVocabulary(page, "Recall the Danish headword");
  const target = recallTarget(exercise.prompt.cue);
  const input = page.getByRole("textbox", { name: "Danish singular headword", exact: true });
  const article = target.headword === "brev" ? "et" : "en";
  await input.fill(`${article} ${target.headword}`);
  await check(page, "Target met", "Recognized Danish headword");
  await expect(page.getByRole("status").getByText("One-word response required", { exact: true })).toBeVisible();
  await expect(page.getByRole("status").getByText(/You identified the introduced headword/)).toBeVisible();
  await expect(page.getByRole("status").getByText("Good work", { exact: true })).toHaveCount(0);
  await page.getByRole("button", { name: "Try again" }).click();
  await input.fill(`${article === "en" ? "et" : "en"} ${target.headword}`);
  await check(page, "Target met", "Known language issue");
  await expect(page.getByRole("status").getByText("One-word response required", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Try again" }).click();
  await input.fill("unfamiliar");
  await check(page, "Target to revisit", "Language status unknown");
  await expect(page.getByRole("status").getByText(/alternative translation/)).toBeVisible();
  await page.getByRole("button", { name: "Try again" }).click();
  await page.getByRole("button", { name: "Learn this first" }).click();
  await input.fill(target.headword);
  await check(page, "Target met", "Recognized Danish headword");
  await expect(page.getByRole("status").getByText("Try 4 · Supported practice", { exact: true })).toBeVisible();
});

test("replays an uncertain lexical submission with its original target and support evidence", async ({ page }) => {
  const payloads: string[] = [];
  await page.route("**/api/v1/attempts", async (route) => {
    payloads.push(route.request().postData() ?? "");
    if (payloads.length === 1) {
      await route.fetch();
      await route.abort("connectionfailed");
    } else await route.continue();
  });
  await open(page);
  const exercise = await chooseVocabulary(page, "Recall the Danish headword");
  const target = recallTarget(exercise.prompt.cue);
  const input = page.getByRole("textbox", { name: "Danish singular headword", exact: true });
  await input.fill(target.headword);
  await page.getByRole("button", { name: "Check answer" }).click();
  await expect(page.getByText("The result is not confirmed")).toBeVisible();
  await expect(input).toBeDisabled();
  for (const letter of ["æ", "ø", "å"]) await expect(page.getByRole("button", { name: `Insert ${letter}`, exact: true })).toBeDisabled();
  await page.getByRole("button", { name: "Check the same attempt" }).click();
  await expect(page.getByRole("status")).toBeFocused();
  await expect(page.getByRole("status").getByText("Recognized Danish headword", { exact: true })).toBeVisible();
  expect(payloads).toHaveLength(2);
  expect(payloads[1]).toBe(payloads[0]);
  await expect(page.getByRole("status").getByText("Try 1 · No support opened", { exact: true })).toBeVisible();
});
