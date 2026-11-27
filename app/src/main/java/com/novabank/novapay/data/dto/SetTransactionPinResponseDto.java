package com.novabank.novapay.data.dto;

import com.google.gson.annotations.SerializedName;

public final class SetTransactionPinResponseDto {

    @SerializedName("ok")
    private boolean ok;

    @SerializedName("hasTransactionPin")
    private boolean hasTransactionPin;

    public boolean isOk() {
        return ok;
    }

    public boolean isHasTransactionPin() {
        return hasTransactionPin;
    }
}
