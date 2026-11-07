import { expect, type Locator, type Page } from "@playwright/test";
import { openGenerationControls } from "./generation-controls.js";

export const VOCABULARY = [
  { headword: "bog", meaning: "book" },
  { headword: "avis", meaning: "newspaper" },
  { headword: "brev", meaning: "letter (written message)" },
] as const;

type VocabularyPresentation = {
  formatId: string;
  prompt: { cue: string; cueLanguage: string; answerLanguage: string };
  lexicalTarget: { entryId: string; senseId: string; facet: string; direction: string };
};

export async function chooseVocabulary(page: Page, title: string, index = 0) {
  await openGenerationControls(page);
  await page.getByRole("textbox", { name: "Next generation seed" }).fill("47");
  await page.getByRole("textbox", { name: "Next generation index" }).fill(String(index));
  const selector = page.getByRole("combobox", { name: "Exercise type" });
  const generated = page.waitForResponse((response) => response.request().method() === "POST" &&
    new URL(response.url()).pathname === "/api/v1/exercises" && response.status() === 201);
  if (await selector.inputValue() === title) {
    await page.getByRole("button", { name: "Regenerate", exact: true }).click();
  } else {
    await selector.click();
    await page.getByRole("option", { name: title, exact: true }).click();
  }
  const exercise = await (await generated).json() as VocabularyPresentation;
  await expect(selector).toHaveValue(title);
  await expect(page.getByText(exercise.prompt.cue, { exact: true })).toBeVisible();
  return exercise;
}

export async function textLanguage(locator: Locator) {
  return locator.evaluate((element) => {
    const walker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT);
    let text = walker.nextNode();
    while (text && !text.textContent?.trim()) text = walker.nextNode();
    return text?.parentElement?.closest("[lang]")?.getAttribute("lang");
  });
}
