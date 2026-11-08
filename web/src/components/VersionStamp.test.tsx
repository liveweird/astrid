import { MantineProvider } from "@mantine/core";
import { render, screen } from "@testing-library/react";
import { APP_VERSION } from "../changelog/version";
import VersionStamp from "./VersionStamp";

describe("VersionStamp", () => {
  it("shows the version, commit and build time", () => {
    render(<MantineProvider env="test"><VersionStamp /></MantineProvider>);
    const stamp = screen.getByTitle("Build version");
    expect(stamp).toHaveTextContent(new RegExp(`^v${APP_VERSION.replaceAll(".", "\\.")} · \\S+ · \\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}$`));
    expect(screen.queryByRole("link")).not.toBeInTheDocument();
  });

  it("links to the changelog and keeps full details on the compact title", () => {
    render(<MantineProvider env="test"><VersionStamp href="/changelog" compact /></MantineProvider>);
    const stamp = screen.getByRole("link", { name: `v${APP_VERSION}` });
    expect(stamp).toHaveAttribute("href", "/changelog");
    expect(stamp).toHaveAttribute("title", expect.stringMatching(new RegExp(`^v${APP_VERSION.replaceAll(".", "\\.")} · `)));
  });
});
