import { expect, test, type Page, type Route } from "@playwright/test";
import { readPractice, type PracticeExercise } from "../support/practice.js";

const FIRST_ATTEMPT_ID = "ffffffff-ffff-4fff-8fff-ffffffffffff";
const SECOND_ATTEMPT_ID = "00000000-0000-4000-8000-000000000000";
const meanings: Record<string, string> = { bog: "book", avis: "newspaper", brev: "letter (written message)" };

async function openRun(page: Page) {
  await page.goto("/practice");
  await expect(page.getByRole("button", { name: "Check answer", exact: true })).toBeVisible();
  const id = new URL(page.url()).searchParams.get("session");
  expect(id).toBeTruthy();
  return { id: id!, state: await readPractice(page.request, id!) };
}

async function useAttemptId(page: Page, attemptId: string) {
  await page.evaluate((id) => {
    Object.defineProperty(window.crypto, "randomUUID", { configurable: true, value: () => id });
  }, attemptId);
}

async function choose(page: Page, answer: string) {
  await page.getByRole("group", { name: "Answer choices", exact: true })
    .getByRole("button", { name: answer, exact: true }).click();
}

async function expectLocked(page: Page) {
  await expect(page.getByRole("alert")).toContainText("Your answer is not confirmed");
  await expect(page.getByRole("button", { name: "Continue", exact: true })).toHaveCount(0);
  await expect(page.getByRole("group", { name: "Answer choices", exact: true }).getByRole("button").first()).toBeDisabled();
  await expect(page.getByRole("button", { name: "Session introduction", exact: true })).toBeDisabled();
}

type SavedOutbox = { identity: string; body: string; queuedAt: number };

async function attemptOutboxes(page: Page, practiceId: string) {
  return page.evaluate((id) => {
    const prefix = `astrid.practice.v1.outbox.${id}.attempt.`;
    return Object.keys(localStorage)
      .filter((key) => key.startsWith(prefix))
      .map((key) => JSON.parse(localStorage.getItem(key)!) as SavedOutbox)
      .sort((left, right) => left.queuedAt - right.queuedAt);
  }, practiceId);
}

test("practice drains two tabs' immutable lost answers in enqueue order without skipping", async ({ page, context }) => {
  const { id, state } = await openRun(page);
  const exercise = state.currentExercise as PracticeExercise;
  expect(exercise.formatId).toBe("E007");
  const wrongAnswers = exercise.prompt.choices.filter((choice) => choice !== meanings[exercise.prompt.cue!]);
  expect(wrongAnswers).toHaveLength(2);

  const other = await context.newPage();
  await other.goto(page.url());
  await expect(other.getByRole("button", { name: "Check answer", exact: true })).toBeVisible();
  await useAttemptId(page, FIRST_ATTEMPT_ID);
  await useAttemptId(other, SECOND_ATTEMPT_ID);

  const firstWireBodies = { pageA: [] as string[], pageB: [] as string[] };
  const loseBeforeServer = (wireBodies: string[]) => async (route: Route) => {
    const body = route.request().postData()!;
    wireBodies.push(body);
    await route.abort("connectionfailed");
  };
  const loseFromPageA = loseBeforeServer(firstWireBodies.pageA);
  const loseFromPageB = loseBeforeServer(firstWireBodies.pageB);
  await page.route("**/api/v1/attempts", loseFromPageA);
  await other.route("**/api/v1/attempts", loseFromPageB);

  await choose(page, wrongAnswers[0]!);
  await page.getByRole("button", { name: "Check answer", exact: true }).click();
  await expectLocked(page);
  await choose(other, wrongAnswers[1]!);
  await other.getByRole("button", { name: "Check answer", exact: true }).click();
  await expectLocked(other);

  const saved = await attemptOutboxes(page, id);
  const queuedBodies = new Map(saved.map((entry) => [entry.identity, entry.body]));
  const queuedA = JSON.parse(queuedBodies.get(FIRST_ATTEMPT_ID)!) as { attemptId: string; exerciseId: string; answer: unknown };
  const queuedB = JSON.parse(queuedBodies.get(SECOND_ATTEMPT_ID)!) as { attemptId: string; exerciseId: string; answer: unknown };
  expect(saved.map(({ identity }) => identity)).toEqual([FIRST_ATTEMPT_ID, SECOND_ATTEMPT_ID]);
  expect(queuedA).toMatchObject({ attemptId: FIRST_ATTEMPT_ID, exerciseId: exercise.id, answer: { kind: "text", text: wrongAnswers[0] } });
  expect(queuedB).toMatchObject({ attemptId: SECOND_ATTEMPT_ID, exerciseId: exercise.id, answer: { kind: "text", text: wrongAnswers[1] } });
  expect(firstWireBodies.pageA).toEqual([queuedBodies.get(FIRST_ATTEMPT_ID)]);
  expect(firstWireBodies.pageB).toEqual([queuedBodies.get(FIRST_ATTEMPT_ID)]);
  expect(saved[0]!.queuedAt).toBeLessThan(saved[1]!.queuedAt);
  expect((await readPractice(page.request, id)).items[0]!.attemptCount).toBe(0);

  await page.unroute("**/api/v1/attempts", loseFromPageA);
  let releaseSecond!: () => void;
  let secondReached!: () => void;
  const holdSecond = new Promise<void>((resolve) => { releaseSecond = resolve; });
  const secondAtServerGate = new Promise<void>((resolve) => { secondReached = resolve; });
  const retryBodies: string[] = [];
  await page.route("**/api/v1/attempts", async (route) => {
    const body = route.request().postData()!;
    retryBodies.push(body);
    if ((JSON.parse(body) as { attemptId: string }).attemptId === SECOND_ATTEMPT_ID) {
      secondReached();
      await holdSecond;
    }
    await route.continue();
  });

  await page.getByRole("button", { name: "Retry saved operation", exact: true }).click();
  await secondAtServerGate;
  try {
    expect(retryBodies).toEqual([
      queuedBodies.get(FIRST_ATTEMPT_ID),
      queuedBodies.get(SECOND_ATTEMPT_ID),
    ]);
    await expect(page.getByRole("button", { name: "Continue", exact: true })).toHaveCount(0);
    await expect(page.getByRole("button", { name: "Retry saved operation", exact: true }).and(page.locator(":enabled"))).toHaveCount(0);
    expect((await readPractice(page.request, id)).items[0]!.attemptCount).toBe(1);
  } finally {
    releaseSecond();
  }

  await expect(page.getByRole("button", { name: "Continue", exact: true })).toBeEnabled();
  expect(await attemptOutboxes(page, id)).toEqual([]);
  let savedState = await readPractice(page.request, id);
  const firstItem = savedState.items[0]!;
  expect(savedState.cursor).toBe(0);
  expect(firstItem.attemptCount).toBe(2);
  expect(firstItem.attemptIds).toEqual([FIRST_ATTEMPT_ID, SECOND_ATTEMPT_ID]);
  expect(firstItem.firstAnswer).toEqual({ kind: "text", text: wrongAnswers[0] });
  expect(firstItem.lastAnswer).toEqual({ kind: "text", text: wrongAnswers[1] });

  await other.unroute("**/api/v1/attempts", loseFromPageB);
  const laterBodies: string[] = [];
  await other.route("**/api/v1/attempts", async (route) => {
    laterBodies.push(route.request().postData()!);
    await route.continue();
  });
  await other.getByRole("button", { name: "Retry saved operation", exact: true }).click();
  await expect(other.getByRole("button", { name: "Continue", exact: true })).toBeEnabled();
  expect(laterBodies).toEqual([queuedBodies.get(FIRST_ATTEMPT_ID)]);
  expect((await readPractice(page.request, id)).items[0]!.attemptCount).toBe(2);
  await other.reload();
  await expect(other.getByRole("button", { name: "Continue", exact: true })).toBeEnabled();
  expect(laterBodies).toHaveLength(1);
  savedState = await readPractice(page.request, id);
  expect(savedState.cursor).toBe(0);
  expect(savedState.items[0]!.attemptCount).toBe(2);

  await page.getByRole("button", { name: "Continue", exact: true }).click();
  await expect(page.getByText("Part 2 of 8", { exact: true })).toBeVisible();
  await other.getByRole("button", { name: "Continue", exact: true }).click();
  await expect(other.getByText("Part 2 of 8", { exact: true })).toBeVisible();
  savedState = await readPractice(page.request, id);
  expect(savedState.cursor).toBe(1);
  expect(savedState.items[0]!.attemptCount).toBe(2);
  await other.close();
});
