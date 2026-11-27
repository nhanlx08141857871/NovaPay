package com.novabank.novapay.presentation.customer.transfer;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.novabank.novapay.R;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.core.ui.BaseFragment;
import com.novabank.novapay.core.ui.StateLayoutHelper;
import com.novabank.novapay.core.util.CurrencyFormatter;
import com.novabank.novapay.databinding.FragmentTransferBinding;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.bank.Bank;
import com.novabank.novapay.domain.model.bank.Beneficiary;
import com.novabank.novapay.domain.model.transaction.AccountLookupResult;
import com.novabank.novapay.domain.model.transaction.TransactionDraft;
import com.novabank.novapay.presentation.customer.otp.OtpVerificationDialogFragment;
import com.novabank.novapay.presentation.customer.payment.DepositWithdrawFragment;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TransferFragment extends BaseFragment<FragmentTransferBinding> {

    private TransferViewModel viewModel;
    private StateLayoutHelper stateLayoutHelper;
    private BeneficiaryAdapter beneficiaryAdapter;
    private boolean deepLinkConsumed = false;
    private String pendingBankCodeFromArgs;

    @NonNull
    @Override
    protected FragmentTransferBinding inflateBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentTransferBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(TransferViewModel.class);
        FragmentTransferBinding b = getBinding();

        b.stateEmpty.tvEmptyTitle.setText(R.string.money_no_active_checking_title);
        b.stateEmpty.tvEmptySubtitle.setText(R.string.money_no_active_checking_desc);

        stateLayoutHelper = new StateLayoutHelper(
                b.contentContainer,
                b.stateLoading,
                b.stateEmpty,
                b.stateError,
                () -> viewModel.loadInitialData()
        );

        if (redirectMortgagePaymentIfRequested()) {
            return;
        }

        beneficiaryAdapter = new BeneficiaryAdapter(new BeneficiaryAdapter.Listener() {
            @Override
            public void onSelectBeneficiary(@NonNull Beneficiary beneficiary) {
                applyBeneficiaryToForm(beneficiary);
            }

            @Override
            public void onDeleteBeneficiary(@NonNull Beneficiary beneficiary) {
                viewModel.deleteBeneficiary(beneficiary.getId());
                showSnackbar(getString(R.string.money_transfer_beneficiary_deleted));
            }
        });
        b.rvBeneficiaries.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        b.rvBeneficiaries.setAdapter(beneficiaryAdapter);

        b.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        b.btnOpenDepositWithdraw.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.depositWithdrawFragment));
        b.btnScanQr.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.qrScannerFragment));

        b.toggleTransferMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                viewModel.setInterbankMode(checkedId == R.id.btnModeInterbank);
            }
        });

        b.actvSourceAccount.setOnItemClickListener((parent, view, position, id) ->
                viewModel.selectSourceAccount(position));
        b.actvBank.setOnItemClickListener((parent, view, position, id) ->
                viewModel.selectBank(position));

        b.etRecipientAccount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.onRecipientAccountEdited(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        b.etRecipientAccount.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && b.toggleTransferMode.getCheckedButtonId() == R.id.btnModeInternal) {
                String acc = getTextTrimmed(b.etRecipientAccount.getText());
                if (!acc.isEmpty()) {
                    viewModel.lookupInternalRecipient(acc);
                }
            }
        });

        b.btnLookupRecipient.setOnClickListener(v -> {
            String acc = getTextTrimmed(b.etRecipientAccount.getText());
            viewModel.lookupInternalRecipient(acc);
        });

        b.chipAmount100k.setOnClickListener(v -> b.etAmount.setText(String.valueOf(100_000L)));
        b.chipAmount500k.setOnClickListener(v -> b.etAmount.setText(String.valueOf(500_000L)));
        b.chipAmount1m.setOnClickListener(v -> b.etAmount.setText(String.valueOf(1_000_000L)));
        b.chipAmount6m.setOnClickListener(v -> b.etAmount.setText(String.valueOf(6_000_000L)));

        b.cbSaveBeneficiary.setOnCheckedChangeListener((buttonView, isChecked) ->
                b.tilAlias.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        b.btnSubmitTransfer.setOnClickListener(v -> submitTransfer());

        registerResultListeners();
        applyQrDeepLinkArgsIfPresent();
        viewModel.loadInitialData();
    }

    private boolean redirectMortgagePaymentIfRequested() {
        Bundle args = getArguments();
        if (args == null) {
            return false;
        }
        String typeArg = args.getString("type");
        if ("MORTGAGE_PAYMENT".equalsIgnoreCase(typeArg)) {
            Bundle destArgs = new Bundle();
            destArgs.putString(DepositWithdrawFragment.ARG_MODE, "MORTGAGE_PAYMENT");
            destArgs.putString(DepositWithdrawFragment.ARG_ACCOUNT_ID, args.getString("accountId"));
            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.transferFragment, true)
                    .build();
            NavHostFragment.findNavController(this)
                    .navigate(R.id.depositWithdrawFragment, destArgs, navOptions);
            return true;
        }
        return false;
    }

    private void registerResultListeners() {
        getParentFragmentManager().setFragmentResultListener(
                QrScannerFragment.REQ_QR_SCAN_RESULT,
                getViewLifecycleOwner(),
                (requestKey, bundle) -> {
                    String acc = bundle.getString(QrScannerFragment.KEY_QR_ACCOUNT_NUMBER, "");
                    String bankCode = bundle.getString(QrScannerFragment.KEY_QR_BANK_CODE);
                    String recName = bundle.getString(QrScannerFragment.KEY_QR_RECIPIENT_NAME);
                    long amount = bundle.getLong(QrScannerFragment.KEY_QR_AMOUNT, 0L);
                    String note = bundle.getString(QrScannerFragment.KEY_QR_NOTE);

                    applyScannedQrFields(acc, bankCode, recName, amount, note);
                    showSnackbar(getString(R.string.money_qr_applied_success));
                }
        );

        getParentFragmentManager().setFragmentResultListener(
                Constants.REQ_OTP_VERIFICATION,
                getViewLifecycleOwner(),
                (requestKey, bundle) -> {
                    boolean verified = bundle.getBoolean(Constants.BUNDLE_OTP_VERIFIED, false);
                    String txId = bundle.getString(Constants.BUNDLE_TX_ID);
                    if (verified && txId != null && !txId.isEmpty()) {
                        viewModel.persistPendingBeneficiaryIfNeeded();
                        Bundle receiptArgs = new Bundle();
                        receiptArgs.putString(ReceiptFragment.ARG_TX_ID, txId);
                        NavHostFragment.findNavController(this)
                                .navigate(R.id.receiptFragment, receiptArgs);
                    }
                }
        );
    }

    private void applyQrDeepLinkArgsIfPresent() {
        if (deepLinkConsumed) {
            return;
        }
        Bundle args = getArguments();
        if (args == null) {
            return;
        }
        String accountNumber = args.getString("accountNumber");
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            return;
        }
        deepLinkConsumed = true;
        String bankCode = args.getString("bankCode");
        String recipientName = args.getString("recipientName");
        long amount = CurrencyFormatter.parseVndDigits(args.getString("amount"));
        String note = args.getString("note");
        applyScannedQrFields(accountNumber, bankCode, recipientName, amount, note);
    }

    private void applyScannedQrFields(
            @NonNull String accountNumber,
            @Nullable String bankCode,
            @Nullable String recipientName,
            long amount,
            @Nullable String note
    ) {
        FragmentTransferBinding b = getBinding();
        boolean isInternal = bankCode == null
                || bankCode.trim().isEmpty()
                || "NOVAPAY".equalsIgnoreCase(bankCode.trim());

        b.toggleTransferMode.check(isInternal ? R.id.btnModeInternal : R.id.btnModeInterbank);
        viewModel.setInterbankMode(!isInternal);

        b.etRecipientAccount.setText(accountNumber.trim());
        if (amount > 0L) {
            b.etAmount.setText(String.valueOf(amount));
        }
        if (note != null && !note.trim().isEmpty()) {
            b.etNote.setText(note.trim());
        }

        if (isInternal) {
            viewModel.lookupInternalRecipient(accountNumber.trim());
        } else {
            pendingBankCodeFromArgs = bankCode.trim();
            viewModel.selectBankByCode(pendingBankCodeFromArgs);
            if (recipientName != null) {
                b.etRecipientName.setText(recipientName.trim());
            }
        }
    }

    private void applyBeneficiaryToForm(@NonNull Beneficiary beneficiary) {
        FragmentTransferBinding b = getBinding();
        boolean internal = beneficiary.isInternal();
        b.toggleTransferMode.check(internal ? R.id.btnModeInternal : R.id.btnModeInterbank);
        viewModel.setInterbankMode(!internal);
        b.etRecipientAccount.setText(beneficiary.getAccountNumber());

        if (internal) {
            viewModel.lookupInternalRecipient(beneficiary.getAccountNumber());
        } else {
            viewModel.selectBankByCode(beneficiary.getBankCode());
            b.etRecipientName.setText(beneficiary.getName());
        }
    }

    @Override
    protected void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                return;
            }
            stateLayoutHelper.render(state);
            TransferUiState data = state.getData();
            if (data != null) {
                renderTransferState(data);
            }
        });

        viewModel.getMessageEvent().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                showSnackbar(msg);
            }
        });

        viewModel.getDraftCreatedEvent().observe(getViewLifecycleOwner(), this::openOtpDialog);
    }

    private void renderTransferState(@NonNull TransferUiState data) {
        FragmentTransferBinding b = getBinding();

        List<String> sourceLabels = new ArrayList<>();
        for (Account acc : data.getCheckingAccounts()) {
            sourceLabels.add(getString(
                    R.string.money_source_account_format,
                    acc.getAccountNumber(),
                    CurrencyFormatter.formatVnd(acc.getBalance())
            ));
        }
        ArrayAdapter<String> sourceAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                sourceLabels
        );
        b.actvSourceAccount.setAdapter(sourceAdapter);
        if (!sourceLabels.isEmpty() && data.getSelectedSourceIndex() < sourceLabels.size()) {
            b.actvSourceAccount.setText(sourceLabels.get(data.getSelectedSourceIndex()), false);
        }

        List<Beneficiary> beneficiaries = data.getBeneficiaries();
        b.tvBeneficiariesEmpty.setVisibility(beneficiaries.isEmpty() ? View.VISIBLE : View.GONE);
        b.rvBeneficiaries.setVisibility(beneficiaries.isEmpty() ? View.GONE : View.VISIBLE);
        beneficiaryAdapter.submitList(beneficiaries);

        if (!data.getBanks().isEmpty()) {
            if (pendingBankCodeFromArgs != null) {
                String code = pendingBankCodeFromArgs;
                pendingBankCodeFromArgs = null;
                viewModel.selectBankByCode(code);
            }
            List<String> bankLabels = new ArrayList<>();
            for (Bank bank : data.getBanks()) {
                bankLabels.add(bank.getBankCode() + " - " + bank.getName());
            }
            ArrayAdapter<String> bankAdapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    bankLabels
            );
            b.actvBank.setAdapter(bankAdapter);
            if (data.getSelectedBankIndex() < bankLabels.size()) {
                b.actvBank.setText(bankLabels.get(data.getSelectedBankIndex()), false);
            }
        }

        boolean interbank = data.isInterbankMode();
        b.tilBank.setVisibility(interbank ? View.VISIBLE : View.GONE);
        b.tilRecipientName.setVisibility(interbank ? View.VISIBLE : View.GONE);
        b.btnLookupRecipient.setVisibility(interbank ? View.GONE : View.VISIBLE);

        if (!interbank) {
            if (data.isLookingUpRecipient()) {
                b.tvLookupStatus.setVisibility(View.VISIBLE);
                b.tvLookupStatus.setText(R.string.money_transfer_lookup_loading);
                b.tvLookupStatus.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.novapay_warning_pending)
                );
            } else if (data.getLookupResult() != null) {
                AccountLookupResult res = data.getLookupResult();
                b.tvLookupStatus.setVisibility(View.VISIBLE);
                b.tvLookupStatus.setText(getString(R.string.money_transfer_lookup_verified, res.getOwnerName()));
                b.tvLookupStatus.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.novapay_income_positive)
                );
            } else if (data.getLookupError() != null && !data.getLookupError().isEmpty()) {
                b.tvLookupStatus.setVisibility(View.VISIBLE);
                b.tvLookupStatus.setText(data.getLookupError());
                b.tvLookupStatus.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.novapay_expense_negative)
                );
            } else {
                b.tvLookupStatus.setVisibility(View.GONE);
            }
        } else {
            b.tvLookupStatus.setVisibility(View.GONE);
        }
    }

    private void submitTransfer() {
        FragmentTransferBinding b = getBinding();
        String recipientAccount = getTextTrimmed(b.etRecipientAccount.getText());
        String recipientName = getTextTrimmed(b.etRecipientName.getText());
        long amount = CurrencyFormatter.parseVndDigits(getTextTrimmed(b.etAmount.getText()));
        String note = getTextTrimmed(b.etNote.getText());
        boolean saveBen = b.cbSaveBeneficiary.isChecked();
        String alias = getTextTrimmed(b.etAlias.getText());

        viewModel.initiateTransfer(
                recipientAccount,
                recipientName,
                amount,
                note,
                saveBen,
                alias,
                getString(R.string.money_transfer_lookup_required)
        );
    }

    private void openOtpDialog(@Nullable TransactionDraft draft) {
        if (draft == null || !isAdded()) {
            return;
        }
        OtpVerificationDialogFragment.newInstance(draft)
                .show(getParentFragmentManager(), OtpVerificationDialogFragment.TAG);
    }

    @NonNull
    private static String getTextTrimmed(@Nullable Editable editable) {
        return editable != null ? editable.toString().trim() : "";
    }
}
