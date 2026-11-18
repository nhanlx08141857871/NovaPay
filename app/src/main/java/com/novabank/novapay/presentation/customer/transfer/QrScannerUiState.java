package com.novabank.novapay.presentation.customer.transfer;

/**
 * Trạng thái màn hình Quét mã QR chuyển khoản (QrScannerFragment).
 */
public final class QrScannerUiState {

    private final boolean scannerReady;

    public QrScannerUiState(boolean scannerReady) {
        this.scannerReady = scannerReady;
    }

    public boolean isScannerReady() {
        return scannerReady;
    }
}
