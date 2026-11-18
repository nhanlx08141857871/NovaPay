package com.novabank.novapay.domain.model.transaction;

import java.time.Instant;
import java.util.Objects;

/**
 * Bút toán sổ cái tại accounts/{accountId}/ledger/{entryId} (Pure Java 17).
 * Số tiền và số dư sau giao dịch bắt buộc dùng kiểu long (VNĐ).
 */
public final class LedgerEntry {

    private final String entryId;
    private final String accountId;
    private final String txId;
    private final LedgerDirection direction;
    private final long amount;
    private final long balanceAfter;
    private final String description;
    private final Instant createdAt;

    public LedgerEntry(
            String entryId,
            String accountId,
            String txId,
            LedgerDirection direction,
            long amount,
            long balanceAfter,
            String description,
            Instant createdAt
    ) {
        this.entryId = Objects.requireNonNull(entryId, "entryId must not be null");
        this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
        this.txId = txId != null ? txId : "";
        this.direction = Objects.requireNonNull(direction, "direction must not be null");
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description != null ? description : "";
        this.createdAt = createdAt;
    }

    public String getEntryId() {
        return entryId;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getTxId() {
        return txId;
    }

    public LedgerDirection getDirection() {
        return direction;
    }

    public long getAmount() {
        return amount;
    }

    public long getBalanceAfter() {
        return balanceAfter;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isInflow() {
        return direction == LedgerDirection.IN;
    }
}
