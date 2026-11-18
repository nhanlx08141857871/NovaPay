package com.novabank.novapay.domain.model.user;

/**
 * Trạng thái định danh khuôn mặt điện tử (eKYC).
 */
public enum EkycStatus {
    UNVERIFIED,
    PENDING,
    VERIFIED,
    REJECTED;

    public static EkycStatus fromString(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return UNVERIFIED;
        }
        String normalized = raw.trim().toUpperCase();
        switch (normalized) {
            case "PENDING":
                return PENDING;
            case "VERIFIED":
                return VERIFIED;
            case "REJECTED":
                return REJECTED;
            default:
                return UNVERIFIED;
        }
    }
}
