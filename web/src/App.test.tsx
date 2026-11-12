import { act, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import App from "./App";
import { APP_VERSION } from "./changelog/version";
import { renderWithProviders } from "./test/render";

vi.mock("./pages/playground/Playground", () => ({ default: () => <div>Learning workspace</div> }));
vi.mock("./pages/lessons/Lessons", () => ({ default: () => (
  <div><h1>Learn Danish</h1><h2 id="review" tabIndex={-1}>Review</h2></div>
) }));
const practiceTestState = vi.hoisted(() => ({ mounts: 0 }));
vi.mock("./pages/practice/Practice", async () => {
  const { useState } = await import("react");
  const { Link, useSearchParams } = await import("react-router-dom");
  return {
    default: function TestPractice() {
      const [instance] = useState(() => { practiceTestState.mounts += 1; return practiceTestState.mounts; });
      const [, setSearchParams] = useSearchParams();
      return (
        <div>
          <p>Practice instance {instance}</p>
          <button type="button" onClick={() => setSearchParams({ session: "bound-run" }, { replace: true })}>Bind run</button>
          <Link to="/practice">Open practice</Link>
        </div>
      );
    },
  };
});
const changelogTestState = vi.hoisted(() => ({ fail: false }));
vi.mock("./pages/Changelog", async (importOriginal) => {
  const actual = await importOriginal<typeof import("./pages/Changelog")>();
  return {
    default: function TestChangelog() {
      if (changelogTestState.fail) throw new Error("lazy changelog chunk unavailable");
      const Changelog = actual.default;
      return <Changelog />;
    },
  };
});

function darkPreference(query: string): MediaQueryList {
  return {
    matches: query.includes("prefers-color-scheme: dark"),
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  };
}

describe("App theme", () => {
  beforeEach(() => {
    changelogTestState.fail = false;
    localStorage.clear();
    window.history.replaceState(null, "", "/");
  });
  afterEach(() => vi.unstubAllGlobals());

  it("reports the effective dark preference and switches the whole document to explicit light", async () => {
    vi.stubGlobal("matchMedia", vi.fn(darkPreference));
    renderWithProviders(<App />, { defaultColorScheme: "auto" });

    await userEvent.click(screen.getByRole("button", { name: "Use light theme" }));

    await waitFor(() => expect(document.documentElement).toHaveAttribute("data-mantine-color-scheme", "light"));
    expect(screen.getByRole("button", { name: "Use dark theme" })).toBeVisible();
  });

  it("exposes native navigation semantics and the burger state", async () => {
    localStorage.setItem("astrid.viewSettings.appShell.navCollapsed", "true");
    renderWithProviders(<App />);

    const navigation = screen.getByRole("navigation");
    const burger = screen.getByLabelText("Toggle navigation");
    expect(navigation).toHaveAttribute("id", "primary-navigation");
    expect(screen.getByRole("link", { name: "Lessons" })).toHaveAttribute("href", "/");
    expect(burger).toHaveAttribute("aria-controls", "primary-navigation");
    expect(burger).toHaveAttribute("aria-expanded", "false");
    const railToggle = screen.getByLabelText("Expand navigation");
    expect(railToggle).toHaveAttribute("aria-controls", "primary-navigation");
    expect(railToggle).toHaveAttribute("aria-expanded", "false");

    await userEvent.click(burger);

    expect(burger).toHaveAttribute("aria-expanded", "true");
    expect(navigation).toHaveAttribute("data-mobile-opened");
  });

  it("links to a lazy changelog and clears the what's-new marker when it opens", async () => {
    window.history.replaceState(null, "", "/changelog");
    renderWithProviders(<App />);

    expect(screen.getByRole("link", { name: "Changelog" })).toHaveAttribute("data-active", "true");
    expect(await screen.findByRole("heading", { level: 1, name: "Changelog" })).toBeVisible();
    await waitFor(() => expect(screen.queryByTitle("What's new")).not.toBeInTheDocument());
    expect(screen.getByRole("link", { name: "Engine playground" })).toHaveAttribute("href", "/playground");
    expect(screen.queryByText("Learning workspace")).not.toBeInTheDocument();
  });

  it("shows an accessible what's-new marker and a linked build stamp", () => {
    renderWithProviders(<App />);

    expect(screen.getByTitle("What's new")).toBeVisible();
    expect(screen.getAllByRole("link", { name: (name) => name.includes(`v${APP_VERSION}`) })[0]).toHaveAttribute("href", "/changelog");
  });

  it("retains the shell and unread marker when the lazy changelog fails", async () => {
    const report = vi.spyOn(console, "error").mockImplementation(() => {});
    changelogTestState.fail = true;
    window.history.replaceState(null, "", "/changelog");
    try {
      renderWithProviders(<App />);

      expect(await screen.findByRole("alert")).toHaveTextContent("Changelog unavailable");
      expect(screen.getByRole("navigation")).toBeVisible();
      expect(screen.getByRole("link", { name: "Lessons" })).toHaveAttribute("href", "/");
      expect(screen.getByTitle("What's new")).toBeVisible();
      expect(screen.getByRole("button", { name: "Reload changelog" })).toBeVisible();
    } finally {
      report.mockRestore();
    }
  });

  it("keeps changelog and in-memory rail controls usable when storage is blocked", async () => {
    const getItem = vi.spyOn(localStorage, "getItem").mockImplementation(() => {
      throw new DOMException("Storage denied", "SecurityError");
    });
    const setItem = vi.spyOn(localStorage, "setItem").mockImplementation(() => {
      throw new DOMException("Storage denied", "SecurityError");
    });
    window.history.replaceState(null, "", "/changelog");
    try {
      renderWithProviders(<App />);

      expect(await screen.findByRole("heading", { level: 1, name: "Changelog" })).toBeVisible();
      expect(screen.queryByText("Learning workspace")).not.toBeInTheDocument();
      await userEvent.click(screen.getByRole("button", { name: "Collapse navigation" }));
      expect(screen.getByRole("button", { name: "Expand navigation" })).toBeVisible();
    } finally {
      getItem.mockRestore();
      setItem.mockRestore();
    }
  });
});

describe("App routes", () => {
  beforeEach(() => {
    practiceTestState.mounts = 0;
    localStorage.clear();
  });

  it("keeps the practice mounted through its own URL binding and restarts it from a pushed bare-practice link", async () => {
    window.history.replaceState(null, "", "/practice");
    renderWithProviders(<App />);

    expect(screen.getByText("Practice instance 1")).toBeVisible();
    expect(screen.getByRole("link", { name: "Skip to learning exercise" })).toHaveAttribute("href", "#main-content");
    await userEvent.click(screen.getByRole("button", { name: "Bind run" }));
    expect(window.location.search).toBe("?session=bound-run");
    expect(screen.getByText("Practice instance 1")).toBeVisible();

    await userEvent.click(screen.getByRole("link", { name: "Open practice" }));
    expect(window.location.pathname).toBe("/practice");
    expect(window.location.search).toBe("");
    expect(screen.getByText("Practice instance 2")).toBeVisible();
  });

  it("remounts the practice when Back lands on a different session than the mounted run", async () => {
    window.history.replaceState(null, "", "/practice?session=run-x");
    renderWithProviders(<App />);
    expect(screen.getByText("Practice instance 1")).toBeVisible();

    await userEvent.click(screen.getByRole("link", { name: "Open practice" }));
    expect(screen.getByText("Practice instance 2")).toBeVisible();
    await userEvent.click(screen.getByRole("button", { name: "Bind run" }));
    expect(window.location.search).toBe("?session=bound-run");
    expect(screen.getByText("Practice instance 2")).toBeVisible();

    act(() => window.history.back());
    await waitFor(() => expect(window.location.search).toBe("?session=run-x"));
    expect(await screen.findByText("Practice instance 3")).toBeVisible();
  });

  it("keeps the mounted practice when a history traversal returns to its own session", async () => {
    window.history.replaceState(null, "", "/practice?session=bound-run");
    renderWithProviders(<App />);
    window.history.pushState(null, "", "/practice?session=bound-run#main-content");
    act(() => { window.dispatchEvent(new PopStateEvent("popstate")); });

    await waitFor(() => expect(window.location.hash).toBe("#main-content"));
    expect(screen.getByText("Practice instance 1")).toBeVisible();
  });

  it("moves focus to the main landmark on pushed navigation only", async () => {
    window.history.replaceState(null, "", "/practice");
    renderWithProviders(<App />);
    const main = document.getElementById("main-content");
    expect(main).not.toHaveFocus();

    const bind = screen.getByRole("button", { name: "Bind run" });
    await userEvent.click(bind);
    expect(bind).toHaveFocus();

    await userEvent.click(screen.getByRole("link", { name: "Changelog" }));
    await waitFor(() => expect(main).toHaveFocus());
    await userEvent.click(screen.getByRole("link", { name: "Engine playground" }));
    await waitFor(() => expect(main).toHaveFocus());
  });

  it("renders the practice runner for a trailing-slash path", () => {
    window.history.replaceState(null, "", "/practice/");
    renderWithProviders(<App />);

    expect(screen.getByText("Practice instance 1")).toBeVisible();
  });

  it("navigates between learner and developer pages and shows a learner-friendly unknown route", async () => {
    window.history.replaceState(null, "", "/unknown");
    renderWithProviders(<App />);

    expect(screen.getByRole("heading", { name: "Page not found" })).toBeVisible();
    await userEvent.click(screen.getByRole("link", { name: "Go to Lessons" }));
    expect(screen.getByRole("heading", { name: "Learn Danish" })).toBeVisible();
    await userEvent.click(screen.getByRole("link", { name: "Engine playground" }));
    expect(window.location.pathname).toBe("/playground");
    expect(screen.getByText("Learning workspace")).toBeVisible();
    await userEvent.click(screen.getByRole("link", { name: "Changelog" }));
    expect(await screen.findByRole("heading", { level: 1, name: "Changelog" })).toBeVisible();
    expect(screen.getByRole("link", { name: "Skip to main content" })).toBeVisible();
    await userEvent.click(screen.getByRole("link", { name: "Lessons" }));
    expect(window.location.pathname).toBe("/");
    expect(screen.getByRole("heading", { name: "Learn Danish" })).toBeVisible();
  });

  it("focuses the review heading for pushed same-page and cross-page review navigation", async () => {
    window.history.replaceState(null, "", "/");
    renderWithProviders(<App />);
    const reviewHeading = screen.getByRole("heading", { name: "Review" });

    await userEvent.click(screen.getByRole("link", { name: "Review" }));
    await waitFor(() => expect(reviewHeading).toHaveFocus());
    expect(window.location.hash).toBe("#review");
    await userEvent.click(screen.getByRole("link", { name: "Engine playground" }));
    await userEvent.click(screen.getByRole("link", { name: "Review" }));
    await waitFor(() => expect(screen.getByRole("heading", { name: "Review" })).toHaveFocus());
  });
});
