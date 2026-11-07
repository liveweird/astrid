import { expect, test } from "@playwright/test";
import { choosePractice, readPractice } from "../support/practice.js";

type Teaching = { title: string; body: string };
type TeachingState = {
  introduction: Teaching[];
  currentExercise: { introduction: Teaching[]; hint: string };
  introductionOpened: boolean;
};

for (const viewport of [{ width: 390, height: 844 }, { width: 768, height: 360 }]) {
  for (const kind of ["introduction", "lesson", "hint"] as const) {
    for (const retry of [false, true]) {
      if (retry && viewport.width !== 768) continue;
      test(`practice reveals ${kind} after saving and keeps it readable at ${viewport.width}×${viewport.height} with 200% text${retry ? " after saved-operation retry" : ""}`, async ({ page }) => {
        await page.setViewportSize(viewport);
        await page.goto("/practice");
        await choosePractice(page);
        await expect(page.getByRole("button", { name: "Check answer", exact: true })).toBeVisible();
        const id = new URL(page.url()).searchParams.get("session")!;
        const source = await (await page.request.get(`/api/v1/practice-sessions/${id}`)).json() as TeachingState;
        const controlName = kind === "introduction" ? "Session introduction" : kind === "lesson" ? "Learn this first" : "Show hint";
        const body = kind === "hint" ? source.currentExercise.hint :
          (kind === "introduction" ? source.introduction : source.currentExercise.introduction)[0]!.body;
        await page.addStyleTag({ content: "html { font-size: 200% !important; }" });
  
        let release!: () => void;
        let committed!: () => void;
        const hold = new Promise<void>((resolve) => { release = resolve; });
        const saved = new Promise<void>((resolve) => { committed = resolve; });
        const bodies: string[] = [];
        await page.route("**/api/v1/practice-sessions/*/support", async (route) => {
          bodies.push(route.request().postData()!);
          if (bodies.length > 1) { await route.continue(); return; }
          const response = await route.fetch();
          expect(response.ok()).toBeTruthy();
          committed();
          await hold;
          if (retry) await route.abort("connectionfailed");
          else await route.fulfill({ response });
        });
        await page.getByRole("button", { name: controlName, exact: true }).click();
        await saved;
        try {
          const persisted = await readPractice(page.request, id);
          if (kind === "introduction") expect((persisted as unknown as TeachingState).introductionOpened).toBe(true);
          else expect(persisted.items[0]![`${kind}Opened`]).toBe(true);
          await expect(page.getByText(body, { exact: true })).toHaveCount(0);
        } finally { release(); }
  
        if (retry) {
          await expect(page.getByRole("alert")).toBeVisible();
          await expect(page.getByText(body, { exact: true })).toHaveCount(0);
          await page.getByRole("button", { name: "Retry saved operation", exact: true }).click();
        }
        const target = page.locator(`[data-practice-focus="${kind}"]`);
        await expect(target).toBeFocused();
        const header = await page.locator("header").boundingBox();
        const targetBox = await target.boundingBox();
        expect(targetBox!.y).toBeGreaterThanOrEqual(header!.height);
        expect(targetBox!.y).toBeLessThan(viewport.height);
  
        const bodyText = page.getByText(body, { exact: true });
        await expect(bodyText).toBeVisible();
        if (kind !== "hint") {
          const region = page.getByRole("region", { name: controlName, exact: true });
          await expect(region).toHaveCSS("overflow-y", "visible");
          await expect(page.getByRole("button", { name: controlName, exact: true })).toHaveAttribute("aria-expanded", "true");
        }
        const bounds = await bodyText.boundingBox();
        expect(Math.min(bounds!.y + bounds!.height, viewport.height) - Math.max(bounds!.y, header!.height),
          "The requested teaching body must intersect the usable viewport below the fixed header").toBeGreaterThan(0);
        expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy();
        if (retry) expect(bodies).toEqual([bodies[0], bodies[0]]);
        else expect(bodies).toHaveLength(1);
      });
    }
  }
}
