package com.novabank.novapay.presentation.customer.otp;

import androidx.annotation.Nullable;

public final class OtpVerificationUiState {

    private final String draftId;
    private final boolean requiresFace;
    private final boolean facePassed;
    private final boolean requiresBiometric;
    private final boolean biometricPassed;
    private final long otpExpiresAtEpochMs;
    private final String inlineError;

    public OtpVerificationUiState(
            String draftId,
            boolean requiresFace,
            boolean facePassed,
            boolean requiresBiometric,
            boolean biometricPassed,
            long otpExpiresAtEpochMs,
            @Nullable String inlineError
    ) {
        this.draftId = draftId != null ? draftId : "";
        this.requiresFace = requiresFace;
        this.facePassed = facePassed;
        this.requiresBiometric = requiresBiometric;
        this.biometricPassed = biometricPassed;
        this.otpExpiresAtEpochMs = otpExpiresAtEpochMs;
        this.inlineError = inlineError;
    }

    public boolean isWaitingForFace() {
        return requiresFace && !facePassed;
    }

    public boolean isWaitingForBiometric() {
        return (!requiresFace || facePassed) && requiresBiometric && !biometricPassed;
    }

    public boolean isReadyForOtpInput() {
        return (!requiresFace || facePassed) && (!requiresBiometric || biometricPassed);
    }

    public String getDraftId() {
        return draftId;
    }

    public boolean isRequiresFace() {
        return requiresFace;
    }

    public boolean isFacePassed() {
        return facePassed;
    }

    public boolean isRequiresBiometric() {
        return requiresBiometric;
    }

    public boolean isBiometricPassed() {
        return biometricPassed;
    }

    public long getOtpExpiresAtEpochMs() {
        return otpExpiresAtEpochMs;
    }

    @Nullable
    public String getInlineError() {
        return inlineError;
    }
}
