package com.novabank.novapay.presentation.ekyc;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novabank.novapay.R;
import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.core.common.SingleLiveEvent;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.domain.usecase.ekyc.SubmitEkycUseCase;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

@HiltViewModel
public class EkycCaptureViewModel extends ViewModel {

    private final SubmitEkycUseCase submitEkycUseCase;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<UiState<EkycCaptureUiState>> uiState =
            new MutableLiveData<>(UiState.success(
                    new EkycCaptureUiState(true, false, R.string.core_liveness_no_face)
            ));
    private final SingleLiveEvent<String> submitSuccessEvent = new SingleLiveEvent<>();

    @Inject
    public EkycCaptureViewModel(SubmitEkycUseCase submitEkycUseCase) {
        this.submitEkycUseCase = submitEkycUseCase;
    }

    public void onLivenessUpdated(FaceLivenessEvaluator.LivenessVerdict verdict) {
        UiState<EkycCaptureUiState> current = uiState.getValue();
        if (current != null && current.getStatus() == UiState.Status.LOADING) {
            return;
        }
        boolean passed = verdict == FaceLivenessEvaluator.LivenessVerdict.READY;
        uiState.setValue(UiState.success(new EkycCaptureUiState(
                true,
                passed,
                FaceLivenessEvaluator.toGuideMessageRes(verdict)
        )));
    }

    public void submitCapturedBase64(String imageBase64, String targetUid) {
        uiState.setValue(UiState.loading());
        disposables.add(
                submitEkycUseCase.execute(imageBase64, targetUid)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::handleSubmitResult, throwable ->
                                uiState.setValue(UiState.error(throwable.getMessage())))
        );
    }

    public void resetCameraReady() {
        uiState.setValue(UiState.success(
                new EkycCaptureUiState(true, false, R.string.core_liveness_no_face)
        ));
    }

    private void handleSubmitResult(AppResult<String> result) {
        if (result.isSuccess()) {
            submitSuccessEvent.setValue(result.getData());
        } else {
            uiState.setValue(UiState.error(result.getMessage()));
        }
    }

    public LiveData<UiState<EkycCaptureUiState>> getUiState() {
        return uiState;
    }

    public SingleLiveEvent<String> getSubmitSuccessEvent() {
        return submitSuccessEvent;
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}
