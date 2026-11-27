package com.novabank.novapay.domain.model.account;

import java.util.Objects;

public final class SettleSavingResult {

    private final String txId;
    private final long principalAmount;
    private final long interestAmount;
    private final long totalPayoutAmount;
    private final boolean earlySettlement;
    private final double appliedAnnualRate;

    public SettleSavingResult(
            String txId,
            long principalAmount,
            long interestAmount,
            long totalPayoutAmount,
            boolean earlySettlement,
            double appliedAnnualRate
    ) {
        this.txId = Objects.requireNonNull(txId, "txId must not be null");
        this.principalAmount = principalAmount;
        this.interestAmount = interestAmount;
        this.totalPayoutAmount = totalPayoutAmount;
        this.earlySettlement = earlySettlement;
        this.appliedAnnualRate = appliedAnnualRate;
    }

    public String getTxId() {
        return txId;
    }

    public long getPrincipalAmount() {
        return principalAmount;
    }

    public long getInterestAmount() {
        return interestAmount;
    }

    public long getTotalPayoutAmount() {
        return totalPayoutAmount;
    }

    public boolean isEarlySettlement() {
        return earlySettlement;
    }

    public double getAppliedAnnualRate() {
        return appliedAnnualRate;
    }
}
