import http from 'k6/http';
import crypto from 'k6/crypto';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

// ---------------------------------------------------------------------------
// CONFIG
// This script is meant to run inside the grafana/k6 Docker container (see
// k6/run.sh and the README). Override any value with k6's -e flag, e.g.
//   docker run ... grafana/k6 run -e MERCHANT_COUNT=2000 /scripts/transaction-load-test.js
//
// BASE_URL defaults to host.docker.internal:8080 because, from inside a
// container, "localhost" is the container itself, not your host machine.
// host.docker.internal resolves to the host (see run.sh for the --add-host flag
// that enables it on Linux/WSL2).
// ---------------------------------------------------------------------------
const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';

// The seed creates MERCHANT_COUNT merchants with these prefixes + a zero-padded
// index (0000..). The script rebuilds the same api key / secret from a random
// index, so no credentials file is needed. Keep these three in sync with the SQL.
const MERCHANT_COUNT = parseInt(__ENV.MERCHANT_COUNT || '1000', 10);
const API_KEY_PREFIX = __ENV.API_KEY_PREFIX || 'loadtest-api-key-';
const SECRET_PREFIX = __ENV.SECRET_PREFIX || 'loadtest-secret-';

// Must match the URI the server sees (no host, no query string, no trailing slash).
const PATH = '/api/v1/payments/transaction';
const METHOD = 'POST';

// The endpoint returns HTTP 201 for BOTH real outcomes, so we read the JSON body's
// data.status to separate SUCCESS from the ~1-in-6 simulated FAILED transactions.
const successCount = new Counter('tx_success');
const failedCount = new Counter('tx_failed');

// ---------------------------------------------------------------------------
// LOAD PROFILE — ramping VUs (virtual users). Each stage means "reach this many
// VUs by the end of this duration"; k6 ramps linearly between stages.
// Heads-up: the service deliberately sleeps 3 seconds inside every transaction,
// so a single VU can only complete ~0.3 requests/second. That is why these VU
// counts look small — edit them freely to push harder.
// ---------------------------------------------------------------------------
export const options = {
  stages: [
    { duration: '30s', target: 100 }, // warm up: 0 -> 100 VUs
    { duration: '1m', target: 500 },  // ramp up: 100 -> 500 VUs
    { duration: '2m', target: 500 },  // hold at 500 VUs
    { duration: '30s', target: 0 },  // ramp down: 500 -> 0 VUs
  ],
  thresholds: {
    // Fail the whole run if more than 1% of HTTP requests are non-2xx (401/429/5xx).
    http_req_failed: ['rate<0.01'],
    // 95% of requests should finish within 5s (the ~3s built-in sleep is the floor).
    http_req_duration: ['p(95)<5000'],
  },
};

// Pick a random one of the seeded merchants and rebuild its credentials from the
// same deterministic pattern the SQL used. Spreading requests across many
// merchants keeps each one far under the per-merchant rate limit (10 req / 60s).
function pickMerchant() {
  const index = Math.floor(Math.random() * MERCHANT_COUNT);
  const suffix = String(index).padStart(4, '0');
  return {
    apiKey: API_KEY_PREFIX + suffix,
    secret: SECRET_PREFIX + suffix,
  };
}

// Reproduce the signature the server computes in PaymentAuthService:
//   canonical = apiKey + "\n" + timestamp + "\n" + method + "\n" + path
//   signature = lowercase-hex HMAC-SHA256(secret, canonical)
function sign(apiKey, secret, timestamp) {
  const canonical = `${apiKey}\n${timestamp}\n${METHOD}\n${PATH}`;
  return crypto.hmac('sha256', secret, canonical, 'hex');
}

export default function () {
  const merchant = pickMerchant();

  // Unix time in SECONDS. The server allows a +/- 300s clock skew.
  const timestamp = Math.floor(Date.now() / 1000).toString();
  const signature = sign(merchant.apiKey, merchant.secret, timestamp);

  // Unique per iteration so every request is a NEW transaction and is not served
  // from the idempotency cache (which would skip the real 3s of work).
  const idempotencyKey = `k6-${__VU}-${__ITER}-${timestamp}`;

  const payload = JSON.stringify({ amount: 100.0, currency: 'VND' });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Api-Key': merchant.apiKey,
      'X-Signature': signature,
      'X-Timestamp': timestamp,
      'Idempotency-Key': idempotencyKey,
    },
  };

  const res = http.post(`${BASE_URL}${PATH}`, payload, params);

  // check() records pass/fail counts but does NOT stop the test.
  const created = check(res, {
    'status is 201': (r) => r.status === 201,
    'has response body': (r) => !!r.body && r.body.length > 0,
  });

  // Tally the business outcome from the response body: { status, message, data: { id, status } }.
  if (created) {
    try {
      const status = res.json('data.status');
      if (status === 'SUCCESS') {
        successCount.add(1);
      } else if (status === 'FAILED') {
        failedCount.add(1);
      }
    } catch (e) {
      // Body was not the JSON we expected (e.g. a 401/429 error). Ignore for the tally.
    }
  }

  // A short think-time between iterations, so each VU is not a tight hot loop.
  sleep(1);
}
