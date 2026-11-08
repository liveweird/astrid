import { QueryClient } from "@tanstack/react-query";
import { screen } from "@testing-library/react";
import { catalog, exercise, json, session } from "../../test/playgroundFixtures";
import { renderWithProviders } from "../../test/render";

afterEach(() => vi.unstubAllGlobals());

it("drops the cached catalog when the engine revision changed during start, so the next start refetches", async () => {
  let catalogReads = 0;
  let exerciseReads = 0;
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("development-session")) return json(session);
    if (path.endsWith("catalog")) { catalogReads += 1; return json(catalog); }
    exerciseReads += 1;
    if (exerciseReads === 1) {
      return Response.json(
        { detail: "The engine revision changed." },
        { status: 409, headers: { "Content-Type": "application/problem+json" } },
      );
    }
    return json(exercise);
  }));
  vi.resetModules();
  const Playground = (await import("./Playground")).default;
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false, staleTime: Infinity } } });
  const first = renderWithProviders(<Playground />, { queryClient });
  expect(await screen.findByText("The engine revision changed. Generate a new exercise.")).toBeVisible();
  expect(queryClient.getQueryData(["catalog"])).toBeUndefined();
  first.unmount();

  renderWithProviders(<Playground />, { queryClient });

  expect(await screen.findByRole("button", { name: "læser" })).toBeVisible();
  expect(catalogReads).toBe(2);
});
