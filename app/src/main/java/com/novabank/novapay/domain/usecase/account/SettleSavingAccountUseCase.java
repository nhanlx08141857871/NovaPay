package com.novabank.novapay.domain.usecase.account;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.account.SettleSavingResult;
import com.novabank.novapay.domain.repository.SavingsRepository;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

/**
 * UseCase tất toán sổ tiết kiệm về tài khoản CHECKING (Pure Java 17).
 */
public final class SettleSavingAccountUseCase {

    private final SavingsRepository savingsRepository;

    @Inject
    public SettleSavingAccountUseCase(SavingsRepository savingsRepository) {
        this.savingsRepository = savingsRepository;
    }

    public Single<AppResult<SettleSavingResult>> execute(
            String savingAccountId,
            String toCheckingAccountId,
            String pinOrNull
    ) {
        if (savingAccountId == null || savingAccountId.trim().isEmpty()) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Mã sổ tiết kiệm không hợp lệ."));
        }
        if (toCheckingAccountId == null || toCheckingAccountId.trim().isEmpty()) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Vui lòng chọn tài khoản thanh toán nhận tiền tất toán."));
        }
        String normalizedPin = pinOrNull != null && !pinOrNull.trim().isEmpty() ? pinOrNull.trim() : null;
        if (normalizedPin != null && !normalizedPin.matches("^\\d{6}$")) {
            return Single.just(AppResult.error("PIN_INVALID", "Mã PIN giao dịch phải gồm đúng 6 chữ số."));
        }
        return savingsRepository.settleSavingAccount(
                savingAccountId.trim(),
                toCheckingAccountId.trim(),
                normalizedPin
        );
    }
}
