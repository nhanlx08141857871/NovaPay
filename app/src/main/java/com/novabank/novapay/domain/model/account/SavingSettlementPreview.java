package com.novabank.novapay.domain.model.account;

public final class SavingSettlementPreview {

    private final long principalAmount;
    private final long interestAmount;
    private final long totalPayoutAmount;
    private final boolean earlySettlement;
    private final double appliedAnnualRate;
    private final long daysHeld;

    public SavingSettlementPreview(
            long principalAmount,
            long interestAmount,
            long totalPayoutAmount,
            boolean earlySettlement,
            double appliedAnnualRate,
            long daysHeld
    ) {
        this.principalAmount = principalAmount;
        this.interestAmount = interestAmount;
        this.totalPayoutAmount = totalPayoutAmount;
        this.earlySettlement = earlySettlement;
        this.appliedAnnualRate = appliedAnnualRate;
        this.daysHeld = daysHeld;
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

    public long getDaysHeld() {
        return daysHeld;
    }
}
