package com.novabank.novapay.presentation.customer.transfer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.bank.Bank;
import com.novabank.novapay.domain.model.bank.Beneficiary;
import com.novabank.novapay.domain.model.transaction.AccountLookupResult;

import java.util.Collections;
import java.util.List;

public final class TransferUiState {

    private final List<Account> checkingAccounts;
    private final int selectedSourceIndex;
    private final List<Bank> banks;
    private final int selectedBankIndex;
    private final List<Beneficiary> beneficiaries;
    private final boolean interbankMode;
    private final boolean lookingUpRecipient;
    private final AccountLookupResult lookupResult;
    private final String lookupError;

    public TransferUiState(
            @NonNull List<Account> checkingAccounts,
            int selectedSourceIndex,
            @NonNull List<Bank> banks,
            int selectedBankIndex,
            @NonNull List<Beneficiary> beneficiaries,
            boolean interbankMode,
            boolean lookingUpRecipient,
            @Nullable AccountLookupResult lookupResult,
            @Nullable String lookupError
    ) {
        this.checkingAccounts = Collections.unmodifiableList(checkingAccounts);
        this.selectedSourceIndex = selectedSourceIndex;
        this.banks = Collections.unmodifiableList(banks);
        this.selectedBankIndex = selectedBankIndex;
        this.beneficiaries = Collections.unmodifiableList(beneficiaries);
        this.interbankMode = interbankMode;
        this.lookingUpRecipient = lookingUpRecipient;
        this.lookupResult = lookupResult;
        this.lookupError = lookupError;
    }

    @Nullable
    public Account getSelectedSourceAccount() {
        if (checkingAccounts.isEmpty()
                || selectedSourceIndex < 0
                || selectedSourceIndex >= checkingAccounts.size()) {
            return null;
        }
        return checkingAccounts.get(selectedSourceIndex);
    }

    @Nullable
    public Bank getSelectedBank() {
        if (banks.isEmpty() || selectedBankIndex < 0 || selectedBankIndex >= banks.size()) {
            return null;
        }
        return banks.get(selectedBankIndex);
    }

    @NonNull
    public List<Account> getCheckingAccounts() {
        return checkingAccounts;
    }

    public int getSelectedSourceIndex() {
        return selectedSourceIndex;
    }

    @NonNull
    public List<Bank> getBanks() {
        return banks;
    }

    public int getSelectedBankIndex() {
        return selectedBankIndex;
    }

    @NonNull
    public List<Beneficiary> getBeneficiaries() {
        return beneficiaries;
    }

    public boolean isInterbankMode() {
        return interbankMode;
    }

    public boolean isLookingUpRecipient() {
        return lookingUpRecipient;
    }

    @Nullable
    public AccountLookupResult getLookupResult() {
        return lookupResult;
    }

    @Nullable
    public String getLookupError() {
        return lookupError;
    }
}
