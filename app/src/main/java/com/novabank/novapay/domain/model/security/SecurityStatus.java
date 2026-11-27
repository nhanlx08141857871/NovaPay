package com.novabank.novapay.domain.model.security;

import java.time.Instant;

public final class SecurityStatus {

    private final boolean hasTransactionPin;
    private final Instant pinLockedUntil;

    public SecurityStatus(boolean hasTransactionPin, Instant pinLockedUntil) {
        this.hasTransactionPin = hasTransactionPin;
        this.pinLockedUntil = pinLockedUntil;
    }

    public boolean hasTransactionPin() {
        return hasTransactionPin;
    }

    public Instant getPinLockedUntil() {
        return pinLockedUntil;
    }

    public boolean isPinLockedNow(Instant now) {
        if (pinLockedUntil == null) {
            return false;
        }
        Instant ref = now != null ? now : Instant.now();
        return pinLockedUntil.isAfter(ref);
    }
}
