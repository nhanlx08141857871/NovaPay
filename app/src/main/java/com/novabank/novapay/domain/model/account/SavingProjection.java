package com.novabank.novapay.domain.model.account;

import com.novabank.novapay.domain.model.rate.InterestRate;

import java.util.Objects;

/**
 * Kết quả kết hợp realtime giữa Tài khoản Tiết kiệm và Biểu lãi suất động (SPEC_CHOT.md Q9).
 * Tiền lãi dự kiến hàng tháng bắt buộc dùng kiểu long (VNĐ).
 */
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
