package com.novabank.novapay.domain.repository;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.bank.Beneficiary;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public interface BeneficiaryRepository {

    Observable<AppResult<List<Beneficiary>>> observeBeneficiaries();

    Single<AppResult<Beneficiary>> saveBeneficiary(Beneficiary beneficiary);

    Single<AppResult<Boolean>> deleteBeneficiary(String beneficiaryId);
}
