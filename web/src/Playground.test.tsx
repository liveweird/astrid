import { MantineProvider } from "@mantine/core";
import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ComponentType } from "react";

const session = {
  mode: "development", inspectorEnabled: false, contentRevision: "pilot-1",
  generatorRevision: "generator-1", evaluationRevision: "evaluation-1",
};
const catalog = {
  contentRevision: "pilot-1", generatorRevision: "generator-1", evaluationRevision: "evaluation-1",
  sentenceCount: 99,
  skills: [{ id: "G004", title: "Present form", explanation: "Choose a present verb.", prerequisiteIds: [] }],
  formats: [
    { id: "E001", title: "Choose the present form", targetSkillId: "G004" },
    { id: "E002", title: "Supply the indefinite article", targetSkillId: "G006" },
  ],
};
const exercise = {
  id: "011835c0-bb42-47ef-b21e-ae21411b24a1",
  formatId: "E001", targetSkillId: "G004", supportSkillIds: [],
  contentRevision: "pilot-1", generatorRevision: "generator-1",
  instruction: "Choose the present-tense verb.", context: "You are describing what happens now.",
  prompt: { before: "Jeg", after: "en bog", choices: ["læse", "læser"], blocks: [], punctuation: "." },
  introduction: [{ title: "læse · to read", body: "Present form: læser" }],
  hint: "The present form usually ends in -r.",
};
const attempt = {
  attemptId: "28d52d72-7a04-4a42-8e87-bf780f821510",
  exerciseId: exercise.id,
  targetSatisfaction: "met", linguisticStatus: "acceptable", issues: [],
  feedback: "You selected the present form.", completedSentence: "Jeg læser en bog.",
  evaluationRevision: "evaluation-1", hintUsed: false, retryNumber: 1, createdAt: 1,
};

function json(body: unknown) {
  return Response.json(body, { headers: { "Content-Type": "application/json" } });
}

async function renderPlayground() {
  vi.resetModules();
  const Playground = (await import("./Playground")).default as ComponentType;
  render(<MantineProvider env="test"><Playground /></MantineProvider>);
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
    expect(choice).toBeDisabled();
    await userEvent.click(screen.getByRole("button", { name: "Check the same attempt" }));

    await waitFor(() => expect(screen.getByRole("status")).toBeVisible());
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
        { status: 409, headers: { "Content-Type": "application/json" } },
      );
    }));
    await renderPlayground();
    await screen.findByRole("button", { name: "læser" });
    await userEvent.click(screen.getByRole("button", { name: "Generation controls" }));
    const selector = screen.getByRole("combobox", { name: "Exercise type" });
    await userEvent.click(selector);
    await userEvent.click(screen.getByRole("option", { name: "Supply the indefinite article" }));

    expect(await screen.findByText("The engine revision changed. Generate a new exercise.")).toBeVisible();
    expect(screen.getByRole("button", { name: "Reload engine" })).toBeVisible();
    expect(screen.getByRole("button", { name: "læser" })).toBeVisible();
    expect(selector).toHaveValue("Choose the present form");
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
        { status: 401, headers: { "Content-Type": "application/json" } },
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
          { status: 409, headers: { "Content-Type": "application/json" } },
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
