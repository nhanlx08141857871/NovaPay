package com.novabank.novapay.data.dto;

import com.google.gson.annotations.SerializedName;

public final class SecurityStatusResponseDto {

    @SerializedName("hasTransactionPin")
    private boolean hasTransactionPin;

    @SerializedName("pinLockedUntilEpochMs")
    private Long pinLockedUntilEpochMs;

    public boolean isHasTransactionPin() {
        return hasTransactionPin;
    }

    public Long getPinLockedUntilEpochMs() {
        return pinLockedUntilEpochMs;
    }
}
