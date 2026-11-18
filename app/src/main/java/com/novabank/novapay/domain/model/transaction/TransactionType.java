package com.novabank.novapay.domain.model.transaction;

/**
 * Loại giao dịch tài chính được hệ thống NovaPay hỗ trợ (GEMINI.md §3).
 */
public enum TransactionType {
    TRANSFER_INTERNAL,
    TRANSFER_INTERBANK,
    DEPOSIT,
    WITHDRAW,
    BILL,
    TOPUP,
    SERVICE_PAYMENT,
    MORTGAGE_PAYMENT,
    GATEWAY
}
