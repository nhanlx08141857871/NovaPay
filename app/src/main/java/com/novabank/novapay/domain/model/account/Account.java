package com.novabank.novapay.domain.model.account;

import java.time.Instant;
import java.util.Objects;

public final class Account {

    private final String accountId;
    private final String ownerUid;
    private final AccountType type;
    private final String accountNumber;
    private final AccountStatus status;
    private final long balance;
    private final String currency;
    private final long perTxnLimit;
    private final long dailyLimit;
    private final SavingInfo saving;
    private final MortgageInfo mortgage;
    private final Instant createdAt;

    public Account(
            String accountId,
            String ownerUid,
            AccountType type,
            String accountNumber,
            AccountStatus status,
            long balance,
            String currency,
            long perTxnLimit,
            long dailyLimit,
            SavingInfo saving,
            MortgageInfo mortgage,
            Instant createdAt
    ) {
        this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
        this.ownerUid = Objects.requireNonNull(ownerUid, "ownerUid must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.accountNumber = Objects.requireNonNull(accountNumber, "accountNumber must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.balance = balance;
        this.currency = currency != null ? currency : "VND";
        this.perTxnLimit = perTxnLimit;
        this.dailyLimit = dailyLimit;
        this.saving = saving;
        this.mortgage = mortgage;
        this.createdAt = createdAt;
    }

    public boolean canBePaymentSource() {
        return type == AccountType.CHECKING && status == AccountStatus.ACTIVE;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getOwnerUid() {
        return ownerUid;
    }

    public AccountType getType() {
        return type;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public long getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }

    public long getPerTxnLimit() {
        return perTxnLimit;
    }

    public long getDailyLimit() {
        return dailyLimit;
    }

    public SavingInfo getSaving() {
        return saving;
    }

    public MortgageInfo getMortgage() {
        return mortgage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
