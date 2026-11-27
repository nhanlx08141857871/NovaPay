package com.novabank.novapay.domain.model.transaction;

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
