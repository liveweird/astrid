import AxeBuilder from "@axe-core/playwright";
import { expect, test, type Page } from "@playwright/test";
import { ownerHeaders, PRACTICE_HEADERS } from "../support/practice.js";

type LibraryForm = { formId: string; label: string; text: string };
type LibrarySense = {
  senseId: string; meaning: string; activeForms: LibraryForm[];
  examples: { exampleId: string; danish: string; english?: string; scene?: string }[];
  lessons: { focusId: string; title: string }[];
};
type Library = {
  libraryRevision: string; contentRevision: string;
  items: { entryId: string; headword: string; situationIds: string[]; senses: LibrarySense[] }[];
};
type TeachingContext = {
  ownerSessionId: string; sessionExpiresAt: number; libraryRevision: string;
  items: { entryId: string; senseId: string; meaningRecorded: boolean; formIds: string[] }[];
};

async function scan(page: Page) {
  const result = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"])
    .analyze();
  expect(result.violations.map(({ id, nodes }) => ({ id, targets: nodes.map((node) => node.target) }))).toEqual([]);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true);
}

test("vocabulary reference is finite and passive without a learner cookie", async ({ request }) => {
  const response = await request.get("/api/v1/vocabulary-entries");
  expect(response.status()).toBe(200);
  expect(response.headers()["cache-control"]).toBe("no-store");
  expect(response.headers()["set-cookie"]).toBeUndefined();
  const catalog = await response.json() as Library;
  expect(catalog.libraryRevision).toBe("vocabulary-library-1");
  expect(catalog.contentRevision).toBe("pilot-20");
  expect(catalog.items).toHaveLength(79);
  expect(new Set(catalog.items.map((entry) => entry.entryId)).size).toBe(79);
  const possession = catalog.items.find((entry) => entry.entryId === "L009")!;
  expect(possession.senses.map((sense) => sense.senseId)).toContain("L009-S02");
  const requestSense = possession.senses.find((sense) => sense.senseId === "L009-S02")!;
  expect(requestSense.activeForms.map((form) => form.text)).not.toContain("har");
  expect(catalog.items.find((entry) => entry.entryId === "L069")!.senses
    .flatMap((sense) => sense.activeForms.map((form) => form.text))).toContain("hjem");
  const serialized = JSON.stringify(catalog);
  for (const privateField of ["expectedAnswer", "answerKey", "acceptedOrders", "introductionExposureKeys", "matchingPairs"]) {
    expect(serialized).not.toContain(`"${privateField}"`);
  }
  expect((await request.storageState()).cookies).toHaveLength(0);
  expect((await request.get("/api/v1/vocabulary-context")).status()).toBe(401);
  const guarded = await request.get("/api/v1/vocabulary-entries", { headers: { Origin: "https://untrusted.invalid" } });
  expect(guarded.status()).toBe(403);
  expect(guarded.headers()["content-type"]).toContain("application/problem+json");
});

test("saved teaching claims remain owner scoped and browsing creates no new teaching", async ({ page, browser }) => {
  expect((await page.request.post("/api/v1/development-session", { headers: PRACTICE_HEADERS, data: {} })).status())
    .toBe(200);
  const initialResponse = await page.request.get("/api/v1/vocabulary-context");
  expect(initialResponse.status()).toBe(200);
  const initial = await initialResponse.json() as TeachingContext;
  expect(initial.items).toEqual([]);
  const runId = crypto.randomUUID();
  expect((await page.request.post("/api/v1/practice-sessions", {
    headers: ownerHeaders(initial.ownerSessionId),
    data: { practiceSessionId: runId, focusId: "vocabulary-p3-s1", planRevision: "vocabulary-p3-s1-1", seed: 11 },
  })).status()).toBe(201);
  expect((await (await page.request.get("/api/v1/vocabulary-context")).json() as TeachingContext).items).toEqual([]);
  expect((await page.request.post(`/api/v1/practice-sessions/${runId}/support`, {
    headers: ownerHeaders(initial.ownerSessionId), data: { kind: "introduction" },
  })).status()).toBe(200);
  const taught = await (await page.request.get("/api/v1/vocabulary-context")).json() as TeachingContext;
  const library = await (await page.request.get("/api/v1/vocabulary-entries")).json() as Library;
  const bicycle = library.items.find((entry) => entry.entryId === "L064")!;
  const bicycleSense = bicycle.senses[0]!;
  const bundle = bicycleSense.activeForms.find((form) => form.text === "en cykel")!;
  expect(bundle).toBeDefined();
  expect(taught.items.find((claim) => claim.senseId === bicycleSense.senseId)?.formIds).toContain(bundle.formId);
  expect(taught.items.some((claim) => claim.entryId === "L075")).toBe(false);
  const before = await (await page.request.get(`/api/v1/practice-sessions/${runId}`)).text();
  const reviewBefore = await (await page.request.get("/api/v1/practice-review-context")).text();
  await page.goto("/vocabulary");
  await expect(page.getByRole("heading", { name: "Vocabulary", exact: true })).toBeVisible();
  await scan(page);
  expect(await (await page.request.get("/api/v1/vocabulary-context")).json()).toEqual(taught);
  expect(await (await page.request.get(`/api/v1/practice-sessions/${runId}`)).text()).toBe(before);
  expect(await (await page.request.get("/api/v1/practice-review-context")).text()).toBe(reviewBefore);
  const other = await browser.newContext({ baseURL: new URL(page.url()).origin });
  try {
    expect((await other.request.post("/api/v1/development-session", { headers: PRACTICE_HEADERS, data: {} })).status())
      .toBe(200);
    const isolated = await (await other.request.get("/api/v1/vocabulary-context")).json() as TeachingContext;
    expect(isolated.ownerSessionId).not.toBe(taught.ownerSessionId);
    expect(isolated.items).toEqual([]);
  } finally { await other.close(); }
});

test("vocabulary search details and lesson links remain passive and keyboard accessible", async ({ page }) => {
  const mutations: string[] = [];
  page.on("request", (request) => {
    if (new URL(request.url()).pathname.startsWith("/api/") && request.method() !== "GET") mutations.push(request.url());
  });
  await page.goto("/");
  await page.getByRole("navigation").getByRole("link", { name: "Vocabulary", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Vocabulary", exact: true })).toBeVisible();
  await expect(page.locator("#main-content")).toBeFocused();
  await expect(page.getByText("79 of 79 entries shown", { exact: true })).toBeVisible();
  const search = page.getByRole("textbox", { name: "Search vocabulary", exact: true });
  await search.fill("hjem");
  await expect(page.getByText("2 of 79 entries shown", { exact: true })).toBeVisible();
  const home = page.getByRole("button", { name: /^hjem / });
  await home.focus();
  await page.keyboard.press("Enter");
  await expect(home).toHaveAttribute("aria-expanded", "true");
  await expect(page.getByText("Nora går hjem.", { exact: true })).toHaveAttribute("lang", "da");
  await expect(page.getByText("Nora walks home.", { exact: true })).toHaveAttribute("lang", "en");
  await scan(page);
  await search.fill("til stationen");
  await expect(page.getByText("1 of 79 entries shown", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: /^til stationen / }).click();
  const lessonLink = page.getByRole("link", { name: "Walking to the station and home", exact: true });
  await expect(lessonLink).toHaveAttribute("href", "/#lesson-vocabulary-p3-s2");
  await lessonLink.click();
  await expect(page).toHaveURL(/\/#lesson-vocabulary-p3-s2$/);
  const row = page.locator("#lesson-vocabulary-p3-s2");
  await expect(row).toBeFocused();
  await expect(row.getByRole("heading", { name: "Walking to the station and home", exact: true })).toBeVisible();
  const bounds = await row.boundingBox();
  const header = await page.locator("header").boundingBox();
  expect(bounds!.y).toBeGreaterThanOrEqual(header!.y + header!.height - 1);
  expect(mutations).toEqual([]);
  expect((await page.context().cookies()).some((cookie) => cookie.name === "astrid_session")).toBe(false);
});

test("saved-teaching filter explains absent ownership without starting a session", async ({ page }) => {
  const mutations: string[] = [];
  page.on("request", (request) => {
    if (new URL(request.url()).pathname.startsWith("/api/") && request.method() !== "GET") mutations.push(request.url());
  });
  await page.goto("/vocabulary");
  await expect(page.getByText("79 of 79 entries shown", { exact: true })).toBeVisible();
  await page.getByRole("checkbox", { name: "In saved lesson teaching", exact: true }).check();
  await expect(page.getByText("No vocabulary matches these filters.", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Load saved lesson teaching", exact: true }).click();
  await expect(page.getByText(/There is no current saved lesson teaching in this browser session/)).toBeVisible();
  await page.getByRole("checkbox", { name: "In saved lesson teaching", exact: true }).uncheck();
  await expect(page.getByText("79 of 79 entries shown", { exact: true })).toBeVisible();
  expect(mutations).toEqual([]);
  expect((await page.context().cookies()).some((cookie) => cookie.name === "astrid_session")).toBe(false);
  await scan(page);
});

for (const theme of ["light", "dark"] as const) {
  test(`vocabulary details filters and empty state reflow at 200 percent text in ${theme}`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: theme });
    await page.setViewportSize({ width: 320, height: 900 });
    await page.addInitScript(() => {
      const enlargeText = () => { document.documentElement.style.fontSize = "200%"; };
      if (document.documentElement) enlargeText();
      else document.addEventListener("DOMContentLoaded", enlargeText, { once: true });
    });
    await page.goto("/vocabulary");
    await expect(page.getByText("79 of 79 entries shown", { exact: true })).toBeVisible();
    await expect(page.locator("html")).toHaveAttribute("data-mantine-color-scheme", theme);
    expect(await page.evaluate(() => getComputedStyle(document.documentElement).fontSize)).toBe("32px");
    expect((await page.request.post("/api/v1/development-session", { headers: PRACTICE_HEADERS, data: {} })).status()).toBe(200);
    const owner = await (await page.request.get("/api/v1/vocabulary-context")).json() as TeachingContext;
    const runId = crypto.randomUUID();
    expect((await page.request.post("/api/v1/practice-sessions", {
      headers: ownerHeaders(owner.ownerSessionId),
      data: { practiceSessionId: runId, focusId: "vocabulary-p3-s1", planRevision: "vocabulary-p3-s1-1", seed: 11 },
    })).status()).toBe(201);
    expect((await page.request.post(`/api/v1/practice-sessions/${runId}/support`, {
      headers: ownerHeaders(owner.ownerSessionId), data: { kind: "introduction" },
    })).status()).toBe(200);
    await page.getByRole("button", { name: "Load saved lesson teaching", exact: true }).click();
    await expect(page.getByText(/selected uses have recorded teaching/)).toBeVisible();
    const search = page.getByRole("textbox", { name: "Search vocabulary", exact: true });
    await search.fill("en cykel");
    await expect(page.getByText("1 of 79 entries shown", { exact: true })).toBeVisible();
    await page.getByRole("button", { name: /^en cykel / }).click();
    await expect(page.getByText("Det er en cykel.", { exact: true })).toBeVisible();
    await expect(page.getByText("Meaning in saved lesson teaching", { exact: true })).toBeVisible();
    const clipped = await page.locator(".mantine-Badge-label, .mantine-Button-label, .mantine-Accordion-label")
      .evaluateAll((elements) => elements.filter((element) => element.getClientRects().length > 0 &&
        (element.scrollWidth > element.clientWidth + 1 || element.scrollHeight > element.clientHeight + 1))
        .map((element) => element.textContent));
    expect(clipped).toEqual([]);
    await scan(page);
    await page.getByRole("combobox", { name: "Situation", exact: true }).click();
    await page.getByRole("option", { name: "Food, cafés and shopping", exact: true }).click();
    await expect(page.getByText("No vocabulary matches these filters.", { exact: true })).toBeVisible();
    await scan(page);
    await page.getByRole("button", { name: "Toggle navigation", exact: true }).click();
    await expect(page.getByRole("navigation").getByRole("link", { name: "Vocabulary", exact: true })).toBeVisible();
    await scan(page);
  });
}

test("vocabulary browsing preserves a pending practice creation without replaying it", async ({ page }) => {
  const creations: string[] = [];
  await page.route("**/api/v1/practice-sessions", async (route) => {
    creations.push(route.request().postData()!);
    if (creations.length === 1) {
      await route.fetch();
      await route.abort("connectionfailed");
    } else await route.continue();
  });
  await page.goto("/");
  await page.getByRole("button", { name: "Start Talking about one person", exact: true }).click();
  await expect(page.getByRole("alert")).toBeVisible();
  expect(creations).toHaveLength(1);
  const savedBefore = await page.evaluate(() => Object.fromEntries(Object.entries(localStorage)
    .filter(([key]) => key.startsWith("astrid.practice.v1."))));
  expect(Object.keys(savedBefore).some((key) => key.includes(".outbox."))).toBe(true);
  await page.getByRole("navigation").getByRole("link", { name: "Vocabulary", exact: true }).click();
  await expect(page.getByText("79 of 79 entries shown", { exact: true })).toBeVisible();
  await page.getByRole("textbox", { name: "Search vocabulary", exact: true }).fill("til stationen");
  await page.getByRole("button", { name: /^til stationen / }).click();
  await page.getByRole("link", { name: "Walking to the station and home", exact: true }).click();
  await expect(page.locator("#lesson-vocabulary-p3-s2")).toBeFocused();
  expect(creations).toHaveLength(1);
  expect(await page.evaluate(() => Object.fromEntries(Object.entries(localStorage)
    .filter(([key]) => key.startsWith("astrid.practice.v1."))))).toEqual(savedBefore);
  await page.getByRole("link", { name: "Resume saved practice", exact: true }).click();
  await expect(page.getByRole("button", { name: "Session introduction", exact: true })).toBeVisible();
  expect(creations).toHaveLength(2);
  expect(creations[1]).toBe(creations[0]);
});
