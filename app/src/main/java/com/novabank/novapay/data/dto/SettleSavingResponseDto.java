package com.novabank.novapay.data.dto;

import com.google.gson.annotations.SerializedName;

public final class SettleSavingResponseDto {

    @SerializedName("txId")
    private String txId;

    @SerializedName("principalAmount")
    private long principalAmount;

    @SerializedName("interestAmount")
    private long interestAmount;

    @SerializedName("totalPayoutAmount")
    private long totalPayoutAmount;

    @SerializedName("earlySettlement")
    private boolean earlySettlement;

    @SerializedName("appliedAnnualRate")
    private double appliedAnnualRate;

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
