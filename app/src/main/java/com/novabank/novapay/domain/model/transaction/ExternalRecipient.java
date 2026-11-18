package com.novabank.novapay.domain.model.transaction;

import java.util.Objects;

/**
 * Thông tin người nhận liên ngân hàng (TRANSFER_INTERBANK).
 */
public final class ExternalRecipient {

    private final String bankCode;
    private final String accountNumber;
    private final String name;

    public ExternalRecipient(String bankCode, String accountNumber, String name) {
        this.bankCode = bankCode != null ? bankCode.trim() : "";
        this.accountNumber = accountNumber != null ? accountNumber.trim() : "";
        this.name = name != null ? name.trim() : "";
    }

    public String getBankCode() {
        return bankCode;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExternalRecipient that)) return false;
        return Objects.equals(bankCode, that.bankCode)
                && Objects.equals(accountNumber, that.accountNumber)
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bankCode, accountNumber, name);
    }
}
