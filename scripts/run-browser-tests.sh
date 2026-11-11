#!/usr/bin/env bash
set -Eeuo pipefail

readonly PLAYWRIGHT_IMAGE="mcr.microsoft.com/playwright:v1.64.1-noble@sha256:2f05f8a734509a7b3f26a912639488ee08f58a7b292d14d62271bcff9038768f"
readonly REQUIRED_NODE_VERSION="24.22.1"

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
e2e_root="$repo_root/e2e"
output_root="${ASTRID_BROWSER_OUTPUT_ROOT:-$e2e_root}"
browser_node="${ASTRID_BROWSER_NODE:-$(command -v node || true)}"

if [[ -z "$browser_node" || "$browser_node" != /* || ! -f "$browser_node" || ! -x "$browser_node" ]]; then
  echo "ASTRID_BROWSER_NODE must name an absolute executable Linux Node binary (setup-node's node is used by default)." >&2
  exit 2
fi

if [[ "$output_root" != /* || "$output_root" == / ]]; then
  echo "ASTRID_BROWSER_OUTPUT_ROOT must be an absolute path when set." >&2
  exit 2
fi
mkdir -p "$output_root"

run_token="${GITHUB_RUN_ID:-local}-${GITHUB_RUN_ATTEMPT:-0}-$$-${RANDOM}"
container_name="astrid-browser-$run_token"
run_dir="$(mktemp -d "${TMPDIR:-/tmp}/astrid-browser.XXXXXX")"
cidfile="$run_dir/container.cid"
node_guard="$run_dir/node-preflight.cjs"
artifact_root="$run_dir/artifacts"
mkdir -p "$artifact_root/test-results" "$artifact_root/playwright-report"
# The JavaScript template expressions are intentionally literal until Node loads the guard.
# shellcheck disable=SC2016
printf '%s\n' \
  '"use strict";' \
  'const expected = process.env.ASTRID_BROWSER_NODE_VERSION;' \
  'if (!expected || process.execPath !== "/astrid-node/node" || process.version !== `v${expected}`) {' \
  '  console.error(`Browser Node boundary mismatch: ${process.version} via ${process.execPath}`);' \
  '  process.exit(2);' \
  '}' \
  'console.error(`Browser Node ${process.version} via ${process.execPath}`);' \
  > "$node_guard"

cleanup() {
  local status=$?
  local cid=""
  local owner=""
  local cleanup_failed=0
  trap - EXIT INT TERM

  if [[ -s "$cidfile" ]]; then
    cid="$(<"$cidfile")"
    if owner="$(docker inspect --format '{{ index .Config.Labels "astrid.dev/browser-run" }}' "$cid" 2>/dev/null)" &&
      [[ -n "$owner" ]]; then
      if [[ "$owner" != "$run_token" ]]; then
        echo "Refusing to remove browser container $cid: ownership label differs." >&2
        cleanup_failed=1
      elif ! docker rm -f "$cid" >/dev/null 2>&1; then
        echo "Failed to remove owned browser container $cid." >&2
        cleanup_failed=1
      fi
    else
      echo "Unable to verify ownership of browser container $cid; it was not removed." >&2
      cleanup_failed=1
    fi
  fi

  for artifact in test-results playwright-report; do
    if ! rm -rf -- "${output_root:?}/$artifact"; then
      echo "Failed to clear previous browser artifact $output_root/$artifact." >&2
      cleanup_failed=1
    elif [[ -d "$artifact_root/$artifact" ]]; then
      if ! mv "$artifact_root/$artifact" "$output_root/$artifact"; then
        echo "Failed to export browser artifact $artifact." >&2
        cleanup_failed=1
      fi
    elif ! mkdir -p "$output_root/$artifact"; then
      echo "Failed to create browser artifact directory $output_root/$artifact." >&2
      cleanup_failed=1
    fi
  done

  if ((cleanup_failed != 0 && status == 0)); then
    status=1
  fi
  rm -f "$cidfile" "$node_guard"
  rmdir "$artifact_root" 2>/dev/null || true
  rmdir "$run_dir" 2>/dev/null || true
  exit "$status"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

expected_commit="$(git -C "$repo_root" rev-parse --short HEAD)"
expected_commit_time="$(git -C "$repo_root" log -1 --format=%cI)"

docker run \
  --cidfile "$cidfile" \
  --name "$container_name" \
  --label "astrid.dev/browser-run=$run_token" \
  --platform linux/amd64 \
  --init \
  --network host \
  --shm-size 1g \
  --workdir /work \
  --mount "type=bind,src=$e2e_root,dst=/work,readonly" \
  --mount "type=bind,src=$artifact_root,dst=/artifacts" \
  --mount "type=bind,src=$browser_node,dst=/astrid-node/node,readonly" \
  --mount "type=bind,src=$node_guard,dst=/astrid-node/preflight.cjs,readonly" \
  --env CI=1 \
  --env "E2E_BASE_URL=${E2E_BASE_URL:-http://127.0.0.1:8088}" \
  --env "E2E_EXPECTED_COMMIT=$expected_commit" \
  --env "E2E_EXPECTED_COMMIT_TIME=$expected_commit_time" \
  --env "ASTRID_BROWSER_NODE_VERSION=$REQUIRED_NODE_VERSION" \
  --env NODE_OPTIONS=--require=/astrid-node/preflight.cjs \
  --env PLAYWRIGHT_HTML_OUTPUT_DIR=/artifacts/playwright-report \
  "$PLAYWRIGHT_IMAGE" \
  /bin/sh -eu -c '
    umask 000
    actual_version="$(/astrid-node/node --version)"
    if [ "$actual_version" != "v$1" ]; then
      echo "Browser runner Node version mismatch: $actual_version != v$1" >&2
      exit 2
    fi
    shift
    exec /astrid-node/node node_modules/@playwright/test/cli.js test "$@" --output=/artifacts/test-results
  ' sh "$REQUIRED_NODE_VERSION" "$@"
