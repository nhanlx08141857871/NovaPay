package com.novabank.novapay.data.dto;

import com.google.gson.annotations.SerializedName;

public final class UpdateAccountControlsResponseDto {

    @SerializedName("ok")
    private boolean ok;

    @SerializedName("status")
    private String status;

    @SerializedName("perTxnLimit")
    private long perTxnLimit;

    @SerializedName("dailyLimit")
    private long dailyLimit;

    public boolean isOk() {
        return ok;
    }

    public String getStatus() {
        return status;
    }

    public long getPerTxnLimit() {
        return perTxnLimit;
    }

    public long getDailyLimit() {
        return dailyLimit;
    }
}
