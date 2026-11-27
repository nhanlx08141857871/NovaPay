package com.novabank.novapay.presentation.customer.account;

import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.account.SavingProjection;

import java.util.Collections;
import java.util.List;

public final class SavingDetailUiState {

    private final SavingProjection projection;
    private final List<Account> checkingAccounts;
    private final double demandAnnualRate;
    private final boolean hasTransactionPin;
    private final boolean settling;

    public SavingDetailUiState(
            SavingProjection projection,
            List<Account> checkingAccounts,
            double demandAnnualRate,
            boolean hasTransactionPin,
            boolean settling
    ) {
        this.projection = projection;
        this.checkingAccounts = checkingAccounts != null ? checkingAccounts : Collections.emptyList();
        this.demandAnnualRate = demandAnnualRate;
        this.hasTransactionPin = hasTransactionPin;
        this.settling = settling;
    }

    public SavingProjection getProjection() {
        return projection;
    }

    public List<Account> getCheckingAccounts() {
        return checkingAccounts;
    }

    public double getDemandAnnualRate() {
        return demandAnnualRate;
    }

    public boolean hasTransactionPin() {
        return hasTransactionPin;
    }

    public boolean isSettling() {
        return settling;
    }
}
