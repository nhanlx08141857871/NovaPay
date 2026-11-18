package com.novabank.novapay.presentation.customer.payment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.novabank.novapay.R;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.core.ui.BaseFragment;
import com.novabank.novapay.core.ui.StateLayoutHelper;
import com.novabank.novapay.core.util.CurrencyFormatter;
import com.novabank.novapay.core.util.DateUtils;
import com.novabank.novapay.databinding.FragmentDepositWithdrawBinding;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.account.MortgageInfo;
import com.novabank.novapay.domain.model.transaction.TransactionDraft;
import com.novabank.novapay.presentation.customer.otp.OtpVerificationDialogFragment;
import com.novabank.novapay.presentation.customer.transfer.ReceiptFragment;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Màn hình Nạp tiền qua cổng Stripe PaymentSheet (GATEWAY), Rút tiền (WITHDRAW)
 * và Thanh toán kỳ hạn khoản vay thế chấp (MORTGAGE_PAYMENT).
 */
@AndroidEntryPoint
public class DepositWithdrawFragment extends BaseFragment<FragmentDepositWithdrawBinding> {

    public static final String ARG_MODE = "mode";
    public static final String ARG_ACCOUNT_ID = "accountId";

    private DepositWithdrawViewModel viewModel;
    private StateLayoutHelper stateLayoutHelper;
    private StripeGatewayHelper stripeGatewayHelper;
    private String lastAutoFilledMortgageId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Đăng ký ActivityResultLauncher của Stripe PaymentSheet trước khi Fragment sang STARTED
        stripeGatewayHelper = new StripeGatewayHelper(this, new StripeGatewayHelper.Callback() {
            @Override
            public void onPaymentCompleted(@NonNull String draftId) {
                if (isAdded() && viewModel != null) {
                    viewModel.finalizeStripeDeposit(draftId);
                }
            }

            @Override
            public void onPaymentCanceled() {
                if (isAdded()) {
                    showSnackbar(getString(R.string.money_stripe_canceled));
                }
            }

            @Override
            public void onPaymentFailed(@NonNull String errorMessage) {
                if (isAdded()) {
                    showSnackbar(getString(R.string.money_stripe_failed, errorMessage));
                }
            }
        });
    }

    @NonNull
    @Override
    protected FragmentDepositWithdrawBinding inflateBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentDepositWithdrawBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(DepositWithdrawViewModel.class);
        FragmentDepositWithdrawBinding b = getBinding();

        b.stateEmpty.tvEmptyTitle.setText(R.string.money_no_active_checking_title);
        b.stateEmpty.tvEmptySubtitle.setText(R.string.money_no_active_checking_desc);

        stateLayoutHelper = new StateLayoutHelper(
                b.contentContainer,
                b.stateLoading,
                b.stateEmpty,
                b.stateError,
                () -> viewModel.loadAccounts()
        );

        Bundle args = getArguments();
        if (args != null) {
            viewModel.applyInitialArgs(
                    args.getString(ARG_MODE),
                    args.getString(ARG_ACCOUNT_ID)
            );
        }

        b.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());

        b.togglePaymentMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            if (checkedId == R.id.btnTabDeposit) {
                viewModel.setMode(DepositWithdrawUiState.Mode.GATEWAY_DEPOSIT);
            } else if (checkedId == R.id.btnTabWithdraw) {
                viewModel.setMode(DepositWithdrawUiState.Mode.WITHDRAW);
            } else if (checkedId == R.id.btnTabMortgage) {
                viewModel.setMode(DepositWithdrawUiState.Mode.MORTGAGE_PAYMENT);
            }
        });

        b.actvCheckingAccount.setOnItemClickListener((parent, view, position, id) ->
                viewModel.selectCheckingAccount(position));
        b.actvMortgageAccount.setOnItemClickListener((parent, view, position, id) -> {
            lastAutoFilledMortgageId = null;
            viewModel.selectMortgageAccount(position);
        });

        b.chipDw200k.setOnClickListener(v -> b.etAmount.setText(String.valueOf(200_000L)));
        b.chipDw2m.setOnClickListener(v -> b.etAmount.setText(String.valueOf(2_000_000L)));
        b.chipDw6m.setOnClickListener(v -> b.etAmount.setText(String.valueOf(6_000_000L)));
        b.chipDw10m.setOnClickListener(v -> b.etAmount.setText(String.valueOf(10_000_000L)));

        b.btnSubmitPayment.setOnClickListener(v -> {
            long amount = CurrencyFormatter.parseVndDigits(
                    b.etAmount.getText() != null ? b.etAmount.getText().toString() : ""
            );
            String withdrawDest = b.etWithdrawDestination.getText() != null
                    ? b.etWithdrawDestination.getText().toString()
                    : "";
            String note = b.etNote.getText() != null
                    ? b.etNote.getText().toString()
                    : "";
            viewModel.submitAction(
                    amount,
                    withdrawDest,
                    note,
                    getString(R.string.money_dw_mortgage_empty)
            );
        });

        getParentFragmentManager().setFragmentResultListener(
                Constants.REQ_OTP_VERIFICATION,
                getViewLifecycleOwner(),
                (requestKey, bundle) -> {
                    boolean verified = bundle.getBoolean(Constants.BUNDLE_OTP_VERIFIED, false);
                    if (!verified) {
                        return;
                    }
                    String draftId = bundle.getString(Constants.BUNDLE_DRAFT_ID, "");
                    String clientSecret = bundle.getString(Constants.BUNDLE_CLIENT_SECRET);
                    String txId = bundle.getString(Constants.BUNDLE_TX_ID);

                    if (clientSecret != null && !clientSecret.trim().isEmpty()) {
                        // Luồng GATEWAY: OTP đã xác thực -> Mở Stripe PaymentSheet
                        boolean launched = stripeGatewayHelper.presentPaymentSheet(
                                requireContext(),
                                getString(R.string.money_stripe_publishable_key),
                                getString(R.string.money_stripe_merchant_name),
                                draftId,
                                clientSecret
                        );
                        if (!launched) {
                            showSnackbar(getString(R.string.money_stripe_key_unconfigured));
                        }
                    } else if (txId != null && !txId.trim().isEmpty()) {
                        navigateToReceipt(txId);
                    }
                }
        );

        viewModel.loadAccounts();
    }

    @Override
    protected void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                return;
            }
            stateLayoutHelper.render(state);
            DepositWithdrawUiState data = state.getData();
            if (data != null) {
                renderUi(data);
            }
        });

        viewModel.getMessageEvent().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                showSnackbar(msg);
            }
        });

        viewModel.getDraftCreatedEvent().observe(getViewLifecycleOwner(), this::openOtpDialog);
        viewModel.getNavigateReceiptEvent().observe(getViewLifecycleOwner(), this::navigateToReceipt);
    }

    private void renderUi(@NonNull DepositWithdrawUiState data) {
        FragmentDepositWithdrawBinding b = getBinding();

        int expectedToggleId = switch (data.getMode()) {
            case GATEWAY_DEPOSIT -> R.id.btnTabDeposit;
            case WITHDRAW -> R.id.btnTabWithdraw;
            case MORTGAGE_PAYMENT -> R.id.btnTabMortgage;
        };
        if (b.togglePaymentMode.getCheckedButtonId() != expectedToggleId) {
            b.togglePaymentMode.check(expectedToggleId);
        }

        List<String> checkingLabels = new ArrayList<>();
        for (Account acc : data.getCheckingAccounts()) {
            checkingLabels.add(getString(
                    R.string.money_source_account_format,
                    acc.getAccountNumber(),
                    CurrencyFormatter.formatVnd(acc.getBalance())
            ));
        }
        b.actvCheckingAccount.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                checkingLabels
        ));
        if (!checkingLabels.isEmpty() && data.getSelectedCheckingIndex() < checkingLabels.size()) {
            b.actvCheckingAccount.setText(checkingLabels.get(data.getSelectedCheckingIndex()), false);
        }

        switch (data.getMode()) {
            case GATEWAY_DEPOSIT -> {
                b.tilCheckingAccount.setHint(getString(R.string.money_dw_deposit_Account_label));
                b.tvModeDescription.setText(R.string.money_dw_deposit_subtitle);
                b.tilWithdrawDestination.setVisibility(View.GONE);
                b.layoutMortgageSection.setVisibility(View.GONE);
                b.chipGroupAmounts.setVisibility(View.VISIBLE);
                b.btnSubmitPayment.setText(R.string.money_dw_action_deposit);
            }
            case WITHDRAW -> {
                b.tilCheckingAccount.setHint(getString(R.string.money_source_account_label));
                b.tvModeDescription.setText(R.string.money_dw_withdraw_subtitle);
                b.tilWithdrawDestination.setVisibility(View.VISIBLE);
                b.layoutMortgageSection.setVisibility(View.GONE);
                b.chipGroupAmounts.setVisibility(View.VISIBLE);
                b.btnSubmitPayment.setText(R.string.money_dw_action_withdraw);
            }
            case MORTGAGE_PAYMENT -> {
                b.tilCheckingAccount.setHint(getString(R.string.money_source_account_label));
                b.tvModeDescription.setText(R.string.money_high_value_security_hint);
                b.tilWithdrawDestination.setVisibility(View.GONE);
                b.layoutMortgageSection.setVisibility(View.VISIBLE);
                b.chipGroupAmounts.setVisibility(View.GONE);
                b.btnSubmitPayment.setText(R.string.money_dw_action_mortgage);
                renderMortgageDetails(data);
            }
        }
    }

    private void renderMortgageDetails(@NonNull DepositWithdrawUiState data) {
        FragmentDepositWithdrawBinding b = getBinding();
        List<Account> mortgages = data.getMortgageAccounts();
        if (mortgages.isEmpty()) {
            b.tvMortgageSummary.setText(R.string.money_dw_mortgage_empty);
            return;
        }

        List<String> mortgageLabels = new ArrayList<>();
        for (Account m : mortgages) {
            long outstanding = m.getMortgage() != null
                    ? m.getMortgage().getOutstandingBalance()
                    : m.getBalance();
            mortgageLabels.add(getString(
                    R.string.money_dw_mortgage_item_format,
                    m.getAccountNumber(),
                    CurrencyFormatter.formatVnd(outstanding)
            ));
        }
        b.actvMortgageAccount.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                mortgageLabels
        ));
        if (data.getSelectedMortgageIndex() < mortgageLabels.size()) {
            b.actvMortgageAccount.setText(mortgageLabels.get(data.getSelectedMortgageIndex()), false);
        }

        Account selectedMortgage = data.getSelectedMortgageAccount();
        if (selectedMortgage != null && selectedMortgage.getMortgage() != null) {
            MortgageInfo info = selectedMortgage.getMortgage();
            String freqLabel = MortgageInfo.FREQUENCY_BIWEEKLY.equalsIgnoreCase(info.getPaymentFrequency())
                    ? getString(R.string.money_dw_mortgage_freq_biweekly)
                    : getString(R.string.money_dw_mortgage_freq_monthly);
            String dueStr = info.getNextDueDate() != null
                    ? DateUtils.formatDate(info.getNextDueDate())
                    : "-";
            b.tvMortgageSummary.setText(getString(
                    R.string.money_dw_mortgage_summary,
                    CurrencyFormatter.formatVnd(info.getOutstandingBalance()),
                    CurrencyFormatter.formatVnd(info.getInstallmentAmount()),
                    freqLabel,
                    dueStr
            ));

            if (!selectedMortgage.getAccountId().equals(lastAutoFilledMortgageId)) {
                lastAutoFilledMortgageId = selectedMortgage.getAccountId();
                long suggestedAmount = info.getInstallmentAmount() > 0L
                        ? Math.min(info.getInstallmentAmount(), info.getOutstandingBalance())
                        : info.getOutstandingBalance();
                if (suggestedAmount > 0L) {
                    b.etAmount.setText(String.valueOf(suggestedAmount));
                }
            }
        }
    }

    private void openOtpDialog(@Nullable TransactionDraft draft) {
        if (draft == null || !isAdded()) {
            return;
        }
        OtpVerificationDialogFragment.newInstance(draft)
                .show(getParentFragmentManager(), OtpVerificationDialogFragment.TAG);
    }

    private void navigateToReceipt(@Nullable String txId) {
        if (txId == null || txId.trim().isEmpty() || !isAdded()) {
            return;
        }
        Bundle args = new Bundle();
        args.putString(ReceiptFragment.ARG_TX_ID, txId.trim());
        NavHostFragment.findNavController(this).navigate(R.id.receiptFragment, args);
    }
}
