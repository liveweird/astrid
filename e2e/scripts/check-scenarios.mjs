// The scenario-parity gate (ported from Flow): every spec has its scenario file and every test() title
// appears as a `## Scenario:` heading VERBATIM — and vice versa (the contract in scenarios/README.md).
// Astrid adaptation: many titles are template literals instantiated per theme/viewport/branch, so there is no
// registered-skip list. The heading must equal the title's source text, placeholders included
// (`## Scenario: ... in ${theme}`); see scenario-titles.mjs.
import { readdirSync, readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { extractTitles } from "./scenario-titles.mjs";

const root = dirname(dirname(fileURLToPath(import.meta.url)));
const testsDir = join(root, "tests");
const scenariosDir = join(root, "scenarios");

let failures = 0;
const fail = (msg) => {
  failures += 1;
  console.error(`✗ ${msg}`);
};

const specs = readdirSync(testsDir).filter((f) => f.endsWith(".spec.ts"));
const scenarioFiles = new Set(
  readdirSync(scenariosDir).filter((f) => f.endsWith(".md") && f !== "README.md"),
);

for (const spec of specs) {
  const base = spec.replace(/\.spec\.ts$/, "");
  const scenarioName = `${base}.md`;
  if (!scenarioFiles.has(scenarioName)) {
    fail(`${spec}: missing scenarios/${scenarioName}`);
    continue;
  }
  scenarioFiles.delete(scenarioName);

  const { titles, declarations } = extractTitles(readFileSync(join(testsDir, spec), "utf8"));
  const scenario = readFileSync(join(scenariosDir, scenarioName), "utf8");
  const headings = [...scenario.matchAll(/^## Scenario: (.+)$/gm)].map((m) => m[1].trimEnd());

  for (const t of titles) {
    if (!headings.includes(t)) fail(`${spec}: title has no matching heading in ${scenarioName}:\n    "${t}"`);
  }
  for (const h of headings) {
    if (!titles.includes(h)) fail(`scenarios/${scenarioName}: heading matches no test() title in ${spec}:\n    "${h}"`);
  }
  for (const h of new Set(headings.filter((h, i) => headings.indexOf(h) !== i))) {
    fail(`scenarios/${scenarioName}: duplicate heading:\n    "${h}"`);
  }
  if (titles.length === 0) fail(`${spec}: no test() titles extracted — the checker's regex needs a look`);
  else if (declarations !== titles.length) {
    fail(`${spec}: ${declarations} test( declaration(s) but ${titles.length} string-literal title(s) — use a literal title`);
  }
}

for (const orphan of scenarioFiles) {
  fail(`scenarios/${orphan}: no matching tests/${orphan.replace(/\.md$/, ".spec.ts")}`);
}

if (failures === 0) {
  console.log(`✓ scenario parity: ${specs.length} specs ↔ scenarios in sync`);
} else {
  console.error(`${failures} scenario-parity failure(s)`);
  process.exit(1);
}
