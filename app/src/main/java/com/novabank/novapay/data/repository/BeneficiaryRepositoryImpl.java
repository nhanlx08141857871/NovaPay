package com.novabank.novapay.data.repository;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.data.remote.firebase.MoneyFirestoreDataSource;
import com.novabank.novapay.domain.model.bank.Beneficiary;
import com.novabank.novapay.domain.repository.BeneficiaryRepository;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

@Singleton
public class BeneficiaryRepositoryImpl implements BeneficiaryRepository {

    private final MoneyFirestoreDataSource firestoreDataSource;

    @Inject
    public BeneficiaryRepositoryImpl(MoneyFirestoreDataSource firestoreDataSource) {
        this.firestoreDataSource = firestoreDataSource;
    }

    @Override
    public Observable<AppResult<List<Beneficiary>>> observeBeneficiaries() {
        return firestoreDataSource.observeBeneficiaries();
    }

    @Override
    public Single<AppResult<Beneficiary>> saveBeneficiary(Beneficiary beneficiary) {
        return firestoreDataSource.saveBeneficiary(beneficiary);
    }

    @Override
    public Single<AppResult<Boolean>> deleteBeneficiary(String beneficiaryId) {
        return firestoreDataSource.deleteBeneficiary(beneficiaryId);
    }
}
