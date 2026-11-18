package com.novabank.novapay.domain.usecase.officer;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.user.CustomerProfile;
import com.novabank.novapay.domain.model.user.CustomerValidation;
import com.novabank.novapay.domain.repository.OfficerRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Observable;

public final class ObserveCustomersUseCase {

    private final OfficerRepository officerRepository;

    @Inject
    public ObserveCustomersUseCase(OfficerRepository officerRepository) {
        this.officerRepository = officerRepository;
    }

    public Observable<AppResult<List<CustomerProfile>>> execute() {
        return officerRepository.observeCustomers();
    }

    public List<CustomerProfile> filter(List<CustomerProfile> customers, String query) {
        if (customers == null || customers.isEmpty()) {
            return Collections.emptyList();
        }
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(customers);
        }
        List<CustomerProfile> filtered = new ArrayList<>();
        for (CustomerProfile customer : customers) {
            if (CustomerValidation.matchesQuery(customer, query)) {
                filtered.add(customer);
            }
        }
        return filtered;
    }
}
