import AxeBuilder from "@axe-core/playwright";
import { expect, test, type Page } from "@playwright/test";
import { choosePractice, readPractice, wrongAnswer, type PracticeExercise } from "../support/practice.js";

const WCAG_TAGS = ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"];

async function scan(page: Page) {
  const result = await new AxeBuilder({ page }).withTags(WCAG_TAGS).analyze();
  expect(result.violations.map(({ id, nodes }) => ({
    id,
    targets: nodes.map((node) => node.target),
  }))).toEqual([]);
  const reflow = await page.evaluate(() => {
    const root = document.documentElement;
    const overflowing = [...document.body.querySelectorAll<HTMLElement>("*")].flatMap((element) => {
      const rect = element.getBoundingClientRect();
      const leftOverflow = Math.max(0, -rect.left);
      const rightOverflow = Math.max(0, rect.right - innerWidth);
      if (!rect.width || !rect.height || (leftOverflow <= 1 && rightOverflow <= 1)) return [];
      const style = getComputedStyle(element);
      return [{
        tag: element.tagName.toLowerCase(),
        text: (element.textContent ?? "").replace(/\s+/g, " ").trim().slice(0, 100),
        className: element.getAttribute("class") ?? "",
        direction: rightOverflow > 1 ? "right" : "left",
        overflow: Math.max(leftOverflow, rightOverflow),
        rect: { left: rect.left, right: rect.right, width: rect.width },
        box: { clientWidth: element.clientWidth, scrollWidth: element.scrollWidth },
        style: {
          display: style.display,
          overflowX: style.overflowX,
          position: style.position,
          whiteSpace: style.whiteSpace,
        },
      }];
    }).sort((left, right) =>
      (left.direction === "right" ? 0 : 1) - (right.direction === "right" ? 0 : 1) ||
      right.overflow - left.overflow).slice(0, 10);
    return {
      scrollWidth: root.scrollWidth,
      clientWidth: root.clientWidth,
      bodyScrollWidth: document.body.scrollWidth,
      bodyClientWidth: document.body.clientWidth,
      innerWidth,
      overflowing,
    };
  });
  expect(reflow.scrollWidth, `Horizontal overflow: ${JSON.stringify(reflow)}`).toBeLessThanOrEqual(reflow.innerWidth);
}

async function chooseWrong(page: Page, exercise: PracticeExercise) {
  const answer = wrongAnswer(exercise);
  if (answer.kind === "order") {
    for (const id of answer.blockIds!) {
      const block = exercise.prompt.blocks.find((candidate) => candidate.id === id)!;
      await page.getByLabel("Available sentence parts").getByRole("button", { name: block.text, exact: true }).click();
    }
  } else if (exercise.prompt.choices.length) {
    await page.getByRole("group", { name: "Answer choices", exact: true })
      .getByRole("button", { name: answer.text, exact: true }).click();
  } else await page.getByRole("textbox").fill(answer.text!);
}

test("home and review navigation stay passive and expose the bounded learning catalogue", async ({ page }) => {
  const apiRequests: string[] = [];
  page.on("request", (request) => {
    if (new URL(request.url()).pathname.startsWith("/api/")) apiRequests.push(request.url());
  });

  await page.goto("/");
  await expect(page.getByRole("heading", { name: "Module 1: First steps", exact: true })).toBeVisible();
  const lessons = page.getByRole("region", { name: "Lessons", exact: true });
  const review = page.getByRole("region", { name: "Review", exact: true });
  await expect(lessons.getByRole("button", { name: /^Start / })).toHaveCount(15);
  await expect(review.getByRole("button", { name: /^Start / })).toHaveCount(3);
  await expect(review).toContainText("familiar words, sentence order, verb forms and articles");

  const navigation = page.getByRole("navigation");
  await expect(navigation.getByRole("link", { name: "Lessons", exact: true })).toHaveAttribute("data-active", "true");
  await expect(navigation.getByText("Developer tools", { exact: true })).toBeVisible();
  await expect(navigation.getByRole("link", { name: "Engine playground", exact: true })).toHaveAttribute("href", "/playground");
  await navigation.getByRole("link", { name: "Review", exact: true }).click();
  await expect(page).toHaveURL(/\/#review$/);
  await expect(page.getByRole("heading", { name: "Review", exact: true })).toBeFocused();
  await expect(navigation.getByRole("link", { name: "Review", exact: true })).toHaveAttribute("data-active", "true");
  expect(apiRequests).toEqual([]);
});

for (const entry of [
  {
    kind: "lesson",
    button: "Start Talking about one person",
    focusId: "module-one-singular",
    planRevision: "module-one-singular-1",
    ready: "Session introduction",
  },
  {
    kind: "review",
    button: "Start Mixed practice",
    focusId: "reading-basics",
    planRevision: "practice-1",
    ready: "Check answer",
  },
] as const) {
  test(`home starts a ${entry.kind} through the saved creation handoff`, async ({ page }) => {
    let requestBody = "";
    let savedBody = "";
    await page.route("**/api/v1/practice-sessions", async (route) => {
      requestBody = route.request().postData()!;
      const practiceSessionId = (JSON.parse(requestBody) as { practiceSessionId: string }).practiceSessionId;
      savedBody = await page.evaluate((id) => {
        for (let index = 0; index < localStorage.length; index += 1) {
          const key = localStorage.key(index);
          if (!key?.startsWith(`astrid.practice.v1.outbox.${id}.creation.`)) continue;
          return (JSON.parse(localStorage.getItem(key)!) as { body: string }).body;
        }
        return "";
      }, practiceSessionId);
      await route.continue();
    });

    await page.goto(entry.kind === "review" ? "/#review" : "/");
    await page.getByRole("button", { name: entry.button, exact: true }).click();
    await expect(page).toHaveURL(/\/practice\?session=[0-9a-f-]+$/);
    await expect(page.getByRole("button", { name: entry.ready, exact: true })).toBeVisible();
    expect(savedBody).toBe(requestBody);
    const body = JSON.parse(requestBody) as {
      practiceSessionId: string;
      focusId: string;
      planRevision: string;
    };
    expect(body).toMatchObject({ focusId: entry.focusId, planRevision: entry.planRevision });
    expect(new URL(page.url()).searchParams.get("session")).toBe(body.practiceSessionId);
    expect(await readPractice(page.request, body.practiceSessionId)).toMatchObject({
      id: body.practiceSessionId,
      focusId: entry.focusId,
      planRevision: entry.planRevision,
    });
  });
}

test("home resume preserves an outstanding attempt identity and replays its exact body", async ({ page }) => {
  await page.goto("/practice");
  await choosePractice(page, "Mixed practice");
  await page.waitForURL(/\/practice\?session=[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i);
  await expect(page.getByRole("button", { name: "Check answer", exact: true })).toBeVisible();
  const practiceSessionId = new URL(page.url()).searchParams.get("session")!;
  const initial = await readPractice(page.request, practiceSessionId);
  const bodies: string[] = [];
  await page.route("**/api/v1/attempts", async (route) => {
    bodies.push(route.request().postData()!);
    if (bodies.length === 1) {
      await route.fetch();
      await route.abort("connectionfailed");
    } else await route.continue();
  });

  await chooseWrong(page, initial.currentExercise!);
  await page.getByRole("button", { name: "Check answer", exact: true }).click();
  await expect(page.getByRole("alert")).toBeVisible();
  await page.getByRole("navigation").getByRole("link", { name: "Lessons", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Module 1: First steps", exact: true })).toBeVisible();
  await expect(page.getByText("A practice run is saved", { exact: true })).toBeVisible();
  await page.getByRole("link", { name: "Resume saved practice", exact: true }).click();

  await expect(page).toHaveURL(`/practice?session=${practiceSessionId}`);
  await expect(page.getByRole("status")).toBeVisible();
  expect(bodies).toHaveLength(2);
  expect(bodies[1]).toBe(bodies[0]);
  expect((JSON.parse(bodies[1]!) as { attemptId: string }).attemptId)
    .toBe((JSON.parse(bodies[0]!) as { attemptId: string }).attemptId);
  expect((await readPractice(page.request, practiceSessionId)).items[0]!.attemptCount).toBe(1);
});

test("an unknown route is safe and returns to Lessons without starting learning", async ({ page }) => {
  const apiRequests: string[] = [];
  page.on("request", (request) => {
    if (new URL(request.url()).pathname.startsWith("/api/")) apiRequests.push(request.url());
  });
  await page.goto("/not-a-learning-route");
  await expect(page.getByRole("heading", { name: "Page not found", exact: true })).toBeVisible();
  await expect(page.getByRole("navigation").getByRole("link", { name: "Engine playground", exact: true })).toBeVisible();
  await page.getByRole("link", { name: "Go to Lessons", exact: true }).click();
  await expect(page).toHaveURL("/");
  await expect(page.getByRole("heading", { name: "Module 1: First steps", exact: true })).toBeVisible();
  expect(apiRequests).toEqual([]);
});

for (const theme of ["light", "dark"] as const) {
  test(`learner navigation reflows at 320px and 200% text in ${theme} mode`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: theme });
    await page.setViewportSize({ width: 320, height: 720 });
    await page.addInitScript(() => {
      const enlargeText = () => {
        const style = document.createElement("style");
        style.textContent = "html { font-size: 200% !important; }";
        document.documentElement.append(style);
      };
      if (document.documentElement) enlargeText();
      else document.addEventListener("DOMContentLoaded", enlargeText, { once: true });
    });
    await page.goto("/");
    await expect(page.locator("html")).toHaveAttribute("data-mantine-color-scheme", theme);
    expect(await page.evaluate(() => getComputedStyle(document.documentElement).fontSize)).toBe("32px");
    await scan(page);

    const burger = page.getByRole("button", { name: "Toggle navigation", exact: true });
    await expect(burger).toHaveAttribute("aria-expanded", "false");
    await burger.click();
    await expect(burger).toHaveAttribute("aria-expanded", "true");
    const navigation = page.getByRole("navigation");
    for (const name of ["Lessons", "Review", "Engine playground", "Changelog"]) {
      await expect(navigation.getByRole("link", { name, exact: true })).toBeVisible();
    }
    await expect(navigation.getByText("Developer tools", { exact: true })).toBeVisible();
    await expect(page.getByTitle("What's new", { exact: true })).toBeVisible();
    await scan(page);
  });
}
