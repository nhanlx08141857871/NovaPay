package com.novabank.novapay.presentation.ekyc;

public final class EkycCaptureUiState {

    private final boolean cameraReady;
    private final boolean livenessPassed;
    private final int guideMessageRes;

    public EkycCaptureUiState(boolean cameraReady, boolean livenessPassed, int guideMessageRes) {
        this.cameraReady = cameraReady;
        this.livenessPassed = livenessPassed;
        this.guideMessageRes = guideMessageRes;
    }

    public boolean isCameraReady() {
        return cameraReady;
    }

    public boolean isLivenessPassed() {
        return livenessPassed;
    }

    public int getGuideMessageRes() {
        return guideMessageRes;
    }
}
