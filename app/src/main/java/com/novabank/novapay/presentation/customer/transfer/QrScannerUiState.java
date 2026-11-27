package com.novabank.novapay.presentation.customer.transfer;

public final class QrScannerUiState {

    private final boolean scannerReady;

    public QrScannerUiState(boolean scannerReady) {
        this.scannerReady = scannerReady;
    }

    public boolean isScannerReady() {
        return scannerReady;
    }
}
