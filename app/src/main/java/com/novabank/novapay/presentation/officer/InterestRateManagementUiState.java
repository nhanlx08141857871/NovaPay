package com.novabank.novapay.presentation.officer;

import com.novabank.novapay.domain.model.rate.InterestRate;

import java.util.List;

public final class InterestRateManagementUiState {

    private final List<InterestRate> rates;

    public InterestRateManagementUiState(List<InterestRate> rates) {
        this.rates = rates;
    }

    public List<InterestRate> getRates() {
        return rates;
    }
}
