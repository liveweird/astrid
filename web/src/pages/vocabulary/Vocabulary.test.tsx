import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { VocabularyContext, VocabularyEntries } from "../../api/models";
import { renderWithProviders } from "../../test/render";
import Vocabulary from "./Vocabulary";

const catalog: VocabularyEntries = {
  libraryRevision: "vocabulary-library-1",
  contentRevision: "pilot-20",
  items: [
    {
      entryId: "L006", headword: "I", situationIds: ["module-one"], senses: [{
        senseId: "L006-S01", meaning: "multiple addressees",
        activeForms: [{ formId: "L006-S01-F1", label: "subject", text: "I" }],
        restrictions: ["Uppercase I is lexical."],
        examples: [{ exampleId: "E1", danish: "I læser.", english: "You are reading.", scene: "A group" }],
        lessons: [{ focusId: "module-one-groups", title: "Talking to and about groups" }],
        sourceUrls: ["https://example.test/i"], reviewState: "draft-awaiting-teaching-review",
      }],
    },
    {
      entryId: "L009", headword: "have", situationIds: ["module-one", "vocabulary-p2"], senses: [
        {
          senseId: "L009-S01", meaning: "possess or have available",
          activeForms: [
            { formId: "L009-S01-F1", label: "infinitive", text: "have" },
            { formId: "L009-S01-F2", label: "finite-present", text: "har" },
          ], restrictions: ["Possession only."], examples: [], exampleNote: "Editorial example missing.",
          lessons: [], sourceUrls: [], reviewState: "draft-awaiting-teaching-review",
        },
        {
          senseId: "L009-S02", meaning: "receive in a polite request",
          activeForms: [{ formId: "L009-S02-F1", label: "bare-infinitive", text: "have" }],
          restrictions: ["Supplied request frame only."], examples: [], lessons: [], sourceUrls: [],
          reviewState: "draft-awaiting-teaching-review",
        },
      ],
    },
    {
      entryId: "L064", headword: "cykel", situationIds: ["vocabulary-p3"], senses: [{
        senseId: "L064-S01", meaning: "bicycle", activeForms: [
          { formId: "L064-S01-F1", label: "indefinite-singular", text: "en cykel" },
        ], restrictions: [], examples: [], lessons: [{ focusId: "vocabulary-p3-s1", title: "Two ways to travel" }],
        sourceUrls: [], reviewState: "draft-awaiting-teaching-review",
      }],
    },
    {
      entryId: "L048", headword: "hjemme", situationIds: ["vocabulary-p1", "vocabulary-p2", "vocabulary-p3"], senses: [{
        senseId: "L048-S01", meaning: "at home", activeForms: [
          { formId: "L048-S01-F1", label: "static-location", text: "hjemme" },
        ], restrictions: [], examples: [], lessons: [], sourceUrls: [], reviewState: "draft-awaiting-teaching-review",
      }],
    },
  ],
};

const context: VocabularyContext = {
  libraryRevision: catalog.libraryRevision,
  ownerSessionId: "28d52d72-7a04-4a42-8e87-bf780f821510",
  sessionExpiresAt: Date.now() + 60_000,
  items: [
    { entryId: "L009", senseId: "L009-S01", meaningRecorded: false, formIds: ["L009-S01-F2"] },
    { entryId: "L064", senseId: "L064-S01", meaningRecorded: true, formIds: ["L064-S01-F1"] },
  ],
};

function response(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

afterEach(() => vi.unstubAllGlobals());

it("browses finite grouped references without loading or creating personal teaching", async () => {
  const fetchMock = vi.fn(async () => response(catalog));
  vi.stubGlobal("fetch", fetchMock);
  renderWithProviders(<Vocabulary />);

  expect(await screen.findByRole("heading", { level: 1, name: "Vocabulary" })).toBeVisible();
  expect(await screen.findByText("4 of 4 entries shown")).toBeVisible();
  expect(screen.getByRole("heading", { name: "Module 1: First steps" })).toBeVisible();
  expect(screen.getByRole("heading", { name: "A day of work and study" })).toBeVisible();
  expect(screen.getByRole("heading", { name: "Places and journeys" })).toBeVisible();
  expect(screen.getByText("en cykel")).toHaveAttribute("lang", "da");
  expect(screen.getByText((_text, element) => element?.tagName === "P" && element.textContent === "Headword: cykel")).toBeVisible();
  expect(screen.getByText("I", { selector: "span" })).toHaveAttribute("lang", "da");
  expect(fetchMock).toHaveBeenCalledTimes(1);
  expect(fetchMock).toHaveBeenCalledWith("/api/v1/vocabulary-entries", expect.objectContaining({ credentials: "same-origin" }));
});

it("searches headwords, active forms and English meanings and filters situation membership", async () => {
  vi.stubGlobal("fetch", vi.fn(async () => response(catalog)));
  renderWithProviders(<Vocabulary />);
  await screen.findByText("4 of 4 entries shown");
  const user = userEvent.setup();

  await user.type(screen.getByRole("textbox", { name: "Search vocabulary" }), "har");
  expect(screen.getByText("1 of 4 entries shown")).toBeVisible();
  expect(screen.getByRole("button", { name: /^have / })).toBeVisible();
  await user.clear(screen.getByRole("textbox", { name: "Search vocabulary" }));
  await user.type(screen.getByRole("textbox", { name: "Search vocabulary" }), "BICYCLE");
  expect(screen.getByText("1 of 4 entries shown")).toBeVisible();

  await user.clear(screen.getByRole("textbox", { name: "Search vocabulary" }));
  await user.click(screen.getByRole("combobox", { name: "Situation" }));
  await user.click(screen.getByRole("option", { name: "Food, cafés and shopping" }));
  expect(screen.getByText("2 of 4 entries shown")).toBeVisible();
  expect(screen.getByRole("button", { name: /^have / })).toBeVisible();
  expect(screen.getByRole("button", { name: /^hjemme / })).toBeVisible();
});

it("loads precise saved teaching only on request and labels the claimed meaning and form", async () => {
  const fetchMock = vi.fn(async (path: string) => response(path.endsWith("vocabulary-context") ? context : catalog));
  vi.stubGlobal("fetch", fetchMock);
  renderWithProviders(<Vocabulary />);
  await screen.findByText("4 of 4 entries shown");
  const user = userEvent.setup();

  expect(fetchMock).toHaveBeenCalledTimes(1);
  await user.click(screen.getByRole("button", { name: "Load saved lesson teaching" }));
  expect(await screen.findByText(/2 selected uses have recorded teaching/)).toBeVisible();
  await user.click(screen.getByRole("checkbox", { name: "In saved lesson teaching" }));
  expect(screen.getByText("2 of 4 entries shown")).toBeVisible();
  expect(screen.queryByRole("button", { name: /^I / })).not.toBeInTheDocument();

  await user.click(screen.getByRole("button", { name: /^have / }));
  const possession = screen.getByRole("region", { name: "possess or have available" });
  expect(within(possession).getByText("har").closest("div")).toHaveTextContent("In saved lesson teaching");
  expect(within(possession).queryByText("Meaning in saved lesson teaching")).not.toBeInTheDocument();
  await user.click(screen.getByRole("button", { name: /^en cykel / }));
  expect(screen.getByText("Meaning in saved lesson teaching")).toBeVisible();
  expect(screen.getByRole("link", { name: "Two ways to travel" })).toHaveAttribute("href", "/#lesson-vocabulary-p3-s1");
  expect(fetchMock.mock.calls.map(([path]) => path)).not.toContain("/api/v1/development-session");
});

it("shows authored detail metadata and an empty result state", async () => {
  vi.stubGlobal("fetch", vi.fn(async () => response(catalog)));
  renderWithProviders(<Vocabulary />);
  const user = userEvent.setup();
  await user.click(await screen.findByRole("button", { name: /^I / }));

  expect(screen.getByText("I læser.")).toHaveAttribute("lang", "da");
  expect(screen.getByText("You are reading.")).toHaveAttribute("lang", "en");
  expect(screen.getByText("Uppercase I is lexical.")).toBeVisible();
  expect(within(screen.getByRole("region", { name: "multiple addressees" }))
    .getByText("Draft — awaiting teaching review")).toBeVisible();
  expect(screen.getByRole("link", { name: "Source 1" })).toHaveAttribute("rel", "noreferrer");

  await user.type(screen.getByRole("textbox", { name: "Search vocabulary" }), "not in the library");
  expect(screen.getByText("No vocabulary matches these filters.")).toBeVisible();
});

it("explains absent ownership and retries a failed reference load", async () => {
  const fetchMock = vi.fn()
    .mockResolvedValueOnce(response({ detail: "Unavailable" }, 500))
    .mockResolvedValueOnce(response(catalog))
    .mockResolvedValueOnce(response({ detail: "No session" }, 401));
  vi.stubGlobal("fetch", fetchMock);
  renderWithProviders(<Vocabulary />);

  expect(await screen.findByRole("alert")).toHaveTextContent("Vocabulary unavailable");
  await userEvent.click(screen.getByRole("button", { name: "Try again" }));
  expect(await screen.findByText("4 of 4 entries shown")).toBeVisible();
  await userEvent.click(screen.getByRole("button", { name: "Load saved lesson teaching" }));
  expect(await screen.findByText(/There is no current saved lesson teaching/)).toBeVisible();
  expect(fetchMock).toHaveBeenCalledTimes(3);
});

it("revalidates loaded personal teaching when the tab returns", async () => {
  const replacement = { ...context, ownerSessionId: "c988af64-f509-498a-b8ff-fd42a96bd5a1", items: [] };
  const fetchMock = vi.fn()
    .mockResolvedValueOnce(response(catalog))
    .mockResolvedValueOnce(response(context))
    .mockResolvedValueOnce(response(replacement));
  vi.stubGlobal("fetch", fetchMock);
  renderWithProviders(<Vocabulary />);
  await screen.findByText("4 of 4 entries shown");
  await userEvent.click(screen.getByRole("button", { name: "Load saved lesson teaching" }));
  expect(await screen.findByText(/2 selected uses have recorded teaching/)).toBeVisible();

  Object.defineProperty(document, "visibilityState", { configurable: true, value: "visible" });
  document.dispatchEvent(new Event("visibilitychange"));
  await waitFor(() => expect(screen.getByText(/0 selected uses have recorded teaching/)).toBeVisible());
  expect(fetchMock).toHaveBeenCalledTimes(3);
});

it("waits for the reference revision before allowing personal teaching to load", async () => {
  let finishCatalog!: (value: Response) => void;
  const deferred = new Promise<Response>((resolve) => { finishCatalog = resolve; });
  const fetchMock = vi.fn().mockReturnValueOnce(deferred).mockResolvedValueOnce(response(context));
  vi.stubGlobal("fetch", fetchMock);
  renderWithProviders(<Vocabulary />);

  const load = screen.getByRole("button", { name: "Load saved lesson teaching" });
  expect(load).toBeDisabled();
  await userEvent.click(load);
  expect(fetchMock).toHaveBeenCalledTimes(1);
  finishCatalog(response(catalog));
  await screen.findByText("4 of 4 entries shown");
  expect(load).toBeEnabled();
  await userEvent.click(load);
  expect(await screen.findByText(/2 selected uses have recorded teaching/)).toBeVisible();
});
