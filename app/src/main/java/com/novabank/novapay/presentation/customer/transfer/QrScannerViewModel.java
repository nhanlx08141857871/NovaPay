package com.novabank.novapay.presentation.customer.transfer;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novabank.novapay.core.common.SingleLiveEvent;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.core.util.QrHelper;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel giải mã dữ liệu QR chuyển khoản bằng QrHelper.parseTransferQrPayload.
 */
@HiltViewModel
public class QrScannerViewModel extends ViewModel {

    private final MutableLiveData<UiState<QrScannerUiState>> uiState =
            new MutableLiveData<>(UiState.success(new QrScannerUiState(true)));
    private final SingleLiveEvent<QrHelper.QrTransferPayload> decodedEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Boolean> invalidQrEvent = new SingleLiveEvent<>();
    private boolean consumed = false;

    @Inject
    public QrScannerViewModel() {
    }

    public LiveData<UiState<QrScannerUiState>> getUiState() {
        return uiState;
    }

    public SingleLiveEvent<QrHelper.QrTransferPayload> getDecodedEvent() {
        return decodedEvent;
    }

    public SingleLiveEvent<Boolean> getInvalidQrEvent() {
        return invalidQrEvent;
    }

    public void onCameraScanRaw(@Nullable String rawValue) {
        if (consumed || rawValue == null || rawValue.trim().isEmpty()) {
            return;
        }
        QrHelper.QrTransferPayload payload = QrHelper.parseTransferQrPayload(rawValue);
        if (payload != null) {
            consumed = true;
            decodedEvent.setValue(payload);
        }
    }

    public void onManualSubmit(@Nullable String rawValue) {
        if (consumed) {
            return;
        }
        QrHelper.QrTransferPayload payload = QrHelper.parseTransferQrPayload(rawValue);
        if (payload != null) {
            consumed = true;
            decodedEvent.setValue(payload);
        } else {
            invalidQrEvent.setValue(Boolean.TRUE);
        }
    }

    public void resetReady() {
        consumed = false;
        uiState.setValue(UiState.success(new QrScannerUiState(true)));
    }
}
