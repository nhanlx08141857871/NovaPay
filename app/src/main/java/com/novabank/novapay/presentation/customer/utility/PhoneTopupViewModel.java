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
import com.novabank.novapay.domain.usecase.GetActiveCheckingAccountsUseCase;
import com.novabank.novapay.domain.usecase.InitiateTransactionUseCase;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * ViewModel nạp tiền điện thoại theo nhà mạng và mệnh giá cố định (kiểu long VNĐ).
 */
@HiltViewModel
public class PhoneTopupViewModel extends ViewModel {

    private static final Pattern VALID_PHONE_PATTERN = Pattern.compile("^[0-9]{9,11}$");

    private final GetActiveCheckingAccountsUseCase getActiveCheckingAccountsUseCase;
    private final InitiateTransactionUseCase initiateTransactionUseCase;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<UiState<PhoneTopupUiState>> uiState = new MutableLiveData<>();
    private final SingleLiveEvent<TransactionDraft> draftCreatedEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> messageEvent = new SingleLiveEvent<>();

    private List<Account> checkingAccounts = new ArrayList<>();
    private int selectedSourceIndex = 0;
    private String selectedCarrier = "Viettel";
    private long selectedAmountVnd = 100_000L;

    @Inject
    public PhoneTopupViewModel(
            GetActiveCheckingAccountsUseCase getActiveCheckingAccountsUseCase,
            InitiateTransactionUseCase initiateTransactionUseCase
    ) {
        this.getActiveCheckingAccountsUseCase = getActiveCheckingAccountsUseCase;
        this.initiateTransactionUseCase = initiateTransactionUseCase;
    }

    public LiveData<UiState<PhoneTopupUiState>> getUiState() {
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

    public void selectCarrier(@NonNull String carrier) {
        selectedCarrier = carrier;
        publishSuccessState();
    }

    public void selectDenomination(long amountVnd) {
        if (amountVnd > 0L) {
            selectedAmountVnd = amountVnd;
            publishSuccessState();
        }
    }

    public void submitTopup(
            @Nullable String rawPhone,
            @NonNull String note,
            @NonNull String invalidPhoneError
    ) {
        if (checkingAccounts.isEmpty()) {
            uiState.setValue(UiState.empty());
            return;
        }
        String normalizedPhone = rawPhone != null
                ? rawPhone.replaceAll("[\\s-]", "").trim()
                : "";
        if (!VALID_PHONE_PATTERN.matcher(normalizedPhone).matches()) {
            messageEvent.setValue(invalidPhoneError);
            return;
        }

        Account sourceAccount = checkingAccounts.get(selectedSourceIndex);
        TransactionInitiateRequest request = new TransactionInitiateRequest(
                TransactionType.TOPUP,
                sourceAccount.getAccountId(),
                null,
                null,
                selectedAmountVnd,
                note,
                TransactionMeta.forTopup(normalizedPhone)
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
        uiState.setValue(UiState.success(new PhoneTopupUiState(
                checkingAccounts,
                selectedSourceIndex,
                selectedCarrier,
                selectedAmountVnd
        )));
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}
