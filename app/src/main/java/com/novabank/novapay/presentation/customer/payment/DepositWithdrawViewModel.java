package com.novabank.novapay.presentation.customer.payment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novabank.novapay.core.common.SingleLiveEvent;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.transaction.PaymentFinalizeResult;
import com.novabank.novapay.domain.model.transaction.TransactionDraft;
import com.novabank.novapay.domain.model.transaction.TransactionInitiateRequest;
import com.novabank.novapay.domain.model.transaction.TransactionMeta;
import com.novabank.novapay.domain.model.transaction.TransactionType;
import com.novabank.novapay.domain.usecase.FinalizeStripePaymentUseCase;
import com.novabank.novapay.domain.usecase.GetActiveCheckingAccountsUseCase;
import com.novabank.novapay.domain.usecase.GetMortgageAccountUseCase;
import com.novabank.novapay.domain.usecase.InitiateTransactionUseCase;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * ViewModel điều phối Nạp tiền Stripe (GATEWAY -> PaymentSheet -> /payments/finalize),
 * Rút tiền (WITHDRAW) và Trả kỳ hạn khoản vay thế chấp (MORTGAGE_PAYMENT).
 */
@HiltViewModel
public class DepositWithdrawViewModel extends ViewModel {

    private final GetActiveCheckingAccountsUseCase getActiveCheckingAccountsUseCase;
    private final GetMortgageAccountUseCase getMortgageAccountUseCase;
    private final InitiateTransactionUseCase initiateTransactionUseCase;
    private final FinalizeStripePaymentUseCase finalizeStripePaymentUseCase;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<UiState<DepositWithdrawUiState>> uiState = new MutableLiveData<>();
    private final SingleLiveEvent<TransactionDraft> draftCreatedEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> navigateReceiptEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> messageEvent = new SingleLiveEvent<>();

    private DepositWithdrawUiState.Mode mode = DepositWithdrawUiState.Mode.GATEWAY_DEPOSIT;
    private List<Account> checkingAccounts = new ArrayList<>();
    private int selectedCheckingIndex = 0;
    private List<Account> mortgageAccounts = new ArrayList<>();
    private int selectedMortgageIndex = 0;
    private String presetMortgageAccountId;

    @Inject
    public DepositWithdrawViewModel(
            GetActiveCheckingAccountsUseCase getActiveCheckingAccountsUseCase,
            GetMortgageAccountUseCase getMortgageAccountUseCase,
            InitiateTransactionUseCase initiateTransactionUseCase,
            FinalizeStripePaymentUseCase finalizeStripePaymentUseCase
    ) {
        this.getActiveCheckingAccountsUseCase = getActiveCheckingAccountsUseCase;
        this.getMortgageAccountUseCase = getMortgageAccountUseCase;
        this.initiateTransactionUseCase = initiateTransactionUseCase;
        this.finalizeStripePaymentUseCase = finalizeStripePaymentUseCase;
    }

    public LiveData<UiState<DepositWithdrawUiState>> getUiState() {
        return uiState;
    }

    public SingleLiveEvent<TransactionDraft> getDraftCreatedEvent() {
        return draftCreatedEvent;
    }

    public SingleLiveEvent<String> getNavigateReceiptEvent() {
        return navigateReceiptEvent;
    }

    public SingleLiveEvent<String> getMessageEvent() {
        return messageEvent;
    }

    public void applyInitialArgs(@Nullable String modeArg, @Nullable String mortgageAccountIdArg) {
        if ("WITHDRAW".equalsIgnoreCase(modeArg)) {
            this.mode = DepositWithdrawUiState.Mode.WITHDRAW;
        } else if ("MORTGAGE_PAYMENT".equalsIgnoreCase(modeArg) || "MORTGAGE".equalsIgnoreCase(modeArg)) {
            this.mode = DepositWithdrawUiState.Mode.MORTGAGE_PAYMENT;
        }
        if (mortgageAccountIdArg != null && !mortgageAccountIdArg.trim().isEmpty()) {
            this.presetMortgageAccountId = mortgageAccountIdArg.trim();
        }
    }

    public void loadAccounts() {
        disposables.clear();
        uiState.setValue(UiState.loading());

        disposables.add(
                getMortgageAccountUseCase.getActiveMortgages()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                            if (result.isSuccess() && result.getData() != null) {
                                mortgageAccounts = new ArrayList<>(result.getData());
                                applyPresetMortgageSelection();
                                publishSuccessStateIfReady();
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
                                    if (selectedCheckingIndex >= checkingAccounts.size()) {
                                        selectedCheckingIndex = 0;
                                    }
                                    publishSuccessState();
                                }
                            } else {
                                uiState.setValue(UiState.error(result.getMessage()));
                            }
                        }, throwable -> uiState.setValue(UiState.error(throwable.getMessage())))
        );
    }

    private void applyPresetMortgageSelection() {
        if (presetMortgageAccountId == null || mortgageAccounts.isEmpty()) {
            return;
        }
        for (int i = 0; i < mortgageAccounts.size(); i++) {
            if (presetMortgageAccountId.equals(mortgageAccounts.get(i).getAccountId())) {
                selectedMortgageIndex = i;
                break;
            }
        }
    }

    public void setMode(@NonNull DepositWithdrawUiState.Mode newMode) {
        if (this.mode != newMode) {
            this.mode = newMode;
            publishSuccessState();
        }
    }

    public void selectCheckingAccount(int index) {
        if (index >= 0 && index < checkingAccounts.size()) {
            selectedCheckingIndex = index;
            publishSuccessState();
        }
    }

    public void selectMortgageAccount(int index) {
        if (index >= 0 && index < mortgageAccounts.size()) {
            selectedMortgageIndex = index;
            publishSuccessState();
        }
    }

    public void submitAction(
            long amountVnd,
            @Nullable String withdrawDestination,
            @Nullable String rawNote,
            @NonNull String noMortgageError
    ) {
        if (checkingAccounts.isEmpty()) {
            uiState.setValue(UiState.empty());
            return;
        }
        Account checking = checkingAccounts.get(selectedCheckingIndex);
        String note = rawNote != null ? rawNote.trim() : "";

        TransactionInitiateRequest request;
        switch (mode) {
            case GATEWAY_DEPOSIT -> request = new TransactionInitiateRequest(
                    TransactionType.GATEWAY,
                    checking.getAccountId(),
                    null,
                    null,
                    amountVnd,
                    note,
                    TransactionMeta.empty()
            );
            case WITHDRAW -> {
                String dest = withdrawDestination != null ? withdrawDestination.trim() : "";
                String combinedNote = !dest.isEmpty()
                        ? (note.isEmpty() ? "Rut tien ve: " + dest : note + " (" + dest + ")")
                        : note;
                request = new TransactionInitiateRequest(
                        TransactionType.WITHDRAW,
                        checking.getAccountId(),
                        null,
                        null,
                        amountVnd,
                        combinedNote,
                        TransactionMeta.empty()
                );
            }
            case MORTGAGE_PAYMENT -> {
                if (mortgageAccounts.isEmpty() || selectedMortgageIndex >= mortgageAccounts.size()) {
                    messageEvent.setValue(noMortgageError);
                    return;
                }
                Account mortgageAcc = mortgageAccounts.get(selectedMortgageIndex);
                request = new TransactionInitiateRequest(
                        TransactionType.MORTGAGE_PAYMENT,
                        checking.getAccountId(),
                        null,
                        null,
                        amountVnd,
                        note,
                        TransactionMeta.forMortgage(mortgageAcc.getAccountId())
                );
            }
            default -> {
                return;
            }
        }

        uiState.setValue(UiState.loading());
        disposables.add(
                initiateTransactionUseCase.execute(request, checking)
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

    public void finalizeStripeDeposit(@NonNull String draftId) {
        uiState.setValue(UiState.loading());
        disposables.add(
                finalizeStripePaymentUseCase.execute(draftId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                            publishSuccessState();
                            if (result.isSuccess() && result.getData() != null) {
                                PaymentFinalizeResult finalized = result.getData();
                                navigateReceiptEvent.setValue(finalized.getTxId());
                            } else {
                                messageEvent.setValue(result.getMessage());
                            }
                        }, throwable -> {
                            publishSuccessState();
                            messageEvent.setValue(throwable.getMessage());
                        })
        );
    }

    private void publishSuccessStateIfReady() {
        if (!checkingAccounts.isEmpty()) {
            publishSuccessState();
        }
    }

    private void publishSuccessState() {
        if (checkingAccounts.isEmpty()) {
            return;
        }
        uiState.setValue(UiState.success(new DepositWithdrawUiState(
                mode,
                checkingAccounts,
                selectedCheckingIndex,
                mortgageAccounts,
                selectedMortgageIndex
        )));
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}
