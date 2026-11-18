package com.novabank.novapay.domain.model.bank;

import java.util.Objects;

/**
 * Danh bạ thụ hưởng đã lưu tại users/{uid}/beneficiaries/{id}.
 */
public final class Beneficiary {

    private final String id;
    private final String name;
    private final String accountNumber;
    private final String bankCode;
    private final String alias;

    public Beneficiary(String id, String name, String accountNumber, String bankCode, String alias) {
        this.id = id != null ? id.trim() : "";
        this.name = name != null ? name.trim() : "";
        this.accountNumber = accountNumber != null ? accountNumber.trim() : "";
        this.bankCode = bankCode != null ? bankCode.trim() : "";
        this.alias = alias != null ? alias.trim() : "";
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getBankCode() {
        return bankCode;
    }

    public String getAlias() {
        return alias;
    }

    public boolean isInternal() {
        return bankCode.isEmpty() || "NOVAPAY".equalsIgnoreCase(bankCode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Beneficiary that)) return false;
        return Objects.equals(id, that.id)
                && Objects.equals(name, that.name)
                && Objects.equals(accountNumber, that.accountNumber)
                && Objects.equals(bankCode, that.bankCode)
                && Objects.equals(alias, that.alias);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, accountNumber, bankCode, alias);
    }
}
