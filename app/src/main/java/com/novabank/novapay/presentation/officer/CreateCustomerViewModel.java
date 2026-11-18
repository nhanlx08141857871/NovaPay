package com.novabank.novapay.presentation.officer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novabank.novapay.R;
import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.core.common.SingleLiveEvent;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.domain.model.user.CustomerValidation;
import com.novabank.novapay.domain.usecase.officer.CreateCustomerUseCase;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

@HiltViewModel
public class CreateCustomerViewModel extends ViewModel {

    private final CreateCustomerUseCase createCustomerUseCase;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<UiState<CreateCustomerUiState>> uiState =
            new MutableLiveData<>(UiState.success(CreateCustomerUiState.initial()));
    private final SingleLiveEvent<String> createdSuccessUidEvent = new SingleLiveEvent<>();

    @Inject
    public CreateCustomerViewModel(CreateCustomerUseCase createCustomerUseCase) {
        this.createCustomerUseCase = createCustomerUseCase;
    }

    public LiveData<UiState<CreateCustomerUiState>> getUiState() {
        return uiState;
    }

    public LiveData<String> getCreatedSuccessUidEvent() {
        return createdSuccessUidEvent;
    }

    public void restoreFormAfterError() {
        uiState.setValue(UiState.success(CreateCustomerUiState.initial()));
    }

    public void submitCreateCustomer(
            String fullName,
            String email,
            String phone,
            String nationalId,
            String dob,
            String address,
            String initialPassword
    ) {
        Integer fullNameErr = CustomerValidation.isValidFullName(fullName)
                ? null : R.string.officer_validation_fullname;
        Integer emailErr = CustomerValidation.isValidEmail(email)
                ? null : R.string.officer_validation_email;
        Integer phoneErr = CustomerValidation.isValidPhone(phone)
                ? null : R.string.officer_validation_phone;
        Integer nationalIdErr = CustomerValidation.isValidNationalId(nationalId)
                ? null : R.string.officer_validation_national_id;
        String normalizedDob = CustomerValidation.normalizeDobToIso(dob);
        Integer dobErr = normalizedDob != null
                ? null : R.string.officer_validation_dob;
        Integer addressErr = CustomerValidation.isValidAddress(address)
                ? null : R.string.officer_validation_address;
        Integer passwordErr = CustomerValidation.isValidInitialPassword(initialPassword)
                ? null : R.string.officer_validation_password;

        if (fullNameErr != null || emailErr != null || phoneErr != null
                || nationalIdErr != null || dobErr != null || addressErr != null
                || passwordErr != null) {
            uiState.setValue(UiState.success(new CreateCustomerUiState(
                    fullNameErr,
                    emailErr,
                    phoneErr,
                    nationalIdErr,
                    dobErr,
                    addressErr,
                    passwordErr,
                    null
            )));
            return;
        }

        uiState.setValue(UiState.loading());
        disposables.add(
                createCustomerUseCase.execute(
                                fullName,
                                email,
                                phone,
                                nationalId,
                                normalizedDob,
                                address,
                                initialPassword
                        )
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                this::handleCreateResult,
                                error -> uiState.setValue(UiState.error(error.getMessage()))
                        )
        );
    }

    private void handleCreateResult(AppResult<String> result) {
        if (result == null) {
            uiState.setValue(UiState.empty());
            return;
        }
        if (result.isError()) {
            uiState.setValue(UiState.error(result.getMessage()));
            return;
        }
        String uid = result.getData() != null ? result.getData() : "";
        uiState.setValue(UiState.success(new CreateCustomerUiState(
                null, null, null, null, null, null, null, uid
        )));
        createdSuccessUidEvent.setValue(uid);
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}
