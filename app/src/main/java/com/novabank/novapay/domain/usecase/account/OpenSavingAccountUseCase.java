package com.novabank.novapay.domain.usecase.account;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.account.OpenSavingResult;
import com.novabank.novapay.domain.repository.SavingsRepository;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

/**
 * UseCase mở sổ tiết kiệm trực tuyến từ tài khoản CHECKING (Pure Java 17).
 */
public final class OpenSavingAccountUseCase {

    public static final long MIN_PRINCIPAL_VND = 1_000_000L;

    private final SavingsRepository savingsRepository;

    @Inject
    public OpenSavingAccountUseCase(SavingsRepository savingsRepository) {
        this.savingsRepository = savingsRepository;
    }

    public Single<AppResult<OpenSavingResult>> execute(
            String fromCheckingAccountId,
            int termMonths,
            long principalAmount,
            String pinOrNull
    ) {
        if (fromCheckingAccountId == null || fromCheckingAccountId.trim().isEmpty()) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Vui lòng chọn tài khoản thanh toán nguồn."));
        }
        if (termMonths <= 0) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Vui lòng chọn kỳ hạn gửi tiết kiệm hợp lệ."));
        }
        if (principalAmount < MIN_PRINCIPAL_VND) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Số tiền gửi tiết kiệm tối thiểu là 1.000.000 ₫."));
        }
        String normalizedPin = pinOrNull != null && !pinOrNull.trim().isEmpty() ? pinOrNull.trim() : null;
        if (normalizedPin != null && !normalizedPin.matches("^\\d{6}$")) {
            return Single.just(AppResult.error("PIN_INVALID", "Mã PIN giao dịch phải gồm đúng 6 chữ số."));
        }
        return savingsRepository.openSavingAccount(
                fromCheckingAccountId.trim(),
                termMonths,
                principalAmount,
                normalizedPin
        );
    }
}
