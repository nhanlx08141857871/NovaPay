package com.novabank.novapay.domain.usecase;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.bank.Beneficiary;
import com.novabank.novapay.domain.repository.BeneficiaryRepository;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Observable;

public class ObserveBeneficiariesUseCase {

    private final BeneficiaryRepository beneficiaryRepository;

    @Inject
    public ObserveBeneficiariesUseCase(BeneficiaryRepository beneficiaryRepository) {
        this.beneficiaryRepository = beneficiaryRepository;
    }

    public Observable<AppResult<List<Beneficiary>>> execute() {
        return beneficiaryRepository.observeBeneficiaries();
    }
}
