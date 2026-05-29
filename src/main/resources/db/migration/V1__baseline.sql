-- FinanceFlow baseline schema
-- Domain tables (wallets, transactions, ledger entries) will be added in later migrations.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE schema_version_marker (
    id          SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    version     TEXT NOT NULL,
    applied_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO schema_version_marker (version) VALUES ('0.0.1-baseline');
