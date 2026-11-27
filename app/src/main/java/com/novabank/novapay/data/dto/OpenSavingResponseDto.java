package com.novabank.novapay.data.dto;

import com.google.gson.annotations.SerializedName;

public final class OpenSavingResponseDto {

    @SerializedName("savingAccountId")
    private String savingAccountId;

    @SerializedName("accountNumber")
    private String accountNumber;

    @SerializedName("txId")
    private String txId;

    @SerializedName("annualRate")
    private double annualRate;

    @SerializedName("maturityDateEpochMs")
    private long maturityDateEpochMs;

    public String getSavingAccountId() {
        return savingAccountId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getTxId() {
        return txId;
    }

    public double getAnnualRate() {
        return annualRate;
    }

    public long getMaturityDateEpochMs() {
        return maturityDateEpochMs;
    }
}
