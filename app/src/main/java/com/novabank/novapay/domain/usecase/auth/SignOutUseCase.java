package com.novabank.novapay.domain.usecase.auth;

import com.novabank.novapay.domain.repository.AuthRepository;

import javax.inject.Inject;

/**
 * UseCase đăng xuất khỏi hệ thống NovaPay.
 */
public final class SignOutUseCase {

    private final AuthRepository authRepository;

    @Inject
    public SignOutUseCase(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public void execute() {
        authRepository.signOut();
    }
}
