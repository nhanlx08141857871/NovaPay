package com.novabank.novapay.domain.usecase.account;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.account.AccountType;
import com.novabank.novapay.domain.model.account.SavingInfo;
import com.novabank.novapay.domain.model.account.SavingProjection;
import com.novabank.novapay.domain.model.rate.InterestRate;
import com.novabank.novapay.domain.repository.AccountRepository;
import com.novabank.novapay.domain.repository.InterestRateRepository;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Observable;

public final class ObserveSavingProjectionUseCase {

    private final AccountRepository accountRepository;
    private final InterestRateRepository interestRateRepository;

    @Inject
    public ObserveSavingProjectionUseCase(
            AccountRepository accountRepository,
            InterestRateRepository interestRateRepository
    ) {
        this.accountRepository = accountRepository;
        this.interestRateRepository = interestRateRepository;
    }

    public Observable<AppResult<SavingProjection>> execute(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return Observable.just(AppResult.error("INVALID_ARGUMENT", "Mã tài khoản tiết kiệm không hợp lệ."));
        }
        return Observable.combineLatest(
                accountRepository.observeAccountById(accountId.trim()),
                interestRateRepository.observeAllRates(),
                ObserveSavingProjectionUseCase::combine
        );
    }

    static AppResult<SavingProjection> combine(
            AppResult<Account> accountResult,
            AppResult<List<InterestRate>> ratesResult
    ) {
        if (accountResult.isError()) {
            return AppResult.error(accountResult.getErrorCode(), accountResult.getMessage());
        }
        Account account = accountResult.getData();
        if (account == null) {
            return AppResult.error("NOT_FOUND", "Không tìm thấy tài khoản tiết kiệm.");
        }
        if (account.getType() != AccountType.SAVING) {
            return AppResult.error("INVALID_ACCOUNT_TYPE", "Tài khoản này không phải tài khoản tiết kiệm.");
        }

        List<InterestRate> rates = ratesResult.isSuccess() && ratesResult.getData() != null
                ? ratesResult.getData()
                : Collections.emptyList();

        SavingInfo saving = account.getSaving();
        InterestRate matchedRate = findBestMatchingRate(saving, rates);
        double effectiveAnnualRate = matchedRate != null ? matchedRate.getAnnualRate() : 0.0;
        long monthlyProfit = SavingInfo.calculateMonthlyProfit(account.getBalance(), effectiveAnnualRate);

        return AppResult.success(new SavingProjection(account, matchedRate, effectiveAnnualRate, monthlyProfit));
    }

    private static InterestRate findBestMatchingRate(SavingInfo saving, List<InterestRate> rates) {
        if (saving == null || rates == null || rates.isEmpty()) {
            return null;
        }
        int targetTerm = saving.getTermMonths();
        String rateId = saving.getInterestRateId();

        InterestRate bestByTerm = null;
        for (InterestRate rate : rates) {
            if (rate == null) {
                continue;
            }
            if (rate.getTermMonths() == targetTerm) {
                if (bestByTerm == null || isNewer(rate, bestByTerm)) {
                    bestByTerm = rate;
                }
            }
        }
        if (bestByTerm != null) {
            return bestByTerm;
        }
        if (rateId != null && !rateId.isEmpty()) {
            for (InterestRate rate : rates) {
                if (rate != null && rateId.equals(rate.getRateId())) {
                    return rate;
                }
            }
        }
        return null;
    }

    private static boolean isNewer(InterestRate candidate, InterestRate current) {
        Instant cTime = candidate.getUpdatedAt() != null ? candidate.getUpdatedAt() : candidate.getEffectiveFrom();
        Instant curTime = current.getUpdatedAt() != null ? current.getUpdatedAt() : current.getEffectiveFrom();
        if (cTime == null) {
            return false;
        }
        if (curTime == null) {
            return true;
        }
        return cTime.isAfter(curTime);
    }
}
