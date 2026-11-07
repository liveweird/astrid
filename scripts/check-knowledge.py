#!/usr/bin/env python3
"""Fast, offline integrity checks for Astrid's Markdown knowledge base."""

from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, Iterable, List, Mapping, Sequence, Set, Tuple
from urllib.parse import unquote, urlsplit


FOUNDATION_PATHS = {
    "grammar": Path("docs/knowledge/danish-grammar-map.md"),
    "constructions": Path("docs/knowledge/danish-sentence-catalogue.md"),
    "vocabulary": Path("docs/knowledge/danish-vocabulary-seed.md"),
    "slice": Path("docs/knowledge/beginner-slice-spec.md"),
}
COVERAGE_PATH = Path("docs/knowledge/foundation-coverage.md")

HEADING_RE = re.compile(r"^###\s+([GC]\d{3})\b")
LEXICON_ROW_RE = re.compile(r"^\s*\|\s*`?(L\d{3})`?\s*\|")
PREREQUISITES_RE = re.compile(r"\*\*Prerequisites:\*\*\s*([^\n]*?)(?=\s+\*\*|$)")
ID_REFERENCE_RE = re.compile(
    r"\b([GCL])(\d{3})(?:\s*[\u2013-]\s*(?:([GCL])?\s*)?(\d{3}))?\b"
)
SOURCE_DEFINITION_RE = re.compile(r"^\s*-\s+\*\*(S\d+)\s+[\u2013\u2014-]")
SOURCE_REFERENCE_RE = re.compile(r"\bS\d+\b")
MARKDOWN_LINK_RE = re.compile(r"(?<!!)\[[^\]\n]*\]\(([^)\n]+)\)")
REFERENCE_LINK_RE = re.compile(r"^\s*\[[^\]]+\]:\s*(\S+)")
COVERAGE_ROW_RE = re.compile(r"^\s*\|\s*`?([GC]\d{3})`?\s*\|")


@dataclass(frozen=True)
class Problem:
    path: Path
    line: int
    message: str

    def format(self, root: Path) -> str:
        try:
            display_path = self.path.relative_to(root)
        except ValueError:
            display_path = self.path
        return f"{display_path}:{self.line}: {self.message}"


@dataclass
class CheckResult:
    counts: Dict[str, int] = field(default_factory=dict)
    problems: List[Problem] = field(default_factory=list)

    @property
    def ok(self) -> bool:
        return not self.problems


def _lines(path: Path) -> List[str]:
    return path.read_text(encoding="utf-8").splitlines()


def _definitions(
    path: Path,
    lines: Sequence[str],
    pattern: re.Pattern[str],
    kind: str = "definition",
) -> Tuple[Dict[str, int], List[Problem]]:
    definitions: Dict[str, int] = {}
    problems: List[Problem] = []
    for line_number, line in enumerate(lines, 1):
        match = pattern.match(line)
        if not match:
            continue
        identifier = match.group(1)
        if identifier in definitions:
            problems.append(
                Problem(
                    path,
                    line_number,
                    f"duplicate {identifier} {kind} (first found on line "
                    f"{definitions[identifier]})",
                )
            )
        else:
            definitions[identifier] = line_number
    return definitions, problems


def _heading_definitions(
    path: Path, lines: Sequence[str], expected_prefix: str
) -> Tuple[Dict[str, int], List[Problem]]:
    """Collect headings belonging to one catalogue and reject swapped namespaces."""
    definitions: Dict[str, int] = {}
    problems: List[Problem] = []
    for line_number, line in enumerate(lines, 1):
        match = HEADING_RE.match(line)
        if not match:
            continue
        identifier = match.group(1)
        if not identifier.startswith(expected_prefix):
            problems.append(
                Problem(
                    path,
                    line_number,
                    f"misplaced {identifier} heading; this document defines "
                    f"{expected_prefix} identifiers",
                )
            )
            continue
        if identifier in definitions:
            problems.append(
                Problem(
                    path,
                    line_number,
                    f"duplicate {identifier} definition (first defined on line "
                    f"{definitions[identifier]})",
                )
            )
        else:
            definitions[identifier] = line_number
    return definitions, problems


def _grammar_records(
    path: Path, lines: Sequence[str]
) -> Tuple[Dict[str, Set[str]], List[Problem]]:
    records: Dict[str, Set[str]] = {}
    problems: List[Problem] = []
    all_headings = [
        (index, match.group(1))
        for index, line in enumerate(lines)
        if (match := HEADING_RE.match(line))
    ]
    for position, (start, identifier) in enumerate(all_headings):
        if not identifier.startswith("G"):
            continue
        end = (
            all_headings[position + 1][0]
            if position + 1 < len(all_headings)
            else len(lines)
        )
        section = "\n".join(lines[start + 1 : end])
        declaration = PREREQUISITES_RE.search(section)
        if not declaration:
            problems.append(
                Problem(path, start + 1, f"{identifier} has no **Prerequisites:** declaration")
            )
            records.setdefault(identifier, set())
            continue
        value = declaration.group(1).strip()
        prerequisites = set(re.findall(r"\bG\d{3}\b", value))
        if value.lower().rstrip(".") != "none" and not prerequisites:
            declaration_line = start + 2 + section[: declaration.start()].count("\n")
            problems.append(
                Problem(path, declaration_line, f"{identifier} has an unreadable prerequisite list")
            )
        records.setdefault(identifier, set()).update(prerequisites)
    return records, problems


def _reference_ids(line: str) -> Iterable[str]:
    for match in ID_REFERENCE_RE.finditer(line):
        prefix, start, end_prefix, end = match.groups()
        yield prefix + start
        if end:
            yield (end_prefix or prefix) + end


def _check_references(
    documents: Mapping[Path, Sequence[str]], known: Set[str]
) -> List[Problem]:
    problems: List[Problem] = []
    for path, lines in documents.items():
        for line_number, line in enumerate(lines, 1):
            for identifier in _reference_ids(line):
                if identifier not in known:
                    problems.append(
                        Problem(path, line_number, f"reference to undefined {identifier}")
                    )
    return problems


def _check_prerequisites(
    path: Path, definitions: Mapping[str, int], graph: Mapping[str, Set[str]]
) -> List[Problem]:
    problems: List[Problem] = []
    for skill, prerequisites in graph.items():
        for prerequisite in sorted(prerequisites):
            if prerequisite not in definitions:
                problems.append(
                    Problem(path, definitions.get(skill, 1), f"{skill} has undefined prerequisite {prerequisite}")
                )
            if prerequisite == skill:
                problems.append(
                    Problem(path, definitions.get(skill, 1), f"{skill} lists itself as a prerequisite")
                )

    state: Dict[str, int] = {}
    stack: List[str] = []
    reported: Set[frozenset[str]] = set()

    def visit(skill: str) -> None:
        state[skill] = 1
        stack.append(skill)
        for prerequisite in sorted(graph.get(skill, set())):
            if prerequisite not in definitions or prerequisite == skill:
                continue
            if state.get(prerequisite, 0) == 0:
                visit(prerequisite)
            elif state.get(prerequisite) == 1:
                cycle_start = stack.index(prerequisite)
                cycle = stack[cycle_start:] + [prerequisite]
                signature = frozenset(cycle)
                if signature not in reported:
                    reported.add(signature)
                    problems.append(
                        Problem(
                            path,
                            definitions.get(skill, 1),
                            "cyclic prerequisites: " + " -> ".join(cycle),
                        )
                    )
        stack.pop()
        state[skill] = 2

    for skill in definitions:
        if state.get(skill, 0) == 0:
            visit(skill)
    return problems


def _check_sources(path: Path, lines: Sequence[str]) -> Tuple[int, List[Problem]]:
    definitions, problems = _definitions(path, lines, SOURCE_DEFINITION_RE)
    for line_number, line in enumerate(lines, 1):
        definition = SOURCE_DEFINITION_RE.match(line)
        definition_id = definition.group(1) if definition else None
        for identifier in SOURCE_REFERENCE_RE.findall(line):
            if identifier == definition_id:
                continue
            if identifier not in definitions:
                problems.append(
                    Problem(path, line_number, f"reference to undefined source {identifier}")
                )
    return len(definitions), problems


def _check_coverage_rows(
    path: Path,
    lines: Sequence[str],
    grammar: Mapping[str, int],
    constructions: Mapping[str, int],
) -> Tuple[Dict[str, int], List[Problem]]:
    rows, problems = _definitions(path, lines, COVERAGE_ROW_RE, "coverage row")
    expected = set(grammar) | set(constructions)
    for identifier, line_number in rows.items():
        if identifier not in expected:
            problems.append(
                Problem(path, line_number, f"coverage row has undefined identifier {identifier}")
            )
    for identifier in sorted(expected - set(rows)):
        problems.append(
            Problem(path, 1, f"coverage table is missing row for {identifier}")
        )
    counts = {
        "coverage_grammar_rows": sum(identifier.startswith("G") for identifier in rows),
        "coverage_construction_rows": sum(identifier.startswith("C") for identifier in rows),
    }
    return counts, problems


def _local_link_target(raw_target: str) -> str | None:
    target = raw_target.strip()
    if target.startswith("<") and ">" in target:
        target = target[1 : target.index(">")]
    else:
        target = target.split(maxsplit=1)[0]
    if not target or target.startswith(("#", "/", "//")):
        return None
    parsed = urlsplit(target)
    if parsed.scheme or parsed.netloc:
        return None
    return unquote(parsed.path) or None


def markdown_files(root: Path) -> List[Path]:
    files = [path for path in (root / "README.md", root / "BACKLOG.md") if path.is_file()]
    docs = root / "docs"
    if docs.is_dir():
        files.extend(sorted(docs.rglob("*.md")))
    return files


def check_markdown_links(root: Path, paths: Iterable[Path]) -> List[Problem]:
    problems: List[Problem] = []
    for path in paths:
        for line_number, line in enumerate(_lines(path), 1):
            raw_targets = [match.group(1) for match in MARKDOWN_LINK_RE.finditer(line)]
            reference = REFERENCE_LINK_RE.match(line)
            if reference:
                raw_targets.append(reference.group(1))
            for raw_target in raw_targets:
                target = _local_link_target(raw_target)
                if target is None:
                    continue
                resolved = (path.parent / target).resolve()
                try:
                    resolved.relative_to(root.resolve())
                except ValueError:
                    problems.append(
                        Problem(path, line_number, f"local link escapes repository: {target}")
                    )
                    continue
                if not resolved.exists():
                    problems.append(
                        Problem(path, line_number, f"local link target does not exist: {target}")
                    )
    return problems


def check_repository(root: Path) -> CheckResult:
    root = root.resolve()
    result = CheckResult()
    documents: Dict[Path, List[str]] = {}
    for relative_path in FOUNDATION_PATHS.values():
        path = root / relative_path
        if not path.is_file():
            result.problems.append(Problem(path, 1, "required foundation document is missing"))
        else:
            documents[path] = _lines(path)
    if len(documents) != len(FOUNDATION_PATHS):
        return result
    coverage_path = root / COVERAGE_PATH
    if coverage_path.is_file():
        documents[coverage_path] = _lines(coverage_path)

    grammar_path = root / FOUNDATION_PATHS["grammar"]
    construction_path = root / FOUNDATION_PATHS["constructions"]
    vocabulary_path = root / FOUNDATION_PATHS["vocabulary"]

    grammar, grammar_duplicates = _heading_definitions(
        grammar_path, documents[grammar_path], "G"
    )
    constructions, construction_duplicates = _heading_definitions(
        construction_path, documents[construction_path], "C"
    )
    vocabulary, vocabulary_duplicates = _definitions(
        vocabulary_path, documents[vocabulary_path], LEXICON_ROW_RE
    )
    result.problems.extend(grammar_duplicates + construction_duplicates + vocabulary_duplicates)
    result.counts.update(
        grammar_skills=len(grammar),
        constructions=len(constructions),
        vocabulary_entries=len(vocabulary),
    )
    for path, definitions, description in (
        (grammar_path, grammar, "G skill"),
        (construction_path, constructions, "C construction"),
        (vocabulary_path, vocabulary, "L vocabulary entry"),
    ):
        if not definitions:
            result.problems.append(
                Problem(path, 1, f"no {description} definitions found")
            )

    graph, record_problems = _grammar_records(grammar_path, documents[grammar_path])
    result.problems.extend(record_problems)
    result.problems.extend(_check_prerequisites(grammar_path, grammar, graph))
    known = set(grammar) | set(constructions) | set(vocabulary)
    result.problems.extend(_check_references(documents, known))
    if coverage_path in documents:
        coverage_counts, coverage_problems = _check_coverage_rows(
            coverage_path, documents[coverage_path], grammar, constructions
        )
        result.counts.update(coverage_counts)
        result.problems.extend(coverage_problems)
    slice_path = root / FOUNDATION_PATHS["slice"]
    slice_references = {
        identifier
        for line in documents[slice_path]
        for identifier in _reference_ids(line)
    }
    for prefix, description in (
        ("G", "grammar skill"),
        ("C", "construction"),
        ("L", "vocabulary entry"),
    ):
        if not any(
            identifier.startswith(prefix) and identifier in known
            for identifier in slice_references
        ):
            result.problems.append(
                Problem(
                    slice_path,
                    1,
                    f"beginner slice has no reference to a defined {description}",
                )
            )
    source_count, source_problems = _check_sources(grammar_path, documents[grammar_path])
    result.counts["grammar_sources"] = source_count
    result.problems.extend(source_problems)

    markdown = markdown_files(root)
    result.counts["markdown_files"] = len(markdown)
    result.problems.extend(check_markdown_links(root, markdown))
    return result


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "root",
        nargs="?",
        type=Path,
        default=Path(__file__).resolve().parent.parent,
        help="repository root (defaults to this script's parent repository)",
    )
    args = parser.parse_args(argv)
    result = check_repository(args.root)
    summary = ", ".join(name.replace("_", " ") + f": {count}" for name, count in result.counts.items())
    print(f"Knowledge inventory: {summary or 'unavailable'}")
    if result.problems:
        for problem in result.problems:
            print(problem.format(args.root.resolve()), file=sys.stderr)
        print(f"Knowledge check failed with {len(result.problems)} problem(s).", file=sys.stderr)
        return 1
    print("Knowledge check passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
