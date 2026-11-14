import { act, renderHook } from "@testing-library/react";
import type { VocabularyContext } from "../../api/models";
import { useVocabularyContext } from "./useVocabularyContext";

const base: VocabularyContext = {
  libraryRevision: "vocabulary-library-1",
  ownerSessionId: "28d52d72-7a04-4a42-8e87-bf780f821510",
  sessionExpiresAt: Date.now() + 60_000,
  items: [],
};

function response(context: VocabularyContext) {
  return Response.json(context);
}

afterEach(() => {
  vi.useRealTimers();
  vi.unstubAllGlobals();
});

it("keeps the latest personal-context request when older work finishes afterward", async () => {
  let finishFirst: ((value: Response) => void) | undefined;
  const first = new Promise<Response>((resolve) => { finishFirst = resolve; });
  const latest = { ...base, ownerSessionId: "c988af64-f509-498a-b8ff-fd42a96bd5a1" };
  vi.stubGlobal("fetch", vi.fn().mockReturnValueOnce(first).mockResolvedValueOnce(response(latest)));
  const { result } = renderHook(() => useVocabularyContext(base.libraryRevision));

  let older!: Promise<void>;
  let newer!: Promise<void>;
  await act(async () => {
    older = result.current.load();
    newer = result.current.load();
    await newer;
  });
  expect(result.current.context?.ownerSessionId).toBe(latest.ownerSessionId);

  await act(async () => {
    finishFirst?.(response(base));
    await older;
  });
  expect(result.current.context?.ownerSessionId).toBe(latest.ownerSessionId);
});

it("clears personal teaching exactly when its fixed owner session expires", async () => {
  vi.useFakeTimers();
  vi.setSystemTime(new Date("2026-11-14T12:00:00Z"));
  const expiring = { ...base, sessionExpiresAt: Date.now() + 100 };
  vi.stubGlobal("fetch", vi.fn(async () => response(expiring)));
  const { result } = renderHook(() => useVocabularyContext(base.libraryRevision));

  await act(async () => { await result.current.load(); });
  expect(result.current.context).toEqual(expiring);
  await act(async () => { await vi.advanceTimersByTimeAsync(100); });

  expect(result.current.context).toBeNull();
  expect(result.current.message).toContain("expired");
});

it("rejects expired and revision-mismatched personal projections", async () => {
  const fetchMock = vi.fn()
    .mockResolvedValueOnce(response({ ...base, sessionExpiresAt: Date.now() - 1 }))
    .mockResolvedValueOnce(response({ ...base, libraryRevision: "later-library" }));
  vi.stubGlobal("fetch", fetchMock);
  const { result } = renderHook(() => useVocabularyContext(base.libraryRevision));

  await act(async () => { await result.current.load(); });
  expect(result.current.message).toContain("expired");
  await act(async () => { await result.current.load(); });
  expect(result.current.message).toContain("different vocabulary revision");
  expect(result.current.context).toBeNull();
});

it("does not request personal teaching before a reference revision is available", async () => {
  const fetchMock = vi.fn();
  vi.stubGlobal("fetch", fetchMock);
  const { result } = renderHook(() => useVocabularyContext());
  await act(async () => { await result.current.load(); });
  expect(fetchMock).not.toHaveBeenCalled();
  expect(result.current.context).toBeNull();
});

it("discards in-flight teaching when the reference revision changes", async () => {
  let finish!: (value: Response) => void;
  const deferred = new Promise<Response>((resolve) => { finish = resolve; });
  const next = { ...base, libraryRevision: "vocabulary-library-2" };
  vi.stubGlobal("fetch", vi.fn().mockReturnValueOnce(deferred).mockResolvedValueOnce(response(next)));
  const { result, rerender } = renderHook(({ revision }) => useVocabularyContext(revision), {
    initialProps: { revision: base.libraryRevision },
  });
  let pending!: Promise<void>;
  await act(async () => { pending = result.current.load(); });
  expect(result.current.loading).toBe(true);
  rerender({ revision: next.libraryRevision });
  expect(result.current.loading).toBe(false);
  await act(async () => { finish(response(base)); await pending; });
  expect(result.current.context).toBeNull();
  expect(result.current.message).toBe("");
  await act(async () => { await result.current.load(); });
  expect(result.current.context).toEqual(next);
});

it("hides already loaded claims immediately when the reference revision changes", async () => {
  vi.stubGlobal("fetch", vi.fn(async () => response(base)));
  const { result, rerender } = renderHook(({ revision }) => useVocabularyContext(revision), {
    initialProps: { revision: base.libraryRevision },
  });
  await act(async () => { await result.current.load(); });
  expect(result.current.context).toEqual(base);
  rerender({ revision: "vocabulary-library-2" });
  expect(result.current.context).toBeNull();
  expect(result.current.message).toBe("");
});
