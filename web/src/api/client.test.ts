import { ApiError, bootstrapPlayground, submitAttempt, UncertainResultError } from "./client";
import type { AttemptRequest } from "./models";

const request: AttemptRequest = {
  attemptId: "28d52d72-7a04-4a42-8e87-bf780f821510",
  exerciseId: "c988af64-f509-498a-b8ff-fd42a96bd5a1",
  answer: { kind: "text", text: "en" },
  hintUsed: false,
};

function abortError() {
  return new DOMException("The operation was aborted.", "AbortError");
}

function waitForAbort(signal: AbortSignal | null | undefined): Promise<Response> {
  return new Promise((_resolve, reject) => {
    signal?.addEventListener("abort", () => reject(abortError()), { once: true });
  });
}

function responseWithStalledJson(status: number, signal: AbortSignal | null | undefined): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: () => waitForAbort(signal),
  } as unknown as Response;
}

afterEach(() => {
  vi.useRealTimers();
  vi.unstubAllGlobals();
});

describe("attempt transport", () => {
  it("can retry an uncertain submission with exactly the same id and payload", async () => {
    const bodies: string[] = [];
    vi.stubGlobal("fetch", vi.fn(async (_path: string, init?: RequestInit) => {
      bodies.push(String(init?.body));
      if (bodies.length === 1) throw new TypeError("connection closed");
      return new Response(JSON.stringify({ attemptId: request.attemptId }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    }));

    await expect(submitAttempt(request)).rejects.toBeInstanceOf(UncertainResultError);
    await submitAttempt(request);

    expect(bodies).toHaveLength(2);
    expect(bodies[1]).toBe(bodies[0]);
    expect(JSON.parse(bodies[1]!)).toMatchObject({ attemptId: request.attemptId, hintUsed: false });
  });

  it("treats a server failure after submission as uncertain", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => new Response(
      JSON.stringify({ detail: "temporarily unavailable" }),
      { status: 503, headers: { "Content-Type": "application/json" } },
    )));
    await expect(submitAttempt(request)).rejects.toBeInstanceOf(UncertainResultError);
  });

  it("keeps a successful submission with a truncated response recoverable", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => new Response("{", {
      status: 200,
      headers: { "Content-Type": "application/json" },
    })));
    await expect(submitAttempt(request)).rejects.toBeInstanceOf(UncertainResultError);
  });

  it("times out before response headers and clears the deadline timer", async () => {
    vi.useFakeTimers();
    vi.stubGlobal("fetch", vi.fn((_path: string, init?: RequestInit) => waitForAbort(init?.signal)));

    const submission = submitAttempt(request);
    const rejection = expect(submission).rejects.toBeInstanceOf(UncertainResultError);
    await vi.advanceTimersByTimeAsync(15_000);

    await rejection;
    expect(vi.getTimerCount()).toBe(0);
  });

  it("times out while reading a successful response body and clears the deadline timer", async () => {
    vi.useFakeTimers();
    vi.stubGlobal("fetch", vi.fn(async (_path: string, init?: RequestInit) =>
      responseWithStalledJson(200, init?.signal)));

    const submission = submitAttempt(request);
    const rejection = expect(submission).rejects.toBeInstanceOf(UncertainResultError);
    await vi.advanceTimersByTimeAsync(15_000);

    await rejection;
    expect(vi.getTimerCount()).toBe(0);
  });

  it.each([401, 409])("preserves HTTP %i when its error body stalls", async (status) => {
    vi.useFakeTimers();
    vi.stubGlobal("fetch", vi.fn(async (_path: string, init?: RequestInit) =>
      responseWithStalledJson(status, init?.signal)));

    const submission = submitAttempt(request);
    const rejection = expect(submission).rejects.toMatchObject({ status });
    await vi.advanceTimersByTimeAsync(15_000);

    await rejection;
    await expect(submission).rejects.toBeInstanceOf(ApiError);
    expect(vi.getTimerCount()).toBe(0);
  });

  it("keeps a 5xx response uncertain when its error body stalls", async () => {
    vi.useFakeTimers();
    vi.stubGlobal("fetch", vi.fn(async (_path: string, init?: RequestInit) =>
      responseWithStalledJson(503, init?.signal)));

    const submission = submitAttempt(request);
    const rejection = expect(submission).rejects.toBeInstanceOf(UncertainResultError);
    await vi.advanceTimersByTimeAsync(15_000);

    await rejection;
    expect(vi.getTimerCount()).toBe(0);
  });

  it("clears the deadline timer after immediate success and failure", async () => {
    vi.useFakeTimers();
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(Response.json({ attemptId: request.attemptId }))
      .mockRejectedValueOnce(new TypeError("offline"));
    vi.stubGlobal("fetch", fetchMock);

    await submitAttempt(request);
    expect(vi.getTimerCount()).toBe(0);
    await expect(submitAttempt(request)).rejects.toBeInstanceOf(UncertainResultError);
    expect(vi.getTimerCount()).toBe(0);
  });

  it("replays the byte-identical request after a deadline expires", async () => {
    vi.useFakeTimers();
    const bodies: string[] = [];
    const inits: RequestInit[] = [];
    vi.stubGlobal("fetch", vi.fn(async (_path: string, init?: RequestInit) => {
      inits.push(init ?? {});
      bodies.push(String(init?.body));
      if (bodies.length === 1) return waitForAbort(init?.signal);
      return Response.json({ attemptId: request.attemptId });
    }));

    const firstSubmission = submitAttempt(request);
    const rejection = expect(firstSubmission).rejects.toBeInstanceOf(UncertainResultError);
    await vi.advanceTimersByTimeAsync(15_000);
    await rejection;
    await submitAttempt(request);

    expect(bodies).toHaveLength(2);
    expect(bodies[1]).toBe(bodies[0]);
    expect(inits[1]).toMatchObject({ credentials: "same-origin", method: "POST" });
    expect(inits[1].headers).toMatchObject({
      "Content-Type": "application/json",
      "X-Astrid-Client": "playground",
    });
    expect(vi.getTimerCount()).toBe(0);
  });
});

describe("playground bootstrap", () => {
  it("waits for the session before loading the protected catalog and can retry a failed start", async () => {
    const paths: string[] = [];
    let firstStart = true;
    vi.stubGlobal("fetch", vi.fn(async (path: string) => {
      paths.push(path);
      if (path.endsWith("development-session") && firstStart) {
        firstStart = false;
        throw new TypeError("offline");
      }
      if (path.endsWith("development-session")) return Response.json({
        mode: "development", inspectorEnabled: false, contentRevision: "pilot-1",
        generatorRevision: "generator-1", evaluationRevision: "evaluation-1",
      });
      if (path.endsWith("catalog")) return Response.json({
        contentRevision: "pilot-1", generatorRevision: "generator-1", evaluationRevision: "evaluation-1",
        sentenceCount: 99, skills: [], formats: [{ id: "E001", title: "Forms", targetSkillId: "G004" }],
      });
      return Response.json({ id: "exercise-1" });
    }));

    await expect(bootstrapPlayground(1, 0)).rejects.toBeInstanceOf(UncertainResultError);
    await bootstrapPlayground(1, 0);

    expect(paths).toEqual([
      "/api/v1/development-session",
      "/api/v1/development-session",
      "/api/v1/catalog",
      "/api/v1/exercises",
    ]);
  });
});
