package com.novabank.novapay.domain.usecase.security;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.repository.AuthRepository;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public final class ChangePasswordUseCase {

    public static final int MIN_PASSWORD_LENGTH = 6;

    private final AuthRepository authRepository;

    @Inject
    public ChangePasswordUseCase(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public Single<AppResult<Boolean>> execute(String currentPassword, String newPassword) {
        if (currentPassword == null || currentPassword.isEmpty()) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Vui lòng nhập mật khẩu hiện tại."));
        }
        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            return Single.just(AppResult.error(
                    "INVALID_ARGUMENT",
                    "Mật khẩu mới phải có ít nhất 6 ký tự."
            ));
        }
        if (currentPassword.equals(newPassword)) {
            return Single.just(AppResult.error(
                    "INVALID_ARGUMENT",
                    "Mật khẩu mới phải khác mật khẩu hiện tại."
            ));
        }
        return authRepository.changePassword(currentPassword, newPassword);
    }
}
