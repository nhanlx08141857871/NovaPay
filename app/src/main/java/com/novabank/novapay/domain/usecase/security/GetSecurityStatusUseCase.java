package com.novabank.novapay.domain.usecase.security;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.security.SecurityStatus;
import com.novabank.novapay.domain.repository.SecurityRepository;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public final class GetSecurityStatusUseCase {

    private final SecurityRepository securityRepository;

    @Inject
    public GetSecurityStatusUseCase(SecurityRepository securityRepository) {
        this.securityRepository = securityRepository;
    }

    public Single<AppResult<SecurityStatus>> execute() {
        return securityRepository.getSecurityStatus();
    }
}
