package com.novabank.novapay.core.common;

import java.util.Objects;

public final class AppResult<T> {

    public enum Status {
        SUCCESS,
        ERROR
    }

    private final Status status;
    private final T data;
    private final String errorCode;
    private final String message;

    private AppResult(Status status, T data, String errorCode, String message) {
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.data = data;
        this.errorCode = errorCode;
        this.message = message;
    }

    public static <T> AppResult<T> success(T data) {
        return new AppResult<>(Status.SUCCESS, data, null, null);
    }

    public static <T> AppResult<T> error(String errorCode, String message) {
        return new AppResult<>(
                Status.ERROR,
                null,
                errorCode != null ? errorCode : "UNKNOWN_ERROR",
                message != null ? message : ""
        );
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isError() {
        return status == Status.ERROR;
    }

    public Status getStatus() {
        return status;
    }

    public T getData() {
        return data;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }
}
