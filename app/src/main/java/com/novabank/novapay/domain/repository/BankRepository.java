package com.novabank.novapay.domain.repository;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.bank.Bank;

import java.util.List;

import io.reactivex.rxjava3.core.Single;

public interface BankRepository {

    Single<AppResult<List<Bank>>> getSupportedBanks();
}
