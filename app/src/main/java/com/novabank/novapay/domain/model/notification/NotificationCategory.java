package com.novabank.novapay.domain.model.notification;

import java.util.Locale;

public enum NotificationCategory {
    BALANCE,
    SECURITY,
    SAVING,
    EKYC;

    public static NotificationCategory fromString(String raw) {
        if (raw == null) {
            return BALANCE;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        for (NotificationCategory value : values()) {
            if (value.name().equals(normalized)) {
                return value;
            }
        }
        return BALANCE;
    }
}
