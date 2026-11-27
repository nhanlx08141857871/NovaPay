package com.novabank.novapay.domain.model.transaction;

import java.time.Instant;
import java.util.Objects;

public final class Transaction {

    private final String txId;
    private final TransactionType type;
    private final String fromAccountId;
    private final String fromAccountNumber;
    private final String toAccountId;
    private final String toAccountNumber;
    private final String toOwnerName;
    private final ExternalRecipient toExternal;
    private final long amount;
    private final long fee;
    private final String note;
    private final TransactionStatus status;
    private final boolean requiresBiometric;
    private final boolean biometricPassed;
    private final boolean otpVerified;
    private final boolean faceChecked;
    private final TransactionMeta meta;
    private final String initiatedBy;
    private final Instant createdAt;

    public Transaction(
            String txId,
            TransactionType type,
            String fromAccountId,
            String toAccountId,
            ExternalRecipient toExternal,
            long amount,
            long fee,
            String note,
            TransactionStatus status,
            boolean requiresBiometric,
            boolean biometricPassed,
            boolean otpVerified,
            boolean faceChecked,
            TransactionMeta meta,
            String initiatedBy,
            Instant createdAt
    ) {
        this(
                txId,
                type,
                fromAccountId,
                null,
                toAccountId,
                null,
                null,
                toExternal,
                amount,
                fee,
                note,
                status,
                requiresBiometric,
                biometricPassed,
                otpVerified,
                faceChecked,
                meta,
                initiatedBy,
                createdAt
        );
    }

    public Transaction(
            String txId,
            TransactionType type,
            String fromAccountId,
            String fromAccountNumber,
            String toAccountId,
            String toAccountNumber,
            String toOwnerName,
            ExternalRecipient toExternal,
            long amount,
            long fee,
            String note,
            TransactionStatus status,
            boolean requiresBiometric,
            boolean biometricPassed,
            boolean otpVerified,
            boolean faceChecked,
            TransactionMeta meta,
            String initiatedBy,
            Instant createdAt
    ) {
        this.txId = Objects.requireNonNull(txId, "txId must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.fromAccountId = fromAccountId;
        this.fromAccountNumber = trimToNull(fromAccountNumber);
        this.toAccountId = toAccountId;
        this.toAccountNumber = trimToNull(toAccountNumber);
        this.toOwnerName = trimToNull(toOwnerName);
        this.toExternal = toExternal;
        this.amount = amount;
        this.fee = fee;
        this.note = note != null ? note : "";
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.requiresBiometric = requiresBiometric;
        this.biometricPassed = biometricPassed;
        this.otpVerified = otpVerified;
        this.faceChecked = faceChecked;
        this.meta = meta != null ? meta : TransactionMeta.empty();
        this.initiatedBy = initiatedBy;
        this.createdAt = createdAt;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public String getTxId() {
        return txId;
    }

    public TransactionType getType() {
        return type;
    }

    public String getFromAccountId() {
        return fromAccountId;
    }

    public String getFromAccountNumber() {
        return fromAccountNumber;
    }

    public String getToAccountId() {
        return toAccountId;
    }

    public String getToAccountNumber() {
        return toAccountNumber;
    }

    public String getToOwnerName() {
        return toOwnerName;
    }

    public ExternalRecipient getToExternal() {
        return toExternal;
    }

    public long getAmount() {
        return amount;
    }

    public long getFee() {
        return fee;
    }

    public String getNote() {
        return note;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public boolean isRequiresBiometric() {
        return requiresBiometric;
    }

    public boolean isBiometricPassed() {
        return biometricPassed;
    }

    public boolean isOtpVerified() {
        return otpVerified;
    }

    public boolean isFaceChecked() {
        return faceChecked;
    }

    public TransactionMeta getMeta() {
        return meta;
    }

    public String getInitiatedBy() {
        return initiatedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
