package com.novabank.novapay.presentation.customer.account;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.domain.model.account.SavingProjection;
import com.novabank.novapay.domain.usecase.account.ObserveSavingProjectionUseCase;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

@HiltViewModel
public class SavingDetailViewModel extends ViewModel {

    private final ObserveSavingProjectionUseCase observeSavingProjectionUseCase;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<UiState<SavingDetailUiState>> uiState =
            new MutableLiveData<>(UiState.loading());

    @Inject
    public SavingDetailViewModel(ObserveSavingProjectionUseCase observeSavingProjectionUseCase) {
        this.observeSavingProjectionUseCase = observeSavingProjectionUseCase;
    }

    public void observeSaving(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            uiState.setValue(UiState.empty());
            return;
        }
        disposables.clear();
        uiState.setValue(UiState.loading());
        disposables.add(
                observeSavingProjectionUseCase.execute(accountId)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::handleResult, throwable ->
                                uiState.setValue(UiState.error(throwable.getMessage())))
        );
    }

    private void handleResult(AppResult<SavingProjection> result) {
        if (result.isError()) {
            if ("NOT_FOUND".equals(result.getErrorCode())) {
                uiState.setValue(UiState.empty());
            } else {
                uiState.setValue(UiState.error(result.getMessage()));
            }
            return;
        }
        SavingProjection projection = result.getData();
        if (projection == null) {
            uiState.setValue(UiState.empty());
            return;
        }
        uiState.setValue(UiState.success(new SavingDetailUiState(projection)));
    }

    public LiveData<UiState<SavingDetailUiState>> getUiState() {
        return uiState;
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}
