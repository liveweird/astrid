#!/usr/bin/env python3
"""Keep locally tested runtime pins aligned with containers and CI."""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
tool_config = (root / "mise.toml").read_text()


def tool_pin(name):
    # The shared toolchain file deliberately contains simple quoted [tools] pins.
    match = re.search(rf'^\s*{name}\s*=\s*"([^"\n]+)"\s*$', tool_config, re.MULTILINE)
    assert match, f"Expected an exact quoted {name} pin in mise.toml"
    return match[1]


node = tool_pin("node")
java_patch = re.search(r"\d+\.\d+\.\d+", tool_pin("java"))[0]
assert (root / ".nvmrc").read_text().strip() == node, ".nvmrc differs from mise.toml"
dockerfile = (root / "Dockerfile").read_text()
assert f"FROM node:{node}-alpine@sha256:" in dockerfile, "Node image pin differs"
assert f"FROM eclipse-temurin:{java_patch}.1_1-jdk-noble@sha256:" in dockerfile, "JDK image pin differs"
assert f"FROM eclipse-temurin:{java_patch}.1_1-jre-noble@sha256:" in dockerfile, "JRE image pin differs"
ci = (root / ".github/workflows/ci.yml").read_text()
assert f"java-version: {java_patch}" in ci, "CI JDK differs"
assert "node-version-file: .nvmrc" in ci, "CI must read the Node pin"
for line in dockerfile.splitlines():
    if line.startswith("FROM "):
        assert re.search(r"@sha256:[0-9a-f]{64}(?: AS \w+)?$", line), f"Unpinned image: {line}"
for path in (root / ".github/workflows").glob("*.yml"):
    for line in path.read_text().splitlines():
        if "uses:" in line and not "uses: ./" in line:
            assert re.search(r"@[0-9a-f]{40}(?:\s|$)", line), f"Unpinned action: {line}"
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
