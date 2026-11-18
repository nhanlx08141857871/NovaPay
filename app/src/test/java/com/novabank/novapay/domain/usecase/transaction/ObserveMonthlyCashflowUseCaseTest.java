package com.novabank.novapay.domain.usecase.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.account.AccountStatus;
import com.novabank.novapay.domain.model.account.AccountType;
import com.novabank.novapay.domain.model.transaction.LedgerDirection;
import com.novabank.novapay.domain.model.transaction.LedgerEntry;
import com.novabank.novapay.domain.model.transaction.MonthlyCashflowSummary;
import com.novabank.novapay.domain.repository.AccountRepository;
import com.novabank.novapay.domain.repository.LedgerRepository;

import org.junit.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;

public class ObserveMonthlyCashflowUseCaseTest {

    @Test
    public void aggregate_sumsInflowAndOutflowAsLong() {
        Instant now = Instant.now();
        List<LedgerEntry> entries = Arrays.asList(
                new LedgerEntry("e1", "a1", "t1", LedgerDirection.IN, 15_000_000L, 25_000_000L, "Salary", now),
                new LedgerEntry("e2", "a1", "t2", LedgerDirection.IN, 2_500_000L, 27_500_000L, "Refund", now),
                new LedgerEntry("e3", "a1", "t3", LedgerDirection.OUT, 1_250_000L, 26_250_000L, "Electric", now),
                new LedgerEntry("e4", "a1", "t4", LedgerDirection.OUT, 750_000L, 25_500_000L, "Transfer", now)
        );

        MonthlyCashflowSummary summary = ObserveMonthlyCashflowUseCase.aggregate("11/2026", entries);
        assertEquals("11/2026", summary.getMonthLabel());
        assertEquals(17_500_000L, summary.getTotalIn());
        assertEquals(2_000_000L, summary.getTotalOut());
        assertEquals(4, summary.getEntryCount());
    }

    @Test
    public void execute_combinesMultipleAccountsIntoMonthlySummary() {
        Instant now = Instant.now();
        Account acc1 = new Account(
                "acc1", "u1", AccountType.CHECKING, "1001", AccountStatus.ACTIVE,
                10_000_000L, "VND", 5_000_000L, 20_000_000L, null, null, now
        );
        Account acc2 = new Account(
                "acc2", "u1", AccountType.SAVING, "2001", AccountStatus.ACTIVE,
                50_000_000L, "VND", 0L, 0L, null, null, now
        );

        AccountRepository fakeAccRepo = new AccountRepository() {
            @Override
            public Observable<AppResult<List<Account>>> observeMyAccounts() {
                return Observable.just(AppResult.success(Arrays.asList(acc1, acc2)));
            }

            @Override
            public Observable<AppResult<Account>> observeAccountById(String accountId) {
                return Observable.empty();
            }

            @Override
            public Single<AppResult<Account>> getAccountById(String accountId) {
                return Single.never();
            }
        };

        LedgerRepository fakeLedgerRepo = (accountId, dir, from, to) -> {
            if ("acc1".equals(accountId)) {
                return Observable.just(AppResult.success(Arrays.asList(
                        new LedgerEntry("e1", "acc1", "t1", LedgerDirection.IN, 5_000_000L, 15_000_000L, "Topup", now),
                        new LedgerEntry("e2", "acc1", "t2", LedgerDirection.OUT, 1_000_000L, 14_000_000L, "Bill", now)
                )));
            }
            return Observable.just(AppResult.success(Collections.singletonList(
                    new LedgerEntry("e3", "acc2", "t3", LedgerDirection.IN, 250_000L, 50_250_000L, "Interest", now)
            )));
        };

        ObserveMonthlyCashflowUseCase useCase =
                new ObserveMonthlyCashflowUseCase(fakeAccRepo, fakeLedgerRepo);
        TestObserver<AppResult<MonthlyCashflowSummary>> observer = useCase.execute().test();

        observer.assertValueCount(1);
        AppResult<MonthlyCashflowSummary> result = observer.values().get(0);
        assertTrue(result.isSuccess());
        assertEquals(5_250_000L, result.getData().getTotalIn());
        assertEquals(1_000_000L, result.getData().getTotalOut());
        assertEquals(3, result.getData().getEntryCount());
        observer.dispose();
    }
}
