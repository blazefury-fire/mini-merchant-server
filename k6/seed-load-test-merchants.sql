-- ---------------------------------------------------------------------------
-- One-time seed of 1000 THROWAWAY merchants used only for k6 load testing.
--
-- Run this ONCE against your LOCAL database. Two easy ways:
--   * Adminer UI:  http://localhost:8081  (System=PostgreSQL, Server=postgres,
--     User=admin, Password=abc123, Database=mini_merchant_db) -> "SQL command".
--   * psql:  docker exec -i postgres_db psql -U admin -d mini_merchant_db < k6/seed-load-test-merchants.sql
--
-- This is intentionally NOT a Flyway migration, so it never runs in real
-- environments. Safe to re-run: ON CONFLICT DO NOTHING makes it a no-op for
-- api_keys that already exist.
--
-- Creates merchants with deterministic credentials that MUST match the pattern
-- in transaction-load-test.js:
--     api_key = 'loadtest-api-key-' || <0000..0999>
--     secret  = 'loadtest-secret-' || <0000..0999>
-- Change generate_series(0, 999) to seed a different number of merchants (also
-- pass -e MERCHANT_COUNT=<n> to the script so the two agree).
-- ---------------------------------------------------------------------------
INSERT INTO merchants (
    id, name, email, api_key, secret, status, created_at, created_by, is_deleted
)
SELECT
    gen_random_uuid(),
    'K6 Load Test Merchant ' || lpad(i::text, 4, '0'),
    'k6-loadtest-' || lpad(i::text, 4, '0') || '@example.com',
    'loadtest-api-key-' || lpad(i::text, 4, '0'),
    'loadtest-secret-' || lpad(i::text, 4, '0'),
    'ACTIVE',
    now(),
    'k6-seed',
    FALSE
FROM generate_series(0, 999) AS s(i)
ON CONFLICT (api_key) DO NOTHING;
