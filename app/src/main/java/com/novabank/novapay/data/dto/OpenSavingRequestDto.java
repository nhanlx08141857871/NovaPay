package com.novabank.novapay.data.dto;

import com.google.gson.annotations.SerializedName;

public final class OpenSavingRequestDto {

    @SerializedName("fromCheckingAccountId")
    private final String fromCheckingAccountId;

    @SerializedName("termMonths")
    private final int termMonths;

    @SerializedName("principalAmount")
    private final long principalAmount;

    @SerializedName("pin")
    private final String pin;

    public OpenSavingRequestDto(
            String fromCheckingAccountId,
            int termMonths,
            long principalAmount,
            String pin
    ) {
        this.fromCheckingAccountId = fromCheckingAccountId;
        this.termMonths = termMonths;
        this.principalAmount = principalAmount;
        this.pin = pin;
    }
}
