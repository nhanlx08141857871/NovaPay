package com.novabank.novapay.data.dto;

public final class EkycReviewRequestDto {
    private final String uid;
    private final String decision;

    public EkycReviewRequestDto(String uid, String decision) {
        this.uid = uid;
        this.decision = decision;
    }
}
