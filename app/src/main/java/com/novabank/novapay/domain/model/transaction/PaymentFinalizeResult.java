package com.novabank.novapay.domain.model.transaction;

public final class PaymentFinalizeResult {

    private final String txId;
    private final String status;

    public PaymentFinalizeResult(String txId, String status) {
        this.txId = txId;
        this.status = status != null ? status : "";
    }

    public String getTxId() {
        return txId;
    }

    public String getStatus() {
        return status;
    }
}
