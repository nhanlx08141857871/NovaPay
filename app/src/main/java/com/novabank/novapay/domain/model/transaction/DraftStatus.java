package com.novabank.novapay.domain.model.transaction;

/**
 * Trạng thái bản nháp giao dịch trong transaction_drafts.
 */
public enum DraftStatus {
    PENDING_FACE,
    PENDING_OTP,
    PENDING_PAYMENT,
    CONFIRMED,
    EXPIRED
}
