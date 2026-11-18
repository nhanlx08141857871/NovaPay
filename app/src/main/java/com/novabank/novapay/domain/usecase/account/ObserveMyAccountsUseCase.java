package com.novabank.novapay.domain.usecase.account;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.repository.AccountRepository;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Observable;

/**
 * UseCase quan sát realtime toàn bộ tài khoản của khách hàng hiện tại.
 */
public final class ObserveMyAccountsUseCase {

    private final AccountRepository accountRepository;

    @Inject
    public ObserveMyAccountsUseCase(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Observable<AppResult<List<Account>>> execute() {
        return accountRepository.observeMyAccounts();
    }
}
