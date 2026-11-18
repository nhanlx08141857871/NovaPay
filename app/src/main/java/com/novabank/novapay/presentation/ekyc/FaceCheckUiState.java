package com.novabank.novapay.presentation.ekyc;

public final class FaceCheckUiState {

    private final boolean livenessPassed;
    private final int guideMessageRes;

    public FaceCheckUiState(boolean livenessPassed, int guideMessageRes) {
        this.livenessPassed = livenessPassed;
        this.guideMessageRes = guideMessageRes;
    }

    public boolean isLivenessPassed() {
        return livenessPassed;
    }

    public int getGuideMessageRes() {
        return guideMessageRes;
    }
}
