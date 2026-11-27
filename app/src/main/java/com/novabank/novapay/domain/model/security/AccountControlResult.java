package com.novabank.novapay.domain.model.security;

import com.novabank.novapay.domain.model.account.AccountStatus;

/**
 * Kết quả cập nhật kiểm soát tài khoản CHECKING từ PATCH /security/accounts/:accountId/controls (Pure Java 17).
 */
public final class AccountControlResult {

    private final AccountStatus status;
    private final long perTxnLimit;
    private final long dailyLimit;

    public AccountControlResult(AccountStatus status, long perTxnLimit, long dailyLimit) {
        this.status = status != null ? status : AccountStatus.ACTIVE;
        this.perTxnLimit = perTxnLimit;
        this.dailyLimit = dailyLimit;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public long getPerTxnLimit() {
        return perTxnLimit;
    }

    public long getDailyLimit() {
        return dailyLimit;
    }
}
