ALTER TABLE wallets ALTER COLUMN user_id DROP NOT NULL;

ALTER TABLE wallets ADD COLUMN system BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE wallets DROP CONSTRAINT wallets_balance_non_negative;

ALTER TABLE wallets ADD CONSTRAINT wallets_balance_non_negative CHECK (system OR balance >= 0);

CREATE UNIQUE INDEX idx_wallets_system_currency ON wallets (currency) WHERE system = TRUE;
