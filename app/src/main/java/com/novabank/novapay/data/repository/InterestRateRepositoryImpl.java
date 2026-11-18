package com.novabank.novapay.data.repository;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.data.remote.firebase.FirestoreInterestRateDataSource;
import com.novabank.novapay.domain.model.rate.InterestRate;
import com.novabank.novapay.domain.repository.InterestRateRepository;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Observable;

@Singleton
public class InterestRateRepositoryImpl implements InterestRateRepository {

    private final FirestoreInterestRateDataSource interestRateDataSource;

    @Inject
    public InterestRateRepositoryImpl(FirestoreInterestRateDataSource interestRateDataSource) {
        this.interestRateDataSource = interestRateDataSource;
    }

    @Override
    public Observable<AppResult<List<InterestRate>>> observeAllRates() {
        return interestRateDataSource.observeAllRates();
    }

    @Override
    public Observable<AppResult<InterestRate>> observeRateForTerm(int termMonths) {
        return interestRateDataSource.observeRateForTerm(termMonths);
    }
}
