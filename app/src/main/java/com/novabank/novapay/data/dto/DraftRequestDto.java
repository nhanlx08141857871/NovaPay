package com.novabank.novapay.data.dto;

public final class DraftRequestDto {

    private final String draftId;

    public DraftRequestDto(String draftId) {
        this.draftId = draftId;
    }

    public String getDraftId() {
        return draftId;
    }
}
