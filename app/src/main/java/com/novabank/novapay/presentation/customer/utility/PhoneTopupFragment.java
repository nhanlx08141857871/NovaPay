package com.novabank.novapay.presentation.customer.utility;

import android.os.Bundle;
import android.view.LayoutInflater;
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
import com.novabank.novapay.databinding.FragmentPhoneTopupBinding;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.transaction.TransactionDraft;
import com.novabank.novapay.presentation.customer.otp.OtpVerificationDialogFragment;
import com.novabank.novapay.presentation.customer.transfer.ReceiptFragment;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Màn hình Nạp tiền điện thoại di động (novapay://topup).
 */
@AndroidEntryPoint
public class PhoneTopupFragment extends BaseFragment<FragmentPhoneTopupBinding> {

    private PhoneTopupViewModel viewModel;
    private StateLayoutHelper stateLayoutHelper;

    @NonNull
    @Override
    protected FragmentPhoneTopupBinding inflateBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentPhoneTopupBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(PhoneTopupViewModel.class);
        FragmentPhoneTopupBinding b = getBinding();

        b.stateEmpty.tvEmptyTitle.setText(R.string.money_no_active_checking_title);
        b.stateEmpty.tvEmptySubtitle.setText(R.string.money_no_active_checking_desc);

        stateLayoutHelper = new StateLayoutHelper(
                b.contentContainer,
                b.stateLoading,
                b.stateEmpty,
                b.stateError,
                () -> viewModel.loadAccounts()
        );

        b.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        b.actvSourceAccount.setOnItemClickListener((parent, view, position, id) ->
                viewModel.selectSourceAccount(position));

        b.chipGroupCarrier.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chipCarrierVinaphone) {
                viewModel.selectCarrier(getString(R.string.money_topup_carrier_vinaphone));
            } else if (checkedId == R.id.chipCarrierMobifone) {
                viewModel.selectCarrier(getString(R.string.money_topup_carrier_mobifone));
            } else if (checkedId == R.id.chipCarrierVietnamobile) {
                viewModel.selectCarrier(getString(R.string.money_topup_carrier_vietnamobile));
            } else {
                viewModel.selectCarrier(getString(R.string.money_topup_carrier_viettel));
            }
        });

        b.chipGroupDenomination.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chipDenom20k) {
                viewModel.selectDenomination(20_000L);
            } else if (checkedId == R.id.chipDenom50k) {
                viewModel.selectDenomination(50_000L);
            } else if (checkedId == R.id.chipDenom200k) {
                viewModel.selectDenomination(200_000L);
            } else if (checkedId == R.id.chipDenom500k) {
                viewModel.selectDenomination(500_000L);
            } else {
                viewModel.selectDenomination(100_000L);
            }
        });

        b.btnSubmitTopup.setOnClickListener(v -> {
            String phone = b.etPhoneNumber.getText() != null
                    ? b.etPhoneNumber.getText().toString().trim()
                    : "";
            PhoneTopupUiState current = viewModel.getUiState().getValue() != null
                    ? viewModel.getUiState().getValue().getData()
                    : null;
            String carrier = current != null
                    ? current.getSelectedCarrier()
                    : getString(R.string.money_topup_carrier_viettel);
            String note = getString(R.string.money_topup_default_note, phone, carrier);
            viewModel.submitTopup(phone, note, getString(R.string.money_topup_invalid_phone));
        });

        getParentFragmentManager().setFragmentResultListener(
                Constants.REQ_OTP_VERIFICATION,
                getViewLifecycleOwner(),
                (requestKey, bundle) -> {
                    boolean verified = bundle.getBoolean(Constants.BUNDLE_OTP_VERIFIED, false);
                    String txId = bundle.getString(Constants.BUNDLE_TX_ID);
                    if (verified && txId != null && !txId.isEmpty()) {
                        Bundle receiptArgs = new Bundle();
                        receiptArgs.putString(ReceiptFragment.ARG_TX_ID, txId);
                        NavHostFragment.findNavController(this)
                                .navigate(R.id.receiptFragment, receiptArgs);
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
            PhoneTopupUiState data = state.getData();
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
    }

    private void renderUi(@NonNull PhoneTopupUiState data) {
        FragmentPhoneTopupBinding b = getBinding();

        List<String> sourceLabels = new ArrayList<>();
        for (Account acc : data.getCheckingAccounts()) {
            sourceLabels.add(getString(
                    R.string.money_source_account_format,
                    acc.getAccountNumber(),
                    CurrencyFormatter.formatVnd(acc.getBalance())
            ));
        }
        b.actvSourceAccount.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                sourceLabels
        ));
        if (!sourceLabels.isEmpty() && data.getSelectedSourceIndex() < sourceLabels.size()) {
            b.actvSourceAccount.setText(sourceLabels.get(data.getSelectedSourceIndex()), false);
        }

        b.tvSelectedTopupAmount.setText(getString(
                R.string.money_topup_selected_amount,
                CurrencyFormatter.formatVnd(data.getSelectedAmountVnd())
        ));
    }

    private void openOtpDialog(@Nullable TransactionDraft draft) {
        if (draft == null || !isAdded()) {
            return;
        }
        OtpVerificationDialogFragment.newInstance(draft)
                .show(getParentFragmentManager(), OtpVerificationDialogFragment.TAG);
    }
}
