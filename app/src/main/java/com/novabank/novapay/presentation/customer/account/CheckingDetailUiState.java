package com.novabank.novapay.presentation.customer.account;

import com.novabank.novapay.domain.model.account.Account;

public final class CheckingDetailUiState {

    private final Account account;

    public CheckingDetailUiState(Account account) {
        this.account = account;
    }

    public Account getAccount() {
        return account;
    }
}
