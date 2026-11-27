package com.novabank.novapay.domain.usecase;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.repository.BeneficiaryRepository;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public class DeleteBeneficiaryUseCase {

    private final BeneficiaryRepository beneficiaryRepository;

    @Inject
    public DeleteBeneficiaryUseCase(BeneficiaryRepository beneficiaryRepository) {
        this.beneficiaryRepository = beneficiaryRepository;
    }

    public Single<AppResult<Boolean>> execute(String beneficiaryId) {
        if (beneficiaryId == null || beneficiaryId.trim().isEmpty()) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Mã người thụ hưởng không hợp lệ."));
        }
        return beneficiaryRepository.deleteBeneficiary(beneficiaryId.trim());
    }
}
