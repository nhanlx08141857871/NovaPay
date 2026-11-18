package com.novabank.novapay.domain.usecase.officer;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.domain.model.user.CustomerProfile;
import com.novabank.novapay.domain.repository.OfficerRepository;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public final class ToggleCustomerLockUseCase {

    private final OfficerRepository officerRepository;

    @Inject
    public ToggleCustomerLockUseCase(OfficerRepository officerRepository) {
        this.officerRepository = officerRepository;
    }

    public Single<AppResult<Boolean>> execute(CustomerProfile customer) {
        if (customer == null || customer.getUid().trim().isEmpty()) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Thiếu thông tin khách hàng."));
        }
        String nextStatus = customer.isLocked()
                ? Constants.USER_STATUS_ACTIVE
                : Constants.USER_STATUS_LOCKED;
        return officerRepository.updateCustomerStatus(customer.getUid(), nextStatus);
    }
}
