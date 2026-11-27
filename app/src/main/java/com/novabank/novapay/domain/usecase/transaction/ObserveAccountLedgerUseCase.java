package com.novabank.novapay.domain.usecase.transaction;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.transaction.LedgerDirection;
import com.novabank.novapay.domain.model.transaction.LedgerEntry;
import com.novabank.novapay.domain.repository.AccountRepository;
import com.novabank.novapay.domain.repository.LedgerRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Observable;

public final class ObserveAccountLedgerUseCase {

    private final AccountRepository accountRepository;
    private final LedgerRepository ledgerRepository;

    @Inject
    public ObserveAccountLedgerUseCase(
            AccountRepository accountRepository,
            LedgerRepository ledgerRepository
    ) {
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
    }

    public Observable<AppResult<List<LedgerEntry>>> execute(
            String accountIdOrNull,
            LedgerDirection directionOrNull,
            Instant fromInclusive,
            Instant toExclusive
    ) {
        if (accountIdOrNull != null && !accountIdOrNull.trim().isEmpty()) {
            return ledgerRepository.observeLedger(
                    accountIdOrNull.trim(),
                    directionOrNull,
                    fromInclusive,
                    toExclusive
            );
        }

        return accountRepository.observeMyAccounts().switchMap(accountsResult -> {
            if (accountsResult.isError()) {
                return Observable.just(AppResult.error(
                        accountsResult.getErrorCode(),
                        accountsResult.getMessage()
                ));
            }
            List<Account> accounts = accountsResult.getData();
            if (accounts == null || accounts.isEmpty()) {
                return Observable.just(AppResult.success(Collections.emptyList()));
            }

            List<Observable<AppResult<List<LedgerEntry>>>> perAccountStreams = new ArrayList<>();
            for (Account account : accounts) {
                perAccountStreams.add(ledgerRepository.observeLedger(
                        account.getAccountId(),
                        directionOrNull,
                        fromInclusive,
                        toExclusive
                ));
            }

            return Observable.combineLatest(perAccountStreams, results -> {
                List<LedgerEntry> merged = new ArrayList<>();
                AppResult<List<LedgerEntry>> firstError = null;
                boolean anySuccess = false;

                for (Object raw : results) {
                    @SuppressWarnings("unchecked")
                    AppResult<List<LedgerEntry>> item = (AppResult<List<LedgerEntry>>) raw;
                    if (item.isSuccess()) {
                        anySuccess = true;
                        if (item.getData() != null) {
                            merged.addAll(item.getData());
                        }
                    } else if (firstError == null) {
                        firstError = item;
                    }
                }

                if (!anySuccess && firstError != null) {
                    return firstError;
                }

                merged.sort(Comparator.comparing(
                        LedgerEntry::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ));
                return AppResult.success(merged);
            });
        });
    }
}
