package com.novabank.novapay.presentation.ekyc;

import androidx.annotation.Nullable;

public final class FaceCheckUiState {

    private final boolean livenessPassed;
    private final int guideMessageRes;
    @Nullable
    private final String inlineError;

    public FaceCheckUiState(boolean livenessPassed, int guideMessageRes) {
        this(livenessPassed, guideMessageRes, null);
    }

    public FaceCheckUiState(boolean livenessPassed, int guideMessageRes, @Nullable String inlineError) {
        this.livenessPassed = livenessPassed;
        this.guideMessageRes = guideMessageRes;
        this.inlineError = (inlineError != null && !inlineError.trim().isEmpty())
                ? inlineError.trim()
                : null;
    }

    public boolean isLivenessPassed() {
        return livenessPassed;
    }

    public int getGuideMessageRes() {
        return guideMessageRes;
    }

    @Nullable
    public String getInlineError() {
        return inlineError;
    }
}
