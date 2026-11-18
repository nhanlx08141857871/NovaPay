package com.novabank.novapay.data.dto;

public final class EkycSubmitRequestDto {

    private final String imageBase64;
    private final String targetUid;

    public EkycSubmitRequestDto(String imageBase64, String targetUid) {
        this.imageBase64 = imageBase64;
        this.targetUid = targetUid;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public String getTargetUid() {
        return targetUid;
    }
}
