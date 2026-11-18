package com.novabank.novapay.domain.repository;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.account.Account;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

/**
 * Hợp đồng Repository Tài khoản dùng chung giữa Thành viên A (triển khai ở data/)
 * và Thành viên B (truy vấn danh sách tài khoản CHECKING nguồn khi thanh toán/chuyển tiền).
 */
public interface AccountRepository {

    Observable<AppResult<List<Account>>> observeMyAccounts();

    Observable<AppResult<Account>> observeAccountById(String accountId);

    Single<AppResult<Account>> getAccountById(String accountId);
}
