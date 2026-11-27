package com.novabank.novapay.domain.repository;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.account.Account;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public interface SourceAccountRepository {

    Observable<AppResult<List<Account>>> observeActiveCheckingAccounts();

    Single<AppResult<List<Account>>> getActiveCheckingAccounts();

    Single<AppResult<List<Account>>> getActiveMortgageAccounts();

    Single<AppResult<Account>> getAccountById(String accountId);
}
