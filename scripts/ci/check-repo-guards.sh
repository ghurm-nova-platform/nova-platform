#!/usr/bin/env bash
# Repository guards for Sprint 0 CI.
# Fails when secrets, private keys, or browser-exposed API-key patterns are present.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

echo "Checking for committed .env files..."
mapfile -t env_files < <(find . -type f \( -name '.env' -o -name '.env.*' \) \
  ! -path './.git/*' \
  ! -name '.env.example' \
  ! -name '*.example' \
  2>/dev/null || true)
if ((${#env_files[@]} > 0)); then
  printf '%s\n' "${env_files[@]}"
  fail "Committed .env files are not allowed"
fi

echo "Checking for obvious private key material..."
if grep -RIn --exclude-dir=.git --exclude-dir=node_modules --exclude-dir=dist \
  --exclude-dir=target --exclude-dir=.venv --exclude-dir=.angular \
  -E 'BEGIN (RSA |OPENSSH |EC )?PRIVATE KEY' .; then
  fail "Private key material detected in the repository"
fi

echo "Checking browser code for internal API-key leakage patterns..."
if [[ -d apps ]]; then
  if grep -RIn --exclude-dir=node_modules --exclude-dir=dist --exclude-dir=.angular \
    --exclude='*.spec.ts' --exclude='*.test.ts' \
    -E 'INTERNAL_API_KEY|X-API-Key' apps; then
    fail "Browser apps must not contain INTERNAL_API_KEY or X-API-Key outside tests"
  fi
fi

echo "Repository guards passed."
