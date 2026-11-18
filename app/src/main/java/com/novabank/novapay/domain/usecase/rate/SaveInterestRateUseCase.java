package com.novabank.novapay.domain.usecase.rate;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.rate.InterestRate;
import com.novabank.novapay.domain.repository.InterestRateRepository;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public final class SaveInterestRateUseCase {

    private final InterestRateRepository interestRateRepository;

    @Inject
    public SaveInterestRateUseCase(InterestRateRepository interestRateRepository) {
        this.interestRateRepository = interestRateRepository;
    }

    public Single<AppResult<String>> execute(int termMonths, double annualRate) {
        if (!InterestRate.isValidTermAndRate(termMonths, annualRate)) {
            return Single.just(AppResult.error(
                    "INVALID_ARGUMENT",
                    "Kỳ hạn (0–120 tháng) hoặc lãi suất năm (0–50 %/năm) không hợp lệ."
            ));
        }
        return interestRateRepository.saveInterestRate(termMonths, annualRate);
    }
}
