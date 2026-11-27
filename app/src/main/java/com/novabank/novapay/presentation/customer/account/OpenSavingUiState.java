package com.novabank.novapay.presentation.customer.account;

import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.rate.InterestRate;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public final class OpenSavingUiState {

    private final List<Account> checkingAccounts;
    private final List<InterestRate> termRates;
    private final Account selectedAccount;
    private final InterestRate selectedRate;
    private final long principalAmount;
    private final long monthlyInterest;
    private final long totalMaturityInterest;
    private final long totalMaturityPayout;
    private final Instant estimatedMaturityDate;
    private final boolean hasTransactionPin;
    private final boolean submitting;

    public OpenSavingUiState(
            List<Account> checkingAccounts,
            List<InterestRate> termRates,
            Account selectedAccount,
            InterestRate selectedRate,
            long principalAmount,
            long monthlyInterest,
            long totalMaturityInterest,
            long totalMaturityPayout,
            Instant estimatedMaturityDate,
            boolean hasTransactionPin,
            boolean submitting
    ) {
        this.checkingAccounts = checkingAccounts != null ? checkingAccounts : Collections.emptyList();
        this.termRates = termRates != null ? termRates : Collections.emptyList();
        this.selectedAccount = selectedAccount;
        this.selectedRate = selectedRate;
        this.principalAmount = principalAmount;
        this.monthlyInterest = monthlyInterest;
        this.totalMaturityInterest = totalMaturityInterest;
        this.totalMaturityPayout = totalMaturityPayout;
        this.estimatedMaturityDate = estimatedMaturityDate;
        this.hasTransactionPin = hasTransactionPin;
        this.submitting = submitting;
    }

    public List<Account> getCheckingAccounts() {
        return checkingAccounts;
    }

    public List<InterestRate> getTermRates() {
        return termRates;
    }

    public Account getSelectedAccount() {
        return selectedAccount;
    }

    public InterestRate getSelectedRate() {
        return selectedRate;
    }

    public long getPrincipalAmount() {
        return principalAmount;
    }

    public long getMonthlyInterest() {
        return monthlyInterest;
    }

    public long getTotalMaturityInterest() {
        return totalMaturityInterest;
    }

    public long getTotalMaturityPayout() {
        return totalMaturityPayout;
    }

    public Instant getEstimatedMaturityDate() {
        return estimatedMaturityDate;
    }

    public boolean hasTransactionPin() {
        return hasTransactionPin;
    }

    public boolean isSubmitting() {
        return submitting;
    }
}
