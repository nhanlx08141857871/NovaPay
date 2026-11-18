package com.novabank.novapay.domain.usecase.rate;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.rate.InterestRate;
import com.novabank.novapay.domain.repository.InterestRateRepository;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Observable;

public final class ObserveInterestRatesUseCase {

    private final InterestRateRepository interestRateRepository;

    @Inject
    public ObserveInterestRatesUseCase(InterestRateRepository interestRateRepository) {
        this.interestRateRepository = interestRateRepository;
    }

    public Observable<AppResult<List<InterestRate>>> execute() {
        return interestRateRepository.observeInterestRates();
    }
}
