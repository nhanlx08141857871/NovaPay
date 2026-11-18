package com.novabank.novapay.domain.model.rate;

import java.time.Instant;
import java.util.Objects;

/**
 * Biểu lãi suất tiết kiệm theo kỳ hạn từ collection interest_rates/{rateId} (Pure Java 17).
 */
public final class InterestRate {

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
}
