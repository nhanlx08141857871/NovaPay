package com.novabank.novapay.domain.usecase;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.bank.Beneficiary;
import com.novabank.novapay.domain.repository.BeneficiaryRepository;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public class SaveBeneficiaryUseCase {

    private final BeneficiaryRepository beneficiaryRepository;

    @Inject
    public SaveBeneficiaryUseCase(BeneficiaryRepository beneficiaryRepository) {
        this.beneficiaryRepository = beneficiaryRepository;
    }

    public Single<AppResult<Beneficiary>> execute(Beneficiary beneficiary) {
        if (beneficiary == null) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Thông tin người thụ hưởng không hợp lệ."));
        }
        if (beneficiary.getName().isEmpty() || beneficiary.getAccountNumber().isEmpty()) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Tên và số tài khoản người thụ hưởng không được để trống."));
        }
        return beneficiaryRepository.saveBeneficiary(beneficiary);
    }
}
