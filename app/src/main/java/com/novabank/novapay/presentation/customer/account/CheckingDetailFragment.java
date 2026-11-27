package com.novabank.novapay.presentation.customer.account;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.novabank.novapay.R;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.core.ui.BaseFragment;
import com.novabank.novapay.core.ui.StateLayoutHelper;
import com.novabank.novapay.core.util.CurrencyFormatter;
import com.novabank.novapay.core.util.QrBitmapEncoder;
import com.novabank.novapay.databinding.FragmentCheckingDetailBinding;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.account.AccountStatus;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Màn hình chi tiết tài khoản Thanh toán (CHECKING):
 * Hiển thị số dư (long), hạn mức giao dịch/ngày và mã QR cá nhân tạo bằng QrBitmapEncoder.
 */
@AndroidEntryPoint
public class CheckingDetailFragment extends BaseFragment<FragmentCheckingDetailBinding> {

    private CheckingDetailViewModel viewModel;
    private StateLayoutHelper stateLayoutHelper;
    private String accountId;
    private String lastQrPayload;

    @NonNull
    @Override
    protected FragmentCheckingDetailBinding inflateBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentCheckingDetailBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(CheckingDetailViewModel.class);
        FragmentCheckingDetailBinding b = getBinding();

        b.stateEmpty.tvEmptyTitle.setText(R.string.core_checking_title);
        b.stateEmpty.tvEmptySubtitle.setText(R.string.core_account_not_found);

        stateLayoutHelper = new StateLayoutHelper(
                b.contentContainer,
                b.stateLoading,
                b.stateEmpty,
                b.stateError,
                () -> viewModel.observeAccount(accountId)
        );

        b.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        b.btnTransfer.setOnClickListener(v -> safeNavigateDeepLink(Constants.DEEPLINK_TRANSFER));
        b.btnHistory.setOnClickListener(v -> safeNavigateDeepLink(Constants.DEEPLINK_TRANSACTIONS_MONTH));
        b.btnManageControls.setOnClickListener(v -> safeNavigateDeepLink(Constants.DEEPLINK_SECURITY));

        Bundle args = getArguments();
        accountId = args != null ? args.getString("accountId") : null;
        viewModel.observeAccount(accountId);
    }

    @Override
    protected void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                return;
            }
            stateLayoutHelper.render(state);
            CheckingDetailUiState data = state.getData();
            if (data != null && data.getAccount() != null) {
                renderAccount(data.getAccount());
            }
        });
    }

    private void renderAccount(@NonNull Account account) {
        FragmentCheckingDetailBinding b = getBinding();
        b.tvBalance.setText(CurrencyFormatter.formatVnd(account.getBalance()));
        b.tvAccountNumber.setText(account.getAccountNumber());
        b.tvStatus.setText(resolveStatusLabel(account.getStatus()));
        b.tvPerTxnLimit.setText(CurrencyFormatter.formatVnd(account.getPerTxnLimit()));
        b.tvDailyLimit.setText(CurrencyFormatter.formatVnd(account.getDailyLimit()));
        b.tvQrCaption.setText(getString(R.string.core_checking_qr_caption, account.getAccountNumber()));

        b.btnCopyAccountNumber.setOnClickListener(v -> copyAccountNumber(account.getAccountNumber()));

        String qrPayload = account.getAccountNumber();
        if (!qrPayload.equals(lastQrPayload)) {
            lastQrPayload = qrPayload;
            Bitmap qrBitmap = QrBitmapEncoder.encodeToBitmap(qrPayload, 480);
            b.ivQrCode.setImageBitmap(qrBitmap);
        }
    }

    private void copyAccountNumber(@NonNull String accountNumber) {
        ClipboardManager clipboard = (ClipboardManager) requireContext()
                .getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("accountNumber", accountNumber));
            showSnackbar(getString(R.string.core_copied_account_number, accountNumber));
        }
    }

    private String resolveStatusLabel(AccountStatus status) {
        if (status == AccountStatus.FROZEN) {
            return getString(R.string.core_account_status_frozen);
        }
        if (status == AccountStatus.CLOSED) {
            return getString(R.string.core_account_status_closed);
        }
        return getString(R.string.core_account_status_active);
    }

    private void safeNavigateDeepLink(@NonNull String uriString) {
        try {
            navigateDeepLink(uriString);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showSnackbar(getString(R.string.core_guard_destination_unavailable));
        }
    }
}
