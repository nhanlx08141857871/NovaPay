package com.novabank.novapay.data.dto;

import com.google.gson.JsonElement;

/**
 * Response body từ POST /transactions/initiate.
 * otpExpiresAt dùng JsonElement để hỗ trợ an toàn cả epoch millis (số) lẫn ISO-8601 (chuỗi) hoặc null.
 */
public final class InitiateTransactionResponseDto {

    private String draftId;
    private boolean requiresBiometric;
    private boolean requiresFace;
    private JsonElement otpExpiresAt;

    public InitiateTransactionResponseDto() {
    }

    public InitiateTransactionResponseDto(
            String draftId,
            boolean requiresBiometric,
            boolean requiresFace,
            JsonElement otpExpiresAt
    ) {
        this.draftId = draftId;
        this.requiresBiometric = requiresBiometric;
        this.requiresFace = requiresFace;
        this.otpExpiresAt = otpExpiresAt;
    }

    public String getDraftId() {
        return draftId;
    }

    public boolean isRequiresBiometric() {
        return requiresBiometric;
    }

    public boolean isRequiresFace() {
        return requiresFace;
    }

    public JsonElement getOtpExpiresAt() {
        return otpExpiresAt;
    }
}
