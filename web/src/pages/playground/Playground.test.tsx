import { act, fireEvent, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { session, catalog, exercise, attempt, json, renderPlayground } from "../../test/playgroundFixtures";

async function verifyKnownSubmissionErrorFocus() {
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("development-session")) return json(session);
    if (path.endsWith("catalog")) return json(catalog);
    if (path.endsWith("exercises")) return json(exercise);
    return Response.json(
      { detail: "The engine revision changed." },
      { status: 409, headers: { "Content-Type": "application/problem+json" } },
    );
  }));
  const user = userEvent.setup();
  await renderPlayground();
  await user.click(await screen.findByRole("button", { name: "læser" }));
  await user.click(screen.getByRole("button", { name: "Check answer" }));

  const alert = await screen.findByRole("alert");
  const reload = screen.getByRole("button", { name: "Reload engine" });
  await waitFor(() => expect(alert).toHaveFocus());
  await user.tab();
  expect(reload).toHaveFocus();
}

async function verifyRepairSupportTracking() {
  const repairExercise = {
    ...exercise,
    id: "f17d982e-7e7f-460f-b922-76e08ccce812",
    formatId: "E005",
    instruction: "Repair the verb form with one word.",
    context: "The highlighted verb is not in the present form.",
    prompt: {
      before: "Jeg", after: "en bog.", choices: [], blocks: [], punctuation: "",
      inputLabel: "Replacement verb", replacementWord: "læse",
    },
  };
  const attemptBodies: Array<Record<string, unknown>> = [];
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("development-session")) return json(session);
    if (path.endsWith("catalog")) return json(catalog);
    if (path.endsWith("exercises")) return json(repairExercise);
    const submitted = JSON.parse(String(init?.body)) as Record<string, unknown>;
    attemptBodies.push(submitted);
    return json({
      ...attempt,
      attemptId: submitted.attemptId,
      exerciseId: repairExercise.id,
      hintUsed: submitted.hintUsed,
    });
  }));
  const user = userEvent.setup();
  await renderPlayground();
  const input = await screen.findByLabelText("Replacement verb");
  await user.type(input, "l");
  await user.click(screen.getByRole("button", { name: "Insert æ" }));
  await user.type(input, "ser");
  await user.click(screen.getByRole("button", { name: "Check answer" }));
  await screen.findByRole("status");

  expect(attemptBodies[0]).toMatchObject({
    exerciseId: repairExercise.id,
    answer: { kind: "text", text: "læser" },
    hintUsed: false,
  });

  await user.click(screen.getByRole("button", { name: "Try again" }));
  await user.click(screen.getByRole("button", { name: "Learn this first" }));
  await user.type(screen.getByLabelText("Replacement verb"), "læser");
  await user.click(screen.getByRole("button", { name: "Check answer" }));
  await waitFor(() => expect(attemptBodies).toHaveLength(2));
  expect(attemptBodies[1]).toMatchObject({ hintUsed: true });
}

async function verifyRepairUncertainRecovery() {
  const repairExercise = {
    ...exercise,
    id: "230f23b7-d1a4-45ac-a262-f5ff0130df78",
    formatId: "E006",
    targetSkillId: "G006",
    instruction: "Repair the article with one word.",
    prompt: {
      before: "Jeg har", after: "hund.", choices: [], blocks: [], punctuation: "",
      inputLabel: "Replacement article", replacementWord: "et",
    },
    introduction: [],
  };
  const attemptBodies: string[] = [];
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("development-session")) return json(session);
    if (path.endsWith("catalog")) return json(catalog);
    if (path.endsWith("exercises")) return json(repairExercise);
    attemptBodies.push(String(init?.body));
    if (attemptBodies.length === 1) throw new TypeError("response connection closed");
    const submitted = JSON.parse(attemptBodies[1]!);
    return json({ ...attempt, attemptId: submitted.attemptId, exerciseId: repairExercise.id });
  }));
  const user = userEvent.setup();
  await renderPlayground();
  const input = await screen.findByLabelText("Replacement article");
  await user.type(input, "en");
  await user.click(screen.getByRole("button", { name: "Check answer" }));
  await screen.findByText("The result is not confirmed");

  expect(input).toBeDisabled();
  expect(input).toHaveValue("en");
  for (const character of ["æ", "ø", "å"]) {
    expect(screen.getByRole("button", { name: `Insert ${character}` })).toBeDisabled();
  }
  await user.click(screen.getByRole("button", { name: "Check the same attempt" }));
  await screen.findByRole("status");
  expect(attemptBodies[1]).toBe(attemptBodies[0]);
  expect(JSON.parse(attemptBodies[1]!)).toMatchObject({
    answer: { kind: "text", text: "en" }, hintUsed: false,
  });
}

async function verifyWritingUncertainRecovery() {
  const writingCatalog = {
    ...catalog,
    contentRevision: "pilot-4", generatorRevision: "generator-4", evaluationRevision: "evaluation-4",
    formats: [{ id: "E009", title: "Write a Danish sentence", targetSkillId: "G005" }],
  };
  const writingExercise = {
    ...exercise,
    id: "35f6c122-3177-441e-bcf9-f0c82607b271", formatId: "E009", targetSkillId: "G005",
    contentRevision: "pilot-4", generatorRevision: "generator-4",
    instruction: "Write one complete Danish statement.", context: "Express the English meaning.",
    prompt: {
      before: "", after: "", choices: [], blocks: [], punctuation: "", cue: "She reads a newspaper.",
      cueLanguage: "en", answerLanguage: "da", inputLabel: "Danish sentence",
    },
    introduction: [], hint: "Begin with the subject.",
  };
  const attemptBodies: string[] = [];
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("development-session")) return json({
      ...session, contentRevision: "pilot-4", generatorRevision: "generator-4", evaluationRevision: "evaluation-4",
    });
    if (path.endsWith("catalog")) return json(writingCatalog);
    if (path.endsWith("exercises")) return json(writingExercise);
    attemptBodies.push(String(init?.body));
    if (attemptBodies.length === 1) throw new TypeError("response connection closed");
    const submitted = JSON.parse(attemptBodies[1]!);
    return json({
      ...attempt, attemptId: submitted.attemptId, exerciseId: writingExercise.id,
      linguisticScope: "danish-sentence", responseConstraintStatus: "met", meaningSatisfaction: "met",
      completedSentence: "Hun læser en avis.",
    });
  }));
  await renderPlayground();
  const input = await screen.findByLabelText("Danish sentence");
  await userEvent.type(input, "Hun læser en avis.");
  await userEvent.click(screen.getByRole("button", { name: "Check answer" }));

  expect(await screen.findByText("The result is not confirmed")).toBeVisible();
  expect(input).toBeDisabled();
  expect(input).toHaveValue("Hun læser en avis.");
  for (const character of ["æ", "ø", "å"]) {
    expect(screen.getByRole("button", { name: `Insert ${character}` })).toBeDisabled();
  }
  await userEvent.click(screen.getByRole("button", { name: "Check the same attempt" }));
  expect(await screen.findByText("Meaning matched")).toBeVisible();
  expect(attemptBodies[1]).toBe(attemptBodies[0]);
  expect(JSON.parse(attemptBodies[1]!)).toMatchObject({
    exerciseId: writingExercise.id,
    answer: { kind: "text", text: "Hun læser en avis." },
    hintUsed: false,
  });
}

describe("Playground", () => {
  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  it("offers a reconnect action when the initial engine session is unavailable", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => { throw new TypeError("offline"); }));
    await renderPlayground();

    expect(await screen.findByText("The learning engine could not be reached. Check that the server is running.")).toBeVisible();
    await userEvent.click(screen.getByRole("button", { name: "Try connection again" }));
  });

  it("establishes the session before protected reads and offers teaching before the prompt", async () => {
    const paths: string[] = [];
    vi.stubGlobal("fetch", vi.fn(async (path: string) => {
      paths.push(path);
      if (path.endsWith("development-session")) return json(session);
      if (path.endsWith("catalog")) return json(catalog);
      return json(exercise);
    }));

    await renderPlayground();

    expect(await screen.findByText("Learn this first")).toBeInTheDocument();
    expect(screen.queryByRole("region", { name: "Learn this first" })).not.toBeInTheDocument();
    expect(screen.getByText(/Using it records supported practice/)).toBeVisible();
    await userEvent.click(screen.getByRole("button", { name: "Learn this first" }));
    expect(screen.getByText("læse · to read")).toBeVisible();
    expect(screen.getByRole("heading", { name: "Choose the present-tense verb." })).toBeVisible();
    expect(paths).toEqual([
      "/api/v1/development-session", "/api/v1/catalog", "/api/v1/exercises",
    ]);
    expect(screen.getByText("99 sentence combinations")).toBeVisible();
  });

  it("submits repair text unsupported, then keeps lesson support sticky for a retry", verifyRepairSupportTracking);

  it("freezes repair input and its character controls while recovering an uncertain attempt", verifyRepairUncertainRecovery);

  it("freezes full-sentence writing and replays the exact pending attempt", verifyWritingUncertainRecovery);

  it("freezes the answer and replays the exact payload after an uncertain submission", async () => {
    const attemptBodies: string[] = [];
    vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
      if (path.endsWith("development-session")) return json(session);
      if (path.endsWith("catalog")) return json(catalog);
      if (path.endsWith("exercises")) return json(exercise);
      attemptBodies.push(String(init?.body));
      if (attemptBodies.length === 1) throw new TypeError("response connection closed");
      const submitted = JSON.parse(attemptBodies[1]!);
      return json({ ...attempt, attemptId: submitted.attemptId, hintUsed: submitted.hintUsed });
    }));
    await renderPlayground();
    await userEvent.click(await screen.findByRole("button", { name: "Learn this first" }));
    const choice = await screen.findByRole("button", { name: "læser" });
    await userEvent.click(choice);
    await userEvent.click(screen.getByRole("button", { name: "Check answer" }));

    expect(await screen.findByText("The result is not confirmed")).toBeVisible();
    const uncertainAlert = screen.getByRole("alert");
    await waitFor(() => expect(uncertainAlert).toHaveFocus());
    expect(choice).toBeDisabled();
    const replay = screen.getByRole("button", { name: "Check the same attempt" });
    await userEvent.tab();
    expect(replay).toHaveFocus();
    await userEvent.click(replay);

    const feedback = await screen.findByRole("status");
    await waitFor(() => expect(feedback).toHaveFocus());
    expect(attemptBodies).toHaveLength(2);
    expect(attemptBodies[1]).toBe(attemptBodies[0]);
    expect(JSON.parse(attemptBodies[1]!)).toMatchObject({ hintUsed: true });
    expect(screen.getByText(/Supported practice/)).toBeVisible();
  });

  it("keeps the timed-out attempt frozen and replays its exact supported payload", async () => {
    const attemptBodies: string[] = [];
    vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
      if (path.endsWith("development-session")) return json(session);
      if (path.endsWith("catalog")) return json(catalog);
      if (path.endsWith("exercises")) return json(exercise);
      attemptBodies.push(String(init?.body));
      if (attemptBodies.length === 1) {
        return new Promise<Response>((_resolve, reject) => {
          init?.signal?.addEventListener("abort", () => {
            reject(new DOMException("The operation was aborted.", "AbortError"));
          }, { once: true });
        });
      }
      const submitted = JSON.parse(attemptBodies[1]!);
      return json({ ...attempt, attemptId: submitted.attemptId, hintUsed: submitted.hintUsed });
    }));
    await renderPlayground();
    const choice = await screen.findByRole("button", { name: "læser" });
    await userEvent.click(screen.getByRole("button", { name: "Show hint" }));
    expect(screen.getByText(exercise.hint)).toBeVisible();
    await userEvent.click(choice);
    await userEvent.click(screen.getByRole("button", { name: "Generation controls" }));

    vi.useFakeTimers();
    fireEvent.click(screen.getByRole("button", { name: "Check answer" }));
    expect(choice).toBeDisabled();
    await act(async () => vi.advanceTimersByTimeAsync(15_000));
    vi.useRealTimers();

    expect(await screen.findByText("The result is not confirmed")).toBeVisible();
    const replay = screen.getByRole("button", { name: "Check the same attempt" });
    expect(replay).toBeEnabled();
    expect(choice).toBeDisabled();
    expect(screen.getByRole("combobox", { name: "Exercise type" })).toBeDisabled();
    expect(screen.getByText(exercise.hint)).toBeVisible();
    expect(JSON.parse(attemptBodies[0]!)).toMatchObject({
      exerciseId: exercise.id,
      answer: { kind: "text", text: "læser" },
      hintUsed: true,
    });

    await userEvent.click(replay);
    expect(await screen.findByRole("status")).toBeVisible();
    expect(attemptBodies).toHaveLength(2);
    expect(attemptBodies[1]).toBe(attemptBodies[0]);
    expect(screen.getByText("Try 1 · Supported practice", { exact: true })).toBeVisible();
  });

  it("keeps the displayed exercise and selector consistent when generation fails", async () => {
    let exerciseRequests = 0;
    vi.stubGlobal("fetch", vi.fn(async (path: string) => {
      if (path.endsWith("development-session")) return json(session);
      if (path.endsWith("catalog")) return json(catalog);
      exerciseRequests += 1;
      if (exerciseRequests === 1) return json(exercise);
      return Response.json(
        { detail: "The requested exercise could not be generated." },
        { status: 409, headers: { "Content-Type": "application/problem+json" } },
      );
    }));
    await renderPlayground();
    await screen.findByRole("button", { name: "læser" });
    await userEvent.click(screen.getByRole("button", { name: "Generation controls" }));
    const selector = screen.getByRole("combobox", { name: "Exercise type" });
    await userEvent.click(selector);
    await userEvent.click(screen.getByRole("option", { name: "Supply the indefinite article" }));

    expect(await screen.findByText("The engine revision changed. Generate a new exercise.")).toBeVisible();
    const alert = screen.getByRole("alert");
    const reload = screen.getByRole("button", { name: "Reload engine" });
    expect(reload).toBeVisible();
    expect(screen.getByRole("button", { name: "læser" })).toBeVisible();
    const liveSelector = screen.getByRole("combobox", { name: "Exercise type" });
    expect(liveSelector).toBe(selector);
    expect(liveSelector).toHaveValue("Choose the present form");
    await waitFor(() => expect(alert).toHaveFocus());
    await userEvent.tab();
    expect(reload).toHaveFocus();
  });

  it("offers session reconnection when a protected call reports an expired session", async () => {
    let exerciseRequests = 0;
    vi.stubGlobal("fetch", vi.fn(async (path: string) => {
      if (path.endsWith("development-session")) return json(session);
      if (path.endsWith("catalog")) return json(catalog);
      exerciseRequests += 1;
      if (exerciseRequests === 1) return json(exercise);
      return Response.json(
        { detail: "The session is no longer valid." },
        { status: 401, headers: { "Content-Type": "application/problem+json" } },
      );
    }));
    await renderPlayground();
    await screen.findByRole("button", { name: "læser" });
    await userEvent.click(screen.getByRole("button", { name: "Generation controls" }));
    await userEvent.click(screen.getByRole("combobox", { name: "Exercise type" }));
    await userEvent.click(screen.getByRole("option", { name: "Supply the indefinite article" }));

    expect(await screen.findByText("Your learning session expired. Reconnect to continue.")).toBeVisible();
    const reconnect = screen.getByRole("button", { name: "Reconnect session" });
    expect(reconnect).toBeVisible();
    expect(screen.getByRole("button", { name: "læser" })).toBeVisible();
    await userEvent.click(reconnect);
  });

  it("focuses a known submission error before its recovery action", verifyKnownSubmissionErrorFocus);

  it("retries the same next index until generation succeeds", async () => {
    const generatedIndexes: number[] = [];
    vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
      if (path.endsWith("development-session")) return json(session);
      if (path.endsWith("catalog")) return json(catalog);
      if (path.endsWith("attempts")) return json(attempt);
      const body = JSON.parse(String(init?.body));
      generatedIndexes.push(body.index);
      if (generatedIndexes.length === 1) return json(exercise);
      if (generatedIndexes.length === 2) {
        return Response.json(
          { detail: "The requested exercise could not be generated." },
          { status: 409, headers: { "Content-Type": "application/problem+json" } },
        );
      }
      return json({ ...exercise, id: "f640dc3e-b2fb-4d6e-920f-6a3b8427beaf" });
    }));
    await renderPlayground();
    await userEvent.click(await screen.findByRole("button", { name: "læser" }));
    await userEvent.click(screen.getByRole("button", { name: "Check answer" }));
    await screen.findByRole("status");

    await userEvent.click(screen.getByRole("button", { name: "Next exercise" }));
    await screen.findByText("The engine revision changed. Generate a new exercise.");
    await userEvent.click(screen.getByRole("button", { name: "Next exercise" }));

    await waitFor(() => expect(generatedIndexes).toEqual([0, 1, 1]));
    expect(screen.getByRole("button", { name: "Check answer" })).toBeVisible();
  });

  it("collapses lesson support and records a fresh unsupported attempt after Next", async () => {
    const attemptSupport: boolean[] = [];
    let exerciseRequest = 0;
    vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
      if (path.endsWith("development-session")) return json(session);
      if (path.endsWith("catalog")) return json(catalog);
      if (path.endsWith("attempts")) {
        const submitted = JSON.parse(String(init?.body));
        attemptSupport.push(submitted.hintUsed);
        return json({ ...attempt, attemptId: submitted.attemptId, exerciseId: submitted.exerciseId, hintUsed: submitted.hintUsed });
      }
      exerciseRequest += 1;
      return json(exerciseRequest === 1 ? exercise : { ...exercise, id: "a812da31-dfb7-4f87-90c3-878306f7f84a" });
    }));
    await renderPlayground();
    await userEvent.click(await screen.findByRole("button", { name: "Learn this first" }));
    await userEvent.click(screen.getByRole("button", { name: "læser" }));
    await userEvent.click(screen.getByRole("button", { name: "Check answer" }));
    await screen.findByRole("status");
    await userEvent.click(screen.getByRole("button", { name: "Next exercise" }));

    const lesson = await screen.findByRole("button", { name: "Learn this first" });
    expect(lesson).toHaveAttribute("aria-expanded", "false");
    expect(screen.queryByRole("region", { name: "Learn this first" })).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "læser" }));
    await userEvent.click(screen.getByRole("button", { name: "Check answer" }));
    await waitFor(() => expect(attemptSupport).toEqual([true, false]));
  });

  it("allows only one in-flight Next generation and disables retry actions", async () => {
    let finishNext: ((response: Response) => void) | undefined;
    const nextResponse = new Promise<Response>((resolve) => { finishNext = resolve; });
    let exerciseRequests = 0;
    vi.stubGlobal("fetch", vi.fn(async (path: string) => {
      if (path.endsWith("development-session")) return json(session);
      if (path.endsWith("catalog")) return json(catalog);
      if (path.endsWith("attempts")) return json(attempt);
      exerciseRequests += 1;
      return exerciseRequests === 1 ? json(exercise) : nextResponse;
    }));
    await renderPlayground();
    await userEvent.click(await screen.findByRole("button", { name: "læser" }));
    await userEvent.click(screen.getByRole("button", { name: "Check answer" }));
    await screen.findByRole("status");
    const next = screen.getByRole("button", { name: "Next exercise" });
    await userEvent.dblClick(next);

    expect(exerciseRequests).toBe(2);
    expect(next).toBeDisabled();
    expect(screen.getByRole("button", { name: "Try again" })).toBeDisabled();
    finishNext?.(json({ ...exercise, id: "7a58bf73-af2f-453c-9277-ab983d34f472" }));
    await screen.findByRole("button", { name: "Check answer" });
  });
});
