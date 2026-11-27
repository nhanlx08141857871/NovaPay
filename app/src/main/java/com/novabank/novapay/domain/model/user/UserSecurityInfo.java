package com.novabank.novapay.domain.model.user;

import java.time.Instant;

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
