package com.novabank.novapay.core.common;

import java.util.Objects;

/**
 * Chuẩn hóa 4 trạng thái giao diện bắt buộc (LOADING, SUCCESS, EMPTY, ERROR)
 * theo quy chuẩn MVVM tại GEMINI.md.
 */
public final class UiState<T> {

    public enum Status {
        LOADING,
        SUCCESS,
        EMPTY,
        ERROR
    }

    private final Status status;
    private final T data;
    private final String errorMessage;

    private UiState(Status status, T data, String errorMessage) {
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.data = data;
        this.errorMessage = errorMessage;
    }

    public static <T> UiState<T> loading() {
        return new UiState<>(Status.LOADING, null, null);
    }

    public static <T> UiState<T> success(T data) {
        return new UiState<>(Status.SUCCESS, data, null);
    }

    public static <T> UiState<T> empty() {
        return new UiState<>(Status.EMPTY, null, null);
    }

    public static <T> UiState<T> error(String errorMessage) {
        return new UiState<>(Status.ERROR, null, errorMessage);
    }

    public Status getStatus() {
        return status;
    }

    public T getData() {
        return data;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
