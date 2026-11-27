package com.novabank.novapay.data.dto;

import com.google.gson.annotations.SerializedName;

public final class UpdateAccountControlsRequestDto {

    @SerializedName("status")
    private final String status;

    @SerializedName("perTxnLimit")
    private final Long perTxnLimit;

    @SerializedName("dailyLimit")
    private final Long dailyLimit;

    @SerializedName("pin")
    private final String pin;

    public UpdateAccountControlsRequestDto(
            String status,
            Long perTxnLimit,
            Long dailyLimit,
            String pin
    ) {
        this.status = status;
        this.perTxnLimit = perTxnLimit;
        this.dailyLimit = dailyLimit;
        this.pin = pin;
    }
}
