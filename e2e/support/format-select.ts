import { expect, type Page } from "@playwright/test";

export async function expectHighlightedFormatVisible(page: Page) {
  const selector = page.getByRole("combobox", { name: "Exercise type" });
  const list = page.getByRole("listbox", { name: "Exercise type" });
  const highlighted = list.locator('[data-combobox-selected="true"]');
  await expect(selector).toBeFocused();
  await expect(highlighted).toHaveCount(1);
  await expect(selector).toHaveAttribute("aria-activedescendant", (await highlighted.getAttribute("id"))!);
  await expect.poll(async () => {
    const viewport = page.viewportSize();
    const listBox = await list.evaluate((element) => {
      const box = element.getBoundingClientRect();
      const top = box.top + element.clientTop;
      const left = box.left + element.clientLeft;
      return { top, left, bottom: top + element.clientHeight, right: left + element.clientWidth };
    });
    const optionBox = await highlighted.boundingBox();
    if (!viewport || !optionBox) return ["missing bounds"];
    const clipped = [];
    if (optionBox.y < Math.max(0, listBox.top) - 1) clipped.push("above visible list");
    if (optionBox.y + optionBox.height > Math.min(viewport.height, listBox.bottom) + 1) clipped.push("below visible list");
    if (optionBox.x < Math.max(0, listBox.left) - 1) clipped.push("left of visible list");
    if (optionBox.x + optionBox.width > Math.min(viewport.width, listBox.right) + 1) clipped.push("right of visible list");
    return clipped;
  }).toEqual([]);
}
