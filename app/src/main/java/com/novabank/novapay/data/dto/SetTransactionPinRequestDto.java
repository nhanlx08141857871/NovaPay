package com.novabank.novapay.data.dto;

import com.google.gson.annotations.SerializedName;

public final class SetTransactionPinRequestDto {

    @SerializedName("currentPin")
    private final String currentPin;

    @SerializedName("newPin")
    private final String newPin;

    public SetTransactionPinRequestDto(String currentPin, String newPin) {
        this.currentPin = currentPin;
        this.newPin = newPin;
    }
}
