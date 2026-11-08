import assert from "node:assert/strict";
import { test } from "node:test";
import { extractTitles } from "./scenario-titles.mjs";

test("extracts double-quoted, single-quoted and template titles verbatim", () => {
  const source = [
    'test("plain title", async () => {});',
    "test('single \\' quoted', async () => {});",
    "  test(`reflows in ${theme}${retry ? \" again\" : \"\"}`, async () => {});",
    'test(\n  "title on the next line",\n  async () => {});',
  ].join("\n");
  assert.deepEqual(extractTitles(source), {
    titles: ["plain title", "single \\' quoted", 'reflows in ${theme}${retry ? " again" : ""}', "title on the next line"],
    declarations: 4,
  });
});

test("counts tests declared through a modifier but not in-test skips or steps", () => {
  const source = [
    'test.fixme("not yet", () => {});',
    "test.skip(`skipped in ${theme}`, () => {});",
    'test.only("focused", () => {});',
    'test.skip(browserName === "webkit", "why");',
    "test.skip();",
    'await test.step("step", () => {});',
  ].join("\n");
  assert.deepEqual(extractTitles(source), { titles: ["not yet", "skipped in ${theme}", "focused"], declarations: 3 });
});

test("reports a declaration without a literal title", () => {
  assert.deepEqual(extractTitles("test(name, () => {});"), { titles: [], declarations: 1 });
});
