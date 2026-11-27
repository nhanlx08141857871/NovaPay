package com.novabank.novapay.presentation.customer.security;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.novabank.novapay.R;
import com.novabank.novapay.core.security.BiometricHelper;
import com.novabank.novapay.core.security.BiometricQuickLoginStore;
import com.novabank.novapay.core.ui.BaseFragment;
import com.novabank.novapay.core.ui.StateLayoutHelper;
import com.novabank.novapay.core.util.CurrencyFormatter;
import com.novabank.novapay.core.util.DateUtils;
import com.novabank.novapay.databinding.DialogBiometricEnrollBinding;
import com.novabank.novapay.databinding.FragmentSecuritySettingsBinding;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.account.AccountStatus;
import com.novabank.novapay.domain.model.security.SecurityStatus;
import com.novabank.novapay.domain.model.user.User;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SecuritySettingsFragment extends BaseFragment<FragmentSecuritySettingsBinding> {

    @Inject
    BiometricHelper biometricHelper;

    @Inject
    BiometricQuickLoginStore biometricQuickLoginStore;

    private SecuritySettingsViewModel viewModel;
    private StateLayoutHelper stateLayoutHelper;
    private SecuritySettingsUiState latestState;
    private List<Account> boundCheckingAccounts = new ArrayList<>();
    private String lastBoundAccountId;
    private boolean updatingBiometricSwitch = false;

    @NonNull
    @Override
    protected FragmentSecuritySettingsBinding inflateBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentSecuritySettingsBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(SecuritySettingsViewModel.class);
        FragmentSecuritySettingsBinding b = getBinding();

        stateLayoutHelper = new StateLayoutHelper(
                b.contentContainer,
                b.stateLoading,
                b.stateEmpty,
                b.stateError,
                () -> viewModel.load()
        );

        b.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());

        b.btnChangePassword.setOnClickListener(v -> {
            String currentPw = textOf(b.etCurrentPassword);
            String newPw = textOf(b.etNewPassword);
            String confirmPw = textOf(b.etConfirmPassword);
            viewModel.changePassword(currentPw, newPw, confirmPw);
        });

        b.btnSavePin.setOnClickListener(v -> {
            String currentPin = textOf(b.etCurrentPin);
            String newPin = textOf(b.etNewPin);
            String confirmPin = textOf(b.etConfirmPin);
            viewModel.saveTransactionPin(currentPin, newPin, confirmPin);
        });

        b.actvCheckingAccount.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < boundCheckingAccounts.size()) {
                lastBoundAccountId = null;
                viewModel.selectCheckingAccount(boundCheckingAccounts.get(position).getAccountId());
            }
        });

        b.btnSaveAccountControls.setOnClickListener(v -> {
            if (latestState == null || latestState.getSelectedCheckingAccount() == null) {
                return;
            }
            Account selected = latestState.getSelectedCheckingAccount();
            boolean frozen = b.switchAccountFrozen.isChecked();
            long perTxnLimit = CurrencyFormatter.parseVndDigits(textOf(b.etPerTxnLimit));
            long dailyLimit = CurrencyFormatter.parseVndDigits(textOf(b.etDailyLimit));
            String pin = textOf(b.etControlsPin);
            viewModel.updateCheckingControls(selected.getAccountId(), frozen, perTxnLimit, dailyLimit, pin);
        });

        setupBiometricSwitch();
        viewModel.load();
    }

    private void setupBiometricSwitch() {
        FragmentSecuritySettingsBinding b = getBinding();
        boolean canAuth = biometricHelper.canAuthenticate(requireContext());
        b.switchBiometricQuickLogin.setEnabled(canAuth);
        refreshBiometricCardUi();

        b.switchBiometricQuickLogin.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (updatingBiometricSwitch) {
                return;
            }
            if (!isChecked) {
                String currentEmail = getCurrentUserEmail();
                if (biometricQuickLoginStore.isEnabledForEmail(currentEmail)) {
                    biometricQuickLoginStore.clear();
                    refreshBiometricCardUi();
                    showSnackbar(getString(R.string.core_security_biometric_disabled_success));
                } else {
                    refreshBiometricCardUi();
                }
                return;
            }
            // Revert until enrollment succeeds
            setSwitchCheckedSilently(false);
            promptBiometricEnrollment();
        });
    }

    private void promptBiometricEnrollment() {
        User currentUser = latestState != null ? latestState.getUser() : null;
        if (currentUser == null || currentUser.getEmail().isEmpty()) {
            showSnackbar(getString(R.string.core_error_network_generic));
            return;
        }
        DialogBiometricEnrollBinding dialogBinding = DialogBiometricEnrollBinding.inflate(getLayoutInflater());
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.core_security_biometric_enroll_title)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.core_saving_settle_cancel, null)
                .setPositiveButton(R.string.core_security_biometric_enroll_confirm, (dialog, which) -> {
                    String password = dialogBinding.etEnrollPassword.getText() != null
                            ? dialogBinding.etEnrollPassword.getText().toString()
                            : "";
                    if (password.isEmpty()) {
                        showSnackbar(getString(R.string.core_login_empty_credentials));
                        return;
                    }
                    biometricHelper.authenticate(
                            this,
                            getString(R.string.core_login_biometric_prompt_title),
                            getString(R.string.core_login_biometric_prompt_subtitle, currentUser.getEmail()),
                            new BiometricHelper.AuthCallback() {
                                @Override
                                public void onAuthSucceeded() {
                                    boolean saved = biometricQuickLoginStore.saveCredentials(
                                            currentUser.getEmail(),
                                            password
                                    );
                                    if (saved) {
                                        refreshBiometricCardUi();
                                        showSnackbar(getString(R.string.core_security_biometric_enabled_success));
                                    } else {
                                        showSnackbar(getString(R.string.core_error_network_generic));
                                    }
                                }

                                @Override
                                public void onAuthError(int errorCode, @NonNull String message) {
                                    if (!message.isEmpty()) {
                                        showSnackbar(message);
                                    }
                                }
                            }
                    );
                })
                .show();
    }

    @Override
    protected void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                return;
            }
            stateLayoutHelper.render(state);
            SecuritySettingsUiState data = state.getData();
            if (data != null) {
                latestState = data;
                renderUi(data);
            }
        });

        viewModel.getMessageResEvent().observe(getViewLifecycleOwner(), resId -> {
            if (resId != null) {
                showSnackbar(getString(resId));
            }
        });

        viewModel.getMessageTextEvent().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                showSnackbar(msg);
            }
        });

        viewModel.getClearPasswordFieldsEvent().observe(getViewLifecycleOwner(), clear -> {
            if (Boolean.TRUE.equals(clear)) {
                FragmentSecuritySettingsBinding b = getBinding();
                b.etCurrentPassword.setText("");
                b.etNewPassword.setText("");
                b.etConfirmPassword.setText("");
            }
        });

        viewModel.getClearPinFieldsEvent().observe(getViewLifecycleOwner(), clear -> {
            if (Boolean.TRUE.equals(clear)) {
                FragmentSecuritySettingsBinding b = getBinding();
                b.etCurrentPin.setText("");
                b.etNewPin.setText("");
                b.etConfirmPin.setText("");
            }
        });

        viewModel.getClearControlsPinEvent().observe(getViewLifecycleOwner(), clear -> {
            if (Boolean.TRUE.equals(clear)) {
                getBinding().etControlsPin.setText("");
            }
        });
    }

    private void renderUi(@NonNull SecuritySettingsUiState data) {
        FragmentSecuritySettingsBinding b = getBinding();
        SecurityStatus pinStatus = data.getSecurityStatus();
        boolean hasPin = pinStatus.hasTransactionPin();

        if (hasPin) {
            b.tvPinStatusBadge.setText(R.string.core_security_pin_status_set);
            b.tvPinStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.novapay_verified_badge));
            b.tvPinStatusBadge.setBackgroundResource(R.drawable.bg_np_badge_verified);
        } else {
            b.tvPinStatusBadge.setText(R.string.core_security_pin_status_unset);
            b.tvPinStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.novapay_warning_pending));
            b.tvPinStatusBadge.setBackgroundResource(R.drawable.bg_np_badge_pending);
        }

        b.tilCurrentPin.setVisibility(hasPin ? View.VISIBLE : View.GONE);
        b.tilControlsPin.setVisibility(data.shouldShowControlsPin() ? View.VISIBLE : View.GONE);

        if (pinStatus.isPinLockedNow(Instant.now())) {
            b.tvPinLockedNotice.setVisibility(View.VISIBLE);
            b.tvPinLockedNotice.setText(getString(
                    R.string.core_security_pin_locked_notice,
                    DateUtils.formatDateTime(pinStatus.getPinLockedUntil())
            ));
        } else {
            b.tvPinLockedNotice.setVisibility(View.GONE);
        }

        refreshBiometricCardUi();

        List<Account> checkings = data.getCheckingAccounts();
        if (checkings.isEmpty()) {
            b.tvCheckingControlsEmpty.setVisibility(View.VISIBLE);
            b.layoutCheckingControlsForm.setVisibility(View.GONE);
        } else {
            b.tvCheckingControlsEmpty.setVisibility(View.GONE);
            b.layoutCheckingControlsForm.setVisibility(View.VISIBLE);

            if (!sameCheckingList(boundCheckingAccounts, checkings)) {
                boundCheckingAccounts = new ArrayList<>(checkings);
                List<String> labels = new ArrayList<>();
                for (Account acc : boundCheckingAccounts) {
                    labels.add(getString(
                            R.string.core_open_saving_source_item,
                            acc.getAccountNumber(),
                            CurrencyFormatter.formatVnd(acc.getBalance())
                    ));
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        labels
                );
                b.actvCheckingAccount.setAdapter(adapter);
            }

            Account selected = data.getSelectedCheckingAccount();
            if (selected != null) {
                String label = getString(
                        R.string.core_open_saving_source_item,
                        selected.getAccountNumber(),
                        CurrencyFormatter.formatVnd(selected.getBalance())
                );
                if (!label.equals(b.actvCheckingAccount.getText().toString())) {
                    b.actvCheckingAccount.setText(label, false);
                }
                if (!selected.getAccountId().equals(lastBoundAccountId)) {
                    lastBoundAccountId = selected.getAccountId();
                    b.switchAccountFrozen.setChecked(selected.getStatus() == AccountStatus.FROZEN);
                    b.etPerTxnLimit.setText(String.valueOf(selected.getPerTxnLimit()));
                    b.etDailyLimit.setText(String.valueOf(selected.getDailyLimit()));
                }
            }
        }

        boolean enabled = !data.isBusy();
        b.btnChangePassword.setEnabled(enabled);
        b.btnSavePin.setEnabled(enabled);
        b.btnSaveAccountControls.setEnabled(enabled);
    }

    private void refreshBiometricCardUi() {
        FragmentSecuritySettingsBinding b = getBinding();
        boolean canAuth = biometricHelper.canAuthenticate(requireContext());
        String currentEmail = getCurrentUserEmail();
        boolean enrolledForCurrentUser = biometricQuickLoginStore.isEnabledForEmail(currentEmail);

        b.switchBiometricQuickLogin.setEnabled(
                canAuth && currentEmail != null && !currentEmail.trim().isEmpty()
        );
        setSwitchCheckedSilently(canAuth && enrolledForCurrentUser);
        if (!canAuth) {
            b.tvBiometricDesc.setText(R.string.core_security_biometric_desc_unavailable);
        } else if (enrolledForCurrentUser && currentEmail != null && !currentEmail.trim().isEmpty()) {
            b.tvBiometricDesc.setText(getString(
                    R.string.core_security_biometric_desc_enrolled,
                    currentEmail.trim()
            ));
        } else {
            b.tvBiometricDesc.setText(R.string.core_security_biometric_desc_ready);
        }
    }

    @Nullable
    private String getCurrentUserEmail() {
        return (latestState != null && latestState.getUser() != null)
                ? latestState.getUser().getEmail()
                : null;
    }

    private void setSwitchCheckedSilently(boolean checked) {
        updatingBiometricSwitch = true;
        try {
            getBinding().switchBiometricQuickLogin.setChecked(checked);
        } finally {
            updatingBiometricSwitch = false;
        }
    }

    @NonNull
    private static String textOf(@NonNull com.google.android.material.textfield.TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString() : "";
    }

    private static boolean sameCheckingList(@NonNull List<Account> a, @NonNull List<Account> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).getAccountId().equals(b.get(i).getAccountId())
                    || a.get(i).getStatus() != b.get(i).getStatus()
                    || a.get(i).getPerTxnLimit() != b.get(i).getPerTxnLimit()
                    || a.get(i).getDailyLimit() != b.get(i).getDailyLimit()) {
                return false;
            }
        }
        return true;
    }
}
