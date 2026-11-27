package com.novabank.novapay.domain.model.user;

import java.time.Instant;

/**
 * Thông tin trạng thái PIN giao dịch được đồng bộ trên hồ sơ users/{uid}.security (Pure Java 17).
 */
public final class UserSecurityInfo {

    private final boolean hasPin;
    private final Instant pinUpdatedAt;

    public UserSecurityInfo(boolean hasPin, Instant pinUpdatedAt) {
        this.hasPin = hasPin;
        this.pinUpdatedAt = pinUpdatedAt;
    }

    public boolean hasPin() {
        return hasPin;
    }

    public Instant getPinUpdatedAt() {
        return pinUpdatedAt;
    }
}
