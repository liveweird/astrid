import { MantineProvider } from "@mantine/core";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import App from "./App";
import { cssVariablesResolver, theme } from "./theme";

vi.mock("./Playground", () => ({ default: () => <div>Learning workspace</div> }));

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
  afterEach(() => vi.unstubAllGlobals());

  it("reports the effective dark preference and switches the whole document to explicit light", async () => {
    localStorage.clear();
    vi.stubGlobal("matchMedia", vi.fn(darkPreference));
    render(
      <MantineProvider env="test" theme={theme} cssVariablesResolver={cssVariablesResolver} defaultColorScheme="auto">
        <App />
      </MantineProvider>,
    );

    await userEvent.click(screen.getByRole("button", { name: "Use light theme" }));

    await waitFor(() => expect(document.documentElement).toHaveAttribute("data-mantine-color-scheme", "light"));
    expect(screen.getByRole("button", { name: "Use dark theme" })).toBeVisible();
  });

  it("exposes native navigation semantics and the burger state", async () => {
    localStorage.setItem("astrid.viewSettings.appShell.navCollapsed", "true");
    render(
      <MantineProvider env="test" theme={theme} cssVariablesResolver={cssVariablesResolver}>
        <App />
      </MantineProvider>,
    );

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
});
