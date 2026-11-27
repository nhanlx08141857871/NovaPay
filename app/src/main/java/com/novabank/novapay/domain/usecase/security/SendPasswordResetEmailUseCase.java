package com.novabank.novapay.domain.usecase.security;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.repository.AuthRepository;

import java.util.regex.Pattern;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public final class SendPasswordResetEmailUseCase {

    private static final Pattern SIMPLE_EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final AuthRepository authRepository;

    @Inject
    public SendPasswordResetEmailUseCase(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public Single<AppResult<Boolean>> execute(String rawEmail) {
        String email = rawEmail != null ? rawEmail.trim() : "";
        if (email.isEmpty() || !SIMPLE_EMAIL_PATTERN.matcher(email).matches()) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Địa chỉ email không hợp lệ."));
        }
        return authRepository.sendPasswordResetEmail(email);
    }
}
