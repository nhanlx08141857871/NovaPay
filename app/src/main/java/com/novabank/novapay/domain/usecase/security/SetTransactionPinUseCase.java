package com.novabank.novapay.domain.usecase.security;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.security.SecurityStatus;
import com.novabank.novapay.domain.repository.SecurityRepository;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public final class SetTransactionPinUseCase {

    private final SecurityRepository securityRepository;

    @Inject
    public SetTransactionPinUseCase(SecurityRepository securityRepository) {
        this.securityRepository = securityRepository;
    }

    public Single<AppResult<SecurityStatus>> execute(
            boolean hasExistingPin,
            String currentPinOrNull,
            String newPin
    ) {
        String normalizedCurrent = currentPinOrNull != null ? currentPinOrNull.trim() : "";
        String normalizedNew = newPin != null ? newPin.trim() : "";

        if (hasExistingPin && !normalizedCurrent.matches("^\\d{6}$")) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Vui lòng nhập mã PIN hiện tại gồm 6 chữ số."));
        }
        if (!normalizedNew.matches("^\\d{6}$")) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Mã PIN giao dịch mới phải gồm đúng 6 chữ số."));
        }
        if (hasExistingPin && normalizedCurrent.equals(normalizedNew)) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Mã PIN mới phải khác mã PIN hiện tại."));
        }
        return securityRepository.setTransactionPin(
                hasExistingPin ? normalizedCurrent : null,
                normalizedNew
        );
    }
}
