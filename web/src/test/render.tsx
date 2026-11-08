import { MantineProvider, type MantineColorScheme } from "@mantine/core";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, type RenderOptions } from "@testing-library/react";
import { type ReactElement, type ReactNode, StrictMode } from "react";
import { BrowserRouter } from "react-router-dom";
import { cssVariablesResolver, theme } from "../theme";

interface Options extends Omit<RenderOptions, "wrapper"> {
  queryClient?: QueryClient;
  /** Mount the whole provider tree in StrictMode, as main.tsx does (replays mount effects). */
  strict?: boolean;
  defaultColorScheme?: MantineColorScheme;
}

/**
 * The app's providers around `ui`. BrowserRouter (not MemoryRouter) on purpose: tests position the
 * URL with `window.history.replaceState` before rendering and assert `window.location` afterwards.
 * Each render gets a fresh QueryClient with the app's defaults unless one is supplied.
 */
export function renderWithProviders(ui: ReactElement, options: Options = {}) {
  const { queryClient: suppliedQueryClient, defaultColorScheme, strict = false, ...rest } = options;
  const queryClient = suppliedQueryClient ?? new QueryClient({
    defaultOptions: { queries: { retry: false, refetchOnWindowFocus: false, staleTime: Infinity } },
  });
  const Providers = ({ children }: { children: ReactNode }) => (
    <MantineProvider env="test" theme={theme} cssVariablesResolver={cssVariablesResolver} defaultColorScheme={defaultColorScheme}>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>{children}</BrowserRouter>
      </QueryClientProvider>
    </MantineProvider>
  );
  // StrictMode replays mount effects only when it is the root element (React 19), so a strict
  // render mounts the providers itself instead of through `wrapper`.
  if (strict) return render(<StrictMode><Providers>{ui}</Providers></StrictMode>, rest);
  return render(ui, { wrapper: Providers, ...rest });
}
