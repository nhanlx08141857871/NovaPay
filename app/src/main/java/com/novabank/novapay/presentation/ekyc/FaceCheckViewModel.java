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

    @Inject
    public FaceCheckViewModel(SubmitFaceCheckUseCase submitFaceCheckUseCase) {
        this.submitFaceCheckUseCase = submitFaceCheckUseCase;
    }

    public void onLivenessUpdated(FaceLivenessEvaluator.LivenessVerdict verdict) {
        UiState<FaceCheckUiState> current = uiState.getValue();
        if (current != null && current.getStatus() == UiState.Status.LOADING) {
            return;
        }
        boolean passed = verdict == FaceLivenessEvaluator.LivenessVerdict.READY;
        uiState.setValue(UiState.success(new FaceCheckUiState(
                passed,
                FaceLivenessEvaluator.toGuideMessageRes(verdict)
        )));
    }

    public void verifyFace(String draftId, String imageBase64) {
        uiState.setValue(UiState.loading());
        disposables.add(
                submitFaceCheckUseCase.execute(draftId, imageBase64)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::handleVerifyResult, throwable ->
                                uiState.setValue(UiState.error(throwable.getMessage())))
        );
    }

    public void resetReady() {
        uiState.setValue(UiState.success(
                new FaceCheckUiState(false, R.string.core_liveness_no_face)
        ));
    }

    private void handleVerifyResult(AppResult<Long> result) {
        if (result.isSuccess() && result.getData() != null) {
            faceCheckPassedEvent.setValue(result.getData());
        } else {
            uiState.setValue(UiState.error(result.getMessage()));
        }
    }

    public LiveData<UiState<FaceCheckUiState>> getUiState() {
        return uiState;
    }

    public SingleLiveEvent<Long> getFaceCheckPassedEvent() {
        return faceCheckPassedEvent;
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}
