package com.novabank.novapay.domain.usecase;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.utility.Order;
import com.novabank.novapay.domain.repository.UtilityRepository;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Observable;

public class ObserveMyOrdersUseCase {

    private final UtilityRepository utilityRepository;

    @Inject
    public ObserveMyOrdersUseCase(UtilityRepository utilityRepository) {
        this.utilityRepository = utilityRepository;
    }

    public Observable<AppResult<List<Order>>> execute() {
        return utilityRepository.observeMyOrders();
    }
}
