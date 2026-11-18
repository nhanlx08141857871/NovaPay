package com.novabank.novapay.domain.usecase;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.account.AccountStatus;
import com.novabank.novapay.domain.model.account.AccountType;
import com.novabank.novapay.domain.repository.SourceAccountRepository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

/**
 * Truy vấn tài khoản vay thế chấp MORTGAGE đang ACTIVE phục vụ luồng trả nợ vay.
 */
public class GetMortgageAccountUseCase {

    private final SourceAccountRepository sourceAccountRepository;

    @Inject
    public GetMortgageAccountUseCase(SourceAccountRepository sourceAccountRepository) {
        this.sourceAccountRepository = sourceAccountRepository;
    }

    public Single<AppResult<List<Account>>> getActiveMortgages() {
        return sourceAccountRepository.getActiveMortgageAccounts()
                .map(result -> {
                    if (result == null || result.isError()) {
                        return result != null ? result : AppResult.error("UNKNOWN_ERROR", "Không thể tải tài khoản vay.");
                    }
                    List<Account> mortgages = new ArrayList<>();
                    if (result.getData() != null) {
                        for (Account account : result.getData()) {
                            if (account != null
                                    && account.getType() == AccountType.MORTGAGE
                                    && account.getStatus() == AccountStatus.ACTIVE) {
                                mortgages.add(account);
                            }
                        }
                    }
                    return AppResult.success(mortgages);
                });
    }

    public Single<AppResult<Account>> getMortgageById(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Mã tài khoản vay không hợp lệ."));
        }
        return sourceAccountRepository.getAccountById(accountId.trim())
                .map(result -> {
                    if (result == null || result.isError()) {
                        return result != null ? result : AppResult.error("ACCOUNT_NOT_FOUND", "Không tìm thấy tài khoản vay.");
                    }
                    Account account = result.getData();
                    if (account == null || account.getType() != AccountType.MORTGAGE) {
                        return AppResult.error("INVALID_ARGUMENT", "Tài khoản được chọn không phải tài khoản vay thế chấp.");
                    }
                    if (account.getStatus() != AccountStatus.ACTIVE) {
                        return AppResult.error("ACCOUNT_NOT_ACTIVE", "Tài khoản vay hiện không ở trạng thái hoạt động.");
                    }
                    return AppResult.success(account);
                });
    }
}
