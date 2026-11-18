package com.novabank.novapay.domain.usecase.officer;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.user.CustomerValidation;
import com.novabank.novapay.domain.repository.OfficerRepository;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public final class UpdateCustomerUseCase {

    private final OfficerRepository officerRepository;

    @Inject
    public UpdateCustomerUseCase(OfficerRepository officerRepository) {
        this.officerRepository = officerRepository;
    }

    public Single<AppResult<Boolean>> execute(
            String uid,
            String fullName,
            String phone,
            String address
    ) {
        if (uid == null || uid.trim().isEmpty()) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Thiếu mã định danh khách hàng."));
        }
        if (!CustomerValidation.isValidFullName(fullName)) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Họ và tên phải có ít nhất 2 ký tự."));
        }
        if (!CustomerValidation.isValidPhone(phone)) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Số điện thoại không hợp lệ."));
        }
        if (!CustomerValidation.isValidAddress(address)) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Địa chỉ không được để trống."));
        }

        return officerRepository.updateCustomerProfile(
                uid.trim(),
                fullName.trim(),
                CustomerValidation.normalizePhone(phone),
                address.trim()
        );
    }
}
