package com.novabank.novapay.domain.repository;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.utility.Order;
import com.novabank.novapay.domain.model.utility.ServiceCategory;
import com.novabank.novapay.domain.model.utility.ServiceItem;
import com.novabank.novapay.domain.model.utility.UtilityBill;
import com.novabank.novapay.domain.model.utility.UtilityBillType;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

/**
 * Hợp đồng Repository tra cứu hóa đơn Điện/Nước, danh mục dịch vụ và đơn hàng đã đặt.
 */
public interface UtilityRepository {

    Single<AppResult<List<UtilityBill>>> lookupUnpaidBills(String customerCode, UtilityBillType type);

    Single<AppResult<List<ServiceItem>>> getServiceCatalog(ServiceCategory category);

    Observable<AppResult<List<Order>>> observeMyOrders();
}
