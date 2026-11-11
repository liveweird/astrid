from __future__ import annotations

import os
from pathlib import Path
import stat
import subprocess
import tempfile
import textwrap
import unittest


ROOT = Path(__file__).resolve().parents[2]
RUNNER = ROOT / "scripts/run-browser-tests.sh"


class BrowserRunnerTest(unittest.TestCase):
    def run_with_fake_docker(
        self,
        *,
        exit_code: int,
        inspect_owner: str = "recorded",
        inspect_exit: int = 0,
        base_url: str | None = None,
        remove_exit: int = 0,
    ):
        with tempfile.TemporaryDirectory() as temporary:
            scratch = Path(temporary)
            binary_dir = scratch / "bin"
            binary_dir.mkdir()
            log = scratch / "docker.log"
            owner_file = scratch / "owner"
            node = scratch / "node"
            node.write_text("linux node placeholder\n")
            node.chmod(node.stat().st_mode | stat.S_IXUSR)

            docker = binary_dir / "docker"
            docker.write_text(
                textwrap.dedent(
                    """\
                    #!/usr/bin/env bash
                    set -eu
                    printf '%q ' "$@" >> "$FAKE_DOCKER_LOG"
                    printf '\n' >> "$FAKE_DOCKER_LOG"
                    command_name="$1"
                    shift
                    case "$command_name" in
                      run)
                        while (($#)); do
                          case "$1" in
                            --cidfile)
                              printf 'owned-container-id' > "$2"
                              shift 2
                              ;;
                            --label)
                              printf '%s' "${2#astrid.dev/browser-run=}" > "$FAKE_DOCKER_OWNER"
                              shift 2
                              ;;
                            --mount)
                              if [[ "$2" == *,dst=/artifacts ]]; then
                                artifact_root="${2#*src=}"
                                artifact_root="${artifact_root%%,dst=*}"
                                mkdir -p "$artifact_root/test-results" "$artifact_root/playwright-report"
                                printf 'trace' > "$artifact_root/test-results/result.txt"
                                printf 'report' > "$artifact_root/playwright-report/index.html"
                              fi
                              shift 2
                              ;;
                            *) shift ;;
                          esac
                        done
                        exit "$FAKE_DOCKER_EXIT"
                        ;;
                      inspect)
                        if [[ "$FAKE_DOCKER_INSPECT_EXIT" != 0 ]]; then
                          exit "$FAKE_DOCKER_INSPECT_EXIT"
                        fi
                        if [[ "$FAKE_DOCKER_INSPECT_OWNER" == recorded ]]; then
                          cat "$FAKE_DOCKER_OWNER"
                        else
                          printf '%s\n' "$FAKE_DOCKER_INSPECT_OWNER"
                        fi
                        ;;
                      rm) exit "$FAKE_DOCKER_REMOVE_EXIT" ;;
                      *) exit 99 ;;
                    esac
                    """
                )
            )
            docker.chmod(docker.stat().st_mode | stat.S_IXUSR)

            env = os.environ.copy()
            env.update(
                {
                    "PATH": f"{binary_dir}:{env['PATH']}",
                    "ASTRID_BROWSER_NODE": str(node),
                    "FAKE_DOCKER_LOG": str(log),
                    "FAKE_DOCKER_OWNER": str(owner_file),
                    "FAKE_DOCKER_EXIT": str(exit_code),
                    "FAKE_DOCKER_INSPECT_OWNER": inspect_owner,
                    "FAKE_DOCKER_INSPECT_EXIT": str(inspect_exit),
                    "FAKE_DOCKER_REMOVE_EXIT": str(remove_exit),
                    "GITHUB_RUN_ID": "test-run",
                    "GITHUB_RUN_ATTEMPT": "1",
                    "ASTRID_BROWSER_OUTPUT_ROOT": str(scratch / "output"),
                }
            )
            if base_url is not None:
                env["E2E_BASE_URL"] = base_url
            else:
                env.pop("E2E_BASE_URL", None)
            result = subprocess.run(
                [str(RUNNER), "tests/changelog.spec.ts", "--project=webkit"],
                cwd=ROOT,
                env=env,
                text=True,
                capture_output=True,
                check=False,
            )
            exported = {
                "result": (scratch / "output/test-results/result.txt").read_text(),
                "report": (scratch / "output/playwright-report/index.html").read_text(),
            }
            return result, log.read_text(), exported

    def test_preserves_browser_failure_and_removes_only_owned_container(self):
        result, calls, exported = self.run_with_fake_docker(exit_code=23)

        self.assertEqual(result.returncode, 23, result.stderr)
        run_call, inspect_call, remove_call = calls.splitlines()
        self.assertIn("run ", run_call)
        self.assertIn("--network host", run_call)
        self.assertIn("--platform linux/amd64", run_call)
        self.assertIn("--shm-size 1g", run_call)
        self.assertIn("dst=/work\\,readonly", run_call)
        self.assertIn("dst=/artifacts", run_call)
        self.assertIn("dst=/astrid-node/node\\,readonly", run_call)
        self.assertIn("dst=/astrid-node/preflight.cjs\\,readonly", run_call)
        self.assertIn("ASTRID_BROWSER_NODE_VERSION=24.22.1", run_call)
        self.assertIn("NODE_OPTIONS=--require=/astrid-node/preflight.cjs", run_call)
        self.assertIn("PLAYWRIGHT_HTML_OUTPUT_DIR=/artifacts/playwright-report", run_call)
        self.assertIn("--output=/artifacts/test-results", run_call)
        self.assertIn("E2E_BASE_URL=http://127.0.0.1:8088", run_call)
        self.assertIn("tests/changelog.spec.ts --project=webkit", run_call)
        self.assertIn("inspect ", inspect_call)
        self.assertEqual(remove_call, "rm -f owned-container-id ")
        self.assertEqual(exported, {"result": "trace", "report": "report"})

    def test_forwards_an_explicit_stack_base_url(self):
        result, calls, _ = self.run_with_fake_docker(
            exit_code=0, base_url="http://127.0.0.1:51466"
        )

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("E2E_BASE_URL=http://127.0.0.1:51466", calls.splitlines()[0])

    def test_cleanup_failure_turns_a_successful_browser_run_into_a_failure(self):
        result, calls, _ = self.run_with_fake_docker(exit_code=0, remove_exit=19)

        self.assertEqual(result.returncode, 1)
        self.assertIn("Failed to remove owned browser container", result.stderr)
        self.assertEqual(calls.splitlines()[-1], "rm -f owned-container-id ")

    def test_cleanup_failure_does_not_hide_the_browser_failure_status(self):
        result, _, _ = self.run_with_fake_docker(exit_code=23, remove_exit=19)

        self.assertEqual(result.returncode, 23)
        self.assertIn("Failed to remove owned browser container", result.stderr)

    def test_inspect_failure_turns_success_into_failure_without_removing_container(self):
        result, calls, exported = self.run_with_fake_docker(exit_code=0, inspect_exit=31)

        self.assertEqual(result.returncode, 1)
        self.assertIn("Unable to verify ownership", result.stderr)
        self.assertEqual(len(calls.splitlines()), 2)
        self.assertEqual(exported, {"result": "trace", "report": "report"})

    def test_refuses_to_remove_container_when_ownership_label_differs(self):
        result, calls, _ = self.run_with_fake_docker(exit_code=17, inspect_owner="another-run")

        self.assertEqual(result.returncode, 17)
        self.assertIn("ownership label differs", result.stderr)
        self.assertEqual(len(calls.splitlines()), 2)


if __name__ == "__main__":
    unittest.main()
