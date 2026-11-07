import { describe, expect, test } from "vitest";
import { CHANGELOG } from "./entries";
import { APP_VERSION } from "./version";

describe("changelog entries", () => {
  test("the application version matches the newest entry", () => {
    expect(CHANGELOG[0]?.version).toBe(APP_VERSION);
  });

  test("entries have unique semantic versions, descending dates, and authored bodies", () => {
    expect(new Set(CHANGELOG.map((entry) => entry.version)).size).toBe(CHANGELOG.length);
    for (const [index, entry] of CHANGELOG.entries()) {
      expect(entry.version).toMatch(/^\d+\.\d+\.\d+$/);
      expect(entry.date).toMatch(/^\d{4}-\d{2}-\d{2}$/);
      expect(entry.body.trim()).not.toBe("");
      if (index > 0) expect(CHANGELOG[index - 1]!.date >= entry.date).toBe(true);
    }
  });
});
