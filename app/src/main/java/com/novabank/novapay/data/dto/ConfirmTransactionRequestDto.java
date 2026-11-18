package com.novabank.novapay.data.dto;

/**
 * Request body cho POST /transactions/confirm.
 */
public final class ConfirmTransactionRequestDto {

    private final String draftId;
    private final String otp;
    private final boolean biometricPassed;

    public ConfirmTransactionRequestDto(String draftId, String otp, boolean biometricPassed) {
        this.draftId = draftId;
        this.otp = otp;
        this.biometricPassed = biometricPassed;
    }

    public String getDraftId() {
        return draftId;
    }

    public String getOtp() {
        return otp;
    }

    public boolean isBiometricPassed() {
        return biometricPassed;
    }
}
