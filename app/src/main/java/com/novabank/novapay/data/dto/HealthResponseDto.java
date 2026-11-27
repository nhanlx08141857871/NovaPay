package com.novabank.novapay.data.dto;

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
