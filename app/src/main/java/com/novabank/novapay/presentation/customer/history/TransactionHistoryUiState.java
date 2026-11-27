package com.novabank.novapay.presentation.customer.history;

import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.transaction.LedgerDirection;
import com.novabank.novapay.domain.model.transaction.LedgerEntry;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public final class TransactionHistoryUiState {

    public enum RangeMode {
        THIS_MONTH,
        RECENT_90_DAYS,
        CUSTOM
    }

    private final List<LedgerEntry> entries;
    private final List<Account> accounts;
    private final String selectedAccountId;
    private final LedgerDirection selectedDirection;
    private final RangeMode rangeMode;
    private final Instant customStart;
    private final Instant customEnd;
    private final long totalIn;
    private final long totalOut;
    private final String customerName;

    public TransactionHistoryUiState(
            List<LedgerEntry> entries,
            List<Account> accounts,
            String selectedAccountId,
            LedgerDirection selectedDirection,
            RangeMode rangeMode,
            Instant customStart,
            Instant customEnd,
            long totalIn,
            long totalOut,
            String customerName
    ) {
        this.entries = entries != null ? entries : Collections.emptyList();
        this.accounts = accounts != null ? accounts : Collections.emptyList();
        this.selectedAccountId = selectedAccountId;
        this.selectedDirection = selectedDirection;
        this.rangeMode = rangeMode != null ? rangeMode : RangeMode.THIS_MONTH;
        this.customStart = customStart;
        this.customEnd = customEnd;
        this.totalIn = totalIn;
        this.totalOut = totalOut;
        this.customerName = customerName != null ? customerName : "";
    }

    public List<LedgerEntry> getEntries() {
        return entries;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public String getSelectedAccountId() {
        return selectedAccountId;
    }

    public LedgerDirection getSelectedDirection() {
        return selectedDirection;
    }

    public RangeMode getRangeMode() {
        return rangeMode;
    }

    public Instant getCustomStart() {
        return customStart;
    }

    public Instant getCustomEnd() {
        return customEnd;
    }

    public long getTotalIn() {
        return totalIn;
    }

    public long getTotalOut() {
        return totalOut;
    }

    public String getCustomerName() {
        return customerName;
    }
}
