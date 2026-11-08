// Extracts the title SOURCE TEXT of every top-level test declaration in a spec, for the scenario-parity gate
// (check-scenarios.mjs). Double-quoted, single-quoted and backtick titles are all accepted and returned exactly as
// written between the quotes: a template-literal title keeps its `${...}` placeholders verbatim, because the
// generated test names are not knowable without running the spec (scenarios/README.md).
// Each quoted body is the unrolled `[^q\\]*(?:\\.[^q\\]*)*` form — linear, no alternation backtracking.
// A test declared through a modifier (`test.fixme("t", …)`, `.skip`, `.only`, `.fail`, `.slow`) counts too: its first
// argument is a string. The in-test forms (`test.skip()`, `test.skip(condition, "why")`) and `test.step(` do not match.
// Limitations: a `${...}` expression inside a backtick title must not itself contain a backtick, and a title built by
// concatenation (`"a" + "b"`) is read as its first literal only. Write titles as one literal.
const CALL = /(?<![.\w])test(?:\.(?:fixme|skip|only|fail|slow))?\(\s*/;
const QUOTED = /"([^"\\]*(?:\\.[^"\\]*)*)"|'([^'\\]*(?:\\.[^'\\]*)*)'|`([^`\\]*(?:\\.[^`\\]*)*)`/;
const TITLE = new RegExp(`${CALL.source}(?:${QUOTED.source})`, "g");
// Plain `test(` with any first argument, or a modifier call whose first argument is a string.
const DECLARATION = /(?<![.\w])test(?:\(|\.(?:fixme|skip|only|fail|slow)\(\s*["'`])/g;

export function extractTitles(source) {
  const titles = [...source.matchAll(TITLE)].map((m) => m[1] ?? m[2] ?? m[3]);
  const declarations = [...source.matchAll(DECLARATION)].length;
  return { titles, declarations };
}
