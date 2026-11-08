import { screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { renderWithProviders } from "../test/render";
import BrandMark from "./BrandMark";

function markSource(container: HTMLElement) {
  return container.querySelector("img")?.getAttribute("src");
}

describe("BrandMark", () => {
  it("shows the light logo with the wordmark in the light scheme", () => {
    const { container } = renderWithProviders(<BrandMark />, { defaultColorScheme: "light" });
    expect(markSource(container)).toBe("/logo-light.svg");
    expect(screen.getByText("Astrid")).toBeInTheDocument();
  });

  it("switches to the dark logo in the dark scheme", () => {
    const { container } = renderWithProviders(<BrandMark />, { defaultColorScheme: "dark" });
    expect(markSource(container)).toBe("/logo-dark.svg");
  });

  it("keeps the logo decorative and drops the wordmark when compact", () => {
    const { container } = renderWithProviders(<BrandMark compact />);
    expect(container.querySelector("img")).toHaveAttribute("alt", "");
    expect(screen.queryByText("Astrid")).not.toBeInTheDocument();
  });
});
