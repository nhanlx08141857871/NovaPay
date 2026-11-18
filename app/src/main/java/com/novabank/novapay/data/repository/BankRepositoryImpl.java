package com.novabank.novapay.data.repository;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.data.remote.firebase.MoneyFirestoreDataSource;
import com.novabank.novapay.domain.model.bank.Bank;
import com.novabank.novapay.domain.repository.BankRepository;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Single;

@Singleton
public class BankRepositoryImpl implements BankRepository {

    private final MoneyFirestoreDataSource firestoreDataSource;

    @Inject
    public BankRepositoryImpl(MoneyFirestoreDataSource firestoreDataSource) {
        this.firestoreDataSource = firestoreDataSource;
    }

    @Override
    public Single<AppResult<List<Bank>>> getSupportedBanks() {
        return firestoreDataSource.getSupportedBanks();
    }
}
