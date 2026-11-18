package com.novabank.novapay.data.dto;

public final class FaceCheckResponseDto {

    private boolean ok;
    private String otpExpiresAt;

    public boolean isOk() {
        return ok;
    }

    public String getOtpExpiresAt() {
        return otpExpiresAt;
    }
}
