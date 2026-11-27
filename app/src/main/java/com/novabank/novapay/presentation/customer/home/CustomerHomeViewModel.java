package com.novabank.novapay.presentation.customer.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.transaction.MonthlyCashflowSummary;
import com.novabank.novapay.domain.model.user.User;
import com.novabank.novapay.domain.usecase.account.ObserveMyAccountsUseCase;
import com.novabank.novapay.domain.usecase.auth.SignOutUseCase;
import com.novabank.novapay.domain.usecase.notification.ObserveUnreadNotificationCountUseCase;
import com.novabank.novapay.domain.usecase.transaction.ObserveMonthlyCashflowUseCase;
import com.novabank.novapay.domain.usecase.user.ObserveMyProfileUseCase;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

@HiltViewModel
public class CustomerHomeViewModel extends ViewModel {

    private final ObserveMyProfileUseCase observeMyProfileUseCase;
    private final ObserveMyAccountsUseCase observeMyAccountsUseCase;
    private final ObserveMonthlyCashflowUseCase observeMonthlyCashflowUseCase;
    private final ObserveUnreadNotificationCountUseCase observeUnreadNotificationCountUseCase;
    private final SignOutUseCase signOutUseCase;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<UiState<CustomerHomeUiState>> uiState =
            new MutableLiveData<>(UiState.loading());

    private boolean balanceHidden = false;
    private User lastUser;
    private List<Account> lastAccounts;
    private MonthlyCashflowSummary lastCashflow;
    private int lastUnreadCount = 0;

    @Inject
    public CustomerHomeViewModel(
            ObserveMyProfileUseCase observeMyProfileUseCase,
            ObserveMyAccountsUseCase observeMyAccountsUseCase,
            ObserveMonthlyCashflowUseCase observeMonthlyCashflowUseCase,
            ObserveUnreadNotificationCountUseCase observeUnreadNotificationCountUseCase,
            SignOutUseCase signOutUseCase
    ) {
        this.observeMyProfileUseCase = observeMyProfileUseCase;
        this.observeMyAccountsUseCase = observeMyAccountsUseCase;
        this.observeMonthlyCashflowUseCase = observeMonthlyCashflowUseCase;
        this.observeUnreadNotificationCountUseCase = observeUnreadNotificationCountUseCase;
        this.signOutUseCase = signOutUseCase;
    }

    public void loadHome() {
        disposables.clear();
        uiState.setValue(UiState.loading());

        disposables.add(
                Observable.combineLatest(
                                observeMyProfileUseCase.execute(),
                                observeMyAccountsUseCase.execute(),
                                observeMonthlyCashflowUseCase.execute(),
                                observeUnreadNotificationCountUseCase.execute()
                                        .onErrorReturnItem(AppResult.success(0)),
                                CombinedResult::new
                        )
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                this::renderCombined,
                                throwable -> uiState.setValue(UiState.error(throwable.getMessage()))
                        )
        );
    }

    public void toggleBalanceVisibility() {
        balanceHidden = !balanceHidden;
        if (lastAccounts != null) {
            uiState.setValue(UiState.success(new CustomerHomeUiState(
                    lastUser,
                    lastAccounts,
                    lastCashflow,
                    balanceHidden,
                    lastUnreadCount
            )));
        }
    }

    public void signOut() {
        signOutUseCase.execute();
    }

    public LiveData<UiState<CustomerHomeUiState>> getUiState() {
        return uiState;
    }

    private void renderCombined(CombinedResult combined) {
        if (combined.accountsResult.isError()) {
            uiState.setValue(UiState.error(combined.accountsResult.getMessage()));
            return;
        }
        List<Account> accounts = combined.accountsResult.getData();
        if (accounts == null || accounts.isEmpty()) {
            uiState.setValue(UiState.empty());
            return;
        }
        lastUser = combined.userResult.isSuccess() ? combined.userResult.getData() : null;
        lastAccounts = accounts;
        lastCashflow = combined.cashflowResult.isSuccess() ? combined.cashflowResult.getData() : null;
        lastUnreadCount = (combined.unreadResult.isSuccess() && combined.unreadResult.getData() != null)
                ? combined.unreadResult.getData()
                : 0;

        uiState.setValue(UiState.success(new CustomerHomeUiState(
                lastUser,
                lastAccounts,
                lastCashflow,
                balanceHidden,
                lastUnreadCount
        )));
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }

    private static final class CombinedResult {
        final AppResult<User> userResult;
        final AppResult<List<Account>> accountsResult;
        final AppResult<MonthlyCashflowSummary> cashflowResult;
        final AppResult<Integer> unreadResult;

        CombinedResult(
                AppResult<User> userResult,
                AppResult<List<Account>> accountsResult,
                AppResult<MonthlyCashflowSummary> cashflowResult,
                AppResult<Integer> unreadResult
        ) {
            this.userResult = userResult;
            this.accountsResult = accountsResult;
            this.cashflowResult = cashflowResult;
            this.unreadResult = unreadResult;
        }
    }
}
