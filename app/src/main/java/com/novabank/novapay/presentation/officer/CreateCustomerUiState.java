package com.novabank.novapay.presentation.officer;

import androidx.annotation.Nullable;

public final class CreateCustomerUiState {

    @Nullable
    private final Integer fullNameError;
    @Nullable
    private final Integer emailError;
    @Nullable
    private final Integer phoneError;
    @Nullable
    private final Integer nationalIdError;
    @Nullable
    private final Integer dobError;
    @Nullable
    private final Integer addressError;
    @Nullable
    private final Integer passwordError;
    @Nullable
    private final String createdUid;

    public CreateCustomerUiState(
            @Nullable Integer fullNameError,
            @Nullable Integer emailError,
            @Nullable Integer phoneError,
            @Nullable Integer nationalIdError,
            @Nullable Integer dobError,
            @Nullable Integer addressError,
            @Nullable Integer passwordError,
            @Nullable String createdUid
    ) {
        this.fullNameError = fullNameError;
        this.emailError = emailError;
        this.phoneError = phoneError;
        this.nationalIdError = nationalIdError;
        this.dobError = dobError;
        this.addressError = addressError;
        this.passwordError = passwordError;
        this.createdUid = createdUid;
    }

    public static CreateCustomerUiState initial() {
        return new CreateCustomerUiState(null, null, null, null, null, null, null, null);
    }

    @Nullable
    public Integer getFullNameError() {
        return fullNameError;
    }

    @Nullable
    public Integer getEmailError() {
        return emailError;
    }

    @Nullable
    public Integer getPhoneError() {
        return phoneError;
    }

    @Nullable
    public Integer getNationalIdError() {
        return nationalIdError;
    }

    @Nullable
    public Integer getDobError() {
        return dobError;
    }

    @Nullable
    public Integer getAddressError() {
        return addressError;
    }

    @Nullable
    public Integer getPasswordError() {
        return passwordError;
    }

    @Nullable
    public String getCreatedUid() {
        return createdUid;
    }
}
