package com.novabank.novapay.presentation.customer.utility;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.novabank.novapay.R;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.core.ui.BaseFragment;
import com.novabank.novapay.core.ui.StateLayoutHelper;
import com.novabank.novapay.core.util.CurrencyFormatter;
import com.novabank.novapay.databinding.FragmentUtilityBillBinding;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.transaction.TransactionDraft;
import com.novabank.novapay.domain.model.utility.UtilityBill;
import com.novabank.novapay.domain.model.utility.UtilityBillType;
import com.novabank.novapay.presentation.customer.otp.OtpVerificationDialogFragment;
import com.novabank.novapay.presentation.customer.transfer.ReceiptFragment;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Màn hình Tra cứu và Thanh toán Hóa đơn Điện / Nước (novapay://bills).
 */
@AndroidEntryPoint
public class UtilityBillFragment extends BaseFragment<FragmentUtilityBillBinding> {

    private UtilityBillViewModel viewModel;
    private StateLayoutHelper stateLayoutHelper;
    private UtilityBillAdapter billAdapter;

    @NonNull
    @Override
    protected FragmentUtilityBillBinding inflateBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentUtilityBillBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(UtilityBillViewModel.class);
        FragmentUtilityBillBinding b = getBinding();

        b.stateEmpty.tvEmptyTitle.setText(R.string.money_no_active_checking_title);
        b.stateEmpty.tvEmptySubtitle.setText(R.string.money_no_active_checking_desc);

        stateLayoutHelper = new StateLayoutHelper(
                b.contentContainer,
                b.stateLoading,
                b.stateEmpty,
                b.stateError,
                () -> viewModel.loadAccounts()
        );

        billAdapter = new UtilityBillAdapter(bill -> {
            String note = getString(
                    R.string.money_bills_default_note,
                    bill.getCustomerCode(),
                    bill.getPeriod()
            );
            viewModel.initiateBillPayment(bill, note);
        });
        b.rvUtilityBills.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvUtilityBills.setAdapter(billAdapter);

        b.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        b.actvSourceAccount.setOnItemClickListener((parent, view, position, id) ->
                viewModel.selectSourceAccount(position));

        b.toggleBillType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            if (checkedId == R.id.btnFilterElectric) {
                viewModel.setFilterType(UtilityBillType.ELECTRIC);
            } else if (checkedId == R.id.btnFilterWater) {
                viewModel.setFilterType(UtilityBillType.WATER);
            } else {
                viewModel.setFilterType(null);
            }
        });

        b.btnSearchBills.setOnClickListener(v -> {
            String code = b.etCustomerCode.getText() != null
                    ? b.etCustomerCode.getText().toString()
                    : "";
            viewModel.searchBills(code);
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
            UtilityBillUiState data = state.getData();
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

    private void renderUi(@NonNull UtilityBillUiState data) {
        FragmentUtilityBillBinding b = getBinding();

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

        List<UtilityBill> bills = data.getBills();
        if (bills.isEmpty()) {
            b.rvUtilityBills.setVisibility(View.GONE);
            b.tvBillsEmptyHint.setVisibility(View.VISIBLE);
            b.tvBillsEmptyHint.setText(data.isHasSearched()
                    ? R.string.money_bills_empty_result
                    : R.string.money_bills_empty_prompt);
        } else {
            b.tvBillsEmptyHint.setVisibility(View.GONE);
            b.rvUtilityBills.setVisibility(View.VISIBLE);
            billAdapter.submitList(bills);
        }
    }

    private void openOtpDialog(@Nullable TransactionDraft draft) {
        if (draft == null || !isAdded()) {
            return;
        }
        OtpVerificationDialogFragment.newInstance(draft)
                .show(getParentFragmentManager(), OtpVerificationDialogFragment.TAG);
    }
}
