package com.novabank.novapay.presentation.auth;

/**
 * Dữ liệu trạng thái màn hình Đăng nhập.
 */
public final class LoginUiState {

    private final boolean ready;
    private final String authenticatedRole;

    public LoginUiState(boolean ready, String authenticatedRole) {
        this.ready = ready;
        this.authenticatedRole = authenticatedRole;
    }

    public boolean isReady() {
        return ready;
    }

    public String getAuthenticatedRole() {
        return authenticatedRole;
    }
}
