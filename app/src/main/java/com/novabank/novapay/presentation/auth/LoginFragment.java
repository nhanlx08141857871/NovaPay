package com.novabank.novapay.presentation.auth;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.novabank.novapay.R;
import com.novabank.novapay.core.security.SessionManager;
import com.novabank.novapay.core.ui.BaseFragment;
import com.novabank.novapay.core.ui.StateLayoutHelper;
import com.novabank.novapay.databinding.FragmentLoginBinding;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Màn hình Đăng nhập chính thức (Firebase Auth + Custom Claim role).
 */
@AndroidEntryPoint
public class LoginFragment extends BaseFragment<FragmentLoginBinding> {

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

        viewModel.checkExistingSession();
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
