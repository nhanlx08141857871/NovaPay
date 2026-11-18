package com.novabank.novapay.presentation.customer.account;

import com.novabank.novapay.domain.model.account.Account;

public final class MortgageDetailUiState {

    private final Account account;

    public MortgageDetailUiState(Account account) {
        this.account = account;
    }

    public Account getAccount() {
        return account;
    }
}
