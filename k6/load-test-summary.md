# Load test summary — Redis on vs off

**Endpoint:** `POST /api/v1/payments/transaction`
**Load profile (both runs):** ramp to **500 VUs**, ~4m22s, 1000 merchants, identical k6 script.
**Only variable:** Redis running vs stopped (`docker compose stop redis`).
Raw results: [`load-with-redis.md`](load-with-redis.md), [`load-without-redis.md`](load-without-redis.md).

> Note: the artificial 3-second `Thread.sleep` in `TransactionService.runCoreLogic()` was commented out
> for both runs, so latency reflects the *real* processing path — which is exactly what makes Redis's
> impact visible here.

## Verdict: Redis is important — decisively

Turning Redis off made p95 latency **~245× worse** and cut useful transaction throughput by **~27%**,
while also silently removing rate-limiting protection and idempotency.

| Metric | 🟢 With Redis | 🔴 Without Redis | Change |
|---|---|---|---|
| **p95 latency** | **21.2 ms** | **5.18 s** | **~245× slower** |
| median latency | 5.4 ms | 2.4 s | ~445× slower |
| max latency | 4.1 s | 8.0 s | ~2× worse |
| **Transactions processed/s** | **~149/s** (124 ✓ + 25 ✗) | **~109/s** (90 ✓ + 18 ✗) | **~27% fewer** |
| Iterations completed | 86,314 | 28,561 | 3× fewer |
| `http_reqs`/s | 328.5 | 108.8 | — |
| `http_req_failed` | 54.7% | 0% | ⚠️ misleading — see below |
| Rate-limit protection | ✅ on | ❌ gone (fails open) | lost |
| Idempotency (no double-charge) | ✅ on | ❌ gone | lost |

## Don't be fooled by the threshold flip

At first glance "without Redis" looks better — it *passed* `http_req_failed` (0%) while "with Redis"
*failed* (54.7%). That reading is backwards:

- **With Redis:** the 54.7% "failures" are the **rate limiter correctly shedding excess load** as fast
  `429`s (a few ms each). Requests that were actually processed stayed fast (~21 ms p95).
- **Without Redis:** nothing is rejected (rate limiter fails open) → 0% "failures" — but the price is that
  **every request slows to 2–5 seconds**. The failing threshold merely moved from error-rate to latency,
  which is the worse outcome for users.

The honest KPIs are **latency of accepted requests** and **useful transactions/s** — Redis wins both.

## Why it collapses without Redis

Two Redis jobs disappear at once, and both hurt:

1. **Merchant-lookup cache gone** (`@Cacheable("merchantByApiKey")`) → every request now runs a Postgres
   query just to authenticate, instead of an in-memory cache hit.
2. **Rate limiter gone** → all 500 VUs hit the processing path simultaneously with **no back-pressure**.

Together they swamp the **HikariCP connection pool (default 10 connections)**: ~500 concurrent requests
queue waiting for a free DB connection, so latency balloons from ~20 ms to seconds. The ~2.4 s average is
real DB/pool contention (the fake 3 s sleep was off), and the run completed only 28,561 iterations vs
86,314 because each took ~3 s instead of ~1 s.

## Bottom line

For this payment API, Redis is **load-bearing in three ways**, not just a speed cache:

- **Speed** — merchant-lookup cache keeps auth off the hot DB path.
- **Protection** — per-merchant rate limiter sheds abuse/overload as cheap 429s.
- **Correctness** — idempotency prevents double-charging on retries.

Under real concurrency, removing it degrades the service from "snappy" to "barely usable" *and* strips
safety guarantees. **Keep Redis.**

*Caveat for rigor: this run disabled all three Redis roles at once, so the delta isn't purely "caching."
To isolate just the cache benefit, keep rate-limiting on. But as an overall "does this system need Redis?"
answer, it is a clear yes.*

## Possible follow-ups

- Raise `spring.datasource.hikari.maximum-pool-size` (default 10) and re-run without Redis to see how much
  of the slowdown is the connection pool vs Postgres itself.
- Re-enable the 3 s `runCoreLogic` sleep if you want the "realistic slow core" profile (it was commented
  out for these runs).
