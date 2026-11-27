package com.novabank.novapay.presentation.customer.home;

import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.account.AccountType;
import com.novabank.novapay.domain.model.transaction.MonthlyCashflowSummary;
import com.novabank.novapay.domain.model.user.User;

import java.util.Collections;
import java.util.List;

/**
 * Dữ liệu trạng thái màn hình Trang chủ Khách hàng (CustomerHomeFragment).
 * Tổng tài sản bắt buộc dùng kiểu long (VNĐ).
 */
public final class CustomerHomeUiState {

    private final User user;
    private final List<Account> accounts;
    private final MonthlyCashflowSummary cashflow;
    private final long totalLiquidAssets;
    private final boolean balanceHidden;
    private final int unreadNotificationCount;

    public CustomerHomeUiState(
            User user,
            List<Account> accounts,
            MonthlyCashflowSummary cashflow,
            boolean balanceHidden,
            int unreadNotificationCount
    ) {
        this.user = user;
        this.accounts = accounts != null ? accounts : Collections.emptyList();
        this.cashflow = cashflow != null ? cashflow : new MonthlyCashflowSummary("", 0L, 0L, 0);
        this.totalLiquidAssets = computeTotalLiquidAssets(this.accounts);
        this.balanceHidden = balanceHidden;
        this.unreadNotificationCount = Math.max(0, unreadNotificationCount);
    }

    public static long computeTotalLiquidAssets(List<Account> accounts) {
        long sum = 0L;
        if (accounts != null) {
            for (Account account : accounts) {
                if (account != null && (account.getType() == AccountType.CHECKING
                        || account.getType() == AccountType.SAVING)) {
                    sum += account.getBalance();
                }
            }
        }
        return sum;
    }

    public User getUser() {
        return user;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public MonthlyCashflowSummary getCashflow() {
        return cashflow;
    }

    public long getTotalLiquidAssets() {
        return totalLiquidAssets;
    }

    public boolean isBalanceHidden() {
        return balanceHidden;
    }

    public int getUnreadNotificationCount() {
        return unreadNotificationCount;
    }
}
