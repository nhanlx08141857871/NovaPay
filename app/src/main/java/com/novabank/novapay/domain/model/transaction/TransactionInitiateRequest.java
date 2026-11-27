package com.novabank.novapay.domain.model.transaction;

import java.util.Objects;

public final class TransactionInitiateRequest {

    private final TransactionType type;
    private final String fromAccountId;
    private final String toAccountId;
    private final ExternalRecipient toExternal;
    private final long amount;
    private final String note;
    private final TransactionMeta meta;

    public TransactionInitiateRequest(
            TransactionType type,
            String fromAccountId,
            String toAccountId,
            ExternalRecipient toExternal,
            long amount,
            String note,
            TransactionMeta meta
    ) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.fromAccountId = fromAccountId != null ? fromAccountId.trim() : "";
        this.toAccountId = toAccountId != null && !toAccountId.trim().isEmpty() ? toAccountId.trim() : null;
        this.toExternal = toExternal;
        this.amount = amount;
        this.note = note != null ? note.trim() : "";
        this.meta = meta;
    }

    public TransactionType getType() {
        return type;
    }

    public String getFromAccountId() {
        return fromAccountId;
    }

    public String getToAccountId() {
        return toAccountId;
    }

    public ExternalRecipient getToExternal() {
        return toExternal;
    }

    public long getAmount() {
        return amount;
    }

    public String getNote() {
        return note;
    }

    public TransactionMeta getMeta() {
        return meta;
    }
}
