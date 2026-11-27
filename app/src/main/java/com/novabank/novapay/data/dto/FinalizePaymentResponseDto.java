package com.novabank.novapay.data.dto;

public final class FinalizePaymentResponseDto {

    private String txId;
    private String status;

    public FinalizePaymentResponseDto() {
    }

    public FinalizePaymentResponseDto(String txId, String status) {
        this.txId = txId;
        this.status = status;
    }

    public String getTxId() {
        return txId;
    }

    public String getStatus() {
        return status;
    }
}
