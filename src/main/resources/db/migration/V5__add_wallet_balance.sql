ALTER TABLE wallets
    ADD COLUMN balance BIGINT NOT NULL DEFAULT 0;

ALTER TABLE wallets
    ADD CONSTRAINT wallets_balance_non_negative CHECK (balance >= 0);
