package com.novabank.novapay.domain.repository;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.bank.Bank;

import java.util.List;

import io.reactivex.rxjava3.core.Single;

/**
 * Hợp đồng Repository danh sách ngân hàng hỗ trợ chuyển tiền liên ngân hàng.
 */
public interface BankRepository {

    Single<AppResult<List<Bank>>> getSupportedBanks();
}
