package com.novabank.novapay.domain.model.rate;

import java.time.Instant;
import java.util.Objects;

/**
 * Biểu lãi suất tiết kiệm theo kỳ hạn từ collection interest_rates/{rateId} (Pure Java 17).
 */
public final class InterestRate {

    public static final int MAX_TERM_MONTHS = 120;
    public static final double MAX_ANNUAL_RATE_PERCENT = 50.0;

    private final String rateId;
    private final int termMonths;
    private final double annualRate;
    private final Instant effectiveFrom;
    private final String updatedBy;
    private final Instant updatedAt;

    public InterestRate(
            String rateId,
            int termMonths,
            double annualRate,
            Instant effectiveFrom,
            String updatedBy,
            Instant updatedAt
    ) {
        this.rateId = Objects.requireNonNull(rateId, "rateId must not be null");
        this.termMonths = termMonths;
        this.annualRate = annualRate;
        this.effectiveFrom = effectiveFrom;
        this.updatedBy = updatedBy != null ? updatedBy : "";
        this.updatedAt = updatedAt;
    }

    public static boolean isValidTermAndRate(int termMonths, double annualRate) {
        return termMonths >= 0
                && termMonths <= MAX_TERM_MONTHS
                && Double.isFinite(annualRate)
                && annualRate > 0.0
                && annualRate <= MAX_ANNUAL_RATE_PERCENT;
    }

    public String getRateId() {
        return rateId;
    }

    public int getTermMonths() {
        return termMonths;
    }

    public double getAnnualRate() {
        return annualRate;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InterestRate that)) return false;
        return termMonths == that.termMonths
                && Double.compare(that.annualRate, annualRate) == 0
                && Objects.equals(rateId, that.rateId)
                && Objects.equals(effectiveFrom, that.effectiveFrom)
                && Objects.equals(updatedBy, that.updatedBy)
                && Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rateId, termMonths, annualRate, effectiveFrom, updatedBy, updatedAt);
    }
}
