import type { Attempt, AttemptRequest, Catalog, DevelopmentSession, Exercise, InspectorDetails } from "./models";

const CLIENT_HEADER = { "X-Astrid-Client": "playground" };
const REQUEST_DEADLINE_MS = 15_000;

export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

export class UncertainResultError extends Error {
  override readonly cause: unknown;

  constructor(cause: unknown) {
    super("The result could not be confirmed.");
    this.cause = cause;
  }
}

async function readJson<T>(path: string, init?: RequestInit): Promise<T> {
  const controller = new AbortController();
  const deadline = window.setTimeout(() => controller.abort(), REQUEST_DEADLINE_MS);
  try {
    let response: Response;
    try {
      response = await fetch(path, { credentials: "same-origin", ...init, signal: controller.signal });
    } catch (cause) {
      throw new UncertainResultError(cause);
    }
    if (!response.ok) {
      const body = (await response.json().catch(() => null)) as { message?: unknown; detail?: unknown } | null;
      const message = typeof body?.message === "string" ? body.message :
        typeof body?.detail === "string" ? body.detail : `Request failed (${response.status}).`;
      throw new ApiError(response.status, message);
    }
    try {
      return (await response.json()) as T;
    } catch (cause) {
      // A committed mutation can still lose or truncate its response body in a proxy.
      throw new UncertainResultError(cause);
    }
  } finally {
    window.clearTimeout(deadline);
  }
}

function jsonInit(method: "POST", body: unknown, extraHeaders?: HeadersInit): RequestInit {
  return {
    method,
    headers: { "Content-Type": "application/json", ...CLIENT_HEADER, ...extraHeaders },
    body: JSON.stringify(body),
  };
}

export const startDevelopmentSession = () =>
  readJson<DevelopmentSession>("/api/v1/development-session", jsonInit("POST", {}));

export const loadCatalog = () => readJson<Catalog>("/api/v1/catalog");

export function createExercise(request: {
  formatId: string; targetSkillId?: string | null; seed: number; index: number;
  contentRevision: string; generatorRevision: string;
}) {
  return readJson<Exercise>("/api/v1/exercises", jsonInit("POST", request));
}

export async function bootstrapPlayground(seed: number, index: number) {
  // Catalog is session-scoped: wait for Set-Cookie before issuing authenticated reads.
  const session = await startDevelopmentSession();
  const catalog = await loadCatalog();
  const format = catalog.formats[0];
  if (!format) throw new Error("The engine catalog has no exercise formats.");
  const exercise = await createExercise({
    formatId: format.id,
    ...(format.targetSkillId ? { targetSkillId: format.targetSkillId } : {}),
    seed,
    index,
    contentRevision: catalog.contentRevision,
    generatorRevision: catalog.generatorRevision,
  });
  return { session, catalog, exercise };
}

export async function submitAttempt(request: AttemptRequest) {
  try {
    return await readJson<Attempt>("/api/v1/attempts", jsonInit("POST", request));
  } catch (error) {
    // A server failure can occur after the durable write but before the browser receives it.
    // Keep the same idempotency identity available for a safe replay.
    if (error instanceof ApiError && error.status >= 500) throw new UncertainResultError(error);
    throw error;
  }
}

export const loadAttempt = (attemptId: string) =>
  readJson<Attempt>(`/api/v1/attempts/${encodeURIComponent(attemptId)}`);

export const inspectExercise = (exerciseId: string, key: string) =>
  readJson<InspectorDetails>(`/api/v1/dev/exercises/${encodeURIComponent(exerciseId)}`, {
    headers: { "X-Astrid-Inspector-Key": key },
  });
