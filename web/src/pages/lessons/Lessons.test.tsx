import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { context, owner, practiceId, profiles, json } from "../../test/practiceFixtures";
import { renderWithProviders } from "../../test/render";
import { listOutbox, saveRunRecovery } from "../practice/practiceRecovery";
import Lessons from "./Lessons";

beforeEach(() => {
  localStorage.clear();
  window.history.replaceState(null, "", "/");
  vi.restoreAllMocks();
});

afterEach(() => vi.unstubAllGlobals());

function reviewContext(itemCount: number, sourceLessonCount = itemCount ? 3 : 0, ownerSessionId = owner,
  planRevision = "cumulative-review-3") {
  return {
    ownerSessionId, sessionExpiresAt: 9999999999999, focusId: "cumulative-review",
    planRevision, title: "Mixed review from your lessons", itemCount, sourceLessonCount,
  };
}

it("renders all lesson and bounded review choices without network or storage mutations", () => {
  const fetch = vi.fn();
  const setItem = vi.spyOn(localStorage, "setItem");
  const removeItem = vi.spyOn(localStorage, "removeItem");
  vi.stubGlobal("fetch", fetch);

  renderWithProviders(<Lessons />);

  expect(screen.getByRole("heading", { level: 1, name: "Learn Danish" })).toBeVisible();
  expect(screen.getByRole("heading", { level: 2, name: "A day of work and study" })).toBeVisible();
  expect(screen.getByRole("heading", { level: 2, name: "Food, cafés and shopping" })).toBeVisible();
  expect(screen.getByRole("heading", { level: 2, name: "Places and journeys" })).toBeVisible();
  expect(screen.getAllByRole("button", { name: /^Start / })).toHaveLength(35);
  expect(screen.getByRole("button", { name: "Start Words for things you read" })).toBeVisible();
  expect(screen.getAllByText("Vocabulary")).toHaveLength(18);
  expect(screen.getByRole("heading", { level: 2, name: "Review" })).toHaveAttribute("tabindex", "-1");
  expect(screen.getByText(/familiar words, sentence order, verb forms and articles/)).toBeVisible();
  expect(fetch).not.toHaveBeenCalled();
  expect(setItem).not.toHaveBeenCalled();
  expect(removeItem).not.toHaveBeenCalled();
});

it("prepares an empty cumulative review only after the explicit action", async () => {
  const paths: string[] = [];
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    paths.push(path);
    if (path.endsWith("development-session")) return json({ mode: "development" });
    if (path.endsWith("practice-review-context")) return json(reviewContext(0));
    throw new Error(`Unexpected ${path}`);
  }));
  renderWithProviders(<Lessons />);

  expect(paths).toEqual([]);
  await userEvent.click(screen.getByRole("button", { name: "Prepare mixed review" }));

  expect(await screen.findByText("No introduced material yet")).toBeVisible();
  expect(screen.getByText("No introduced material yet").closest('[role="alert"]')).toHaveFocus();
  expect(screen.getByText(/Open and save a lesson’s teaching first/)).toBeVisible();
  expect(screen.queryByRole("button", { name: "Start mixed review" })).not.toBeInTheDocument();
  expect(paths).toEqual(["/api/v1/development-session", "/api/v1/practice-review-context"]);
  expect(localStorage.length).toBe(0);
});

it("refreshes empty and prepared review counts without creating a run", async () => {
  const contexts = [reviewContext(0), reviewContext(1, 1), reviewContext(8, 4)];
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("development-session")) return json({ mode: "development" });
    if (path.endsWith("practice-review-context")) return json(contexts.shift());
    throw new Error(`Unexpected ${path}`);
  }));
  renderWithProviders(<Lessons />);

  await userEvent.click(screen.getByRole("button", { name: "Prepare mixed review" }));
  await screen.findByText("No introduced material yet");
  await userEvent.click(screen.getByRole("button", { name: "Refresh preparation" }));
  expect(await screen.findByText("1 part from 1 lesson")).toBeVisible();
  expect(screen.getByRole("status")).toHaveFocus();
  expect(screen.getByText(/only 1 eligible item is available/)).toBeVisible();
  await userEvent.click(screen.getByRole("button", { name: "Refresh preparation" }));
  expect(await screen.findByText("8 parts from 4 lessons")).toBeVisible();

  expect(contexts).toHaveLength(0);
  expect(localStorage.length).toBe(0);
});

it("rejects an unsupported review preparation without exposing a start action", async () => {
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("development-session")) return json({ mode: "development" });
    if (path.endsWith("practice-review-context")) return json(reviewContext(9, 4));
    throw new Error(`Unexpected ${path}`);
  }));
  renderWithProviders(<Lessons />);

  await userEvent.click(screen.getByRole("button", { name: "Prepare mixed review" }));

  expect(await screen.findByRole("alert")).toHaveTextContent("unavailable or unsupported");
  expect(screen.queryByRole("button", { name: "Start mixed review" })).not.toBeInTheDocument();
  expect(localStorage.length).toBe(0);
});

it("keeps cumulative preparation retryable when its fresh context read fails", async () => {
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("development-session")) return json({ mode: "development" });
    if (path.endsWith("practice-review-context")) return json({ detail: "Review preparation is temporarily unavailable." }, 503);
    throw new Error(`Unexpected ${path}`);
  }));
  renderWithProviders(<Lessons />);

  await userEvent.click(screen.getByRole("button", { name: "Prepare mixed review" }));

  expect(await screen.findByRole("alert")).toHaveTextContent("Review preparation is temporarily unavailable.");
  expect(screen.getByRole("alert")).toHaveFocus();
  expect(screen.getByRole("button", { name: "Prepare mixed review" })).toBeEnabled();
  expect(localStorage.length).toBe(0);
});

it("shows an honest short preparation and saves its exact cumulative creation before navigation", async () => {
  const createdId = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa";
  vi.spyOn(crypto, "randomUUID").mockReturnValue(createdId);
  vi.spyOn(crypto, "getRandomValues").mockImplementation((array) => {
    (array as Uint32Array)[0] = 456;
    return array;
  });
  let reviewReads = 0;
  const fetch = vi.fn(async (path: string) => {
    if (path.endsWith("development-session")) return json({ mode: "development" });
    if (path.endsWith("practice-review-context")) {
      reviewReads += 1;
      return json(reviewContext(3, 2));
    }
    if (path.endsWith("practice-context")) return json(context());
    throw new Error(`Unexpected ${path}`);
  });
  vi.stubGlobal("fetch", fetch);
  renderWithProviders(<Lessons />);

  await userEvent.click(screen.getByRole("button", { name: "Prepare mixed review" }));
  expect(await screen.findByText("3 parts from 2 lessons")).toBeVisible();
  expect(screen.getByRole("status")).toHaveFocus();
  expect(screen.getByText(/shorter because only 3 eligible items are available/)).toBeVisible();
  expect(screen.getByText(/not a mastery score or cross-device progress/)).toBeVisible();
  await userEvent.click(screen.getByRole("button", { name: "Start mixed review" }));

  await waitFor(() => expect(window.location.search).toBe(`?session=${createdId}`));
  expect(reviewReads).toBe(2);
  expect(listOutbox(createdId)[0]).toMatchObject({
    ownerSessionId: owner, practiceSessionId: createdId, kind: "creation", identity: createdId,
    body: JSON.stringify({
      practiceSessionId: createdId, focusId: "cumulative-review", planRevision: "cumulative-review-3", seed: 456,
    }),
  });
});

it("returns to an empty preparation when eligibility disappears before creation", async () => {
  let reviewReads = 0;
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("development-session")) return json({ mode: "development" });
    if (path.endsWith("practice-review-context")) {
      reviewReads += 1;
      return json(reviewReads === 1 ? reviewContext(3, 2) : reviewContext(0));
    }
    if (path.endsWith("practice-context")) return json(context());
    throw new Error(`Unexpected ${path}`);
  }));
  renderWithProviders(<Lessons />);

  await userEvent.click(screen.getByRole("button", { name: "Prepare mixed review" }));
  await screen.findByText("3 parts from 2 lessons");
  await userEvent.click(screen.getByRole("button", { name: "Start mixed review" }));

  const unavailable = await screen.findByText("There is no introduced material to review yet. Start a lesson first.");
  expect(unavailable).toBeVisible();
  expect(unavailable.closest('[role="alert"]')).toHaveFocus();
  expect(screen.getByText("No introduced material yet")).toBeVisible();
  expect(screen.queryByRole("button", { name: "Start mixed review" })).not.toBeInTheDocument();
  expect(localStorage.length).toBe(0);
});

it("refuses a stale prepared owner without creating or replacing saved recovery", async () => {
  saveRunRecovery({ ownerSessionId: owner, practiceSessionId: practiceId,
    focusId: "reading-basics", planRevision: "practice-1", seed: 47 });
  let reviewReads = 0;
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("development-session")) return json({ mode: "development" });
    if (path.endsWith("practice-review-context")) {
      reviewReads += 1;
      return json(reviewContext(8, 4, reviewReads === 1 ? owner : "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"));
    }
    if (path.endsWith("practice-context")) return json(context());
    throw new Error(`Unexpected ${path}`);
  }));
  renderWithProviders(<Lessons />);

  await userEvent.click(screen.getByRole("button", { name: "Prepare mixed review" }));
  expect(await screen.findByText("8 parts from 4 lessons")).toBeVisible();
  await userEvent.click(screen.getByRole("button", { name: "Start mixed review" }));
  await userEvent.click(screen.getByRole("button", { name: "Start new mixed review" }));

  expect(await screen.findByRole("alert")).toHaveTextContent(/preparation expired/);
  expect(window.location.pathname).toBe("/");
  expect(localStorage.getItem("astrid.practice.v1.latest")).toBe(practiceId);
  expect(listOutbox(practiceId)).toEqual([]);
});

it("cancels the cumulative saved-run choice without changing its prepared context", async () => {
  saveRunRecovery({ ownerSessionId: owner, practiceSessionId: practiceId,
    focusId: "reading-basics", planRevision: "practice-1", seed: 47 });
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("development-session")) return json({ mode: "development" });
    if (path.endsWith("practice-review-context")) return json(reviewContext(8, 4));
    throw new Error(`Unexpected ${path}`);
  }));
  renderWithProviders(<Lessons />);

  await userEvent.click(screen.getByRole("button", { name: "Prepare mixed review" }));
  await screen.findByText("8 parts from 4 lessons");
  await userEvent.click(screen.getByRole("button", { name: "Start mixed review" }));
  expect(screen.getByText("Choose what to do with saved practice")).toBeVisible();
  await userEvent.click(screen.getByRole("button", { name: "Cancel" }));

  expect(screen.queryByText("Choose what to do with saved practice")).not.toBeInTheDocument();
  expect(screen.getByText("8 parts from 4 lessons")).toBeVisible();
  expect(localStorage.getItem("astrid.practice.v1.latest")).toBe(practiceId);
  expect(listOutbox(practiceId)).toEqual([]);
});

it("does not save a ghost cumulative run when delayed revalidation finishes after unmount", async () => {
  saveRunRecovery({ ownerSessionId: owner, practiceSessionId: practiceId,
    focusId: "reading-basics", planRevision: "practice-1", seed: 47 });
  const before = Array.from({ length: localStorage.length }, (_, index) => {
    const key = localStorage.key(index)!;
    return [key, localStorage.getItem(key)] as const;
  }).sort();
  let reviewReads = 0;
  let resolveReview!: (response: Response) => void;
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("development-session")) return json({ mode: "development" });
    if (path.endsWith("practice-review-context")) {
      reviewReads += 1;
      if (reviewReads === 1) return json(reviewContext(8, 4));
      return new Promise<Response>((resolve) => { resolveReview = resolve; });
    }
    if (path.endsWith("practice-context")) return json(context());
    throw new Error(`Unexpected ${path}`);
  }));
  const view = renderWithProviders(<Lessons />);
  await userEvent.click(screen.getByRole("button", { name: "Prepare mixed review" }));
  await screen.findByText("8 parts from 4 lessons");
  await userEvent.click(screen.getByRole("button", { name: "Start mixed review" }));
  await userEvent.click(screen.getByRole("button", { name: "Start new mixed review" }));
  await waitFor(() => expect(reviewReads).toBe(2));

  view.unmount();
  resolveReview(json(reviewContext(8, 4)));

  await waitFor(() => expect(localStorage.getItem("astrid.practice.v1.latest")).toBe(practiceId));
  const after = Array.from({ length: localStorage.length }, (_, index) => {
    const key = localStorage.key(index)!;
    return [key, localStorage.getItem(key)] as const;
  }).sort();
  expect(after).toEqual(before);
});

it("keeps the prepared review unsent when creation recovery cannot be saved", async () => {
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("development-session")) return json({ mode: "development" });
    if (path.endsWith("practice-review-context")) return json(reviewContext(8, 4));
    if (path.endsWith("practice-context")) return json(context());
    throw new Error(`Unexpected ${path}`);
  }));
  renderWithProviders(<Lessons />);
  await userEvent.click(screen.getByRole("button", { name: "Prepare mixed review" }));
  await screen.findByText("8 parts from 4 lessons");
  const originalSetItem = localStorage.setItem.bind(localStorage);
  const storage = vi.spyOn(localStorage, "setItem").mockImplementation((key, value) => {
    if (key.includes(".outbox.") && key.includes(".creation.")) throw new DOMException("Full", "QuotaExceededError");
    originalSetItem(key, value);
  });

  await userEvent.click(screen.getByRole("button", { name: "Start mixed review" }));

  expect(await screen.findByRole("alert")).toHaveTextContent("The operation was not sent");
  expect(window.location.pathname).toBe("/");
  expect(localStorage.getItem("astrid.practice.v1.latest")).toBeNull();
  storage.mockRestore();
});

it("persists the exact selected creation before handing it to the practice route", async () => {
  const createdId = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa";
  vi.spyOn(crypto, "randomUUID").mockReturnValue(createdId);
  vi.spyOn(crypto, "getRandomValues").mockImplementation((array) => {
    (array as Uint32Array)[0] = 123;
    return array;
  });
  const fetch = vi.fn(async (path: string) => {
    if (path.endsWith("practice-profiles")) return json(profiles());
    throw new Error(`Unexpected ${path}`);
  });
  vi.stubGlobal("fetch", fetch);

  renderWithProviders(<Lessons />);
  await userEvent.click(screen.getByRole("button", { name: "Start Talking about one person" }));

  await waitFor(() => expect(window.location.href).toContain(`/practice?session=${createdId}`));
  const entries = listOutbox(createdId);
  expect(entries).toHaveLength(1);
  expect(entries[0]).toMatchObject({ kind: "creation", identity: createdId, ownerSessionId: owner });
  expect(entries[0]?.body).toBe(JSON.stringify({
    practiceSessionId: createdId,
    focusId: "module-one-singular",
    planRevision: "module-one-singular-1",
    seed: 123,
  }));
  expect(fetch).toHaveBeenCalledTimes(1);
});

it.each([
  ["Work, reading and today", "vocabulary-p1-s1", "vocabulary-p1-s1-2"],
  ["Reading and writing", "vocabulary-p1-s2", "vocabulary-p1-s2-2"],
  ["Study, courses and Danish", "vocabulary-p1-s3", "vocabulary-p1-s3-2"],
  ["Coffee and water", "vocabulary-p1-s4", "vocabulary-p1-s4-2"],
  ["Where and when", "vocabulary-p1-s5", "vocabulary-p1-s5-2"],
])("saves the independently selected vocabulary lesson %s before navigation", async (title, focusId, planRevision) => {
  const createdId = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa";
  vi.spyOn(crypto, "randomUUID").mockReturnValue(createdId);
  vi.spyOn(crypto, "getRandomValues").mockImplementation((array) => {
    (array as Uint32Array)[0] = 123;
    return array;
  });
  const fetch = vi.fn(async (_path: string) => json(profiles()));
  vi.stubGlobal("fetch", fetch);
  renderWithProviders(<Lessons />);

  await userEvent.click(screen.getByRole("button", { name: `Start ${title}` }));

  await waitFor(() => expect(window.location.search).toBe(`?session=${createdId}`));
  expect(listOutbox(createdId)[0]?.body).toBe(JSON.stringify({
    practiceSessionId: createdId, focusId, planRevision, seed: 123,
  }));
  expect(fetch).toHaveBeenCalledTimes(1);
  expect(fetch.mock.calls[0]?.[0]).toBe("/api/v1/practice-profiles");
});

it("rejects a duplicate catalog entry instead of treating a missing vocabulary session as available", async () => {
  const catalog = profiles();
  catalog.profiles[catalog.profiles.length - 1] = catalog.profiles[0]!;
  vi.stubGlobal("fetch", vi.fn(async () => json(catalog)));
  renderWithProviders(<Lessons />);

  await userEvent.click(screen.getByRole("button", { name: "Start Where and when" }));

  expect(await screen.findByRole("alert")).toHaveTextContent("unsupported profile");
  expect(window.location.pathname).toBe("/");
  expect(localStorage.getItem("astrid.practice.v1.latest")).toBeNull();
});

it("offers an explicit resume or new-run choice and keeps the saved run on cancel", async () => {
  saveRunRecovery({ ownerSessionId: owner, practiceSessionId: practiceId,
    focusId: "reading-basics", planRevision: "practice-1", seed: 47 });
  const fetch = vi.fn();
  vi.stubGlobal("fetch", fetch);
  renderWithProviders(<Lessons />);

  expect(screen.getByRole("link", { name: "Resume saved practice" })).toHaveAttribute(
    "href", `/practice?session=${practiceId}`,
  );
  await userEvent.click(screen.getByRole("button", { name: "Start Present verb forms" }));
  expect(screen.getByText("Choose what to do with saved practice")).toBeVisible();
  expect(screen.getByRole("button", { name: "Start new practice" })).toBeVisible();
  await userEvent.click(screen.getByRole("button", { name: "Cancel" }));

  expect(screen.queryByText("Choose what to do with saved practice")).not.toBeInTheDocument();
  expect(localStorage.getItem("astrid.practice.v1.latest")).toBe(practiceId);
  expect(fetch).not.toHaveBeenCalled();
});

it("ignores corrupt or blocked saved recovery while keeping lessons browsable", () => {
  localStorage.setItem("astrid.practice.v1.latest", practiceId);
  localStorage.setItem(`astrid.practice.v1.run.${practiceId}`, JSON.stringify({ practiceSessionId: 17 }));
  const first = renderWithProviders(<Lessons />);
  expect(screen.queryByRole("link", { name: "Resume saved practice" })).not.toBeInTheDocument();
  first.unmount();

  const getItem = vi.spyOn(localStorage, "getItem").mockImplementation(() => {
    throw new DOMException("Storage denied", "SecurityError");
  });
  try {
    renderWithProviders(<Lessons />);
    expect(screen.getByRole("heading", { name: "Module 1: First steps" })).toBeVisible();
    expect(screen.queryByRole("link", { name: "Resume saved practice" })).not.toBeInTheDocument();
  } finally {
    getItem.mockRestore();
  }
});

it("rejects a saved run whose embedded identity differs from the latest pointer", () => {
  const embeddedId = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa";
  localStorage.setItem("astrid.practice.v1.latest", practiceId);
  localStorage.setItem(`astrid.practice.v1.run.${practiceId}`, JSON.stringify({
    ownerSessionId: owner,
    practiceSessionId: embeddedId,
    focusId: "reading-basics",
    planRevision: "practice-1",
    seed: 47,
  }));

  renderWithProviders(<Lessons />);

  expect(screen.queryByRole("link", { name: "Resume saved practice" })).not.toBeInTheDocument();
  expect(localStorage.getItem("astrid.practice.v1.latest")).toBe(practiceId);
  expect(localStorage.getItem(`astrid.practice.v1.run.${practiceId}`)).not.toBeNull();
});

it("does not navigate or duplicate catalog reads when an explicit start fails", async () => {
  let resolve!: (response: Response) => void;
  const fetch = vi.fn(() => new Promise<Response>((done) => { resolve = done; }));
  vi.stubGlobal("fetch", fetch);
  renderWithProviders(<Lessons />);

  const start = screen.getByRole("button", { name: "Start Talking about one person" });
  await userEvent.click(start);
  expect(start).toBeDisabled();
  await userEvent.click(start);
  expect(fetch).toHaveBeenCalledTimes(1);
  resolve(json({ detail: "Practice catalog unavailable." }, 503));

  expect(await screen.findByRole("alert")).toHaveTextContent("Practice catalog unavailable.");
  expect(window.location.pathname).toBe("/");
  expect(localStorage.getItem("astrid.practice.v1.latest")).toBeNull();
});

it("does not claim unsaved recovery when the profile read cannot reach the server", async () => {
  vi.stubGlobal("fetch", vi.fn(async () => { throw new TypeError("connection blocked"); }));
  renderWithProviders(<Lessons />);

  await userEvent.click(screen.getByRole("button", { name: "Start Talking about one person" }));

  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Practice could not reach the learning server. No new practice was started.",
  );
  expect(screen.getByRole("alert")).not.toHaveTextContent("pending operation");
  expect(localStorage.getItem("astrid.practice.v1.latest")).toBeNull();
  expect(listOutbox(practiceId)).toHaveLength(0);
});

it("sends no creation and preserves the previous latest run when saving fails", async () => {
  saveRunRecovery({ ownerSessionId: owner, practiceSessionId: practiceId,
    focusId: "reading-basics", planRevision: "practice-1", seed: 47 });
  vi.spyOn(crypto, "randomUUID").mockReturnValue("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("practice-profiles")) return json(profiles());
    throw new Error(`Unexpected ${path}`);
  }));
  renderWithProviders(<Lessons />);
  const originalSetItem = localStorage.setItem.bind(localStorage);
  const storage = vi.spyOn(localStorage, "setItem").mockImplementation((key, value) => {
    if (key.includes(".outbox.") && key.includes(".creation.")) throw new DOMException("Full", "QuotaExceededError");
    originalSetItem(key, value);
  });

  await userEvent.click(screen.getByRole("button", { name: "Start Talking about one person" }));
  await userEvent.click(screen.getByRole("button", { name: "Start new practice" }));

  expect(await screen.findByRole("alert")).toHaveTextContent("The operation was not sent");
  expect(window.location.pathname).toBe("/");
  expect(localStorage.getItem("astrid.practice.v1.latest")).toBe(practiceId);
  storage.mockRestore();
});

it("reports a persistence failure and sends nothing when storage reads become blocked during start", async () => {
  const fetch = vi.fn(async (path: string) => {
    if (path.endsWith("practice-profiles")) return json(profiles());
    throw new Error(`Unexpected ${path}`);
  });
  vi.stubGlobal("fetch", fetch);
  renderWithProviders(<Lessons />);
  const getItem = vi.spyOn(localStorage, "getItem").mockImplementation(() => {
    throw new DOMException("Storage denied", "SecurityError");
  });

  await userEvent.click(screen.getByRole("button", { name: "Start Talking about one person" }));

  expect(await screen.findByRole("alert")).toHaveTextContent(
    "This browser could not save recovery data. The operation was not sent.",
  );
  expect(fetch).toHaveBeenCalledTimes(1);
  getItem.mockRestore();
  expect(localStorage.getItem("astrid.practice.v1.latest")).toBeNull();
  expect(localStorage.length).toBe(0);
});
