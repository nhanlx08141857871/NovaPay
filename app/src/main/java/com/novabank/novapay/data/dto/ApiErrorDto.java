package com.novabank.novapay.data.dto;

/**
 * Phong bì lỗi chuẩn từ Node.js Server (server/src/middleware/auth.ts): { "error": ..., "message": ... }.
 */
public final class ApiErrorDto {

    private String error;
    private String message;

    public ApiErrorDto() {
    }

    public ApiErrorDto(String error, String message) {
        this.error = error;
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }
}
