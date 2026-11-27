package com.novabank.novapay.presentation.customer.transfer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novabank.novapay.core.common.SingleLiveEvent;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.bank.Bank;
import com.novabank.novapay.domain.model.bank.Beneficiary;
import com.novabank.novapay.domain.model.transaction.AccountLookupResult;
import com.novabank.novapay.domain.model.transaction.ExternalRecipient;
import com.novabank.novapay.domain.model.transaction.TransactionDraft;
import com.novabank.novapay.domain.model.transaction.TransactionInitiateRequest;
import com.novabank.novapay.domain.model.transaction.TransactionMeta;
import com.novabank.novapay.domain.model.transaction.TransactionType;
import com.novabank.novapay.domain.usecase.DeleteBeneficiaryUseCase;
import com.novabank.novapay.domain.usecase.GetActiveCheckingAccountsUseCase;
import com.novabank.novapay.domain.usecase.GetSupportedBanksUseCase;
import com.novabank.novapay.domain.usecase.InitiateTransactionUseCase;
import com.novabank.novapay.domain.usecase.LookupInternalAccountUseCase;
import com.novabank.novapay.domain.usecase.ObserveBeneficiariesUseCase;
import com.novabank.novapay.domain.usecase.SaveBeneficiaryUseCase;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class TransferViewModel extends ViewModel {

    private final GetActiveCheckingAccountsUseCase getActiveCheckingAccountsUseCase;
    private final GetSupportedBanksUseCase getSupportedBanksUseCase;
    private final ObserveBeneficiariesUseCase observeBeneficiariesUseCase;
    private final SaveBeneficiaryUseCase saveBeneficiaryUseCase;
    private final DeleteBeneficiaryUseCase deleteBeneficiaryUseCase;
    private final LookupInternalAccountUseCase lookupInternalAccountUseCase;
    private final InitiateTransactionUseCase initiateTransactionUseCase;

    private final CompositeDisposable disposables = new CompositeDisposable();
    private Disposable lookupDisposable;

    private final MutableLiveData<UiState<TransferUiState>> uiState = new MutableLiveData<>();
    private final SingleLiveEvent<TransactionDraft> draftCreatedEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> messageEvent = new SingleLiveEvent<>();

    private List<Account> checkingAccounts = new ArrayList<>();
    private int selectedSourceIndex = 0;
    private List<Bank> banks = new ArrayList<>();
    private int selectedBankIndex = 0;
    private List<Beneficiary> beneficiaries = new ArrayList<>();
    private boolean interbankMode = false;
    private boolean lookingUpRecipient = false;
    private AccountLookupResult lookupResult;
    private String lookupError;
    private Beneficiary pendingBeneficiaryToSave;

    @Inject
    public TransferViewModel(
            GetActiveCheckingAccountsUseCase getActiveCheckingAccountsUseCase,
            GetSupportedBanksUseCase getSupportedBanksUseCase,
            ObserveBeneficiariesUseCase observeBeneficiariesUseCase,
            SaveBeneficiaryUseCase saveBeneficiaryUseCase,
            DeleteBeneficiaryUseCase deleteBeneficiaryUseCase,
            LookupInternalAccountUseCase lookupInternalAccountUseCase,
            InitiateTransactionUseCase initiateTransactionUseCase
    ) {
        this.getActiveCheckingAccountsUseCase = getActiveCheckingAccountsUseCase;
        this.getSupportedBanksUseCase = getSupportedBanksUseCase;
        this.observeBeneficiariesUseCase = observeBeneficiariesUseCase;
        this.saveBeneficiaryUseCase = saveBeneficiaryUseCase;
        this.deleteBeneficiaryUseCase = deleteBeneficiaryUseCase;
        this.lookupInternalAccountUseCase = lookupInternalAccountUseCase;
        this.initiateTransactionUseCase = initiateTransactionUseCase;
    }

    public LiveData<UiState<TransferUiState>> getUiState() {
        return uiState;
    }

    public SingleLiveEvent<TransactionDraft> getDraftCreatedEvent() {
        return draftCreatedEvent;
    }

    public SingleLiveEvent<String> getMessageEvent() {
        return messageEvent;
    }

    public void loadInitialData() {
        disposables.clear();
        uiState.setValue(UiState.loading());

        disposables.add(
                getSupportedBanksUseCase.execute()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                            if (result.isSuccess() && result.getData() != null) {
                                banks = new ArrayList<>(result.getData());
                                if (selectedBankIndex >= banks.size()) {
                                    selectedBankIndex = 0;
                                }
                                publishCurrentStateIfReady();
                            }
                        }, ignored -> {
                        })
        );

        disposables.add(
                observeBeneficiariesUseCase.execute()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                            if (result.isSuccess() && result.getData() != null) {
                                beneficiaries = new ArrayList<>(result.getData());
                                publishCurrentStateIfReady();
                            }
                        }, ignored -> {
                        })
        );

        disposables.add(
                getActiveCheckingAccountsUseCase.observe()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                            if (result.isSuccess()) {
                                List<Account> list = result.getData();
                                if (list == null || list.isEmpty()) {
                                    checkingAccounts = new ArrayList<>();
                                    uiState.setValue(UiState.empty());
                                } else {
                                    checkingAccounts = new ArrayList<>(list);
                                    if (selectedSourceIndex >= checkingAccounts.size()) {
                                        selectedSourceIndex = 0;
                                    }
                                    publishSuccessState();
                                }
                            } else {
                                uiState.setValue(UiState.error(result.getMessage()));
                            }
                        }, throwable -> uiState.setValue(UiState.error(throwable.getMessage())))
        );
    }

    public void selectSourceAccount(int index) {
        if (index >= 0 && index < checkingAccounts.size()) {
            selectedSourceIndex = index;
            publishSuccessState();
        }
    }

    public void selectBank(int index) {
        if (index >= 0 && index < banks.size()) {
            selectedBankIndex = index;
            publishSuccessState();
        }
    }

    public void selectBankByCode(@Nullable String bankCode) {
        if (bankCode == null || bankCode.trim().isEmpty()) {
            return;
        }
        for (int i = 0; i < banks.size(); i++) {
            if (bankCode.trim().equalsIgnoreCase(banks.get(i).getBankCode())) {
                selectedBankIndex = i;
                publishSuccessState();
                return;
            }
        }
    }

    public void setInterbankMode(boolean interbank) {
        if (this.interbankMode != interbank) {
            this.interbankMode = interbank;
            if (interbank) {
                lookupResult = null;
                lookupError = null;
                lookingUpRecipient = false;
            }
            publishSuccessState();
        }
    }

    public void onRecipientAccountEdited(@Nullable String accountNumber) {
        String trimmed = accountNumber != null ? accountNumber.trim() : "";
        if (lookupResult != null && !trimmed.equals(lookupResult.getAccountNumber())) {
            lookupResult = null;
            lookupError = null;
            publishSuccessState();
        }
    }

    public void lookupInternalRecipient(@Nullable String accountNumber) {
        if (interbankMode) {
            return;
        }
        if (lookupDisposable != null && !lookupDisposable.isDisposed()) {
            lookupDisposable.dispose();
        }
        lookingUpRecipient = true;
        lookupResult = null;
        lookupError = null;
        publishSuccessState();

        lookupDisposable = lookupInternalAccountUseCase.execute(accountNumber)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    lookingUpRecipient = false;
                    if (result.isSuccess() && result.getData() != null) {
                        lookupResult = result.getData();
                        lookupError = null;
                    } else {
                        lookupResult = null;
                        lookupError = result.getMessage();
                    }
                    publishSuccessState();
                }, throwable -> {
                    lookingUpRecipient = false;
                    lookupResult = null;
                    lookupError = throwable.getMessage();
                    publishSuccessState();
                });
        disposables.add(lookupDisposable);
    }

    public void deleteBeneficiary(@NonNull String beneficiaryId) {
        disposables.add(
                deleteBeneficiaryUseCase.execute(beneficiaryId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                            if (!result.isSuccess()) {
                                messageEvent.setValue(result.getMessage());
                            }
                        }, throwable -> messageEvent.setValue(throwable.getMessage()))
        );
    }

    public void initiateTransfer(
            @Nullable String recipientAccountNumber,
            @Nullable String interbankRecipientName,
            long amountVnd,
            @Nullable String note,
            boolean saveToBeneficiaries,
            @Nullable String alias,
            @NonNull String missingLookupWarning
    ) {
        if (checkingAccounts.isEmpty()) {
            uiState.setValue(UiState.empty());
            return;
        }
        Account sourceAccount = checkingAccounts.get(selectedSourceIndex);
        String cleanAccount = recipientAccountNumber != null ? recipientAccountNumber.trim() : "";

        TransactionInitiateRequest request;
        if (!interbankMode) {
            if (lookupResult == null || !cleanAccount.equals(lookupResult.getAccountNumber())) {
                messageEvent.setValue(missingLookupWarning);
                return;
            }
            request = new TransactionInitiateRequest(
                    TransactionType.TRANSFER_INTERNAL,
                    sourceAccount.getAccountId(),
                    lookupResult.getAccountId(),
                    null,
                    amountVnd,
                    note,
                    TransactionMeta.empty()
            );
            pendingBeneficiaryToSave = saveToBeneficiaries
                    ? new Beneficiary("", lookupResult.getOwnerName(), lookupResult.getAccountNumber(), "NOVAPAY", alias)
                    : null;
        } else {
            Bank bank = !banks.isEmpty() && selectedBankIndex < banks.size()
                    ? banks.get(selectedBankIndex)
                    : null;
            String bankCode = bank != null ? bank.getBankCode() : "";
            String cleanName = interbankRecipientName != null ? interbankRecipientName.trim() : "";
            ExternalRecipient ext = new ExternalRecipient(bankCode, cleanAccount, cleanName);
            request = new TransactionInitiateRequest(
                    TransactionType.TRANSFER_INTERBANK,
                    sourceAccount.getAccountId(),
                    null,
                    ext,
                    amountVnd,
                    note,
                    TransactionMeta.empty()
            );
            pendingBeneficiaryToSave = saveToBeneficiaries
                    ? new Beneficiary("", cleanName, cleanAccount, bankCode, alias)
                    : null;
        }

        uiState.setValue(UiState.loading());
        disposables.add(
                initiateTransactionUseCase.execute(request, sourceAccount)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                            publishSuccessState();
                            if (result.isSuccess() && result.getData() != null) {
                                draftCreatedEvent.setValue(result.getData());
                            } else {
                                messageEvent.setValue(result.getMessage());
                            }
                        }, throwable -> {
                            publishSuccessState();
                            messageEvent.setValue(throwable.getMessage());
                        })
        );
    }

    public void persistPendingBeneficiaryIfNeeded() {
        Beneficiary toSave = pendingBeneficiaryToSave;
        pendingBeneficiaryToSave = null;
        if (toSave == null) {
            return;
        }
        disposables.add(
                saveBeneficiaryUseCase.execute(toSave)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(ignored -> {
                        }, ignored -> {
                        })
        );
    }

    private void publishCurrentStateIfReady() {
        if (!checkingAccounts.isEmpty()) {
            publishSuccessState();
        }
    }

    private void publishSuccessState() {
        if (checkingAccounts.isEmpty()) {
            return;
        }
        uiState.setValue(UiState.success(new TransferUiState(
                checkingAccounts,
                selectedSourceIndex,
                banks,
                selectedBankIndex,
                beneficiaries,
                interbankMode,
                lookingUpRecipient,
                lookupResult,
                lookupError
        )));
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}
