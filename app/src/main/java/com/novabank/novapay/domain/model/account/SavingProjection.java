package com.novabank.novapay.domain.model.account;

import com.novabank.novapay.domain.model.rate.InterestRate;

import java.util.Objects;

public final class SavingProjection {

    private final Account account;
    private final InterestRate interestRate;
    private final double effectiveAnnualRate;
    private final long monthlyProfit;

    public SavingProjection(
            Account account,
            InterestRate interestRate,
            double effectiveAnnualRate,
            long monthlyProfit
    ) {
        this.account = Objects.requireNonNull(account, "account must not be null");
        this.interestRate = interestRate;
        this.effectiveAnnualRate = effectiveAnnualRate;
        this.monthlyProfit = monthlyProfit;
    }

    public Account getAccount() {
        return account;
    }

    public InterestRate getInterestRate() {
        return interestRate;
    }

    public double getEffectiveAnnualRate() {
        return effectiveAnnualRate;
    }

    public long getMonthlyProfit() {
        return monthlyProfit;
    }
}
