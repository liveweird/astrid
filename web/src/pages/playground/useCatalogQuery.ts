import { queryOptions, useQuery } from "@tanstack/react-query";
import { loadCatalog } from "../../api/client";

/** GET /catalog. Session-scoped: read it only once the development session is established. */
export const catalogQuery = queryOptions({ queryKey: ["catalog"], queryFn: loadCatalog });

export function useCatalogQuery(sessionReady: boolean) {
  return useQuery({ ...catalogQuery, enabled: sessionReady });
}
