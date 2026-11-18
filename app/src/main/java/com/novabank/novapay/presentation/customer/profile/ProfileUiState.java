package com.novabank.novapay.presentation.customer.profile;

import com.novabank.novapay.domain.model.user.User;

public final class ProfileUiState {

    private final User user;
    private final boolean saving;

    public ProfileUiState(User user, boolean saving) {
        this.user = user;
        this.saving = saving;
    }

    public User getUser() {
        return user;
    }

    public boolean isSaving() {
        return saving;
    }
}
