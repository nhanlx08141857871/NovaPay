package com.novabank.novapay.data.dto;

public final class SaveInterestRateRequestDto {
    private final int termMonths;
    private final double annualRate;

    public SaveInterestRateRequestDto(int termMonths, double annualRate) {
        this.termMonths = termMonths;
        this.annualRate = annualRate;
    }
}
