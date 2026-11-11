from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[2]


class ToolchainPinTest(unittest.TestCase):
    def test_rejects_playwright_image_digest_drift(self):
        files = [
            ".nvmrc",
            "Dockerfile",
            "mise.toml",
            "docker-compose.yaml",
            "e2e/package.json",
            "e2e/package-lock.json",
            "k8s/postgres.yaml",
            "scripts/check-toolchain.py",
            "scripts/run-browser-tests.sh",
            "server/src/test/kotlin/app/astrid/server/PostgresTestDatabase.kt",
        ]
        files.extend(
            str(path.relative_to(ROOT)) for path in (ROOT / ".github/workflows").glob("*.yml")
        )

        with tempfile.TemporaryDirectory() as temporary:
            checkout = Path(temporary)
            for relative in files:
                source = ROOT / relative
                destination = checkout / relative
                destination.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(source, destination)

            runner = checkout / "scripts/run-browser-tests.sh"
            runner.write_text(
                runner.read_text().replace(
                    "2f05f8a734509a7b3f26a912639488ee08f58a7b292d14d62271bcff9038768f",
                    "0" * 64,
                )
            )
            result = subprocess.run(
                ["python3", str(checkout / "scripts/check-toolchain.py")],
                text=True,
                capture_output=True,
                check=False,
            )

        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Browser runner Playwright image pin differs", result.stderr)


if __name__ == "__main__":
    unittest.main()
