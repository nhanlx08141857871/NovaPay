package com.novabank.novapay.data.dto;

/**
 * Response body từ GET /health.
 */
public final class HealthResponseDto {

    private boolean ok;

    public HealthResponseDto() {
    }

    public HealthResponseDto(boolean ok) {
        this.ok = ok;
    }

    public boolean isOk() {
        return ok;
    }
}
