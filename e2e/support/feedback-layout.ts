import { expect, type Page } from "@playwright/test";

export async function expectFeedbackBadgesUnclipped(page: Page) {
  const clipped = await page.getByRole("status").locator(".mantine-Badge-root").evaluateAll((badges) => badges.flatMap((badge) => {
    const box = badge.getBoundingClientRect();
    const style = getComputedStyle(badge);
    const length = (value: string, extent: number) => parseFloat(value) * (value.endsWith("%") ? extent / 100 : 1);
    const radius = (value: string) => {
      const parts = value.split(" ");
      return { x: length(parts[0]!, box.width), y: length(parts[1] ?? parts[0]!, box.height) };
    };
    const radii = [style.borderTopLeftRadius, style.borderTopRightRadius, style.borderBottomRightRadius, style.borderBottomLeftRadius].map(radius);
    const [tl, tr, br, bl] = radii as [{ x: number; y: number }, { x: number; y: number }, { x: number; y: number }, { x: number; y: number }];
    // CSS scales oversized corner radii together until opposing corners fit the box.
    const scale = Math.min(1, box.width / (tl.x + tr.x), box.width / (bl.x + br.x),
      box.height / (tl.y + bl.y), box.height / (tr.y + br.y));
    const corners = radii.map((r, index) => ({
      rx: r.x * scale,
      ry: r.y * scale,
      x: index === 0 || index === 3 ? box.left + r.x * scale : box.right - r.x * scale,
      y: index < 2 ? box.top + r.y * scale : box.bottom - r.y * scale,
    }));
    const outside = (x: number, y: number) => {
      if (x < box.left || x > box.right || y < box.top || y > box.bottom) return true;
      return corners.some((corner, index) => {
        const inX = index === 0 || index === 3 ? x < corner.x : x > corner.x;
        const inY = index < 2 ? y < corner.y : y > corner.y;
        return inX && inY && corner.rx > 0 && corner.ry > 0 &&
          ((x - corner.x) / corner.rx) ** 2 + ((y - corner.y) / corner.ry) ** 2 > 1;
      });
    };
    const walker = document.createTreeWalker(badge, NodeFilter.SHOW_TEXT);
    let node = walker.nextNode();
    let clippedText = false;
    while (node) {
      if (node.textContent?.trim()) {
        const range = document.createRange();
        range.selectNodeContents(node);
        for (const rect of range.getClientRects()) {
          // A one-pixel inset permits subpixel font and border rounding.
          const points = [[rect.left + 1, rect.top + 1], [rect.right - 1, rect.top + 1],
            [rect.left + 1, rect.bottom - 1], [rect.right - 1, rect.bottom - 1]];
          if (points.some(([x, y]) => outside(x!, y!))) clippedText = true;
        }
      }
      node = walker.nextNode();
    }
    return clippedText ? [badge.textContent] : [];
  }));
  expect(clipped, "Feedback text must fit inside the badge's actual rounded clipping shape").toEqual([]);
}
