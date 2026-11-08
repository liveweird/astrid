import { expect, test } from "@playwright/test";
import { createPractice, creationBody, ownerHeaders, practiceOwner, readPractice, saveWrongAttempt, wrongAnswer, type PracticeState } from "../support/practice.js";

test("practice completes after eight unsuccessful attempts without skipping or implying mastery", async ({ request }) => {
  const { owner, body, state: initial } = await createPractice(request);
  let state = initial;
  expect(state.itemCount).toBe(8);
  expect(state.items).toHaveLength(8);
  const forbidden = await request.post(`/api/v1/practice-sessions/${state.id}/advance`, { headers: ownerHeaders(owner), data: { position: 0 } });
  expect(forbidden.status()).toBe(409);
  for (let position = 0; position < 8; position += 1) {
    expect(state.cursor).toBe(position);
    expect(state.currentExercise?.id).toBe(state.items[position]!.exerciseId);
    for (const item of state.items) expect(item).not.toHaveProperty("prompt");
    const attempt = await saveWrongAttempt(request, state.currentExercise!);
    const replay = await request.post("/api/v1/attempts", { headers: ownerHeaders(owner), data: attempt });
    expect(replay.status()).toBe(200);
    const advance = () => request.post(`/api/v1/practice-sessions/${state.id}/advance`, { headers: ownerHeaders(owner), data: { position } });
    expect((await advance()).status()).toBe(200);
    state = await (await advance()).json() as PracticeState;
    expect(state.cursor).toBe(position + 1);
    const reload = await request.post("/api/v1/practice-sessions", { headers: ownerHeaders(owner), data: body });
    expect(reload.status()).toBe(200);
    expect((await reload.json() as PracticeState).cursor).toBe(position + 1);
  }
  expect(state.state).toBe("completed");
  expect(state.currentExercise ?? null).toBeNull();
  expect(state.summary).toMatchObject({ attemptedItems: 8, completedItems: 8, attemptCount: 8, retryCount: 0 });
  expect(state.summary.rows).toHaveLength(8);
});

test("practice freezes first-attempt support and exposure while retaining optional retries", async ({ request }) => {
  const { owner, state } = await createPractice(request);
  await saveWrongAttempt(request, state.currentExercise!);
  const first = await readPractice(request, state.id);
  expect(first.items[0]!.firstAttemptContext).toMatchObject({ introductionOpened: false, lessonOpened: false, hintOpened: false, priorExposure: [] });
  for (const data of [{ kind: "introduction" }, { kind: "lesson", position: 0 }, { kind: "hint", position: 0 }]) {
    const response = await request.post(`/api/v1/practice-sessions/${state.id}/support`, { headers: ownerHeaders(owner), data });
    expect(response.status()).toBe(200);
  }
  await saveWrongAttempt(request, state.currentExercise!);
  const retried = await readPractice(request, state.id);
  expect(retried.items[0]!.attemptCount).toBe(2);
  expect(retried.items[0]!.firstAttemptContext).toEqual(first.items[0]!.firstAttemptContext);
  expect(retried.items[0]!.lastAttemptContext).toMatchObject({ introductionOpened: true, lessonOpened: true, hintOpened: true });
  expect(retried.items[0]!.lastAttemptContext!.priorExposure).toContainEqual(expect.objectContaining({ position: 0, source: "feedback" }));
  const premature = await request.post(`/api/v1/practice-sessions/${state.id}/support`, { headers: ownerHeaders(owner), data: { kind: "hint", position: 7 } });
  expect(premature.status()).toBe(409);
});

test("practice serializes concurrent retries and duplicate advancement and rejects stale new answers", async ({ request }) => {
  const { owner, state } = await createPractice(request);
  const exercise = state.currentExercise!;
  const bodies = [0, 1].map(() => ({ attemptId: crypto.randomUUID(), exerciseId: exercise.id, answer: wrongAnswer(exercise), hintUsed: false }));
  const results = await Promise.all(bodies.map((data) => request.post("/api/v1/attempts", { headers: ownerHeaders(owner), data })));
  for (const response of results) expect(response.status()).toBe(200);
  const recorded = await readPractice(request, state.id);
  expect(recorded.items[0]!.attemptCount).toBe(2);
  const advances = await Promise.all([0, 1, 2].map(() => request.post(`/api/v1/practice-sessions/${state.id}/advance`, { headers: ownerHeaders(owner), data: { position: 0 } })));
  for (const response of advances) expect(response.status()).toBe(200);
  expect((await readPractice(request, state.id)).cursor).toBe(1);
  const stale = await request.post("/api/v1/attempts", { headers: ownerHeaders(owner), data: { ...bodies[0], attemptId: crypto.randomUUID() } });
  expect(stale.status()).toBe(409);
  const replay = await request.post("/api/v1/attempts", { headers: ownerHeaders(owner), data: bodies[0] });
  expect(replay.status()).toBe(200);
});

test("practice continuity header prevents a pending creation being reassociated after cookie renewal", async ({ context }) => {
  const request = context.request;
  const oldOwner = await practiceOwner(request);
  const body = creationBody();
  await context.clearCookies();
  const newOwner = await practiceOwner(request);
  expect(newOwner).not.toBe(oldOwner);
  const rotated = await request.post("/api/v1/practice-sessions", { headers: ownerHeaders(oldOwner), data: body });
  expect(rotated.status()).toBe(409);
  const missing = await request.get(`/api/v1/practice-sessions/${body.practiceSessionId}`);
  expect(missing.status()).toBe(404);
});
