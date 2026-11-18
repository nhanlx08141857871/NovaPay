package com.novabank.novapay.domain.model.transaction;

/**
 * Kết quả xác nhận OTP từ POST /transactions/confirm.
 * Với giao dịch GATEWAY (Stripe), status là "PENDING_PAYMENT" kèm clientSecret và txId == null.
 */
public final class TransactionConfirmResult {

    private final String txId;
    private final String status;
    private final String clientSecret;

    public TransactionConfirmResult(String txId, String status, String clientSecret) {
        this.txId = txId;
        this.status = status != null ? status : "";
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

    public boolean isPendingPayment() {
        return "PENDING_PAYMENT".equalsIgnoreCase(status);
    }

    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(status) || "CONFIRMED".equalsIgnoreCase(status);
    }
}
