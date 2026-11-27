package com.novabank.novapay.presentation.customer.security;

import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.security.SecurityStatus;
import com.novabank.novapay.domain.model.user.User;

import java.util.Collections;
import java.util.List;

public final class SecuritySettingsUiState {

    private final User user;
    private final SecurityStatus securityStatus;
    private final List<Account> checkingAccounts;
    private final Account selectedCheckingAccount;
    private final boolean securityStatusLoaded;
    private final boolean busy;

    public SecuritySettingsUiState(
            User user,
            SecurityStatus securityStatus,
            List<Account> checkingAccounts,
            Account selectedCheckingAccount,
            boolean busy
    ) {
        this(user, securityStatus, checkingAccounts, selectedCheckingAccount, true, busy);
    }

    public SecuritySettingsUiState(
            User user,
            SecurityStatus securityStatus,
            List<Account> checkingAccounts,
            Account selectedCheckingAccount,
            boolean securityStatusLoaded,
            boolean busy
    ) {
        this.user = user;
        this.securityStatus = securityStatus != null ? securityStatus : new SecurityStatus(false, null);
        this.checkingAccounts = checkingAccounts != null ? checkingAccounts : Collections.emptyList();
        this.selectedCheckingAccount = selectedCheckingAccount;
        this.securityStatusLoaded = securityStatusLoaded;
        this.busy = busy;
    }

    public User getUser() {
        return user;
    }

    public SecurityStatus getSecurityStatus() {
        return securityStatus;
    }

    public List<Account> getCheckingAccounts() {
        return checkingAccounts;
    }

    public Account getSelectedCheckingAccount() {
        return selectedCheckingAccount;
    }

    public boolean isSecurityStatusLoaded() {
        return securityStatusLoaded;
    }

    public boolean shouldShowControlsPin() {
        return securityStatus.hasTransactionPin() || !securityStatusLoaded;
    }

    public boolean isBusy() {
        return busy;
    }
}
