package com.novabank.novapay.data.repository;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.data.remote.firebase.MoneyFirestoreDataSource;
import com.novabank.novapay.domain.model.utility.Order;
import com.novabank.novapay.domain.model.utility.ServiceCategory;
import com.novabank.novapay.domain.model.utility.ServiceItem;
import com.novabank.novapay.domain.model.utility.UtilityBill;
import com.novabank.novapay.domain.model.utility.UtilityBillType;
import com.novabank.novapay.domain.repository.UtilityRepository;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

@Singleton
public class UtilityRepositoryImpl implements UtilityRepository {

    private final MoneyFirestoreDataSource firestoreDataSource;

    @Inject
    public UtilityRepositoryImpl(MoneyFirestoreDataSource firestoreDataSource) {
        this.firestoreDataSource = firestoreDataSource;
    }

    @Override
    public Single<AppResult<List<UtilityBill>>> lookupUnpaidBills(String customerCode, UtilityBillType type) {
        return firestoreDataSource.lookupUnpaidBills(customerCode, type);
    }

    @Override
    public Single<AppResult<List<ServiceItem>>> getServiceCatalog(ServiceCategory category) {
        return firestoreDataSource.getServiceCatalog(category);
    }

    @Override
    public Observable<AppResult<List<Order>>> observeMyOrders() {
        return firestoreDataSource.observeMyOrders();
    }
}
