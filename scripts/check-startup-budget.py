#!/usr/bin/env python3
"""Keep process health budgets above the bounded database bootstrap policy."""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
bootstrap = (root / "server/src/main/kotlin/app/astrid/server/DatabaseBootstrap.kt").read_text()
configuration = (root / "server/src/main/kotlin/app/astrid/server/ServerConfig.kt").read_text()


def constant(name):
    match = re.search(rf"const val {name} = (\d+)", bootstrap)
    assert match, f"Missing reviewed bootstrap bound: {name}"
    return int(match[1])


retry_match = re.search(r"require\(connectRetries in 0\.\.(\d+)\)", configuration)
assert retry_match, "Cannot find the validated maximum POSTGRES_CONNECT_RETRIES"
retries = int(retry_match[1])
interval = constant("CONNECT_RETRY_MAX_INTERVAL_SECONDS")
login = constant("MIGRATION_LOGIN_TIMEOUT_SECONDS")
connect = constant("MIGRATION_CONNECT_TIMEOUT_SECONDS")
assert 0 < connect <= login, "Migration TCP connect must be bounded by its login deadline"
backoff = sum(min(2 ** attempt, interval) for attempt in range(retries))
envelope = (retries + 1) * login + backoff
# Margin for the small pilot's JVM, migrations and readiness round trip. Future
# long-running migrations must reassess this policy rather than disable probes.
required = envelope + 30

manifest = (root / "k8s/app.yaml").read_text()
startup = manifest.split("startupProbe:", 1)[1].split("readinessProbe:", 1)[0]
threshold = int(re.search(r"failureThreshold:\s*(\d+)", startup)[1])
period = int(re.search(r"periodSeconds:\s*(\d+)", startup)[1])
# The first probe may run immediately, so use the earliest final failure.
kubernetes_budget = (threshold - 1) * period
dockerfile = (root / "Dockerfile").read_text()
docker_budget = int(re.search(r"HEALTHCHECK .*--start-period=(\d+)s", dockerfile)[1])
assert kubernetes_budget >= required, f"Kubernetes startup budget {kubernetes_budget}s is below {required}s"
assert docker_budget >= required, f"Docker health grace {docker_budget}s is below {required}s"
print(f"Bootstrap envelope {envelope}s + 30s margin fits Kubernetes ({kubernetes_budget}s) and Docker ({docker_budget}s).")
