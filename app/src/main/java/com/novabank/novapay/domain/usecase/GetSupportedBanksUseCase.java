package com.novabank.novapay.domain.usecase;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.bank.Bank;
import com.novabank.novapay.domain.repository.BankRepository;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

/**
 * Tải danh sách ngân hàng hỗ trợ chuyển tiền liên ngân hàng.
 */
public class GetSupportedBanksUseCase {

    private final BankRepository bankRepository;

    @Inject
    public GetSupportedBanksUseCase(BankRepository bankRepository) {
        this.bankRepository = bankRepository;
    }

    public Single<AppResult<List<Bank>>> execute() {
        return bankRepository.getSupportedBanks();
    }
}
