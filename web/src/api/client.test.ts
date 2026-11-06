import { bootstrapPlayground, submitAttempt, UncertainResultError } from "./client";
import type { AttemptRequest } from "./models";

const request: AttemptRequest = {
  attemptId: "28d52d72-7a04-4a42-8e87-bf780f821510",
  exerciseId: "c988af64-f509-498a-b8ff-fd42a96bd5a1",
  answer: { kind: "text", text: "en" },
  hintUsed: false,
};

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
    vi.unstubAllGlobals();
  });

  it("treats a server failure after submission as uncertain", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => new Response(
      JSON.stringify({ detail: "temporarily unavailable" }),
      { status: 503, headers: { "Content-Type": "application/json" } },
    )));
    await expect(submitAttempt(request)).rejects.toBeInstanceOf(UncertainResultError);
    vi.unstubAllGlobals();
  });

  it("keeps a successful submission with a truncated response recoverable", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => new Response("{", {
      status: 200,
      headers: { "Content-Type": "application/json" },
    })));
    await expect(submitAttempt(request)).rejects.toBeInstanceOf(UncertainResultError);
    vi.unstubAllGlobals();
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
    vi.unstubAllGlobals();
  });
});
