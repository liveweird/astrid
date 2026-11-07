#!/bin/sh
# Run from any directory. Docker must be available for PostgreSQL integration tests.
set -eu
cd "$(dirname "$0")/.."
python3 scripts/check-toolchain.py
python3 -m unittest discover -s scripts/tests -p 'test_*.py'
python3 scripts/check-knowledge.py
python3 scripts/check-startup-budget.py
git diff --check
./gradlew check :engine:koverVerify :server:koverVerify :server:installDist --dependency-verification strict --no-daemon
npm --prefix web run lint:api
npm --prefix web run check:api
npm --prefix web run lint
npm --prefix web run knip
npm --prefix web run test:coverage
npm --prefix web run build
npm --prefix e2e run lint
npm --prefix e2e run knip
npm --prefix e2e run typecheck
docker compose --env-file .env.example config --quiet
kubectl kustomize k8s > /dev/null
