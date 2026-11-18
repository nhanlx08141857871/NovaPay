package com.novabank.novapay.data.dto;

public final class FaceCheckRequestDto {

    private final String draftId;
    private final String imageBase64;

    public FaceCheckRequestDto(String draftId, String imageBase64) {
        this.draftId = draftId;
        this.imageBase64 = imageBase64;
    }

    public String getDraftId() {
        return draftId;
    }

    public String getImageBase64() {
        return imageBase64;
    }
}
