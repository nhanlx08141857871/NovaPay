package com.novabank.novapay.data.repository;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.data.remote.firebase.MoneyFirestoreDataSource;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.account.AccountStatus;
import com.novabank.novapay.domain.model.account.AccountType;
import com.novabank.novapay.domain.repository.AccountRepository;
import com.novabank.novapay.domain.repository.SourceAccountRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

/**
 * Triển khai SourceAccountRepository sử dụng Optional<AccountRepository> (từ Module A sau khi merge)
 * và tự động fallback sang MoneyFirestoreDataSource khi chạy độc lập trên nhánh Module B.
 */
@Singleton
public class SourceAccountRepositoryImpl implements SourceAccountRepository {

    private final Optional<AccountRepository> optionalAccountRepository;
    private final MoneyFirestoreDataSource firestoreDataSource;

    @Inject
    public SourceAccountRepositoryImpl(
            Optional<AccountRepository> optionalAccountRepository,
            MoneyFirestoreDataSource firestoreDataSource
    ) {
        this.optionalAccountRepository = optionalAccountRepository;
        this.firestoreDataSource = firestoreDataSource;
    }

    @Override
    public Observable<AppResult<List<Account>>> observeActiveCheckingAccounts() {
        Observable<AppResult<List<Account>>> source = optionalAccountRepository.isPresent()
                ? optionalAccountRepository.get().observeMyAccounts()
                : firestoreDataSource.observeMyAccounts();
        return source.map(SourceAccountRepositoryImpl::filterCheckingActive);
    }

    @Override
    public Single<AppResult<List<Account>>> getActiveCheckingAccounts() {
        if (optionalAccountRepository.isPresent()) {
            return optionalAccountRepository.get()
                    .observeMyAccounts()
                    .firstOrError()
                    .map(SourceAccountRepositoryImpl::filterCheckingActive);
        }
        return firestoreDataSource.getMyAccountsOnce()
                .map(SourceAccountRepositoryImpl::filterCheckingActive);
    }

    @Override
    public Single<AppResult<List<Account>>> getActiveMortgageAccounts() {
        if (optionalAccountRepository.isPresent()) {
            return optionalAccountRepository.get()
                    .observeMyAccounts()
                    .firstOrError()
                    .map(SourceAccountRepositoryImpl::filterMortgageActive);
        }
        return firestoreDataSource.getMyAccountsOnce()
                .map(SourceAccountRepositoryImpl::filterMortgageActive);
    }

    @Override
    public Single<AppResult<Account>> getAccountById(String accountId) {
        if (optionalAccountRepository.isPresent()) {
            return optionalAccountRepository.get().getAccountById(accountId);
        }
        return firestoreDataSource.getAccountById(accountId);
    }

    private static AppResult<List<Account>> filterCheckingActive(AppResult<List<Account>> result) {
        if (result == null || result.isError()) {
            return result != null ? result : AppResult.error("UNKNOWN_ERROR", "Không thể tải danh sách tài khoản.");
        }
        List<Account> filtered = new ArrayList<>();
        if (result.getData() != null) {
            for (Account account : result.getData()) {
                if (account != null && account.canBePaymentSource()) {
                    filtered.add(account);
                }
            }
        }
        return AppResult.success(filtered);
    }

    private static AppResult<List<Account>> filterMortgageActive(AppResult<List<Account>> result) {
        if (result == null || result.isError()) {
            return result != null ? result : AppResult.error("UNKNOWN_ERROR", "Không thể tải tài khoản vay.");
        }
        List<Account> filtered = new ArrayList<>();
        if (result.getData() != null) {
            for (Account account : result.getData()) {
                if (account != null
                        && account.getType() == AccountType.MORTGAGE
                        && account.getStatus() == AccountStatus.ACTIVE) {
                    filtered.add(account);
                }
            }
        }
        return AppResult.success(filtered);
    }
}
