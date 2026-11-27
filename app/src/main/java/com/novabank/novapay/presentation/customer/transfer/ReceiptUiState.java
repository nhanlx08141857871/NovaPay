package com.novabank.novapay.presentation.customer.transfer;

import androidx.annotation.NonNull;

import com.novabank.novapay.domain.model.transaction.Transaction;

import java.util.Objects;

public final class ReceiptUiState {

    private final Transaction transaction;

    public ReceiptUiState(@NonNull Transaction transaction) {
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
    }

    @NonNull
    public Transaction getTransaction() {
        return transaction;
    }
}
