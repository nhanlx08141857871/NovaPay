package com.novabank.novapay.domain.usecase.officer;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.user.CustomerValidation;
import com.novabank.novapay.domain.repository.OfficerRepository;

import java.util.Locale;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public final class CreateCustomerUseCase {

    private final OfficerRepository officerRepository;

    @Inject
    public CreateCustomerUseCase(OfficerRepository officerRepository) {
        this.officerRepository = officerRepository;
    }

    public Single<AppResult<String>> execute(
            String fullName,
            String email,
            String phone,
            String nationalId,
            String dob,
            String address,
            String initialPassword
    ) {
        if (!CustomerValidation.isValidFullName(fullName)) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Họ và tên phải có ít nhất 2 ký tự."));
        }
        if (!CustomerValidation.isValidEmail(email)) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Địa chỉ email không hợp lệ."));
        }
        if (!CustomerValidation.isValidPhone(phone)) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Số điện thoại không hợp lệ."));
        }
        if (!CustomerValidation.isValidNationalId(nationalId)) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Số CMND/CCCD phải gồm 9 hoặc 12 chữ số."));
        }
        String normalizedDob = CustomerValidation.normalizeDobToIso(dob);
        if (normalizedDob == null) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Ngày sinh không hợp lệ."));
        }
        if (!CustomerValidation.isValidAddress(address)) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Địa chỉ không được để trống."));
        }
        if (!CustomerValidation.isValidInitialPassword(initialPassword)) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Mật khẩu khởi tạo phải từ 6 ký tự."));
        }

        return officerRepository.createCustomer(
                fullName.trim(),
                email.trim().toLowerCase(Locale.ROOT),
                CustomerValidation.normalizePhone(phone),
                nationalId.trim(),
                normalizedDob,
                address.trim(),
                initialPassword
        );
    }
}
