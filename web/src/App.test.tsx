import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import App from "./App";
import { APP_VERSION } from "./changelog/version";
import { renderWithProviders } from "./test/render";

vi.mock("./pages/playground/Playground", () => ({ default: () => <div>Learning workspace</div> }));
const practiceTestState = vi.hoisted(() => ({ mounts: 0 }));
vi.mock("./pages/practice/Practice", async () => {
  const { useState } = await import("react");
  const { useSearchParams } = await import("react-router-dom");
  return {
    default: function TestPractice() {
      const [instance] = useState(() => { practiceTestState.mounts += 1; return practiceTestState.mounts; });
      const [, setSearchParams] = useSearchParams();
      return (
        <div>
          <p>Practice instance {instance}</p>
          <button type="button" onClick={() => setSearchParams({ session: "bound-run" }, { replace: true })}>Bind run</button>
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
    expect(screen.getByRole("link", { name: "Engine playground" })).toHaveAttribute("href", "#main-content");
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
    expect(screen.getByRole("link", { name: "Engine playground" })).toHaveAttribute("href", "/");
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
      expect(screen.getByRole("link", { name: "Engine playground" })).toHaveAttribute("href", "/");
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

  it("keeps the practice mounted through its own URL binding and restarts it from the navigation link", async () => {
    window.history.replaceState(null, "", "/practice");
    renderWithProviders(<App />);

    expect(screen.getByText("Practice instance 1")).toBeVisible();
    expect(screen.getByRole("link", { name: "Practice" })).toHaveAttribute("data-active", "true");
    expect(screen.getByRole("link", { name: "Skip to learning exercise" })).toHaveAttribute("href", "#main-content");
    await userEvent.click(screen.getByRole("button", { name: "Bind run" }));
    expect(window.location.search).toBe("?session=bound-run");
    expect(screen.getByText("Practice instance 1")).toBeVisible();

    await userEvent.click(screen.getByRole("link", { name: "Practice" }));
    expect(window.location.pathname).toBe("/practice");
    expect(window.location.search).toBe("");
    expect(screen.getByText("Practice instance 2")).toBeVisible();
  });

  it("navigates between pages in the app and falls back to the playground for unknown paths", async () => {
    window.history.replaceState(null, "", "/unknown");
    renderWithProviders(<App />);

    expect(screen.getByText("Learning workspace")).toBeVisible();
    expect(screen.getByRole("link", { name: "Engine playground" })).toHaveAttribute("href", "#main-content");
    await userEvent.click(screen.getByRole("link", { name: "Practice" }));
    expect(window.location.pathname).toBe("/practice");
    expect(screen.getByText("Practice instance 1")).toBeVisible();
    await userEvent.click(screen.getByRole("link", { name: "Changelog" }));
    expect(await screen.findByRole("heading", { level: 1, name: "Changelog" })).toBeVisible();
    expect(screen.getByRole("link", { name: "Skip to main content" })).toBeVisible();
    await userEvent.click(screen.getByRole("link", { name: "Engine playground" }));
    expect(window.location.pathname).toBe("/");
    expect(screen.getByText("Learning workspace")).toBeVisible();
  });
});
