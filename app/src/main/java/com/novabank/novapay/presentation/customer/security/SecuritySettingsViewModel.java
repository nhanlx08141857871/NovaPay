package com.novabank.novapay.presentation.customer.security;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novabank.novapay.R;
import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.core.common.SingleLiveEvent;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.core.security.BiometricQuickLoginStore;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.account.AccountStatus;
import com.novabank.novapay.domain.model.account.AccountType;
import com.novabank.novapay.domain.model.security.SecurityStatus;
import com.novabank.novapay.domain.model.user.User;
import com.novabank.novapay.domain.usecase.account.ObserveMyAccountsUseCase;
import com.novabank.novapay.domain.usecase.security.ChangePasswordUseCase;
import com.novabank.novapay.domain.usecase.security.GetSecurityStatusUseCase;
import com.novabank.novapay.domain.usecase.security.SetTransactionPinUseCase;
import com.novabank.novapay.domain.usecase.security.UpdateCheckingControlsUseCase;
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
public class SecuritySettingsViewModel extends ViewModel {

    private final ObserveMyProfileUseCase observeMyProfileUseCase;
    private final ObserveMyAccountsUseCase observeMyAccountsUseCase;
    private final GetSecurityStatusUseCase getSecurityStatusUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final SetTransactionPinUseCase setTransactionPinUseCase;
    private final UpdateCheckingControlsUseCase updateCheckingControlsUseCase;
    private final BiometricQuickLoginStore biometricQuickLoginStore;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<UiState<SecuritySettingsUiState>> uiState =
            new MutableLiveData<>(UiState.loading());
    private final SingleLiveEvent<Integer> messageResEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> messageTextEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Boolean> clearPasswordFieldsEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Boolean> clearPinFieldsEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Boolean> clearControlsPinEvent = new SingleLiveEvent<>();

    private User lastUser;
    private SecurityStatus lastSecurityStatus = new SecurityStatus(false, null);
    private boolean securityStatusLoaded = false;
    private boolean pinRequiredFallback = false;
    private List<Account> checkingAccounts = Collections.emptyList();
    private String selectedCheckingAccountId;
    private boolean busy = false;

    @Inject
    public SecuritySettingsViewModel(
            ObserveMyProfileUseCase observeMyProfileUseCase,
            ObserveMyAccountsUseCase observeMyAccountsUseCase,
            GetSecurityStatusUseCase getSecurityStatusUseCase,
            ChangePasswordUseCase changePasswordUseCase,
            SetTransactionPinUseCase setTransactionPinUseCase,
            UpdateCheckingControlsUseCase updateCheckingControlsUseCase,
            BiometricQuickLoginStore biometricQuickLoginStore
    ) {
        this.observeMyProfileUseCase = observeMyProfileUseCase;
        this.observeMyAccountsUseCase = observeMyAccountsUseCase;
        this.getSecurityStatusUseCase = getSecurityStatusUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
        this.setTransactionPinUseCase = setTransactionPinUseCase;
        this.updateCheckingControlsUseCase = updateCheckingControlsUseCase;
        this.biometricQuickLoginStore = biometricQuickLoginStore;
    }

    public void load() {
        disposables.clear();
        uiState.setValue(UiState.loading());

        disposables.add(
                Observable.combineLatest(
                                observeMyProfileUseCase.execute(),
                                observeMyAccountsUseCase.execute(),
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
        this.selectedCheckingAccountId = accountId;
        emitCurrentState();
    }

    public void changePassword(
            @NonNull String currentPassword,
            @NonNull String newPassword,
            @NonNull String confirmPassword
    ) {
        if (busy) {
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            messageResEvent.setValue(R.string.core_security_password_mismatch);
            return;
        }
        busy = true;
        emitCurrentState();

        disposables.add(
                changePasswordUseCase.execute(currentPassword, newPassword)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                            busy = false;
                            emitCurrentState();
                            if (result.isSuccess()) {
                                if (lastUser != null && biometricQuickLoginStore.isEnabledForEmail(lastUser.getEmail())) {
                                    biometricQuickLoginStore.saveCredentials(lastUser.getEmail(), newPassword);
                                }
                                clearPasswordFieldsEvent.setValue(Boolean.TRUE);
                                messageResEvent.setValue(R.string.core_security_password_success);
                            } else {
                                dispatchError(result);
                            }
                        }, throwable -> {
                            busy = false;
                            emitCurrentState();
                            messageResEvent.setValue(R.string.core_error_network_generic);
                        })
        );
    }

    public void saveTransactionPin(
            @Nullable String currentPin,
            @NonNull String newPin,
            @NonNull String confirmPin
    ) {
        if (busy) {
            return;
        }
        if (!newPin.trim().equals(confirmPin.trim())) {
            messageResEvent.setValue(R.string.core_security_pin_mismatch);
            return;
        }
        busy = true;
        emitCurrentState();

        boolean hasPin = lastSecurityStatus != null && lastSecurityStatus.hasTransactionPin();
        disposables.add(
                setTransactionPinUseCase.execute(hasPin, currentPin, newPin)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                            busy = false;
                            if (result.isSuccess() && result.getData() != null) {
                                securityStatusLoaded = true;
                                pinRequiredFallback = false;
                                lastSecurityStatus = result.getData();
                                clearPinFieldsEvent.setValue(Boolean.TRUE);
                                emitCurrentState();
                                messageResEvent.setValue(R.string.core_security_pin_success);
                            } else {
                                if (isPinErrorCode(result.getErrorCode())) {
                                    pinRequiredFallback = true;
                                    lastSecurityStatus = new SecurityStatus(
                                            true,
                                            lastSecurityStatus != null ? lastSecurityStatus.getPinLockedUntil() : null
                                    );
                                }
                                emitCurrentState();
                                dispatchError(result);
                            }
                        }, throwable -> {
                            busy = false;
                            emitCurrentState();
                            messageResEvent.setValue(R.string.core_error_network_generic);
                        })
        );
    }

    public void updateCheckingControls(
            @NonNull String accountId,
            boolean frozen,
            long perTxnLimit,
            long dailyLimit,
            @Nullable String pin
    ) {
        if (busy) {
            return;
        }
        Account current = findCheckingById(accountId);
        UpdateCheckingControlsUseCase.ControlsPatch patch = UpdateCheckingControlsUseCase.buildPatch(
                current,
                frozen,
                perTxnLimit,
                dailyLimit
        );
        if (!patch.hasChanges()) {
            messageResEvent.setValue(R.string.core_security_controls_no_changes);
            return;
        }
        busy = true;
        emitCurrentState();

        disposables.add(
                updateCheckingControlsUseCase.execute(
                                accountId,
                                patch.getStatusOrNull(),
                                patch.getPerTxnLimitOrNull(),
                                patch.getDailyLimitOrNull(),
                                pin
                        )
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                            busy = false;
                            if (result.isSuccess()) {
                                clearControlsPinEvent.setValue(Boolean.TRUE);
                                emitCurrentState();
                                messageResEvent.setValue(R.string.core_security_controls_success);
                            } else {
                                if (isPinErrorCode(result.getErrorCode())) {
                                    pinRequiredFallback = true;
                                    lastSecurityStatus = new SecurityStatus(
                                            true,
                                            lastSecurityStatus != null ? lastSecurityStatus.getPinLockedUntil() : null
                                    );
                                }
                                emitCurrentState();
                                dispatchError(result);
                            }
                        }, throwable -> {
                            busy = false;
                            emitCurrentState();
                            messageResEvent.setValue(R.string.core_error_network_generic);
                        })
        );
    }

    private void handleCombined(CombinedInput input) {
        if (input.profileResult.isError()) {
            uiState.setValue(UiState.error(input.profileResult.getMessage()));
            return;
        }
        User user = input.profileResult.getData();
        if (user == null) {
            uiState.setValue(UiState.empty());
            return;
        }
        lastUser = user;
        if (input.securityResult.isSuccess() && input.securityResult.getData() != null) {
            securityStatusLoaded = true;
            SecurityStatus fetched = input.securityResult.getData();
            lastSecurityStatus = new SecurityStatus(
                    fetched.hasTransactionPin() || pinRequiredFallback,
                    fetched.getPinLockedUntil()
            );
        } else {
            securityStatusLoaded = false;
            lastSecurityStatus = new SecurityStatus(
                    user.hasTransactionPin() || pinRequiredFallback,
                    null
            );
        }

        List<Account> checkings = new ArrayList<>();
        if (input.accountsResult.isSuccess() && input.accountsResult.getData() != null) {
            for (Account account : input.accountsResult.getData()) {
                if (account != null
                        && account.getType() == AccountType.CHECKING
                        && account.getStatus() != AccountStatus.CLOSED) {
                    checkings.add(account);
                }
            }
        }
        checkingAccounts = checkings;
        if (findSelectedChecking() == null && !checkingAccounts.isEmpty()) {
            selectedCheckingAccountId = checkingAccounts.get(0).getAccountId();
        }
        emitCurrentState();
    }

    private void emitCurrentState() {
        if (lastUser == null) {
            return;
        }
        uiState.setValue(UiState.success(new SecuritySettingsUiState(
                lastUser,
                lastSecurityStatus,
                checkingAccounts,
                findSelectedChecking(),
                securityStatusLoaded,
                busy
        )));
    }

    @Nullable
    private Account findSelectedChecking() {
        return findCheckingById(selectedCheckingAccountId);
    }

    @Nullable
    private Account findCheckingById(@Nullable String accountId) {
        if (accountId == null) {
            return null;
        }
        for (Account account : checkingAccounts) {
            if (accountId.equals(account.getAccountId())) {
                return account;
            }
        }
        return null;
    }

    public static boolean isPinErrorCode(@Nullable String errorCode) {
        if (errorCode == null) {
            return false;
        }
        return "PIN_REQUIRED".equalsIgnoreCase(errorCode)
                || "PIN_INVALID".equalsIgnoreCase(errorCode)
                || "PIN_LOCKED".equalsIgnoreCase(errorCode);
    }

    private void dispatchError(AppResult<?> result) {
        String msg = result.getMessage();
        if (msg != null && !msg.trim().isEmpty()) {
            messageTextEvent.setValue(msg);
        } else {
            messageResEvent.setValue(R.string.core_error_network_generic);
        }
    }

    public LiveData<UiState<SecuritySettingsUiState>> getUiState() {
        return uiState;
    }

    public SingleLiveEvent<Integer> getMessageResEvent() {
        return messageResEvent;
    }

    public SingleLiveEvent<String> getMessageTextEvent() {
        return messageTextEvent;
    }

    public SingleLiveEvent<Boolean> getClearPasswordFieldsEvent() {
        return clearPasswordFieldsEvent;
    }

    public SingleLiveEvent<Boolean> getClearPinFieldsEvent() {
        return clearPinFieldsEvent;
    }

    public SingleLiveEvent<Boolean> getClearControlsPinEvent() {
        return clearControlsPinEvent;
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }

    private static final class CombinedInput {
        final AppResult<User> profileResult;
        final AppResult<List<Account>> accountsResult;
        final AppResult<SecurityStatus> securityResult;

        CombinedInput(
                AppResult<User> profileResult,
                AppResult<List<Account>> accountsResult,
                AppResult<SecurityStatus> securityResult
        ) {
            this.profileResult = profileResult;
            this.accountsResult = accountsResult;
            this.securityResult = securityResult;
        }
    }
}
