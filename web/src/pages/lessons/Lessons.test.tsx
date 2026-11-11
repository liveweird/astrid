import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { owner, practiceId, profiles, json } from "../../test/practiceFixtures";
import { renderWithProviders } from "../../test/render";
import { listOutbox, saveRunRecovery } from "../practice/practiceRecovery";
import Lessons from "./Lessons";

beforeEach(() => {
  localStorage.clear();
  window.history.replaceState(null, "", "/");
  vi.restoreAllMocks();
});

afterEach(() => vi.unstubAllGlobals());

it("renders all lesson and bounded review choices without network or storage mutations", () => {
  const fetch = vi.fn();
  const setItem = vi.spyOn(localStorage, "setItem");
  const removeItem = vi.spyOn(localStorage, "removeItem");
  vi.stubGlobal("fetch", fetch);

  renderWithProviders(<Lessons />);

  expect(screen.getByRole("heading", { level: 1, name: "Module 1: First steps" })).toBeVisible();
  expect(screen.getAllByRole("button", { name: /^Start / })).toHaveLength(18);
  expect(screen.getByRole("button", { name: "Start Words for things you read" })).toBeVisible();
  expect(screen.getByText("Vocabulary")).toBeVisible();
  expect(screen.getByRole("heading", { level: 2, name: "Review" })).toHaveAttribute("tabindex", "-1");
  expect(screen.getByText(/familiar words, sentence order, verb forms and articles/)).toBeVisible();
  expect(fetch).not.toHaveBeenCalled();
  expect(setItem).not.toHaveBeenCalled();
  expect(removeItem).not.toHaveBeenCalled();
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
