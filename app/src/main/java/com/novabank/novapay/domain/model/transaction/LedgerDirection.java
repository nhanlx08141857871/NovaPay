package com.novabank.novapay.domain.model.transaction;

/**
 * Chiều biến động số dư trong sổ cái tài khoản (IN = tiền vào, OUT = tiền ra).
 */
public enum LedgerDirection {
    IN,
    OUT;

    public static LedgerDirection fromString(String raw) {
        if (raw != null && "IN".equalsIgnoreCase(raw.trim())) {
            return IN;
        }
        return OUT;
    }
}
