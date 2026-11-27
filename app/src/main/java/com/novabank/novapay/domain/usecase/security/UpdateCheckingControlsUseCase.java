package com.novabank.novapay.domain.usecase.security;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.account.AccountStatus;
import com.novabank.novapay.domain.model.security.AccountControlResult;
import com.novabank.novapay.domain.repository.SecurityRepository;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public final class UpdateCheckingControlsUseCase {

    public static final long MIN_PER_TXN_LIMIT_VND = 1_000_000L;
    public static final long MAX_PER_TXN_LIMIT_VND = 100_000_000L;
    public static final long MIN_DAILY_LIMIT_VND = 5_000_000L;
    public static final long MAX_DAILY_LIMIT_VND = 500_000_000L;

    public static final class ControlsPatch {
        private final AccountStatus statusOrNull;
        private final Long perTxnLimitOrNull;
        private final Long dailyLimitOrNull;

        public ControlsPatch(
                AccountStatus statusOrNull,
                Long perTxnLimitOrNull,
                Long dailyLimitOrNull
        ) {
            this.statusOrNull = statusOrNull;
            this.perTxnLimitOrNull = perTxnLimitOrNull;
            this.dailyLimitOrNull = dailyLimitOrNull;
        }

        public AccountStatus getStatusOrNull() {
            return statusOrNull;
        }

        public Long getPerTxnLimitOrNull() {
            return perTxnLimitOrNull;
        }

        public Long getDailyLimitOrNull() {
            return dailyLimitOrNull;
        }

        public boolean hasChanges() {
            return statusOrNull != null || perTxnLimitOrNull != null || dailyLimitOrNull != null;
        }
    }

    public static ControlsPatch buildPatch(
            Account currentAccount,
            boolean targetFrozen,
            long inputPerTxnLimit,
            long inputDailyLimit
    ) {
        if (currentAccount == null) {
            return new ControlsPatch(
                    targetFrozen ? AccountStatus.FROZEN : AccountStatus.ACTIVE,
                    inputPerTxnLimit,
                    inputDailyLimit
            );
        }
        AccountStatus targetStatus = targetFrozen ? AccountStatus.FROZEN : AccountStatus.ACTIVE;
        AccountStatus statusOrNull = targetStatus != currentAccount.getStatus() ? targetStatus : null;

        boolean isFreezingNow = targetStatus == AccountStatus.FROZEN
                && currentAccount.getStatus() != AccountStatus.FROZEN;
        if (isFreezingNow) {
            // Khi tạm khóa khẩn cấp, bỏ qua trường hạn mức để không bị chặn bởi hạn mức cũ ngoài biên.
            return new ControlsPatch(AccountStatus.FROZEN, null, null);
        }

        Long perTxnOrNull = inputPerTxnLimit != currentAccount.getPerTxnLimit() ? inputPerTxnLimit : null;
        Long dailyOrNull = inputDailyLimit != currentAccount.getDailyLimit() ? inputDailyLimit : null;
        return new ControlsPatch(statusOrNull, perTxnOrNull, dailyOrNull);
    }

    private final SecurityRepository securityRepository;

    @Inject
    public UpdateCheckingControlsUseCase(SecurityRepository securityRepository) {
        this.securityRepository = securityRepository;
    }

    public Single<AppResult<AccountControlResult>> execute(
            String accountId,
            AccountStatus statusOrNull,
            Long perTxnLimitOrNull,
            Long dailyLimitOrNull,
            String pinOrNull
    ) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Tài khoản thanh toán không hợp lệ."));
        }
        if (statusOrNull == null && perTxnLimitOrNull == null && dailyLimitOrNull == null) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Không có thay đổi cấu hình nào cần cập nhật."));
        }
        if (statusOrNull != null
                && statusOrNull != AccountStatus.ACTIVE
                && statusOrNull != AccountStatus.FROZEN) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Trạng thái điều chỉnh chỉ hỗ trợ Hoạt động hoặc Tạm khóa."));
        }
        if (perTxnLimitOrNull != null
                && (perTxnLimitOrNull < MIN_PER_TXN_LIMIT_VND || perTxnLimitOrNull > MAX_PER_TXN_LIMIT_VND)) {
            return Single.just(AppResult.error(
                    "INVALID_ARGUMENT",
                    "Hạn mức mỗi giao dịch phải nằm trong khoảng 1.000.000 ₫ – 100.000.000 ₫."
            ));
        }
        if (dailyLimitOrNull != null
                && (dailyLimitOrNull < MIN_DAILY_LIMIT_VND || dailyLimitOrNull > MAX_DAILY_LIMIT_VND)) {
            return Single.just(AppResult.error(
                    "INVALID_ARGUMENT",
                    "Hạn mức mỗi ngày phải nằm trong khoảng 5.000.000 ₫ – 500.000.000 ₫."
            ));
        }
        if (perTxnLimitOrNull != null && dailyLimitOrNull != null && perTxnLimitOrNull > dailyLimitOrNull) {
            return Single.just(AppResult.error(
                    "INVALID_ARGUMENT",
                    "Hạn mức mỗi giao dịch không được vượt quá hạn mức mỗi ngày."
            ));
        }
        String normalizedPin = pinOrNull != null && !pinOrNull.trim().isEmpty() ? pinOrNull.trim() : null;
        if (normalizedPin != null && !normalizedPin.matches("^\\d{6}$")) {
            return Single.just(AppResult.error("PIN_INVALID", "Mã PIN giao dịch phải gồm đúng 6 chữ số."));
        }
        return securityRepository.updateCheckingControls(
                accountId.trim(),
                statusOrNull,
                perTxnLimitOrNull,
                dailyLimitOrNull,
                normalizedPin
        );
    }
}
