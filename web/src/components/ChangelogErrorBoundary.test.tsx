import { MantineProvider } from "@mantine/core";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import ChangelogErrorBoundary from "./ChangelogErrorBoundary";

function FailedChangelog(): never {
  throw new Error("lazy changelog chunk unavailable");
}

describe("ChangelogErrorBoundary", () => {
  it("keeps the shell available and offers a full-page retry when changelog rendering fails", async () => {
    const report = vi.spyOn(console, "error").mockImplementation(() => {});
    const reload = vi.fn();
    try {
      render(
        <MantineProvider env="test">
          <nav aria-label="Learning navigation"><a href="/">Engine playground</a></nav>
          <main>
            <ChangelogErrorBoundary reload={reload}>
              <FailedChangelog />
            </ChangelogErrorBoundary>
          </main>
        </MantineProvider>,
      );

      expect(screen.getByRole("navigation", { name: "Learning navigation" })).toBeVisible();
      expect(screen.getByRole("alert")).toHaveTextContent("Changelog unavailable");
      await userEvent.click(screen.getByRole("button", { name: "Reload changelog" }));
      expect(reload).toHaveBeenCalledOnce();
    } finally {
      report.mockRestore();
    }
  });

  it("renders the changelog content while it is healthy", () => {
    render(
      <MantineProvider env="test">
        <ChangelogErrorBoundary><p>Release notes</p></ChangelogErrorBoundary>
      </MantineProvider>,
    );
    expect(screen.getByText("Release notes")).toBeVisible();
  });
});
