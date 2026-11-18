package com.novabank.novapay.presentation.customer.utility;

import androidx.annotation.NonNull;

import com.novabank.novapay.domain.model.account.Account;

import java.util.Collections;
import java.util.List;

/**
 * Trạng thái màn hình Nạp tiền điện thoại (PhoneTopupFragment).
 */
public final class PhoneTopupUiState {

    private final List<Account> checkingAccounts;
    private final int selectedSourceIndex;
    private final String selectedCarrier;
    private final long selectedAmountVnd;

    public PhoneTopupUiState(
            @NonNull List<Account> checkingAccounts,
            int selectedSourceIndex,
            @NonNull String selectedCarrier,
            long selectedAmountVnd
    ) {
        this.checkingAccounts = Collections.unmodifiableList(checkingAccounts);
        this.selectedSourceIndex = selectedSourceIndex;
        this.selectedCarrier = selectedCarrier;
        this.selectedAmountVnd = selectedAmountVnd;
    }

    @NonNull
    public List<Account> getCheckingAccounts() {
        return checkingAccounts;
    }

    public int getSelectedSourceIndex() {
        return selectedSourceIndex;
    }

    @NonNull
    public String getSelectedCarrier() {
        return selectedCarrier;
    }

    public long getSelectedAmountVnd() {
        return selectedAmountVnd;
    }
}
