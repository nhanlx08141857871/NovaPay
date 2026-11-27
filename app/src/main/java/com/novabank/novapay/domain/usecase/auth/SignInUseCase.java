package com.novabank.novapay.domain.usecase.auth;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.user.UserRole;
import com.novabank.novapay.domain.repository.AuthRepository;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public final class SignInUseCase {

    private final AuthRepository authRepository;

    @Inject
    public SignInUseCase(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public Single<AppResult<UserRole>> execute(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Email và mật khẩu không được để trống."));
        }
        return authRepository.signIn(email.trim(), password);
    }
}
