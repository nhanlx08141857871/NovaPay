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
import com.novabank.novapay.domain.model.account.OpenSavingResult;
import com.novabank.novapay.domain.model.account.SavingCalculator;
import com.novabank.novapay.domain.model.rate.InterestRate;
import com.novabank.novapay.domain.model.security.SecurityStatus;
import com.novabank.novapay.domain.model.user.User;
import com.novabank.novapay.domain.usecase.account.OpenSavingAccountUseCase;
import com.novabank.novapay.domain.usecase.account.ObserveMyAccountsUseCase;
import com.novabank.novapay.domain.usecase.rate.ObserveInterestRatesUseCase;
import com.novabank.novapay.domain.usecase.security.GetSecurityStatusUseCase;
import com.novabank.novapay.domain.usecase.user.ObserveMyProfileUseCase;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

@HiltViewModel
public class OpenSavingViewModel extends ViewModel {

    private final ObserveMyAccountsUseCase observeMyAccountsUseCase;
    private final ObserveInterestRatesUseCase observeInterestRatesUseCase;
    private final ObserveMyProfileUseCase observeMyProfileUseCase;
    private final GetSecurityStatusUseCase getSecurityStatusUseCase;
    private final OpenSavingAccountUseCase openSavingAccountUseCase;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<UiState<OpenSavingUiState>> uiState =
            new MutableLiveData<>(UiState.loading());
    private final SingleLiveEvent<OpenSavingResult> openSuccessEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Integer> messageResEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> messageTextEvent = new SingleLiveEvent<>();

    private List<Account> checkingAccounts = Collections.emptyList();
    private List<InterestRate> termRates = Collections.emptyList();
    private String selectedAccountId;
    private int selectedTermMonths = -1;
    private long principalAmount = 5_000_000L;
    private boolean hasTransactionPin = false;
    private boolean pinRequiredOverride = false;
    private boolean submitting = false;

    @Inject
    public OpenSavingViewModel(
            ObserveMyAccountsUseCase observeMyAccountsUseCase,
            ObserveInterestRatesUseCase observeInterestRatesUseCase,
            ObserveMyProfileUseCase observeMyProfileUseCase,
            GetSecurityStatusUseCase getSecurityStatusUseCase,
            OpenSavingAccountUseCase openSavingAccountUseCase
    ) {
        this.observeMyAccountsUseCase = observeMyAccountsUseCase;
        this.observeInterestRatesUseCase = observeInterestRatesUseCase;
        this.observeMyProfileUseCase = observeMyProfileUseCase;
        this.getSecurityStatusUseCase = getSecurityStatusUseCase;
        this.openSavingAccountUseCase = openSavingAccountUseCase;
    }

    public void load() {
        disposables.clear();
        uiState.setValue(UiState.loading());

        disposables.add(
                Observable.combineLatest(
                                observeMyAccountsUseCase.execute(),
                                observeInterestRatesUseCase.execute(),
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
                                CombinedInput::new
                        )
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                this::handleCombined,
                                throwable -> uiState.setValue(UiState.error(throwable.getMessage()))
                        )
        );
    }

    public void selectCheckingAccount(@NonNull String accountId) {
        this.selectedAccountId = accountId;
        emitState();
    }

    public void selectTermMonths(int termMonths) {
        this.selectedTermMonths = termMonths;
        emitState();
    }

    public void updatePrincipalAmount(long amount) {
        this.principalAmount = Math.max(0L, amount);
        emitState();
    }

    public void submitOpenSaving(@Nullable String pin) {
        if (submitting) {
            return;
        }
        Account selectedAccount = findSelectedAccount();
        InterestRate selectedRate = findSelectedRate();
        if (selectedAccount == null) {
            messageResEvent.setValue(R.string.core_open_saving_no_checking);
            return;
        }
        if (selectedRate == null) {
            messageResEvent.setValue(R.string.core_open_saving_no_rates);
            return;
        }
        if (principalAmount > selectedAccount.getBalance()) {
            messageResEvent.setValue(R.string.core_open_saving_insufficient);
            return;
        }
        if (hasTransactionPin && (pin == null || !pin.trim().matches("^\\d{6}$"))) {
            messageResEvent.setValue(R.string.core_open_saving_pin_invalid);
            return;
        }

        submitting = true;
        emitState();

        disposables.add(
                openSavingAccountUseCase.execute(
                                selectedAccount.getAccountId(),
                                selectedRate.getTermMonths(),
                                principalAmount,
                                hasTransactionPin ? pin : null
                        )
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                            submitting = false;
                            if (result.isSuccess() && result.getData() != null) {
                                emitState();
                                openSuccessEvent.setValue(result.getData());
                            } else {
                                if (isPinErrorCode(result.getErrorCode())) {
                                    pinRequiredOverride = true;
                                    hasTransactionPin = true;
                                }
                                emitState();
                                String msg = result.getMessage();
                                if (msg != null && !msg.trim().isEmpty()) {
                                    messageTextEvent.setValue(msg);
                                } else {
                                    messageResEvent.setValue(R.string.core_error_network_generic);
                                }
                            }
                        }, throwable -> {
                            submitting = false;
                            emitState();
                            messageResEvent.setValue(R.string.core_error_network_generic);
                        })
        );
    }

    private void handleCombined(CombinedInput input) {
        if (input.accountsResult.isError()) {
            uiState.setValue(UiState.error(input.accountsResult.getMessage()));
            return;
        }
        if (input.ratesResult.isError()) {
            uiState.setValue(UiState.error(input.ratesResult.getMessage()));
            return;
        }

        List<Account> filteredChecking = new ArrayList<>();
        if (input.accountsResult.getData() != null) {
            for (Account account : input.accountsResult.getData()) {
                if (account != null && account.canBePaymentSource()) {
                    filteredChecking.add(account);
                }
            }
        }
        this.checkingAccounts = filteredChecking;

        List<InterestRate> positiveRates = new ArrayList<>();
        if (input.ratesResult.getData() != null) {
            for (InterestRate rate : input.ratesResult.getData()) {
                if (rate != null && rate.getTermMonths() > 0 && rate.getAnnualRate() > 0.0) {
                    positiveRates.add(rate);
                }
            }
        }
        positiveRates.sort(Comparator.comparingInt(InterestRate::getTermMonths));
        this.termRates = positiveRates;

        this.hasTransactionPin = resolveHasTransactionPin(
                input.securityResult,
                input.profileResult,
                pinRequiredOverride
        );

        if (this.checkingAccounts.isEmpty() || this.termRates.isEmpty()) {
            uiState.setValue(UiState.empty());
            return;
        }

        if (findSelectedAccount() == null) {
            this.selectedAccountId = this.checkingAccounts.get(0).getAccountId();
        }
        if (findSelectedRate() == null) {
            InterestRate defaultSixMonth = null;
            for (InterestRate rate : this.termRates) {
                if (rate.getTermMonths() == 6) {
                    defaultSixMonth = rate;
                    break;
                }
            }
            this.selectedTermMonths = defaultSixMonth != null
                    ? defaultSixMonth.getTermMonths()
                    : this.termRates.get(0).getTermMonths();
        }

        emitState();
    }

    private void emitState() {
        if (checkingAccounts.isEmpty() || termRates.isEmpty()) {
            return;
        }
        Account selectedAccount = findSelectedAccount();
        InterestRate selectedRate = findSelectedRate();
        double annualRate = selectedRate != null ? selectedRate.getAnnualRate() : 0.0;
        int termMonths = selectedRate != null ? selectedRate.getTermMonths() : 0;

        long monthlyInterest = SavingCalculator.calculateMonthlyProfit(principalAmount, annualRate);
        long totalMaturityInterest = SavingCalculator.calculateMaturityInterest(principalAmount, annualRate, termMonths);
        long totalMaturityPayout = principalAmount + totalMaturityInterest;
        Instant maturityDate = termMonths > 0
                ? SavingCalculator.estimateMaturityDate(Instant.now(), termMonths)
                : null;

        uiState.setValue(UiState.success(new OpenSavingUiState(
                checkingAccounts,
                termRates,
                selectedAccount,
                selectedRate,
                principalAmount,
                monthlyInterest,
                totalMaturityInterest,
                totalMaturityPayout,
                maturityDate,
                hasTransactionPin,
                submitting
        )));
    }

    @Nullable
    private Account findSelectedAccount() {
        if (selectedAccountId == null) {
            return null;
        }
        for (Account account : checkingAccounts) {
            if (selectedAccountId.equals(account.getAccountId())) {
                return account;
            }
        }
        return null;
    }

    @Nullable
    private InterestRate findSelectedRate() {
        for (InterestRate rate : termRates) {
            if (rate.getTermMonths() == selectedTermMonths) {
                return rate;
            }
        }
        return null;
    }

    public LiveData<UiState<OpenSavingUiState>> getUiState() {
        return uiState;
    }

    public SingleLiveEvent<OpenSavingResult> getOpenSuccessEvent() {
        return openSuccessEvent;
    }

    public SingleLiveEvent<Integer> getMessageResEvent() {
        return messageResEvent;
    }

    public SingleLiveEvent<String> getMessageTextEvent() {
        return messageTextEvent;
    }

    public static boolean resolveHasTransactionPin(
            @Nullable AppResult<SecurityStatus> securityResult,
            @Nullable AppResult<User> profileResult,
            boolean pinRequiredOverride
    ) {
        if (pinRequiredOverride) {
            return true;
        }
        if (securityResult != null && securityResult.isSuccess() && securityResult.getData() != null) {
            return securityResult.getData().hasTransactionPin();
        }
        if (profileResult != null && profileResult.isSuccess() && profileResult.getData() != null) {
            return profileResult.getData().hasTransactionPin();
        }
        return false;
    }

    public static boolean isPinErrorCode(@Nullable String errorCode) {
        if (errorCode == null) {
            return false;
        }
        return "PIN_REQUIRED".equalsIgnoreCase(errorCode)
                || "PIN_INVALID".equalsIgnoreCase(errorCode)
                || "PIN_LOCKED".equalsIgnoreCase(errorCode);
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }

    private static final class CombinedInput {
        final AppResult<List<Account>> accountsResult;
        final AppResult<List<InterestRate>> ratesResult;
        final AppResult<User> profileResult;
        final AppResult<SecurityStatus> securityResult;

        CombinedInput(
                AppResult<List<Account>> accountsResult,
                AppResult<List<InterestRate>> ratesResult,
                AppResult<User> profileResult,
                AppResult<SecurityStatus> securityResult
        ) {
            this.accountsResult = accountsResult;
            this.ratesResult = ratesResult;
            this.profileResult = profileResult;
            this.securityResult = securityResult;
        }
    }
}
