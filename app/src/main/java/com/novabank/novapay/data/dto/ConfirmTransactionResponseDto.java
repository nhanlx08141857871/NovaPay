package com.novabank.novapay.data.dto;

/**
 * Response body từ POST /transactions/confirm.
 */
public final class ConfirmTransactionResponseDto {

    private String txId;
    private String status;
    private String clientSecret;

    public ConfirmTransactionResponseDto() {
    }

    public ConfirmTransactionResponseDto(String txId, String status, String clientSecret) {
        this.txId = txId;
        this.status = status;
        this.clientSecret = clientSecret;
    }

    public String getTxId() {
        return txId;
    }

    public String getStatus() {
        return status;
    }

    public String getClientSecret() {
        return clientSecret;
    }
}
