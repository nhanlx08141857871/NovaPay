package com.novabank.novapay.domain.usecase;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.utility.UtilityBill;
import com.novabank.novapay.domain.model.utility.UtilityBillType;
import com.novabank.novapay.domain.repository.UtilityRepository;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

/**
 * Tra cứu hóa đơn Điện / Nước chưa thanh toán theo mã khách hàng.
 */
public class LookupUtilityBillsUseCase {

    private final UtilityRepository utilityRepository;

    @Inject
    public LookupUtilityBillsUseCase(UtilityRepository utilityRepository) {
        this.utilityRepository = utilityRepository;
    }

    public Single<AppResult<List<UtilityBill>>> execute(String customerCode, UtilityBillType type) {
        if (customerCode == null || customerCode.trim().isEmpty()) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Vui lòng nhập mã khách hàng điện/nước."));
        }
        return utilityRepository.lookupUnpaidBills(customerCode.trim(), type);
    }
}
