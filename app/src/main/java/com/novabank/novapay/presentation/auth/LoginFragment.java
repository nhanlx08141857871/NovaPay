package com.novabank.novapay.presentation.auth;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.novabank.novapay.R;
import com.novabank.novapay.core.ui.BaseFragment;
import com.novabank.novapay.core.ui.StateLayoutHelper;
import com.novabank.novapay.databinding.FragmentLoginBinding;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Màn hình khởi đầu Day-0 của sub-graph nav_core.xml.
 * Tuân thủ nghiêm ngặt chuẩn 3 class + 1 XML + StateLayoutHelper của GEMINI.md.
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
                null
        );

        b.btnLogin.setOnClickListener(v -> {
            String email = b.etEmail.getText() != null ? b.etEmail.getText().toString() : "";
            String password = b.etPassword.getText() != null ? b.etPassword.getText().toString() : "";
            viewModel.onLoginClicked(
                    email,
                    password,
                    R.string.core_login_empty_credentials,
                    R.string.core_login_skeleton_notice
            );
        });
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
    }
}
