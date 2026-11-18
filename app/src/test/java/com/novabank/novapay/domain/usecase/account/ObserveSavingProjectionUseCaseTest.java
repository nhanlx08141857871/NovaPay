package com.novabank.novapay.domain.usecase.account;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.account.AccountStatus;
import com.novabank.novapay.domain.model.account.AccountType;
import com.novabank.novapay.domain.model.account.SavingInfo;
import com.novabank.novapay.domain.model.account.SavingProjection;
import com.novabank.novapay.domain.model.rate.InterestRate;
import com.novabank.novapay.domain.repository.AccountRepository;
import com.novabank.novapay.domain.repository.InterestRateRepository;

import org.junit.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class ObserveSavingProjectionUseCaseTest {

    @Test
    public void execute_combinesRealtimeRateUpdateAndComputesLongProfit() {
        Instant now = Instant.parse("2026-11-18T00:00:00Z");
        Account savingAccount = new Account(
                "acc_sav_1",
                "u1",
                AccountType.SAVING,
                "2001000001",
                AccountStatus.ACTIVE,
                120_000_000L,
                "VND",
                0L,
                0L,
                new SavingInfo(12, 120_000_000L, "rate_12m", now, now.plusSeconds(86400)),
                null,
                now
        );

        BehaviorSubject<AppResult<Account>> accountSubject =
                BehaviorSubject.createDefault(AppResult.success(savingAccount));
        BehaviorSubject<AppResult<List<InterestRate>>> ratesSubject =
                BehaviorSubject.createDefault(AppResult.success(Collections.singletonList(
                        new InterestRate("rate_12m", 12, 6.0, now, "officer1", now)
                )));

        AccountRepository fakeAccountRepo = new AccountRepository() {
            @Override
            public Observable<AppResult<List<Account>>> observeMyAccounts() {
                return Observable.empty();
            }

            @Override
            public Observable<AppResult<Account>> observeAccountById(String accountId) {
                return accountSubject;
            }

            @Override
            public Single<AppResult<Account>> getAccountById(String accountId) {
                return Single.just(AppResult.success(savingAccount));
            }
        };

        InterestRateRepository fakeRateRepo = new InterestRateRepository() {
            @Override
            public Observable<AppResult<List<InterestRate>>> observeAllRates() {
                return ratesSubject;
            }

            @Override
            public Observable<AppResult<InterestRate>> observeRateForTerm(int termMonths) {
                return Observable.empty();
            }

            @Override
            public Observable<AppResult<List<InterestRate>>> observeInterestRates() {
                return ratesSubject;
            }

            @Override
            public Single<AppResult<String>> saveInterestRate(int termMonths, double annualRate) {
                return Single.just(AppResult.success("rate_" + termMonths));
            }
        };

        ObserveSavingProjectionUseCase useCase =
                new ObserveSavingProjectionUseCase(fakeAccountRepo, fakeRateRepo);
        TestObserver<AppResult<SavingProjection>> observer = useCase.execute("acc_sav_1").test();

        observer.assertValueCount(1);
        SavingProjection first = observer.values().get(0).getData();
        // floor((120_000_000 * 6.0) / 1200.0) = 600_000
        assertEquals(600_000L, first.getMonthlyProfit());
        assertEquals(6.0, first.getEffectiveAnnualRate(), 0.0001);

        // Officer updates 12m interest rate to 7.2%
        ratesSubject.onNext(AppResult.success(Collections.singletonList(
                new InterestRate("rate_12m", 12, 7.2, now, "officer1", now.plusSeconds(60))
        )));

        observer.assertValueCount(2);
        SavingProjection updated = observer.values().get(1).getData();
        // floor((120_000_000 * 7.2) / 1200.0) = 720_000
        assertEquals(720_000L, updated.getMonthlyProfit());
        assertEquals(7.2, updated.getEffectiveAnnualRate(), 0.0001);
        observer.dispose();
    }

    @Test
    public void execute_nonSavingAccount_returnsError() {
        Account checking = new Account(
                "acc_chk_1",
                "u1",
                AccountType.CHECKING,
                "1001000001",
                AccountStatus.ACTIVE,
                5_000_000L,
                "VND",
                5_000_000L,
                20_000_000L,
                null,
                null,
                Instant.now()
        );
        AppResult<SavingProjection> result = ObserveSavingProjectionUseCase.combine(
                AppResult.success(checking),
                AppResult.success(Collections.emptyList())
        );
        assertTrue(result.isError());
        assertEquals("INVALID_ACCOUNT_TYPE", result.getErrorCode());
    }
}
