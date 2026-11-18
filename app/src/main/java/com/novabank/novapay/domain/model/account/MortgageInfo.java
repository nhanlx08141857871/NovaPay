package com.novabank.novapay.domain.model.account;

import java.time.Instant;

/**
 * Thông tin chuyên biệt của tài khoản Vay thế chấp (MORTGAGE).
 * Dư nợ gốc, khoản vay và số tiền trả định kỳ bắt buộc dùng kiểu long (VNĐ).
 */
public final class MortgageInfo {

    public static final String FREQUENCY_MONTHLY = "MONTHLY";
    public static final String FREQUENCY_BIWEEKLY = "BIWEEKLY";

    private final long loanAmount;
    private final long outstandingBalance;
    private final int termMonths;
    private final double annualRate;
    private final String paymentFrequency;
    private final long installmentAmount;
    private final Instant nextDueDate;

    public MortgageInfo(
            long loanAmount,
            long outstandingBalance,
            int termMonths,
            double annualRate,
            String paymentFrequency,
            long installmentAmount,
            Instant nextDueDate
    ) {
        this.loanAmount = loanAmount;
        this.outstandingBalance = outstandingBalance;
        this.termMonths = termMonths;
        this.annualRate = annualRate;
        this.paymentFrequency = paymentFrequency;
        this.installmentAmount = installmentAmount;
        this.nextDueDate = nextDueDate;
    }

    public long getLoanAmount() {
        return loanAmount;
    }

    public long getOutstandingBalance() {
        return outstandingBalance;
    }

    public int getTermMonths() {
        return termMonths;
    }

    public double getAnnualRate() {
        return annualRate;
    }

    public String getPaymentFrequency() {
        return paymentFrequency;
    }

    public long getInstallmentAmount() {
        return installmentAmount;
    }

    public Instant getNextDueDate() {
        return nextDueDate;
    }
}
