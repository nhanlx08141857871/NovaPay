package com.novabank.novapay.presentation.customer.utility;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novabank.novapay.core.common.SingleLiveEvent;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.transaction.TransactionDraft;
import com.novabank.novapay.domain.model.transaction.TransactionInitiateRequest;
import com.novabank.novapay.domain.model.transaction.TransactionMeta;
import com.novabank.novapay.domain.model.transaction.TransactionType;
import com.novabank.novapay.domain.model.utility.UtilityBill;
import com.novabank.novapay.domain.model.utility.UtilityBillType;
import com.novabank.novapay.domain.usecase.GetActiveCheckingAccountsUseCase;
import com.novabank.novapay.domain.usecase.InitiateTransactionUseCase;
import com.novabank.novapay.domain.usecase.LookupUtilityBillsUseCase;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * ViewModel tra cứu và thanh toán hóa đơn Điện / Nước (utility_bills).
 */
@HiltViewModel
public class UtilityBillViewModel extends ViewModel {

    private final GetActiveCheckingAccountsUseCase getActiveCheckingAccountsUseCase;
    private final LookupUtilityBillsUseCase lookupUtilityBillsUseCase;
    private final InitiateTransactionUseCase initiateTransactionUseCase;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<UiState<UtilityBillUiState>> uiState = new MutableLiveData<>();
    private final SingleLiveEvent<TransactionDraft> draftCreatedEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> messageEvent = new SingleLiveEvent<>();

    private List<Account> checkingAccounts = new ArrayList<>();
    private int selectedSourceIndex = 0;
    private UtilityBillType filterType = null;
    private List<UtilityBill> bills = new ArrayList<>();
    private boolean hasSearched = false;
    private String lastSearchedCode = "";

    @Inject
    public UtilityBillViewModel(
            GetActiveCheckingAccountsUseCase getActiveCheckingAccountsUseCase,
            LookupUtilityBillsUseCase lookupUtilityBillsUseCase,
            InitiateTransactionUseCase initiateTransactionUseCase
    ) {
        this.getActiveCheckingAccountsUseCase = getActiveCheckingAccountsUseCase;
        this.lookupUtilityBillsUseCase = lookupUtilityBillsUseCase;
        this.initiateTransactionUseCase = initiateTransactionUseCase;
    }

    public LiveData<UiState<UtilityBillUiState>> getUiState() {
        return uiState;
    }

    public SingleLiveEvent<TransactionDraft> getDraftCreatedEvent() {
        return draftCreatedEvent;
    }

    public SingleLiveEvent<String> getMessageEvent() {
        return messageEvent;
    }

    public void loadAccounts() {
        disposables.clear();
        uiState.setValue(UiState.loading());
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

    public void setFilterType(@Nullable UtilityBillType newType) {
        this.filterType = newType;
        if (hasSearched && !lastSearchedCode.isEmpty()) {
            searchBills(lastSearchedCode);
        } else {
            publishSuccessState();
        }
    }

    public void searchBills(@Nullable String customerCode) {
        if (checkingAccounts.isEmpty()) {
            uiState.setValue(UiState.empty());
            return;
        }
        String cleanCode = customerCode != null ? customerCode.trim() : "";
        lastSearchedCode = cleanCode;
        uiState.setValue(UiState.loading());
        disposables.add(
                lookupUtilityBillsUseCase.execute(cleanCode, filterType)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                            if (result.isSuccess()) {
                                hasSearched = true;
                                bills = result.getData() != null
                                        ? new ArrayList<>(result.getData())
                                        : new ArrayList<>();
                                publishSuccessState();
                            } else {
                                publishSuccessState();
                                messageEvent.setValue(result.getMessage());
                            }
                        }, throwable -> {
                            publishSuccessState();
                            messageEvent.setValue(throwable.getMessage());
                        })
        );
    }

    public void initiateBillPayment(@NonNull UtilityBill bill, @NonNull String note) {
        if (checkingAccounts.isEmpty()) {
            uiState.setValue(UiState.empty());
            return;
        }
        Account sourceAccount = checkingAccounts.get(selectedSourceIndex);
        TransactionInitiateRequest request = new TransactionInitiateRequest(
                TransactionType.BILL,
                sourceAccount.getAccountId(),
                null,
                null,
                bill.getAmount(),
                note,
                TransactionMeta.forBill(bill.getBillId(), bill.getCustomerCode())
        );

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

    private void publishSuccessState() {
        if (checkingAccounts.isEmpty()) {
            return;
        }
        uiState.setValue(UiState.success(new UtilityBillUiState(
                checkingAccounts,
                selectedSourceIndex,
                filterType,
                bills,
                hasSearched
        )));
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}
