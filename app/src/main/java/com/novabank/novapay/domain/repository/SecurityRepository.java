package com.novabank.novapay.domain.repository;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.account.AccountStatus;
import com.novabank.novapay.domain.model.security.AccountControlResult;
import com.novabank.novapay.domain.model.security.SecurityStatus;

import io.reactivex.rxjava3.core.Single;

public interface SecurityRepository {

    Single<AppResult<SecurityStatus>> getSecurityStatus();

    Single<AppResult<SecurityStatus>> setTransactionPin(String currentPinOrNull, String newPin);

    Single<AppResult<AccountControlResult>> updateCheckingControls(
            String accountId,
            AccountStatus statusOrNull,
            Long perTxnLimitOrNull,
            Long dailyLimitOrNull,
            String pinOrNull
    );
}
