package com.novabank.novapay.domain.model.user;

/**
 * Vai trò người dùng trên hệ thống NovaPay (khớp Firebase Custom Claim role).
 */
public enum UserRole {
    CUSTOMER,
    OFFICER;

    public static UserRole fromString(String raw) {
        if (raw != null && "OFFICER".equalsIgnoreCase(raw.trim())) {
            return OFFICER;
        }
        return CUSTOMER;
    }

    public String asClaimValue() {
        return name();
    }
}
