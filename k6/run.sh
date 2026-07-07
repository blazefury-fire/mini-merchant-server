#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Run the k6 load test INSIDE Docker (no local k6 install needed).
#
# Usage:
#   bash k6/run.sh
#   BASE_URL=http://172.25.179.201:8080 MERCHANT_COUNT=2000 bash k6/run.sh
#
# Prereqs (see k6/README.md):
#   1) docker compose up -d          # Postgres + Redis
#   2) ./mvnw spring-boot:run        # app on the host at :8080
#   3) seed the merchants (README step 3)
#
# Networking: the app runs on THIS host and binds 0.0.0.0:8080. On Docker Desktop
# + WSL2, host.docker.internal points to the Windows host, NOT your WSL2 distro,
# so we target the host's own IP (which the k6 container can route to). Override
# by exporting BASE_URL yourself.
# ---------------------------------------------------------------------------
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# First non-loopback IP of this host (the WSL2 distro IP under Docker Desktop).
HOST_IP="$(hostname -I 2>/dev/null | awk '{print $1}')"
BASE_URL="${BASE_URL:-http://${HOST_IP}:8080}"
MERCHANT_COUNT="${MERCHANT_COUNT:-1000}"

# Preflight: fail fast with a clear message instead of a 3-minute wall of
# "connection refused" if the app is not reachable at BASE_URL.
if ! curl -fsS -m 4 -o /dev/null "${BASE_URL}/ping"; then
  echo "ERROR: cannot reach the app at ${BASE_URL}/ping" >&2
  echo "  - Is the app running?   ./mvnw spring-boot:run   (must listen on :8080)" >&2
  echo "  - Wrong address? override it:   BASE_URL=http://<host-ip>:8080 bash k6/run.sh" >&2
  echo "    (find <host-ip> with:  hostname -I | awk '{print \$1}')" >&2
  exit 1
fi

echo "Load testing ${BASE_URL} with ${MERCHANT_COUNT} merchants ..."
docker run --rm -i \
  -v "${SCRIPT_DIR}:/scripts" \
  grafana/k6 run \
  -e BASE_URL="${BASE_URL}" \
  -e MERCHANT_COUNT="${MERCHANT_COUNT}" \
  /scripts/transaction-load-test.js
