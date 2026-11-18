package com.novabank.novapay.presentation.customer.account;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.core.common.SingleLiveEvent;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.usecase.account.ObserveAccountDetailUseCase;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

@HiltViewModel
public class AccountRouterViewModel extends ViewModel {

    private final ObserveAccountDetailUseCase observeAccountDetailUseCase;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<UiState<AccountRouterUiState>> uiState =
            new MutableLiveData<>(UiState.loading());
    private final SingleLiveEvent<AccountRouterUiState> routeEvent = new SingleLiveEvent<>();

    @Inject
    public AccountRouterViewModel(ObserveAccountDetailUseCase observeAccountDetailUseCase) {
        this.observeAccountDetailUseCase = observeAccountDetailUseCase;
    }

    public void resolveAccount(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            uiState.setValue(UiState.empty());
            return;
        }
        disposables.clear();
        uiState.setValue(UiState.loading());
        disposables.add(
                observeAccountDetailUseCase.getOnce(accountId.trim())
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
        AccountRouterUiState target = new AccountRouterUiState(account.getAccountId(), account.getType());
        uiState.setValue(UiState.success(target));
        routeEvent.setValue(target);
    }

    public LiveData<UiState<AccountRouterUiState>> getUiState() {
        return uiState;
    }

    public SingleLiveEvent<AccountRouterUiState> getRouteEvent() {
        return routeEvent;
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}
