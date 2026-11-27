package com.novabank.novapay.presentation.auth;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.novabank.novapay.R;
import com.novabank.novapay.core.security.BiometricHelper;
import com.novabank.novapay.core.security.BiometricQuickLoginStore;
import com.novabank.novapay.core.security.SessionManager;
import com.novabank.novapay.core.ui.BaseFragment;
import com.novabank.novapay.core.ui.StateLayoutHelper;
import com.novabank.novapay.databinding.DialogForgotPasswordBinding;
import com.novabank.novapay.databinding.FragmentLoginBinding;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Màn hình Đăng nhập chính thức (Firebase Auth + Custom Claim role + Quên mật khẩu + Sinh trắc học).
 */
@AndroidEntryPoint
public class LoginFragment extends BaseFragment<FragmentLoginBinding> {

    @Inject
    BiometricHelper biometricHelper;

    @Inject
    BiometricQuickLoginStore biometricQuickLoginStore;

    private LoginViewModel viewModel;
    private StateLayoutHelper stateLayoutHelper;

    @NonNull
    @Override
    protected FragmentLoginBinding inflateBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentLoginBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        FragmentLoginBinding b = getBinding();

        stateLayoutHelper = new StateLayoutHelper(
                b.contentContainer,
                b.stateLoading,
                b.stateEmpty,
                b.stateError,
                () -> viewModel.resetToReadyForm()
        );

        b.btnLogin.setOnClickListener(v -> {
            String email = b.etEmail.getText() != null ? b.etEmail.getText().toString() : "";
            String password = b.etPassword.getText() != null ? b.etPassword.getText().toString() : "";
            viewModel.onLoginClicked(email, password);
        });

        b.btnForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        setupBiometricQuickLoginButton();
        viewModel.checkExistingSession();
    }

    private void setupBiometricQuickLoginButton() {
        FragmentLoginBinding b = getBinding();
        boolean available = biometricHelper.canAuthenticate(requireContext())
                && biometricQuickLoginStore.isEnabled()
                && biometricQuickLoginStore.getEnrolledEmail() != null;
        b.btnBiometricLogin.setVisibility(available ? View.VISIBLE : View.GONE);
        if (!available) {
            return;
        }
        String enrolledEmail = biometricQuickLoginStore.getEnrolledEmail();
        if (b.etEmail.getText() == null || b.etEmail.getText().toString().trim().isEmpty()) {
            b.etEmail.setText(enrolledEmail);
        }
        b.btnBiometricLogin.setOnClickListener(v -> biometricHelper.authenticate(
                this,
                getString(R.string.core_login_biometric_prompt_title),
                getString(
                        R.string.core_login_biometric_prompt_subtitle,
                        enrolledEmail != null ? enrolledEmail : ""
                ),
                new BiometricHelper.AuthCallback() {
                    @Override
                    public void onAuthSucceeded() {
                        BiometricQuickLoginStore.Credentials creds = biometricQuickLoginStore.readCredentials();
                        if (creds == null) {
                            b.btnBiometricLogin.setVisibility(View.GONE);
                            showSnackbar(getString(R.string.core_error_network_generic));
                            return;
                        }
                        b.etEmail.setText(creds.email);
                        viewModel.onLoginClicked(creds.email, creds.password);
                    }

                    @Override
                    public void onAuthError(int errorCode, @NonNull String message) {
                        if (!message.isEmpty()) {
                            showSnackbar(message);
                        }
                    }
                }
        ));
    }

    private void showForgotPasswordDialog() {
        DialogForgotPasswordBinding dialogBinding = DialogForgotPasswordBinding.inflate(getLayoutInflater());
        String currentEmail = getBinding().etEmail.getText() != null
                ? getBinding().etEmail.getText().toString().trim()
                : "";
        if (!currentEmail.isEmpty()) {
            dialogBinding.etResetEmail.setText(currentEmail);
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.core_login_forgot_dialog_title)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.core_saving_settle_cancel, null)
                .setPositiveButton(R.string.core_login_forgot_send_action, (dialog, which) -> {
                    String email = dialogBinding.etResetEmail.getText() != null
                            ? dialogBinding.etResetEmail.getText().toString()
                            : "";
                    viewModel.sendPasswordResetEmail(email);
                })
                .show();
    }

    @Override
    protected void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state != null) {
                stateLayoutHelper.render(state);
            }
        });

        viewModel.getMessageResEvent().observe(getViewLifecycleOwner(), msgResId -> {
            if (msgResId != null) {
                showSnackbar(getString(msgResId));
            }
        });

        viewModel.getMessageTextEvent().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                showSnackbar(msg);
            }
        });

        viewModel.getResetEmailSentEvent().observe(getViewLifecycleOwner(), sentEmail -> {
            if (sentEmail != null && !sentEmail.isEmpty()) {
                showSnackbar(getString(R.string.core_login_forgot_sent_success, sentEmail));
            }
        });

        viewModel.getNavigateDeepLinkEvent().observe(getViewLifecycleOwner(), uriString -> {
            if (uriString == null || uriString.isEmpty()) {
                return;
            }
            try {
                NavOptions options = new NavOptions.Builder()
                        .setPopUpTo(R.id.loginFragment, true)
                        .build();
                NavHostFragment.findNavController(this).navigate(Uri.parse(uriString), options);
            } catch (IllegalArgumentException | IllegalStateException ex) {
                if (SessionManager.DEEPLINK_OFFICER.equals(uriString)) {
                    showSnackbar(getString(R.string.core_login_officer_redirect));
                } else {
                    showSnackbar(getString(R.string.core_guard_destination_unavailable));
                }
            }
        });
    }
}
