package com.novabank.novapay.domain.repository;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.account.OpenSavingResult;
import com.novabank.novapay.domain.model.account.SettleSavingResult;

import io.reactivex.rxjava3.core.Single;

public interface SavingsRepository {

    Single<AppResult<OpenSavingResult>> openSavingAccount(
            String fromCheckingAccountId,
            int termMonths,
            long principalAmount,
            String pinOrNull
    );

    Single<AppResult<SettleSavingResult>> settleSavingAccount(
            String savingAccountId,
            String toCheckingAccountId,
            String pinOrNull
    );
}
