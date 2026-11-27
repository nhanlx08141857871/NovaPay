package com.novabank.novapay.presentation.customer.account;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.novabank.novapay.R;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.core.ui.BaseFragment;
import com.novabank.novapay.core.ui.StateLayoutHelper;
import com.novabank.novapay.core.util.CurrencyFormatter;
import com.novabank.novapay.core.util.DateUtils;
import com.novabank.novapay.databinding.DialogSettleSavingBinding;
import com.novabank.novapay.databinding.FragmentSavingDetailBinding;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.account.AccountStatus;
import com.novabank.novapay.domain.model.account.SavingCalculator;
import com.novabank.novapay.domain.model.account.SavingInfo;
import com.novabank.novapay.domain.model.account.SavingProjection;
import com.novabank.novapay.domain.model.account.SavingSettlementPreview;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Màn hình chi tiết tài khoản Tiết kiệm (SAVING):
 * Kết hợp realtime accounts/{id} + interest_rates theo termMonths, hỗ trợ Tất toán & Mở thêm sổ mới.
 */
@AndroidEntryPoint
public class SavingDetailFragment extends BaseFragment<FragmentSavingDetailBinding> {

    private SavingDetailViewModel viewModel;
    private StateLayoutHelper stateLayoutHelper;
    private String accountId;
    private SavingDetailUiState latestState;
    private BottomSheetDialog activeSettleDialog;
    private DialogSettleSavingBinding activeSettleBinding;

    @NonNull
    @Override
    protected FragmentSavingDetailBinding inflateBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentSavingDetailBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(SavingDetailViewModel.class);
        FragmentSavingDetailBinding b = getBinding();

        b.stateEmpty.tvEmptyTitle.setText(R.string.core_saving_title);
        b.stateEmpty.tvEmptySubtitle.setText(R.string.core_account_not_found);

        stateLayoutHelper = new StateLayoutHelper(
                b.contentContainer,
                b.stateLoading,
                b.stateEmpty,
                b.stateError,
                () -> viewModel.observeSaving(accountId)
        );

        b.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        b.btnOpenNewSaving.setOnClickListener(v -> safeNavigateDeepLink(Constants.DEEPLINK_SAVINGS_OPEN));
        b.btnSettleSaving.setOnClickListener(v -> openSettlementBottomSheet());

        Bundle args = getArguments();
        accountId = args != null ? args.getString("accountId") : null;
        viewModel.observeSaving(accountId);
    }

    @Override
    protected void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                return;
            }
            stateLayoutHelper.render(state);
            SavingDetailUiState data = state.getData();
            if (data != null && data.getProjection() != null) {
                latestState = data;
                renderProjection(data);
                if (activeSettleBinding != null) {
                    activeSettleBinding.tilSettlePin.setVisibility(
                            data.hasTransactionPin() ? View.VISIBLE : View.GONE
                    );
                    activeSettleBinding.btnConfirmSettle.setEnabled(!data.isSettling());
                }
            }
        });

        viewModel.getSettleSuccessEvent().observe(getViewLifecycleOwner(), result -> {
            dismissSettleDialog();
            if (result == null) {
                return;
            }
            showSnackbar(getString(
                    R.string.core_saving_settle_success,
                    CurrencyFormatter.formatVnd(result.getTotalPayoutAmount())
            ));
            if (result.getTxId() != null && !result.getTxId().isEmpty()) {
                safeNavigateDeepLink(Constants.DEEPLINK_RECEIPT_PREFIX + Uri.encode(result.getTxId()));
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

    private void renderProjection(@NonNull SavingDetailUiState state) {
        SavingProjection projection = state.getProjection();
        FragmentSavingDetailBinding b = getBinding();
        Account account = projection.getAccount();
        SavingInfo saving = account.getSaving();

        b.tvBalance.setText(CurrencyFormatter.formatVnd(account.getBalance()));
        b.tvAccountNumber.setText(account.getAccountNumber());
        b.tvStatus.setText(resolveStatusLabel(account.getStatus()));
        b.tvMonthlyProfit.setText(CurrencyFormatter.formatVnd(projection.getMonthlyProfit()));
        b.tvAnnualRate.setText(getString(R.string.core_saving_rate_value, projection.getEffectiveAnnualRate()));

        if (saving != null) {
            b.tvPrincipal.setText(CurrencyFormatter.formatVnd(saving.getPrincipal()));
            if (saving.getTermMonths() <= 0) {
                b.tvTermMonths.setText(R.string.core_saving_term_demand);
            } else {
                b.tvTermMonths.setText(getString(R.string.core_saving_term_months, saving.getTermMonths()));
            }
            b.tvOpenedDate.setText(DateUtils.formatDate(saving.getOpenedAt()));
            b.tvMaturityDate.setText(DateUtils.formatDate(saving.getMaturityDate()));
        } else {
            b.tvPrincipal.setText(CurrencyFormatter.formatVnd(account.getBalance()));
            b.tvTermMonths.setText(R.string.core_saving_term_demand);
            b.tvOpenedDate.setText(DateUtils.formatDate(account.getCreatedAt()));
            b.tvMaturityDate.setText("-");
        }

        boolean isActive = account.getStatus() == AccountStatus.ACTIVE;
        b.btnSettleSaving.setVisibility(isActive ? View.VISIBLE : View.GONE);
        b.btnSettleSaving.setEnabled(!state.isSettling());
    }

    private void openSettlementBottomSheet() {
        if (latestState == null || latestState.getProjection() == null) {
            return;
        }
        List<Account> checkingAccounts = latestState.getCheckingAccounts();
        if (checkingAccounts.isEmpty()) {
            showSnackbar(getString(R.string.core_saving_settle_no_checking));
            return;
        }

        dismissSettleDialog();
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        DialogSettleSavingBinding sheetBinding = DialogSettleSavingBinding.inflate(getLayoutInflater());
        dialog.setContentView(sheetBinding.getRoot());

        SavingSettlementPreview preview = SavingCalculator.previewSettlement(
                latestState.getProjection(),
                latestState.getDemandAnnualRate(),
                Instant.now()
        );

        if (preview.isEarlySettlement()) {
            sheetBinding.tvSettlementTypeNotice.setText(getString(
                    R.string.core_saving_settle_type_early,
                    preview.getAppliedAnnualRate(),
                    preview.getDaysHeld()
            ));
            sheetBinding.tvSettlementTypeNotice.setBackgroundResource(R.drawable.bg_np_badge_pending);
            sheetBinding.tvSettlementTypeNotice.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.novapay_warning_pending)
            );
        } else {
            sheetBinding.tvSettlementTypeNotice.setText(R.string.core_saving_settle_type_mature);
            sheetBinding.tvSettlementTypeNotice.setBackgroundResource(R.drawable.bg_np_badge_verified);
            sheetBinding.tvSettlementTypeNotice.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.novapay_income_positive)
            );
        }

        sheetBinding.tvSettlePrincipal.setText(CurrencyFormatter.formatVnd(preview.getPrincipalAmount()));
        sheetBinding.tvSettleInterest.setText(CurrencyFormatter.formatVnd(preview.getInterestAmount()));
        sheetBinding.tvSettleTotalPayout.setText(CurrencyFormatter.formatVnd(preview.getTotalPayoutAmount()));

        List<String> labels = new ArrayList<>();
        for (Account account : checkingAccounts) {
            labels.add(getString(
                    R.string.core_open_saving_source_item,
                    account.getAccountNumber(),
                    CurrencyFormatter.formatVnd(account.getBalance())
            ));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                labels
        );
        sheetBinding.actvTargetChecking.setAdapter(adapter);
        final int[] selectedIndex = {0};
        sheetBinding.actvTargetChecking.setText(labels.get(0), false);
        sheetBinding.actvTargetChecking.setOnItemClickListener((parent, view, position, id) ->
                selectedIndex[0] = position);

        boolean requirePin = latestState.hasTransactionPin();
        sheetBinding.tilSettlePin.setVisibility(requirePin ? View.VISIBLE : View.GONE);

        sheetBinding.btnCancelSettle.setOnClickListener(v -> dialog.dismiss());
        sheetBinding.btnConfirmSettle.setOnClickListener(v -> {
            boolean pinRequiredNow = latestState != null && latestState.hasTransactionPin();
            String pin = sheetBinding.etSettlePin.getText() != null
                    ? sheetBinding.etSettlePin.getText().toString()
                    : null;
            if (pinRequiredNow && (pin == null || !pin.trim().matches("^\\d{6}$"))) {
                sheetBinding.tilSettlePin.setError(getString(R.string.core_open_saving_pin_invalid));
                return;
            }
            sheetBinding.tilSettlePin.setError(null);
            Account target = checkingAccounts.get(Math.max(0, Math.min(selectedIndex[0], checkingAccounts.size() - 1)));
            viewModel.settleSaving(
                    latestState.getProjection().getAccount().getAccountId(),
                    target.getAccountId(),
                    pinRequiredNow ? pin : null
            );
        });

        activeSettleDialog = dialog;
        activeSettleBinding = sheetBinding;
        dialog.setOnDismissListener(d -> {
            if (activeSettleDialog == dialog) {
                activeSettleDialog = null;
                activeSettleBinding = null;
            }
        });
        dialog.show();
    }

    private void dismissSettleDialog() {
        if (activeSettleDialog != null && activeSettleDialog.isShowing()) {
            activeSettleDialog.dismiss();
        }
        activeSettleDialog = null;
        activeSettleBinding = null;
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

    @Override
    public void onDestroyView() {
        dismissSettleDialog();
        super.onDestroyView();
    }
}
