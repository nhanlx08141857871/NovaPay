package com.novabank.novapay.data.repository;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.data.remote.firebase.FirebaseAuthDataSource;
import com.novabank.novapay.data.remote.firebase.FirestoreAccountDataSource;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.repository.AccountRepository;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

/**
 * Triển khai AccountRepository dùng chung toàn dự án (A sở hữu, B tiêu thụ qua DI).
 */
@Singleton
public class AccountRepositoryImpl implements AccountRepository {

    private final FirebaseAuthDataSource authDataSource;
    private final FirestoreAccountDataSource accountDataSource;

    @Inject
    public AccountRepositoryImpl(
            FirebaseAuthDataSource authDataSource,
            FirestoreAccountDataSource accountDataSource
    ) {
        this.authDataSource = authDataSource;
        this.accountDataSource = accountDataSource;
    }

    @Override
    public Observable<AppResult<List<Account>>> observeMyAccounts() {
        String uid = authDataSource.getCurrentUid();
        if (uid == null || uid.isEmpty()) {
            return Observable.just(AppResult.error("UNAUTHENTICATED", "Vui lòng đăng nhập để xem tài khoản."));
        }
        return accountDataSource.observeAccountsForUser(uid);
    }

    @Override
    public Observable<AppResult<Account>> observeAccountById(String accountId) {
        return accountDataSource.observeAccountById(accountId);
    }

    @Override
    public Single<AppResult<Account>> getAccountById(String accountId) {
        return accountDataSource.getAccountById(accountId);
    }
}
