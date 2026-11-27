package com.novabank.novapay.domain.model.account;

import java.time.Instant;
import java.util.Objects;

public final class OpenSavingResult {

    private final String savingAccountId;
    private final String accountNumber;
    private final String txId;
    private final double annualRate;
    private final Instant maturityDate;

    public OpenSavingResult(
            String savingAccountId,
            String accountNumber,
            String txId,
            double annualRate,
            Instant maturityDate
    ) {
        this.savingAccountId = Objects.requireNonNull(savingAccountId, "savingAccountId must not be null");
        this.accountNumber = accountNumber != null ? accountNumber : "";
        this.txId = txId != null ? txId : "";
        this.annualRate = annualRate;
        this.maturityDate = maturityDate;
    }

    public String getSavingAccountId() {
        return savingAccountId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getTxId() {
        return txId;
    }

    public double getAnnualRate() {
        return annualRate;
    }

    public Instant getMaturityDate() {
        return maturityDate;
    }
}
