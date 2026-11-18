package com.novabank.novapay.presentation.customer.account;

import com.novabank.novapay.domain.model.account.AccountType;

public final class AccountRouterUiState {

    private final String accountId;
    private final AccountType accountType;

    public AccountRouterUiState(String accountId, AccountType accountType) {
        this.accountId = accountId;
        this.accountType = accountType;
    }

    public String getAccountId() {
        return accountId;
    }

    public AccountType getAccountType() {
        return accountType;
    }
}
