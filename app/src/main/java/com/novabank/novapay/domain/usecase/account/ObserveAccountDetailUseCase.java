package com.novabank.novapay.domain.usecase.account;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.repository.AccountRepository;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

/**
 * UseCase lấy hoặc quan sát realtime chi tiết một tài khoản theo accountId.
 */
public final class ObserveAccountDetailUseCase {

    private final AccountRepository accountRepository;

    @Inject
    public ObserveAccountDetailUseCase(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Observable<AppResult<Account>> observe(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return Observable.just(AppResult.error("INVALID_ARGUMENT", "Mã tài khoản không hợp lệ."));
        }
        return accountRepository.observeAccountById(accountId.trim());
    }

    public Single<AppResult<Account>> getOnce(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Mã tài khoản không hợp lệ."));
        }
        return accountRepository.getAccountById(accountId.trim());
    }
}
