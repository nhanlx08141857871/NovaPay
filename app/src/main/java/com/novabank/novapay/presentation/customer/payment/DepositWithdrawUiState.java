package com.novabank.novapay.presentation.customer.payment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.novabank.novapay.domain.model.account.Account;

import java.util.Collections;
import java.util.List;

public final class DepositWithdrawUiState {

    public enum Mode {
        GATEWAY_DEPOSIT,
        WITHDRAW,
        MORTGAGE_PAYMENT
    }

    private final Mode mode;
    private final List<Account> checkingAccounts;
    private final int selectedCheckingIndex;
    private final List<Account> mortgageAccounts;
    private final int selectedMortgageIndex;

    public DepositWithdrawUiState(
            @NonNull Mode mode,
            @NonNull List<Account> checkingAccounts,
            int selectedCheckingIndex,
            @NonNull List<Account> mortgageAccounts,
            int selectedMortgageIndex
    ) {
        this.mode = mode;
        this.checkingAccounts = Collections.unmodifiableList(checkingAccounts);
        this.selectedCheckingIndex = selectedCheckingIndex;
        this.mortgageAccounts = Collections.unmodifiableList(mortgageAccounts);
        this.selectedMortgageIndex = selectedMortgageIndex;
    }

    @Nullable
    public Account getSelectedCheckingAccount() {
        if (checkingAccounts.isEmpty()
                || selectedCheckingIndex < 0
                || selectedCheckingIndex >= checkingAccounts.size()) {
            return null;
        }
        return checkingAccounts.get(selectedCheckingIndex);
    }

    @Nullable
    public Account getSelectedMortgageAccount() {
        if (mortgageAccounts.isEmpty()
                || selectedMortgageIndex < 0
                || selectedMortgageIndex >= mortgageAccounts.size()) {
            return null;
        }
        return mortgageAccounts.get(selectedMortgageIndex);
    }

    @NonNull
    public Mode getMode() {
        return mode;
    }

    @NonNull
    public List<Account> getCheckingAccounts() {
        return checkingAccounts;
    }

    public int getSelectedCheckingIndex() {
        return selectedCheckingIndex;
    }

    @NonNull
    public List<Account> getMortgageAccounts() {
        return mortgageAccounts;
    }

    public int getSelectedMortgageIndex() {
        return selectedMortgageIndex;
    }
}
