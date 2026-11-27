package com.novabank.novapay.domain.model.user;

public enum UserStatus {
    ACTIVE,
    LOCKED;

    public static UserStatus fromString(String raw) {
        if (raw != null && "LOCKED".equalsIgnoreCase(raw.trim())) {
            return LOCKED;
        }
        return ACTIVE;
    }
}
