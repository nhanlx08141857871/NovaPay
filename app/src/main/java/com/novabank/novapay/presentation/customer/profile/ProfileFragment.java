package com.novabank.novapay.presentation.customer.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.novabank.novapay.R;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.core.ui.BaseFragment;
import com.novabank.novapay.core.ui.StateLayoutHelper;
import com.novabank.novapay.databinding.FragmentProfileBinding;
import com.novabank.novapay.domain.model.user.EkycStatus;
import com.novabank.novapay.domain.model.user.User;
import com.novabank.novapay.domain.model.user.UserRole;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Màn hình Hồ sơ cá nhân (novapay://profile):
 * Cập nhật an toàn phone, address, avatarUrl khi user ACTIVE; hỗ trợ mở eKYC và Đăng xuất.
 */
@AndroidEntryPoint
public class ProfileFragment extends BaseFragment<FragmentProfileBinding> {

    private ProfileViewModel viewModel;
    private StateLayoutHelper stateLayoutHelper;
    private String lastBoundUid;

    @NonNull
    @Override
    protected FragmentProfileBinding inflateBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentProfileBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        FragmentProfileBinding b = getBinding();

        stateLayoutHelper = new StateLayoutHelper(
                b.contentContainer,
                b.stateLoading,
                b.stateEmpty,
                b.stateError,
                () -> viewModel.loadProfile()
        );

        b.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        b.btnStartEkyc.setOnClickListener(v -> navigateDeepLink(Constants.DEEPLINK_EKYC));
        b.btnSaveProfile.setOnClickListener(v -> {
            String phone = b.etPhone.getText() != null ? b.etPhone.getText().toString() : "";
            String address = b.etAddress.getText() != null ? b.etAddress.getText().toString() : "";
            String avatar = b.etAvatarUrl.getText() != null ? b.etAvatarUrl.getText().toString() : "";
            viewModel.saveProfile(phone, address, avatar);
        });
        b.btnLogout.setOnClickListener(v -> {
            viewModel.signOut();
            NavOptions options = new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_core, true)
                    .build();
            NavHostFragment.findNavController(this).navigate(R.id.loginFragment, null, options);
        });

        viewModel.loadProfile();
    }

    @Override
    protected void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                return;
            }
            stateLayoutHelper.render(state);
            ProfileUiState data = state.getData();
            if (data != null && data.getUser() != null) {
                renderProfile(data.getUser(), data.isSaving());
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
    }

    private void renderProfile(@NonNull User user, boolean isSaving) {
        FragmentProfileBinding b = getBinding();
        b.tvFullName.setText(user.getFullName());
        b.tvEmail.setText(user.getEmail());
        b.tvNationalId.setText(user.getNationalId());
        b.tvDob.setText(user.getDob());
        b.tvRole.setText(
                user.getRole() == UserRole.OFFICER
                        ? R.string.core_profile_role_officer
                        : R.string.core_profile_role_customer
        );

        EkycStatus ekycStatus = user.getEkyc().getStatus();
        if (ekycStatus == EkycStatus.VERIFIED) {
            b.tvEkycStatus.setText(R.string.core_profile_ekyc_verified);
            b.tvEkycStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.novapay_verified_badge));
            b.btnStartEkyc.setVisibility(View.GONE);
        } else if (ekycStatus == EkycStatus.PENDING) {
            b.tvEkycStatus.setText(R.string.core_profile_ekyc_pending);
            b.tvEkycStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.novapay_warning_pending));
            b.btnStartEkyc.setVisibility(View.GONE);
        } else if (ekycStatus == EkycStatus.REJECTED) {
            b.tvEkycStatus.setText(R.string.core_profile_ekyc_rejected);
            b.tvEkycStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.novapay_expense_negative));
            b.btnStartEkyc.setVisibility(user.getRole() == UserRole.CUSTOMER ? View.VISIBLE : View.GONE);
        } else {
            b.tvEkycStatus.setText(R.string.core_profile_ekyc_unverified);
            b.tvEkycStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.novapay_warning_pending));
            b.btnStartEkyc.setVisibility(user.getRole() == UserRole.CUSTOMER ? View.VISIBLE : View.GONE);
        }

        if (!user.getUid().equals(lastBoundUid)) {
            lastBoundUid = user.getUid();
            b.etPhone.setText(user.getPhone());
            b.etAddress.setText(user.getAddress());
            b.etAvatarUrl.setText(user.getAvatarUrl());
        }

        boolean active = user.isActive();
        b.etPhone.setEnabled(active && !isSaving);
        b.etAddress.setEnabled(active && !isSaving);
        b.etAvatarUrl.setEnabled(active && !isSaving);
        b.btnSaveProfile.setEnabled(active && !isSaving);
        b.tvLockedNotice.setVisibility(active ? View.GONE : View.VISIBLE);
    }
}
