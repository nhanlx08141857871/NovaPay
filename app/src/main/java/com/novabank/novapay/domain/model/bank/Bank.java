package com.novabank.novapay.domain.model.bank;

import java.util.Objects;

public final class Bank {

    private final String bankCode;
    private final String name;
    private final String logoUrl;
    private final String napasCode;

    public Bank(String bankCode, String name, String logoUrl, String napasCode) {
        this.bankCode = Objects.requireNonNull(bankCode, "bankCode must not be null");
        this.name = name != null ? name : bankCode;
        this.logoUrl = logoUrl != null ? logoUrl : "";
        this.napasCode = napasCode != null ? napasCode : "";
    }

    public String getBankCode() {
        return bankCode;
    }

    public String getName() {
        return name;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getNapasCode() {
        return napasCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bank bank)) return false;
        return Objects.equals(bankCode, bank.bankCode)
                && Objects.equals(name, bank.name)
                && Objects.equals(logoUrl, bank.logoUrl)
                && Objects.equals(napasCode, bank.napasCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bankCode, name, logoUrl, napasCode);
    }
}
