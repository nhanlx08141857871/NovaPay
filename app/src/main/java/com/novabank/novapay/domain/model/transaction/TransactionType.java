package com.novabank.novapay.domain.model.transaction;

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
