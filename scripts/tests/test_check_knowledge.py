import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).resolve().parents[1] / "check-knowledge.py"
SPEC = importlib.util.spec_from_file_location("check_knowledge", SCRIPT)
check_knowledge = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
sys.modules[SPEC.name] = check_knowledge
SPEC.loader.exec_module(check_knowledge)


class KnowledgeCheckTests(unittest.TestCase):
    def setUp(self):
        self.temporary_directory = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary_directory.name)
        self.knowledge = self.root / "docs" / "knowledge"
        self.knowledge.mkdir(parents=True)
        self.write("README.md", "[Knowledge](docs/knowledge/beginner-slice-spec.md)\n")
        self.write("BACKLOG.md", "# Backlog\n")
        self.write(
            "docs/knowledge/danish-grammar-map.md",
            """# Grammar
### G001 First
**Prerequisites:** none. **Objective:** First. [S1]
### G002 Second
**Prerequisites:** G001. **Objective:** Second. [S1]
## Sources
- **S1 — Source:** Example.
""",
        )
        self.write(
            "docs/knowledge/danish-sentence-catalogue.md",
            "# Catalogue\n### C001 Sentence\nDepends on G001–G002.\n",
        )
        self.write(
            "docs/knowledge/danish-vocabulary-seed.md",
            "# Vocabulary\n| ID | Word |\n| --- | --- |\n| L001 | ord | C001 |\n",
        )
        self.write(
            "docs/knowledge/beginner-slice-spec.md",
            "# Slice\nUses G001–G002, C001, and L001.\n",
        )

    def tearDown(self):
        self.temporary_directory.cleanup()

    def write(self, relative_path, contents):
        path = self.root / relative_path
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(contents, encoding="utf-8")

    def messages(self):
        return [problem.message for problem in check_knowledge.check_repository(self.root).problems]

    def test_valid_fixture_reports_discovered_counts(self):
        result = check_knowledge.check_repository(self.root)
        self.assertTrue(result.ok, [problem.message for problem in result.problems])
        self.assertEqual(result.counts["grammar_skills"], 2)
        self.assertEqual(result.counts["constructions"], 1)
        self.assertEqual(result.counts["vocabulary_entries"], 1)

    def test_title_only_foundation_docs_cannot_pass_with_empty_inventories(self):
        for relative_path, title in (
            ("docs/knowledge/danish-grammar-map.md", "# Grammar\n"),
            ("docs/knowledge/danish-sentence-catalogue.md", "# Catalogue\n"),
            ("docs/knowledge/danish-vocabulary-seed.md", "# Vocabulary\n"),
            ("docs/knowledge/beginner-slice-spec.md", "# Slice\n"),
        ):
            self.write(relative_path, title)
        messages = self.messages()
        self.assertTrue(any("no G skill definitions" in message for message in messages))
        self.assertTrue(any("no C construction definitions" in message for message in messages))
        self.assertTrue(any("no L vocabulary entry definitions" in message for message in messages))
        self.assertTrue(any("no reference to a defined grammar skill" in message for message in messages))
        self.assertTrue(any("no reference to a defined construction" in message for message in messages))
        self.assertTrue(any("no reference to a defined vocabulary entry" in message for message in messages))

    def test_optional_coverage_tables_match_dynamic_catalogues(self):
        self.write(
            "docs/knowledge/foundation-coverage.md",
            """# Coverage
| Grammar | Construction |
| --- | --- |
| G001 | C001 |
| G002 | C001 |

| Construction | Grammar |
| --- | --- |
| C001 | G001, G002, L001 |
""",
        )
        result = check_knowledge.check_repository(self.root)
        self.assertTrue(result.ok, [problem.message for problem in result.problems])
        self.assertEqual(result.counts["coverage_grammar_rows"], 2)
        self.assertEqual(result.counts["coverage_construction_rows"], 1)

    def test_optional_coverage_reports_missing_duplicate_and_unknown_rows(self):
        self.write(
            "docs/knowledge/foundation-coverage.md",
            """# Coverage
| G001 | C001 |
| G001 | C999 |
| G999 | C001 |
| C999 | G001 |
| C999 | G002 |
""",
        )
        messages = self.messages()
        self.assertTrue(any("duplicate G001" in message for message in messages))
        self.assertTrue(any("duplicate C999" in message for message in messages))
        self.assertTrue(any("undefined identifier G999" in message for message in messages))
        self.assertTrue(any("undefined identifier C999" in message for message in messages))
        self.assertTrue(any("missing row for G002" in message for message in messages))
        self.assertTrue(any("missing row for C001" in message for message in messages))
        self.assertTrue(any("reference to undefined G999" in message for message in messages))
        self.assertTrue(any("reference to undefined C999" in message for message in messages))

    def test_duplicate_and_missing_definitions_are_reported(self):
        path = "docs/knowledge/danish-sentence-catalogue.md"
        self.write(path, "# Catalogue\n### C001 One\n### C001 Again\nUses C999.\n")
        messages = self.messages()
        self.assertTrue(any("duplicate C001" in message for message in messages))
        self.assertTrue(any("undefined C999" in message for message in messages))

    def test_wrong_heading_prefix_does_not_define_cross_catalogue_id(self):
        grammar_path = "docs/knowledge/danish-grammar-map.md"
        self.write(
            grammar_path,
            """# Grammar
### G001 First
**Prerequisites:** none. **Objective:** First. [S1]
### C999 Misplaced construction
### G002 Second
**Prerequisites:** G001. **Objective:** Second. [S1]
## Sources
- **S1 — Source:** Example.
""",
        )
        catalogue_path = "docs/knowledge/danish-sentence-catalogue.md"
        self.write(catalogue_path, "# Catalogue\n### C001 Sentence\n### G999 Misplaced skill\n")
        result = check_knowledge.check_repository(self.root)
        messages = [problem.message for problem in result.problems]
        self.assertTrue(any("misplaced C999" in message for message in messages))
        self.assertTrue(any("misplaced G999" in message for message in messages))
        self.assertEqual(result.counts["grammar_skills"], 2)
        self.assertEqual(result.counts["constructions"], 1)

    def test_unknown_prerequisite_and_reference_are_reported(self):
        path = "docs/knowledge/danish-grammar-map.md"
        self.write(
            path,
            """# Grammar
### G001 First
**Prerequisites:** G999. **Objective:** First. [S1]
### G002 Second
**Prerequisites:** G001. **Objective:** Uses L999. [S1]
## Sources
- **S1 — Source:** Example.
""",
        )
        messages = self.messages()
        self.assertTrue(any("undefined prerequisite G999" in message for message in messages))
        self.assertTrue(any("undefined L999" in message for message in messages))

    def test_self_dependency_and_cycle_are_reported(self):
        path = "docs/knowledge/danish-grammar-map.md"
        self.write(
            path,
            """# Grammar
### G001 First
**Prerequisites:** G001. **Objective:** First. [S1]
### G002 Second
**Prerequisites:** G003. **Objective:** Second. [S1]
### G003 Third
**Prerequisites:** G002. **Objective:** Third. [S1]
## Sources
- **S1 — Source:** Example.
""",
        )
        messages = self.messages()
        self.assertTrue(any("lists itself" in message for message in messages))
        self.assertTrue(any("cyclic prerequisites" in message for message in messages))

    def test_missing_prerequisite_declaration_is_reported(self):
        path = "docs/knowledge/danish-grammar-map.md"
        self.write(
            path,
            """# Grammar
### G001 First
**Objective:** First. [S1]
## Sources
- **S1 — Source:** Example.
""",
        )
        self.assertTrue(
            any("has no **Prerequisites:**" in message for message in self.messages())
        )

    def test_duplicate_and_undefined_sources_are_reported(self):
        path = "docs/knowledge/danish-grammar-map.md"
        self.write(
            path,
            """# Grammar
### G001 First
**Prerequisites:** none. **Objective:** First. [S1, S999]
## Sources
- **S1 — Source:** Example.
- **S1 — Duplicate:** Example.
""",
        )
        messages = self.messages()
        self.assertTrue(any("duplicate S1" in message for message in messages))
        self.assertTrue(any("undefined source S999" in message for message in messages))

    def test_missing_relative_markdown_file_is_reported(self):
        self.write("README.md", "[Missing](docs/does-not-exist.md)\n")
        messages = self.messages()
        self.assertTrue(any("does-not-exist.md" in message for message in messages))


if __name__ == "__main__":
    unittest.main()
