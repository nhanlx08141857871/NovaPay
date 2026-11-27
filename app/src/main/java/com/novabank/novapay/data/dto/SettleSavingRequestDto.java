package com.novabank.novapay.data.dto;

import com.google.gson.annotations.SerializedName;

public final class SettleSavingRequestDto {

    @SerializedName("toCheckingAccountId")
    private final String toCheckingAccountId;

    @SerializedName("pin")
    private final String pin;

    public SettleSavingRequestDto(String toCheckingAccountId, String pin) {
        this.toCheckingAccountId = toCheckingAccountId;
        this.pin = pin;
    }
}
