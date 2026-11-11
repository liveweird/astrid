#!/usr/bin/env python3
"""Keep locally tested runtime pins aligned with containers and CI."""
from pathlib import Path
import json
import re

root = Path(__file__).resolve().parents[1]
tool_config = (root / "mise.toml").read_text()


def tool_pin(name):
    # The shared toolchain file deliberately contains simple quoted [tools] pins.
    match = re.search(rf'^\s*{name}\s*=\s*"([^"\n]+)"\s*$', tool_config, re.MULTILINE)
    assert match, f"Expected an exact quoted {name} pin in mise.toml"
    return match[1]


node = tool_pin("node")
# Ported from Flow's check-toolchain-pins.mjs. Adoptium folds a respin into mise's build number (21.0.12.1+1 is
# temurin-21.0.12+101: respin x 100 + build), while the Docker tag names the respin (21.0.12.1_1) and setup-java takes the patch only.
java_match = re.fullmatch(r"temurin-(\d+\.\d+\.\d+)\+(\d+)(?:\.\d+)?\.LTS", tool_pin("java"))
assert java_match, "Expected a temurin-<patch>+<build>.LTS java pin in mise.toml"
java_patch = java_match[1]
java_respin = int(java_match[2]) // 100
java_tag = f"{java_patch}.{java_respin}" if java_respin else java_patch
assert (root / ".nvmrc").read_text().strip() == node, ".nvmrc differs from mise.toml"
dockerfile = (root / "Dockerfile").read_text()
assert f"FROM node:{node}-alpine@sha256:" in dockerfile, "Node image pin differs"
temurin_images = re.findall(r"^FROM eclipse-temurin:([\d.]+)_\d+-(jdk|jre)-noble@sha256:", dockerfile, re.MULTILINE)
assert {kind for _, kind in temurin_images} == {"jdk", "jre"}, "Dockerfile needs pinned Temurin JDK and JRE stages"
for tag, kind in temurin_images:
    assert tag == java_tag, f"{kind.upper()} image pin differs: {tag} != {java_tag}"
ci = (root / ".github/workflows/ci.yml").read_text()
assert f"java-version: {java_patch}" in ci, "CI JDK differs"
assert "node-version-file: .nvmrc" in ci, "CI must read the Node pin"
ubuntu_mirror = "https://mirrors.edge.kernel.org/ubuntu/"
assert 'ARG ASTRID_UBUNTU_MIRROR=""' in dockerfile, "Dockerfile needs an opt-in Ubuntu mirror argument"
assert f'[ "$ASTRID_UBUNTU_MIRROR" = "{ubuntu_mirror}" ]' in dockerfile, (
    "Dockerfile Ubuntu mirror allowlist differs"
)
for workflow_name in ("ci.yml", "e2e.yml"):
    workflow = (root / ".github/workflows" / workflow_name).read_text()
    expected_arg = f"--build-arg ASTRID_UBUNTU_MIRROR={ubuntu_mirror}"
    assert workflow.count(expected_arg) == 1, f"{workflow_name} Ubuntu mirror build argument differs"

e2e_manifest = json.loads((root / "e2e/package.json").read_text())
e2e_lock = json.loads((root / "e2e/package-lock.json").read_text())
playwright_version = e2e_manifest["devDependencies"]["@playwright/test"]
assert e2e_lock["packages"][""]["devDependencies"]["@playwright/test"] == playwright_version, (
    "Playwright package manifest and lock root differ"
)
assert e2e_lock["packages"]["node_modules/@playwright/test"]["version"] == playwright_version, (
    "Locked Playwright package differs from the manifest"
)
browser_runner = (root / "scripts/run-browser-tests.sh").read_text()
expected_browser_image = (
    "mcr.microsoft.com/playwright:v1.64.1-noble@"
    "sha256:2f05f8a734509a7b3f26a912639488ee08f58a7b292d14d62271bcff9038768f"
)
browser_image_matches = re.findall(
    r'mcr\.microsoft\.com/playwright:v[\d.]+-noble@sha256:[0-9a-f]{64}', browser_runner
)
assert browser_image_matches == [expected_browser_image], "Browser runner Playwright image pin differs"
image_version = re.search(r"playwright:v([\d.]+)-noble", expected_browser_image).group(1)
assert image_version == playwright_version, "Playwright image and package versions differ"
assert re.search(rf'^readonly REQUIRED_NODE_VERSION="{re.escape(node)}"$', browser_runner, re.MULTILINE), (
    "Browser runner Node version differs from .nvmrc"
)
for workflow_name in ("ci.yml", "e2e.yml"):
    workflow = (root / ".github/workflows" / workflow_name).read_text()
    assert workflow.count("scripts/run-browser-tests.sh") == 1, (
        f"{workflow_name} must use the shared browser runner exactly once"
    )
    assert "playwright install" not in workflow, f"{workflow_name} must not install browsers on the host"
for line in dockerfile.splitlines():
    if line.startswith("FROM "):
        assert re.search(r"@sha256:[0-9a-f]{64}(?: AS \w+)?$", line), f"Unpinned image: {line}"
for path in (root / ".github/workflows").glob("*.yml"):
    for line in path.read_text().splitlines():
        if "uses:" in line and not "uses: ./" in line:
            assert re.search(r"@[0-9a-f]{40}(?:\s|$)", line), f"Unpinned action: {line}"
trivy_pins = set()
for path in (root / ".github/workflows").glob("*.yml"):
    trivy_pins.update(re.findall(r"aquasecurity/trivy:[\w.-]+@sha256:[0-9a-f]{64}", path.read_text()))
assert len(trivy_pins) == 1, f"Workflows must share one pinned Trivy image, found {sorted(trivy_pins)}"
postgres_paths = (
    "docker-compose.yaml",
    "k8s/postgres.yaml",
    "server/src/test/kotlin/app/astrid/server/PostgresTestDatabase.kt",
)
postgres_pins = []
for name in postgres_paths:
    pins = re.findall(r"postgres:[\w.-]+@sha256:[0-9a-f]{64}", (root / name).read_text())
    assert len(set(pins)) == 1, f"Expected one pinned PostgreSQL image in {name}"
    postgres_pins.append(pins[0])
assert len(set(postgres_pins)) == 1, "Compose, Kubernetes and Testcontainers PostgreSQL pins differ"
print("Runtime and action pins are aligned.")
