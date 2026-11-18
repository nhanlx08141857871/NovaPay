package com.novabank.novapay.presentation.officer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.core.common.SingleLiveEvent;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.domain.model.user.CustomerProfile;
import com.novabank.novapay.domain.usecase.officer.ObserveCustomersUseCase;
import com.novabank.novapay.domain.usecase.officer.ToggleCustomerLockUseCase;
import com.novabank.novapay.domain.usecase.officer.UpdateCustomerUseCase;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

@HiltViewModel
public class CustomerManagementViewModel extends ViewModel {

    private final ObserveCustomersUseCase observeCustomersUseCase;
    private final UpdateCustomerUseCase updateCustomerUseCase;
    private final ToggleCustomerLockUseCase toggleCustomerLockUseCase;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private Disposable observeDisposable;

    private final MutableLiveData<UiState<CustomerManagementUiState>> uiState =
            new MutableLiveData<>(UiState.loading());
    private final MutableLiveData<Boolean> actionInProgress = new MutableLiveData<>(false);
    private final SingleLiveEvent<String> feedbackMessageEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Integer> feedbackResEvent = new SingleLiveEvent<>();

    private List<CustomerProfile> cachedCustomers = Collections.emptyList();
    private String currentSearchQuery = "";

    @Inject
    public CustomerManagementViewModel(
            ObserveCustomersUseCase observeCustomersUseCase,
            UpdateCustomerUseCase updateCustomerUseCase,
            ToggleCustomerLockUseCase toggleCustomerLockUseCase
    ) {
        this.observeCustomersUseCase = observeCustomersUseCase;
        this.updateCustomerUseCase = updateCustomerUseCase;
        this.toggleCustomerLockUseCase = toggleCustomerLockUseCase;
        loadCustomers();
    }

    public LiveData<UiState<CustomerManagementUiState>> getUiState() {
        return uiState;
    }

    public LiveData<Boolean> getActionInProgress() {
        return actionInProgress;
    }

    public LiveData<String> getFeedbackMessageEvent() {
        return feedbackMessageEvent;
    }

    public LiveData<Integer> getFeedbackResEvent() {
        return feedbackResEvent;
    }

    public void loadCustomers() {
        if (observeDisposable != null && !observeDisposable.isDisposed()) {
            observeDisposable.dispose();
        }
        uiState.setValue(UiState.loading());
        observeDisposable = observeCustomersUseCase.execute()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::handleCustomersResult,
                        error -> uiState.setValue(UiState.error(error.getMessage()))
                );
        disposables.add(observeDisposable);
    }

    public void onSearchQueryChanged(String query) {
        currentSearchQuery = query != null ? query : "";
        applyFilterAndEmit();
    }

    public void toggleCustomerLock(CustomerProfile customer, int lockedResId, int unlockedResId) {
        if (customer == null || Boolean.TRUE.equals(actionInProgress.getValue())) {
            return;
        }
        boolean wasLocked = customer.isLocked();
        actionInProgress.setValue(true);
        disposables.add(
                toggleCustomerLockUseCase.execute(customer)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                result -> {
                                    actionInProgress.setValue(false);
                                    if (result.isSuccess()) {
                                        feedbackResEvent.setValue(wasLocked ? unlockedResId : lockedResId);
                                    } else {
                                        feedbackMessageEvent.setValue(result.getMessage());
                                    }
                                },
                                error -> {
                                    actionInProgress.setValue(false);
                                    feedbackMessageEvent.setValue(error.getMessage());
                                }
                        )
        );
    }

    public void updateCustomerDetails(
            String uid,
            String fullName,
            String phone,
            String address,
            int successResId
    ) {
        if (Boolean.TRUE.equals(actionInProgress.getValue())) {
            return;
        }
        actionInProgress.setValue(true);
        disposables.add(
                updateCustomerUseCase.execute(uid, fullName, phone, address)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                result -> {
                                    actionInProgress.setValue(false);
                                    if (result.isSuccess()) {
                                        feedbackResEvent.setValue(successResId);
                                    } else {
                                        feedbackMessageEvent.setValue(result.getMessage());
                                    }
                                },
                                error -> {
                                    actionInProgress.setValue(false);
                                    feedbackMessageEvent.setValue(error.getMessage());
                                }
                        )
        );
    }

    private void handleCustomersResult(AppResult<List<CustomerProfile>> result) {
        if (result == null) {
            uiState.setValue(UiState.empty());
            return;
        }
        if (result.isError()) {
            uiState.setValue(UiState.error(result.getMessage()));
            return;
        }
        cachedCustomers = result.getData() != null ? result.getData() : Collections.emptyList();
        applyFilterAndEmit();
    }

    private void applyFilterAndEmit() {
        List<CustomerProfile> filtered = observeCustomersUseCase.filter(cachedCustomers, currentSearchQuery);
        if (filtered.isEmpty()) {
            uiState.setValue(UiState.empty());
        } else {
            uiState.setValue(UiState.success(new CustomerManagementUiState(filtered, currentSearchQuery)));
        }
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}
