import "@fontsource-variable/inter";
import "@mantine/core/styles.css";
import "./index.css";
import { MantineProvider } from "@mantine/core";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./App";
import { cssVariablesResolver, theme } from "./theme";

const queryClient = new QueryClient({
  // Queries cover only the session-scoped catalog reads (GET /catalog, GET /practice-profiles).
  // retry: false — the practice recovery protocol owns every retry (outbox replay with the stored
  // identity and body); a hidden retry would also delay a 503 that must surface immediately.
  // refetchOnWindowFocus: false — a focus refetch would race the explicit owner checks other tabs
  // rely on (practice multitab flow). staleTime: Infinity — revisions change only with a deploy,
  // which a 409 already turns into a reload.
  defaultOptions: { queries: { retry: false, refetchOnWindowFocus: false, staleTime: Infinity } },
});

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <MantineProvider theme={theme} cssVariablesResolver={cssVariablesResolver} defaultColorScheme="auto">
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </QueryClientProvider>
    </MantineProvider>
  </StrictMode>,
);
