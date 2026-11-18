package com.novabank.novapay.data.repository;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.data.dto.SaveInterestRateRequestDto;
import com.novabank.novapay.data.mapper.ApiErrorParser;
import com.novabank.novapay.data.mapper.OfficerFirestoreMapper;
import com.novabank.novapay.data.remote.firebase.FirestoreInterestRateDataSource;
import com.novabank.novapay.data.remote.server.OfficerApi;
import com.novabank.novapay.domain.model.rate.InterestRate;
import com.novabank.novapay.domain.repository.InterestRateRepository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

@Singleton
public final class InterestRateRepositoryImpl implements InterestRateRepository {

    private final FirestoreInterestRateDataSource interestRateDataSource;
    private final FirebaseFirestore firestore;
    private final OfficerApi officerApi;
    private final ApiErrorParser apiErrorParser;

    @Inject
    public InterestRateRepositoryImpl(
            FirestoreInterestRateDataSource interestRateDataSource,
            FirebaseFirestore firestore,
            OfficerApi officerApi,
            ApiErrorParser apiErrorParser
    ) {
        this.interestRateDataSource = interestRateDataSource;
        this.firestore = firestore;
        this.officerApi = officerApi;
        this.apiErrorParser = apiErrorParser;
    }

    @Override
    public Observable<AppResult<List<InterestRate>>> observeAllRates() {
        return interestRateDataSource.observeAllRates();
    }

    @Override
    public Observable<AppResult<InterestRate>> observeRateForTerm(int termMonths) {
        return interestRateDataSource.observeRateForTerm(termMonths);
    }

    @Override
    public Observable<AppResult<List<InterestRate>>> observeInterestRates() {
        return Observable.<AppResult<List<InterestRate>>>create(emitter -> {
            ListenerRegistration registration = firestore.collection("interest_rates")
                    .orderBy("termMonths", Query.Direction.ASCENDING)
                    .addSnapshotListener((snapshot, error) -> {
                        if (emitter.isDisposed()) {
                            return;
                        }
                        if (error != null) {
                            emitter.onNext(AppResult.error(
                                    "FIRESTORE_ERROR",
                                    error.getMessage() != null
                                            ? error.getMessage()
                                            : "Không thể tải bảng lãi suất."
                            ));
                            return;
                        }
                        List<InterestRate> rates = new ArrayList<>();
                        if (snapshot != null) {
                            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                InterestRate rate = OfficerFirestoreMapper.toInterestRate(doc);
                                if (rate != null) {
                                    rates.add(rate);
                                }
                            }
                        }
                        emitter.onNext(AppResult.success(rates));
                    });
            emitter.setCancellable(registration::remove);
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Single<AppResult<String>> saveInterestRate(int termMonths, double annualRate) {
        return officerApi.saveInterestRate(new SaveInterestRateRequestDto(termMonths, annualRate))
                .map(dto -> AppResult.success(dto.getRateId()))
                .onErrorReturn(apiErrorParser::toErrorResult)
                .subscribeOn(Schedulers.io());
    }
}
