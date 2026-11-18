package com.novabank.novapay.presentation.auth;

/**
 * Dữ liệu trạng thái màn hình Đăng nhập (khung Day-0 chuẩn MVVM cho Thành viên A mở rộng ở 3B).
 */
public final class LoginUiState {

    private final boolean ready;

    public LoginUiState(boolean ready) {
        this.ready = ready;
    }

    public boolean isReady() {
        return ready;
    }
}
