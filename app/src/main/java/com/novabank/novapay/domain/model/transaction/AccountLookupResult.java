package com.novabank.novapay.domain.model.transaction;

import java.util.Objects;

/**
 * Kết quả tra cứu tài khoản nội bộ an toàn từ GET /accounts/lookup.
 */
public final class AccountLookupResult {

    private final String accountId;
    private final String accountNumber;
    private final String ownerName;

    public AccountLookupResult(String accountId, String accountNumber, String ownerName) {
        this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
        this.accountNumber = Objects.requireNonNull(accountNumber, "accountNumber must not be null");
        this.ownerName = ownerName != null ? ownerName : "";
    }

    public String getAccountId() {
        return accountId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccountLookupResult that)) return false;
        return Objects.equals(accountId, that.accountId)
                && Objects.equals(accountNumber, that.accountNumber)
                && Objects.equals(ownerName, that.ownerName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, accountNumber, ownerName);
    }
}
