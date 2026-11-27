package com.novabank.novapay.presentation.customer.account;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novabank.novapay.R;
import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.core.common.SingleLiveEvent;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.account.SavingCalculator;
import com.novabank.novapay.domain.model.account.SavingProjection;
import com.novabank.novapay.domain.model.account.SettleSavingResult;
import com.novabank.novapay.domain.model.rate.InterestRate;
import com.novabank.novapay.domain.model.security.SecurityStatus;
import com.novabank.novapay.domain.model.user.User;
import com.novabank.novapay.domain.usecase.account.ObserveMyAccountsUseCase;
import com.novabank.novapay.domain.usecase.account.ObserveSavingProjectionUseCase;
import com.novabank.novapay.domain.usecase.account.SettleSavingAccountUseCase;
import com.novabank.novapay.domain.usecase.rate.ObserveInterestRatesUseCase;
import com.novabank.novapay.domain.usecase.security.GetSecurityStatusUseCase;
import com.novabank.novapay.domain.usecase.user.ObserveMyProfileUseCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

@HiltViewModel
public class SavingDetailViewModel extends ViewModel {

    private final ObserveSavingProjectionUseCase observeSavingProjectionUseCase;
    private final ObserveMyAccountsUseCase observeMyAccountsUseCase;
    private final ObserveInterestRatesUseCase observeInterestRatesUseCase;
    private final ObserveMyProfileUseCase observeMyProfileUseCase;
    private final GetSecurityStatusUseCase getSecurityStatusUseCase;
    private final SettleSavingAccountUseCase settleSavingAccountUseCase;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<UiState<SavingDetailUiState>> uiState =
            new MutableLiveData<>(UiState.loading());
    private final SingleLiveEvent<SettleSavingResult> settleSuccessEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Integer> messageResEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> messageTextEvent = new SingleLiveEvent<>();

    private SavingProjection lastProjection;
    private List<Account> checkingAccounts = Collections.emptyList();
    private double demandAnnualRate = SavingCalculator.DEFAULT_DEMAND_ANNUAL_RATE;
    private boolean hasTransactionPin = false;
    private boolean pinRequiredOverride = false;
    private boolean settling = false;

    @Inject
    public SavingDetailViewModel(
            ObserveSavingProjectionUseCase observeSavingProjectionUseCase,
            ObserveMyAccountsUseCase observeMyAccountsUseCase,
            ObserveInterestRatesUseCase observeInterestRatesUseCase,
            ObserveMyProfileUseCase observeMyProfileUseCase,
            GetSecurityStatusUseCase getSecurityStatusUseCase,
            SettleSavingAccountUseCase settleSavingAccountUseCase
    ) {
        this.observeSavingProjectionUseCase = observeSavingProjectionUseCase;
        this.observeMyAccountsUseCase = observeMyAccountsUseCase;
        this.observeInterestRatesUseCase = observeInterestRatesUseCase;
        this.observeMyProfileUseCase = observeMyProfileUseCase;
        this.getSecurityStatusUseCase = getSecurityStatusUseCase;
        this.settleSavingAccountUseCase = settleSavingAccountUseCase;
    }

    public void observeSaving(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            uiState.setValue(UiState.empty());
            return;
        }
        disposables.clear();
        uiState.setValue(UiState.loading());
        disposables.add(
                Observable.combineLatest(
                                observeSavingProjectionUseCase.execute(accountId),
                                observeMyAccountsUseCase.execute()
                                        .onErrorReturnItem(AppResult.success(Collections.emptyList())),
                                observeInterestRatesUseCase.execute()
                                        .onErrorReturnItem(AppResult.success(Collections.emptyList())),
                                observeMyProfileUseCase.execute()
                                        .onErrorReturn(throwable -> AppResult.error(
                                                "PROFILE_UNAVAILABLE",
                                                throwable.getMessage()
                                        )),
                                getSecurityStatusUseCase.execute()
                                        .onErrorReturn(throwable -> AppResult.error(
                                                "SECURITY_STATUS_UNAVAILABLE",
                                                throwable.getMessage()
                                        ))
                                        .toObservable(),
                                CombinedResult::new
                        )
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::handleResult, throwable ->
                                uiState.setValue(UiState.error(throwable.getMessage())))
        );
    }

    public void settleSaving(
            @NonNull String savingAccountId,
            @NonNull String toCheckingAccountId,
            @Nullable String pin
    ) {
        if (settling) {
            return;
        }
        settling = true;
        emitCurrentState();

        disposables.add(
                settleSavingAccountUseCase.execute(savingAccountId, toCheckingAccountId, pin)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                            settling = false;
                            if (result.isSuccess() && result.getData() != null) {
                                emitCurrentState();
                                settleSuccessEvent.setValue(result.getData());
                            } else {
                                if (OpenSavingViewModel.isPinErrorCode(result.getErrorCode())) {
                                    pinRequiredOverride = true;
                                    hasTransactionPin = true;
                                }
                                emitCurrentState();
                                String msg = result.getMessage();
                                if (msg != null && !msg.trim().isEmpty()) {
                                    messageTextEvent.setValue(msg);
                                } else {
                                    messageResEvent.setValue(R.string.core_error_network_generic);
                                }
                            }
                        }, throwable -> {
                            settling = false;
                            emitCurrentState();
                            messageResEvent.setValue(R.string.core_error_network_generic);
                        })
        );
    }

    private void handleResult(CombinedResult combined) {
        AppResult<SavingProjection> result = combined.projectionResult;
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

        lastProjection = projection;

        List<Account> filteredChecking = new ArrayList<>();
        if (combined.accountsResult.isSuccess() && combined.accountsResult.getData() != null) {
            for (Account account : combined.accountsResult.getData()) {
                if (account != null && account.canBePaymentSource()) {
                    filteredChecking.add(account);
                }
            }
        }
        checkingAccounts = filteredChecking;

        double foundDemandRate = SavingCalculator.DEFAULT_DEMAND_ANNUAL_RATE;
        if (combined.ratesResult.isSuccess() && combined.ratesResult.getData() != null) {
            for (InterestRate rate : combined.ratesResult.getData()) {
                if (rate != null && rate.getTermMonths() == 0 && rate.getAnnualRate() > 0.0) {
                    foundDemandRate = rate.getAnnualRate();
                    break;
                }
            }
        }
        demandAnnualRate = foundDemandRate;

        hasTransactionPin = OpenSavingViewModel.resolveHasTransactionPin(
                combined.securityResult,
                combined.profileResult,
                pinRequiredOverride
        );

        emitCurrentState();
    }

    private void emitCurrentState() {
        if (lastProjection == null) {
            return;
        }
        uiState.setValue(UiState.success(new SavingDetailUiState(
                lastProjection,
                checkingAccounts,
                demandAnnualRate,
                hasTransactionPin,
                settling
        )));
    }

    public LiveData<UiState<SavingDetailUiState>> getUiState() {
        return uiState;
    }

    public SingleLiveEvent<SettleSavingResult> getSettleSuccessEvent() {
        return settleSuccessEvent;
    }

    public SingleLiveEvent<Integer> getMessageResEvent() {
        return messageResEvent;
    }

    public SingleLiveEvent<String> getMessageTextEvent() {
        return messageTextEvent;
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }

    private static final class CombinedResult {
        final AppResult<SavingProjection> projectionResult;
        final AppResult<List<Account>> accountsResult;
        final AppResult<List<InterestRate>> ratesResult;
        final AppResult<User> profileResult;
        final AppResult<SecurityStatus> securityResult;

        CombinedResult(
                AppResult<SavingProjection> projectionResult,
                AppResult<List<Account>> accountsResult,
                AppResult<List<InterestRate>> ratesResult,
                AppResult<User> profileResult,
                AppResult<SecurityStatus> securityResult
        ) {
            this.projectionResult = projectionResult;
            this.accountsResult = accountsResult;
            this.ratesResult = ratesResult;
            this.profileResult = profileResult;
            this.securityResult = securityResult;
        }
    }
}
