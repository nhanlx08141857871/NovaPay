package com.novabank.novapay.presentation.customer.account;

import com.novabank.novapay.domain.model.account.SavingProjection;

public final class SavingDetailUiState {

    private final SavingProjection projection;

    public SavingDetailUiState(SavingProjection projection) {
        this.projection = projection;
    }

    public SavingProjection getProjection() {
        return projection;
    }
}
