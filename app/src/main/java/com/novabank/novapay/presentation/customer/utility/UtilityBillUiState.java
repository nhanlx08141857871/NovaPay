package com.novabank.novapay.presentation.customer.utility;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.utility.UtilityBill;
import com.novabank.novapay.domain.model.utility.UtilityBillType;

import java.util.Collections;
import java.util.List;

public final class UtilityBillUiState {

    private final List<Account> checkingAccounts;
    private final int selectedSourceIndex;
    private final UtilityBillType filterType;
    private final List<UtilityBill> bills;
    private final boolean hasSearched;

    public UtilityBillUiState(
            @NonNull List<Account> checkingAccounts,
            int selectedSourceIndex,
            @Nullable UtilityBillType filterType,
            @NonNull List<UtilityBill> bills,
            boolean hasSearched
    ) {
        this.checkingAccounts = Collections.unmodifiableList(checkingAccounts);
        this.selectedSourceIndex = selectedSourceIndex;
        this.filterType = filterType;
        this.bills = Collections.unmodifiableList(bills);
        this.hasSearched = hasSearched;
    }

    @NonNull
    public List<Account> getCheckingAccounts() {
        return checkingAccounts;
    }

    public int getSelectedSourceIndex() {
        return selectedSourceIndex;
    }

    @Nullable
    public UtilityBillType getFilterType() {
        return filterType;
    }

    @NonNull
    public List<UtilityBill> getBills() {
        return bills;
    }

    public boolean isHasSearched() {
        return hasSearched;
    }
}
