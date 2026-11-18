package com.novabank.novapay.presentation.customer.account;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.usecase.account.ObserveAccountDetailUseCase;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

@HiltViewModel
public class MortgageDetailViewModel extends ViewModel {

    private final ObserveAccountDetailUseCase observeAccountDetailUseCase;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<UiState<MortgageDetailUiState>> uiState =
            new MutableLiveData<>(UiState.loading());

    @Inject
    public MortgageDetailViewModel(ObserveAccountDetailUseCase observeAccountDetailUseCase) {
        this.observeAccountDetailUseCase = observeAccountDetailUseCase;
    }

    public void observeMortgage(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            uiState.setValue(UiState.empty());
            return;
        }
        disposables.clear();
        uiState.setValue(UiState.loading());
        disposables.add(
                observeAccountDetailUseCase.observe(accountId)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::handleResult, throwable ->
                                uiState.setValue(UiState.error(throwable.getMessage())))
        );
    }

    private void handleResult(AppResult<Account> result) {
        if (result.isError()) {
            if ("NOT_FOUND".equals(result.getErrorCode())) {
                uiState.setValue(UiState.empty());
            } else {
                uiState.setValue(UiState.error(result.getMessage()));
            }
            return;
        }
        Account account = result.getData();
        if (account == null) {
            uiState.setValue(UiState.empty());
            return;
        }
        uiState.setValue(UiState.success(new MortgageDetailUiState(account)));
    }

    public LiveData<UiState<MortgageDetailUiState>> getUiState() {
        return uiState;
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}
