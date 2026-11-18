package com.novabank.novapay.data.dto;

/**
 * Request body dùng chung cho POST /transactions/resend-otp và POST /payments/finalize.
 */
public final class DraftRequestDto {

    private final String draftId;

    public DraftRequestDto(String draftId) {
        this.draftId = draftId;
    }

    public String getDraftId() {
        return draftId;
    }
}
