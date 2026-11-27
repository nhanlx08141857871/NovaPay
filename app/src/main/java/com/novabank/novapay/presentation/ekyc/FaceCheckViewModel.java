package com.novabank.novapay.presentation.ekyc;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novabank.novapay.R;
import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.core.common.SingleLiveEvent;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.domain.usecase.ekyc.SubmitFaceCheckUseCase;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

@HiltViewModel
public class FaceCheckViewModel extends ViewModel {

    private final SubmitFaceCheckUseCase submitFaceCheckUseCase;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<UiState<FaceCheckUiState>> uiState =
            new MutableLiveData<>(UiState.success(
                    new FaceCheckUiState(false, R.string.core_liveness_no_face)
            ));
    private final SingleLiveEvent<Long> faceCheckPassedEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> retryErrorEvent = new SingleLiveEvent<>();
    private String lastInlineError;

    @Inject
    public FaceCheckViewModel(SubmitFaceCheckUseCase submitFaceCheckUseCase) {
        this.submitFaceCheckUseCase = submitFaceCheckUseCase;
    }

    public static final String MISMATCH_WARNING_MESSAGE =
            "Khuôn mặt không khớp với hồ sơ eKYC đã đăng ký. Vui lòng thử lại!";

    public static boolean isTerminalErrorCode(String errorCode) {
        if (errorCode == null) {
            return false;
        }
        String normalized = errorCode.trim().toUpperCase();
        return "TOO_MANY_ATTEMPTS".equals(normalized)
                || "DRAFT_EXPIRED".equals(normalized)
                || "EKYC_REQUIRED".equals(normalized)
                || "EKYC_FACE_NOT_FOUND".equals(normalized)
                || "PERMISSION_DENIED".equals(normalized);
    }

    public void onLivenessUpdated(FaceLivenessEvaluator.LivenessVerdict verdict) {
        UiState<FaceCheckUiState> current = uiState.getValue();
        if (current != null && (current.getStatus() == UiState.Status.LOADING
                || current.getStatus() == UiState.Status.ERROR)) {
            return;
        }
        boolean passed = verdict == FaceLivenessEvaluator.LivenessVerdict.READY;
        uiState.setValue(UiState.success(new FaceCheckUiState(
                passed,
                FaceLivenessEvaluator.toGuideMessageRes(verdict),
                lastInlineError
        )));
    }

    public void verifyFace(String draftId, String imageBase64) {
        lastInlineError = null;
        uiState.setValue(UiState.loading());
        disposables.add(
                submitFaceCheckUseCase.execute(draftId, imageBase64)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::handleVerifyResult, throwable ->
                                emitRetryableFailure(throwable != null ? throwable.getMessage() : null))
        );
    }

    public void resetReady() {
        lastInlineError = null;
        uiState.setValue(UiState.success(
                new FaceCheckUiState(false, R.string.core_liveness_no_face)
        ));
    }

    private void handleVerifyResult(AppResult<Long> result) {
        if (result.isSuccess() && result.getData() != null) {
            lastInlineError = null;
            faceCheckPassedEvent.setValue(result.getData());
            return;
        }
        if (isTerminalErrorCode(result.getErrorCode())) {
            lastInlineError = null;
            uiState.setValue(UiState.error(result.getMessage()));
        } else if ("FACE_MISMATCH".equalsIgnoreCase(result.getErrorCode())) {
            emitRetryableFailure(MISMATCH_WARNING_MESSAGE);
        } else {
            emitRetryableFailure(result.getMessage());
        }
    }

    private void emitRetryableFailure(String message) {
        lastInlineError = (message != null && !message.trim().isEmpty())
                ? message.trim()
                : null;
        if (lastInlineError != null) {
            retryErrorEvent.setValue(lastInlineError);
        }
        uiState.setValue(UiState.success(new FaceCheckUiState(
                false,
                R.string.core_liveness_no_face,
                lastInlineError
        )));
    }

    public LiveData<UiState<FaceCheckUiState>> getUiState() {
        return uiState;
    }

    public SingleLiveEvent<Long> getFaceCheckPassedEvent() {
        return faceCheckPassedEvent;
    }

    public SingleLiveEvent<String> getRetryErrorEvent() {
        return retryErrorEvent;
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}
