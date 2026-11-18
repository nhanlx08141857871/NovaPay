package com.novabank.novapay.data.repository;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.data.remote.firebase.FirestoreLedgerDataSource;
import com.novabank.novapay.domain.model.transaction.LedgerDirection;
import com.novabank.novapay.domain.model.transaction.LedgerEntry;
import com.novabank.novapay.domain.repository.LedgerRepository;

import java.time.Instant;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Observable;

@Singleton
public class LedgerRepositoryImpl implements LedgerRepository {

    private final FirestoreLedgerDataSource ledgerDataSource;

    @Inject
    public LedgerRepositoryImpl(FirestoreLedgerDataSource ledgerDataSource) {
        this.ledgerDataSource = ledgerDataSource;
    }

    @Override
    public Observable<AppResult<List<LedgerEntry>>> observeLedger(
            String accountId,
            LedgerDirection directionOrNull,
            Instant fromInclusive,
            Instant toExclusive
    ) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return Observable.just(AppResult.error("INVALID_ARGUMENT", "Mã tài khoản không hợp lệ."));
        }
        return ledgerDataSource.observeLedger(
                accountId.trim(),
                directionOrNull,
                fromInclusive,
                toExclusive
        );
    }
}
