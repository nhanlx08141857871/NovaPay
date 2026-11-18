package com.novabank.novapay.presentation.customer.history;

import com.novabank.novapay.domain.model.transaction.LedgerDirection;
import com.novabank.novapay.domain.model.transaction.LedgerEntry;

import java.util.Collections;
import java.util.List;

public final class TransactionHistoryUiState {

    private final List<LedgerEntry> entries;
    private final LedgerDirection selectedDirection;
    private final boolean monthOnlyRange;

    public TransactionHistoryUiState(
            List<LedgerEntry> entries,
            LedgerDirection selectedDirection,
            boolean monthOnlyRange
    ) {
        this.entries = entries != null ? entries : Collections.emptyList();
        this.selectedDirection = selectedDirection;
        this.monthOnlyRange = monthOnlyRange;
    }

    public List<LedgerEntry> getEntries() {
        return entries;
    }

    public LedgerDirection getSelectedDirection() {
        return selectedDirection;
    }

    public boolean isMonthOnlyRange() {
        return monthOnlyRange;
    }
}
