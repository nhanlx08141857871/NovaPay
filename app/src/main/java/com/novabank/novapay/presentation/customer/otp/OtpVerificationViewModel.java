package com.novabank.novapay.presentation.customer.otp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.core.common.SingleLiveEvent;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.domain.model.transaction.TransactionConfirmResult;
import com.novabank.novapay.domain.usecase.ConfirmTransactionUseCase;
import com.novabank.novapay.domain.usecase.ResendOtpUseCase;

import java.time.Instant;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class OtpVerificationViewModel extends ViewModel {

    private final ConfirmTransactionUseCase confirmTransactionUseCase;
    private final ResendOtpUseCase resendOtpUseCase;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<UiState<OtpVerificationUiState>> uiState = new MutableLiveData<>();
    private final SingleLiveEvent<TransactionConfirmResult> confirmedEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Boolean> otpResentEvent = new SingleLiveEvent<>();

    private OtpVerificationUiState currentData;
    private boolean initialized = false;

    @Inject
    public OtpVerificationViewModel(
            ConfirmTransactionUseCase confirmTransactionUseCase,
            ResendOtpUseCase resendOtpUseCase
    ) {
        this.confirmTransactionUseCase = confirmTransactionUseCase;
        this.resendOtpUseCase = resendOtpUseCase;
    }

    public LiveData<UiState<OtpVerificationUiState>> getUiState() {
        return uiState;
    }

    public SingleLiveEvent<TransactionConfirmResult> getConfirmedEvent() {
        return confirmedEvent;
    }

    public SingleLiveEvent<Boolean> getOtpResentEvent() {
        return otpResentEvent;
    }

    public void initialize(
            @Nullable String draftId,
            boolean requiresFace,
            boolean requiresBiometric,
            long initialOtpExpiresAtEpochMs
    ) {
        if (initialized) {
            return;
        }
        initialized = true;
        if (draftId == null || draftId.trim().isEmpty()) {
            uiState.setValue(UiState.empty());
            return;
        }
        long effectiveExpiry = initialOtpExpiresAtEpochMs;
        if (!requiresFace && effectiveExpiry <= 0L) {
            effectiveExpiry = System.currentTimeMillis() + (Constants.OTP_EXPIRY_SECONDS * 1000L);
        }
        currentData = new OtpVerificationUiState(
                draftId.trim(),
                requiresFace,
                !requiresFace,
                requiresBiometric,
                !requiresBiometric,
                effectiveExpiry,
                null
        );
        uiState.setValue(UiState.success(currentData));
    }

    public void onFaceCheckResult(boolean passed, long otpExpiresAtEpochMs, @NonNull String failMessage) {
        if (currentData == null) {
            return;
        }
        if (passed) {
            long effectiveExpiry = otpExpiresAtEpochMs > 0L
                    ? otpExpiresAtEpochMs
                    : System.currentTimeMillis() + (Constants.OTP_EXPIRY_SECONDS * 1000L);
            currentData = new OtpVerificationUiState(
                    currentData.getDraftId(),
                    currentData.isRequiresFace(),
                    true,
                    currentData.isRequiresBiometric(),
                    currentData.isBiometricPassed(),
                    effectiveExpiry,
                    null
            );
        } else {
            currentData = new OtpVerificationUiState(
                    currentData.getDraftId(),
                    currentData.isRequiresFace(),
                    false,
                    currentData.isRequiresBiometric(),
                    currentData.isBiometricPassed(),
                    currentData.getOtpExpiresAtEpochMs(),
                    failMessage
            );
        }
        uiState.setValue(UiState.success(currentData));
    }

    public void onBiometricSucceeded() {
        if (currentData == null) {
            return;
        }
        currentData = new OtpVerificationUiState(
                currentData.getDraftId(),
                currentData.isRequiresFace(),
                currentData.isFacePassed(),
                currentData.isRequiresBiometric(),
                true,
                currentData.getOtpExpiresAtEpochMs(),
                null
        );
        uiState.setValue(UiState.success(currentData));
    }

    public void onBiometricFailed(@NonNull String message) {
        if (currentData == null) {
            return;
        }
        currentData = new OtpVerificationUiState(
                currentData.getDraftId(),
                currentData.isRequiresFace(),
                currentData.isFacePassed(),
                currentData.isRequiresBiometric(),
                false,
                currentData.getOtpExpiresAtEpochMs(),
                message
        );
        uiState.setValue(UiState.success(currentData));
    }

    public void resendOtp() {
        if (currentData == null || currentData.isWaitingForFace()) {
            return;
        }
        uiState.setValue(UiState.loading());
        disposables.add(
                resendOtpUseCase.execute(currentData.getDraftId())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                            if (result.isSuccess() && result.getData() != null) {
                                Instant expiresAt = result.getData();
                                currentData = new OtpVerificationUiState(
                                        currentData.getDraftId(),
                                        currentData.isRequiresFace(),
                                        currentData.isFacePassed(),
                                        currentData.isRequiresBiometric(),
                                        currentData.isBiometricPassed(),
                                        expiresAt.toEpochMilli(),
                                        null
                                );
                                uiState.setValue(UiState.success(currentData));
                                otpResentEvent.setValue(Boolean.TRUE);
                            } else {
                                currentData = new OtpVerificationUiState(
                                        currentData.getDraftId(),
                                        currentData.isRequiresFace(),
                                        currentData.isFacePassed(),
                                        currentData.isRequiresBiometric(),
                                        currentData.isBiometricPassed(),
                                        currentData.getOtpExpiresAtEpochMs(),
                                        result.getMessage()
                                );
                                uiState.setValue(UiState.success(currentData));
                            }
                        }, throwable -> uiState.setValue(UiState.error(throwable.getMessage())))
        );
    }

    public void confirmOtp(@Nullable String otpCode) {
        if (currentData == null || !currentData.isReadyForOtpInput()) {
            return;
        }
        uiState.setValue(UiState.loading());
        disposables.add(
                confirmTransactionUseCase.execute(
                                currentData.getDraftId(),
                                otpCode,
                                currentData.isBiometricPassed()
                        )
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                            if (result.isSuccess() && result.getData() != null) {
                                uiState.setValue(UiState.success(currentData));
                                confirmedEvent.setValue(result.getData());
                            } else {
                                String code = result.getErrorCode();
                                if ("TOO_MANY_ATTEMPTS".equals(code) || "DRAFT_EXPIRED".equals(code)) {
                                    uiState.setValue(UiState.error(result.getMessage()));
                                } else {
                                    currentData = new OtpVerificationUiState(
                                            currentData.getDraftId(),
                                            currentData.isRequiresFace(),
                                            currentData.isFacePassed(),
                                            currentData.isRequiresBiometric(),
                                            currentData.isBiometricPassed(),
                                            currentData.getOtpExpiresAtEpochMs(),
                                            result.getMessage()
                                    );
                                    uiState.setValue(UiState.success(currentData));
                                }
                            }
                        }, throwable -> uiState.setValue(UiState.error(throwable.getMessage())))
        );
    }

    public void restoreFromError() {
        if (currentData != null) {
            uiState.setValue(UiState.success(currentData));
        }
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}
