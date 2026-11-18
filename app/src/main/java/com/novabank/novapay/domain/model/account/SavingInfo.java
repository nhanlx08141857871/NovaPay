package com.novabank.novapay.domain.model.account;

import java.time.Instant;

/**
 * Thông tin chuyên biệt của tài khoản Tiết kiệm (SAVING).
 * Tiền gốc và tiền lãi bắt buộc dùng kiểu long (VNĐ); thời gian dùng java.time.Instant.
 */
public final class SavingInfo {

    private final int termMonths;
    private final long principal;
    private final String interestRateId;
    private final Instant openedAt;
    private final Instant maturityDate;

    public SavingInfo(
            int termMonths,
            long principal,
            String interestRateId,
            Instant openedAt,
            Instant maturityDate
    ) {
        this.termMonths = termMonths;
        this.principal = principal;
        this.interestRateId = interestRateId;
        this.openedAt = openedAt;
        this.maturityDate = maturityDate;
    }

    /**
     * Tính tiền lãi dự kiến hàng tháng kiểu long theo công thức bắt buộc tại GEMINI.md:
     * monthlyProfit = floor((balance * annualRate) / (12.0 * 100.0))
     */
    public static long calculateMonthlyProfit(long balance, double annualRate) {
        if (balance <= 0L || annualRate <= 0.0) {
            return 0L;
        }
        return (long) Math.floor((balance * annualRate) / (12.0 * 100.0));
    }

    public int getTermMonths() {
        return termMonths;
    }

    public long getPrincipal() {
        return principal;
    }

    public String getInterestRateId() {
        return interestRateId;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public Instant getMaturityDate() {
        return maturityDate;
    }
}
