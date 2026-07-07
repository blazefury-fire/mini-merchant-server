# Load testing `POST /transaction` with k6 (in Docker)

A beginner-friendly guide to load testing the payment endpoint
`POST /api/v1/payments/transaction` using [k6](https://k6.io/) with **1000 merchants**,
running k6 **inside Docker** (no local install). If you have never used k6, read
top to bottom once — every step is spelled out.

## What is k6 (30-second version)

k6 is a load-testing tool. You write a small JavaScript file describing what one
"virtual user" (VU) does, and k6 runs many VUs at once and measures how the API
behaves under load (response times, error rate, throughput). It fires raw HTTP
requests (no browser) — exactly what we want for an API. We run it via the official
`grafana/k6` Docker image, so there is nothing to install.

Files in this folder:

| File | What it is |
| --- | --- |
| `transaction-load-test.js` | The k6 script that signs and sends requests. |
| `seed-load-test-merchants.sql` | One-time SQL to create 1000 test merchants. |
| `run.sh` | Convenience wrapper: finds your host IP, checks the app, runs k6. |
| `README.md` | This guide. |

## Why 1000 merchants?

The API rate-limits **each merchant** to 10 requests per 60 seconds. If every
request used one merchant you would hit `429 Too Many Requests` immediately. By
spreading requests across 1000 merchants (the script picks a random one per
request), each merchant sees only a trickle of traffic, the rate limit never trips,
and you measure the endpoint's true performance — like real multi-tenant traffic.

## Why the requests are signed

`POST /transaction` is protected. Every request must carry three headers the server
verifies (`PaymentApiKeyAuthFilter` → `PaymentAuthService`): `X-Api-Key`,
`X-Timestamp` (Unix seconds, within 5 min of the server clock), and `X-Signature`
(lowercase-hex HMAC-SHA256 of `apiKey\ntimestamp\nPOST\n/api/v1/payments/transaction`
using that merchant's secret). Plus an `Idempotency-Key` header and a JSON body.
The script does all of this for you and rotates through the 1000 merchants.

## How the k6 container reaches your app (important)

Your app runs on the **host** and binds `0.0.0.0:8080`. k6 runs in a **container**,
where `localhost` means the container itself — not your app. So k6 must use an
address that reaches the host:

- **Docker Desktop + WSL2 (this project's setup):** `host.docker.internal` points to
  the **Windows host**, not the WSL2 distro your app runs in, so it does **not** work.
  Instead we use the WSL2 distro's own IP (e.g. `172.x.x.x`). `run.sh` finds it
  automatically with `hostname -I`.
- **Native Linux Docker Engine:** the host's IP also works (that's what `run.sh`
  uses), or `host.docker.internal` via `--add-host=host.docker.internal:host-gateway`.
- **Docker Desktop on Mac/Windows with the app on the host (not WSL):**
  `host.docker.internal` works natively — set `BASE_URL=http://host.docker.internal:8080`.

`run.sh` handles the common case for you; you rarely need to think about this.

## Step 1 — Prerequisites (Docker only)

You already use Docker for the database, so there is nothing new to install. Just
confirm Docker works and (optionally) pre-pull the k6 image:

```bash
docker version
docker pull grafana/k6   # optional; run.sh pulls it automatically the first time
```

## Step 2 — Start infra and run the app

```bash
# infrastructure (Postgres + Redis; Kafka is optional for this test)
docker compose up -d

# the app on the host (default settings are fine)
./mvnw spring-boot:run
```

Leave this running in its own terminal. Confirm it is up from the host:

```bash
curl localhost:8080/ping   # should print a small JSON body (HTTP 200)
```

> Rate limit note: with 1000 merchants and the default load profile you should not
> hit the per-merchant limit, so the default config is fine. Only at very high
> throughput (~150+ req/s sustained) might a single merchant exceed 10 req/60s; if
> you want to rule it out, start the app with the limit raised (runtime-only, no
> code change): `RATELIMIT_PAYMENTS_LIMIT=100000000 ./mvnw spring-boot:run`

## Step 3 — Seed the 1000 load-test merchants (once)

```bash
docker exec -i postgres_db psql -U admin -d mini_merchant_db < k6/seed-load-test-merchants.sql
```

Or paste `seed-load-test-merchants.sql` into Adminer (http://localhost:8081) →
**SQL command**. Safe to run more than once. This creates merchants
`loadtest-api-key-0000` … `loadtest-api-key-0999` with matching secrets; the script
rebuilds these same strings from a random index, so the two always agree.

## Step 4 — Run the load test (in Docker)

Easiest — use the wrapper (from the repo root):

```bash
bash k6/run.sh
```

`run.sh` finds your host IP, does a quick connectivity check (and stops early with a
clear message if the app is not reachable), then runs k6. Override the merchant count
or target host via environment variables:

```bash
MERCHANT_COUNT=2000 bash k6/run.sh
BASE_URL=http://172.25.179.201:8080 bash k6/run.sh   # explicit host IP if autodetect is wrong
```

Prefer the raw command? This is what `run.sh` runs (replace `<host-ip>` with the
output of `hostname -I | awk '{print $1}'`):

```bash
docker run --rm -i \
  -v "$(pwd)/k6:/scripts" \
  grafana/k6 run \
  -e BASE_URL=http://<host-ip>:8080 \
  -e MERCHANT_COUNT=1000 \
  /scripts/transaction-load-test.js
```

The run takes ~3 minutes (the ramp stages), showing a live progress bar then a
summary table.

## Step 5 — Read the results

Key lines in the summary:

- **`http_req_duration`** — how long requests took. Look at `avg` and especially
  `p(95)`. Expect **~3s** because the service intentionally sleeps 3 seconds inside
  every transaction — that sleep, not network or DB, is the floor.
- **`http_req_failed`** — fraction of **HTTP** failures (non-2xx). Should be near `0%`.
- **`http_reqs`** — total requests and requests/second (your throughput).
- **`iterations`** / **`vus`** — how many times the script body ran and the VU count.
- **`checks`** — pass rate of the `check()` assertions.
- **`tx_success` / `tx_failed`** — custom counters. The API marks roughly 1 in 6
  transactions `FAILED` on purpose (still HTTP 201), so some `tx_failed` is expected.

Two things shape the numbers: the **3-second sleep** caps each VU at ~0.3 req/s, and
the **DB connection pool** (HikariCP, default 10) means once more than ~10 requests
are mid-transaction the rest queue and wait — so latency climbs as VUs ramp up.

## Step 6 — Tweak the load

Edit the `stages` array in `transaction-load-test.js`. Each entry is "reach `target`
VUs by the end of `duration`":

```javascript
stages: [
  { duration: '1m', target: 200 },  // ramp to 200 VUs over 1 minute
  { duration: '2m', target: 200 },  // hold 200 VUs for 2 minutes
  { duration: '30s', target: 0 },   // ramp back down
],
```

## Troubleshooting

| Symptom | Likely cause | Fix |
| --- | --- | --- |
| `run.sh` prints "cannot reach the app" and exits | App not running, or wrong host IP | Start `./mvnw spring-boot:run`; confirm `curl localhost:8080/ping` works; if autodetect picked the wrong IP, pass `BASE_URL=http://<host-ip>:8080 bash k6/run.sh`. |
| `dial tcp ... connection refused` **inside k6** (preflight passed) | Container can't route to the host IP | Make sure you used the host's real IP (`hostname -I`), not `localhost`/`host.docker.internal`; check no host firewall blocks the WSL2 IP. |
| Almost all requests `401` | Merchants not seeded, `MERCHANT_COUNT` > seeded, or clock off | Re-run step 3; keep `MERCHANT_COUNT` <= how many you seeded; check your machine clock (±5 min). |
| Some requests `429` | Very high VU count concentrating traffic on a merchant | Lower VU target, seed more merchants, or raise the limit (step 2 note). |
| `docker: command not found` / daemon errors | Docker not installed or not running | Start Docker (the same one you use for `docker compose`). |

## Cleanup

- Stop the app: `Ctrl+C` in its terminal.
- Stop infra: `docker compose down`.
- Remove the seeded merchants:
  `DELETE FROM merchants WHERE api_key LIKE 'loadtest-api-key-%';`
