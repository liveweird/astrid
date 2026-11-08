import { MantineProvider } from "@mantine/core";
import { render, screen } from "@testing-library/react";
import { CHANGELOG } from "../changelog/entries";
import { APP_VERSION } from "../changelog/version";
import Changelog from "./Changelog";

describe("Changelog", () => {
  beforeEach(() => localStorage.clear());

  it("renders every version and authored release content", () => {
    render(<MantineProvider env="test"><Changelog /></MantineProvider>);
    expect(screen.getByRole("heading", { level: 1, name: "Changelog" })).toBeVisible();
    for (const entry of CHANGELOG) {
      expect(screen.getByText(`v${entry.version}`)).toBeVisible();
      expect(screen.getAllByText(entry.date).length).toBeGreaterThan(0);
    }
    expect(screen.getByText(/Astrid now shows its application version/)).toBeVisible();
    expect(JSON.parse(localStorage.getItem("astrid.changelog")!)).toEqual({ seenVersion: APP_VERSION });
  });
});
