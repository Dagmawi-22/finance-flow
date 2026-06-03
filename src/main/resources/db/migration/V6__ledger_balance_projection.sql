COMMENT ON COLUMN wallets.balance IS
    'Materialized projection of completed ledger entries (credits minus debits); updated only when posting ledger entries';

CREATE INDEX idx_ledger_entries_wallet_direction
    ON ledger_entries (wallet_id, direction);
