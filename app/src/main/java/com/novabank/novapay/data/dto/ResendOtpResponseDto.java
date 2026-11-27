package com.novabank.novapay.data.dto;

import com.google.gson.JsonElement;

public final class ResendOtpResponseDto {

    private JsonElement otpExpiresAt;

    public ResendOtpResponseDto() {
    }

    public ResendOtpResponseDto(JsonElement otpExpiresAt) {
        this.otpExpiresAt = otpExpiresAt;
    }

    public JsonElement getOtpExpiresAt() {
        return otpExpiresAt;
    }
}
