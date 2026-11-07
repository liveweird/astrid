import { expect, type Page } from "@playwright/test";

export async function waitForGenerationControls(page: Page) {
  // During expansion the panel clips its contents; scrolling the Select then can auto-scroll
  // that transient container and leave the portaled dropdown hidden after it settles.
  await expect(page.getByRole("region", { name: "Generation controls" })).toHaveCSS("overflow-y", "visible");
}

export async function openGenerationControls(page: Page) {
  const control = page.getByRole("button", { name: "Generation controls" });
  if ((await control.getAttribute("aria-expanded")) !== "true") await control.click();
  await expect(control).toHaveAttribute("aria-expanded", "true");
  await waitForGenerationControls(page);
}
