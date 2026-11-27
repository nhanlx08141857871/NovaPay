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

import com.google.android.material.tabs.TabLayout;
import com.novabank.novapay.R;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.core.ui.BaseFragment;
import com.novabank.novapay.core.ui.StateLayoutHelper;
import com.novabank.novapay.core.util.CurrencyFormatter;
import com.novabank.novapay.databinding.FragmentServiceBookingBinding;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.transaction.TransactionDraft;
import com.novabank.novapay.domain.model.utility.ServiceItem;
import com.novabank.novapay.presentation.customer.otp.OtpVerificationDialogFragment;
import com.novabank.novapay.presentation.customer.transfer.ReceiptFragment;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ServiceBookingFragment extends BaseFragment<FragmentServiceBookingBinding> {

    private ServiceBookingViewModel viewModel;
    private StateLayoutHelper stateLayoutHelper;
    private ServiceCatalogAdapter catalogAdapter;
    private MyOrderAdapter orderAdapter;

    @NonNull
    @Override
    protected FragmentServiceBookingBinding inflateBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentServiceBookingBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(ServiceBookingViewModel.class);
        FragmentServiceBookingBinding b = getBinding();

        b.stateEmpty.tvEmptyTitle.setText(R.string.money_no_active_checking_title);
        b.stateEmpty.tvEmptySubtitle.setText(R.string.money_no_active_checking_desc);

        stateLayoutHelper = new StateLayoutHelper(
                b.contentContainer,
                b.stateLoading,
                b.stateEmpty,
                b.stateError,
                () -> viewModel.loadInitialData()
        );

        catalogAdapter = new ServiceCatalogAdapter(item -> viewModel.selectServiceItem(item));
        orderAdapter = new MyOrderAdapter(order -> {
            if (order.getTxId() != null && !order.getTxId().isEmpty()) {
                navigateToReceipt(order.getTxId());
            }
        });

        b.rvServicesOrOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvServicesOrOrders.setAdapter(catalogAdapter);

        b.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        b.actvSourceAccount.setOnItemClickListener((parent, view, position, id) ->
                viewModel.selectSourceAccount(position));

        b.tabServiceCategory.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewModel.onTabSelected(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        b.btnQtyMinus.setOnClickListener(v -> viewModel.changeQuantity(-1));
        b.btnQtyPlus.setOnClickListener(v -> viewModel.changeQuantity(1));

        b.btnCheckoutPay.setOnClickListener(v -> {
            ServiceBookingUiState data = viewModel.getUiState().getValue() != null
                    ? viewModel.getUiState().getValue().getData()
                    : null;
            if (data == null || data.getSelectedItem() == null) {
                return;
            }
            ServiceItem item = data.getSelectedItem();
            String note = getString(
                    R.string.money_services_default_note,
                    item.getTitle(),
                    data.getQuantity()
            );
            viewModel.submitServiceBooking(note);
        });

        getParentFragmentManager().setFragmentResultListener(
                Constants.REQ_OTP_VERIFICATION,
                getViewLifecycleOwner(),
                (requestKey, bundle) -> {
                    boolean verified = bundle.getBoolean(Constants.BUNDLE_OTP_VERIFIED, false);
                    String txId = bundle.getString(Constants.BUNDLE_TX_ID);
                    if (verified && txId != null && !txId.isEmpty()) {
                        navigateToReceipt(txId);
                    }
                }
        );

        viewModel.loadInitialData();
    }

    @Override
    protected void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                return;
            }
            stateLayoutHelper.render(state);
            ServiceBookingUiState data = state.getData();
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

    private void renderUi(@NonNull ServiceBookingUiState data) {
        FragmentServiceBookingBinding b = getBinding();

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

        if (data.isShowingOrdersTab()) {
            b.cardSelectedItemCheckout.setVisibility(View.GONE);
            if (b.rvServicesOrOrders.getAdapter() != orderAdapter) {
                b.rvServicesOrOrders.setAdapter(orderAdapter);
            }
            boolean emptyOrders = data.getMyOrders().isEmpty();
            b.tvServicesListEmpty.setVisibility(emptyOrders ? View.VISIBLE : View.GONE);
            b.tvServicesListEmpty.setText(R.string.money_services_empty_orders);
            b.rvServicesOrOrders.setVisibility(emptyOrders ? View.GONE : View.VISIBLE);
            orderAdapter.submitList(data.getMyOrders());
            return;
        }

        if (b.rvServicesOrOrders.getAdapter() != catalogAdapter) {
            b.rvServicesOrOrders.setAdapter(catalogAdapter);
        }
        boolean emptyCatalog = data.getCatalogItems().isEmpty();
        b.tvServicesListEmpty.setVisibility(emptyCatalog ? View.VISIBLE : View.GONE);
        b.tvServicesListEmpty.setText(R.string.money_services_empty_catalog);
        b.rvServicesOrOrders.setVisibility(emptyCatalog ? View.GONE : View.VISIBLE);
        catalogAdapter.submitList(data.getCatalogItems());

        ServiceItem selected = data.getSelectedItem();
        if (selected == null) {
            b.cardSelectedItemCheckout.setVisibility(View.GONE);
        } else {
            b.cardSelectedItemCheckout.setVisibility(View.VISIBLE);
            b.tvCheckoutItemTitle.setText(selected.getTitle());
            b.tvCheckoutQuantity.setText(getString(R.string.money_services_quantity_format, data.getQuantity()));
            b.tvCheckoutTotal.setText(getString(
                    R.string.money_services_total_format,
                    CurrencyFormatter.formatVnd(data.getComputedTotalAmountVnd())
            ));
        }
    }

    private void openOtpDialog(@Nullable TransactionDraft draft) {
        if (draft == null || !isAdded()) {
            return;
        }
        OtpVerificationDialogFragment.newInstance(draft)
                .show(getParentFragmentManager(), OtpVerificationDialogFragment.TAG);
    }

    private void navigateToReceipt(@NonNull String txId) {
        Bundle args = new Bundle();
        args.putString(ReceiptFragment.ARG_TX_ID, txId);
        NavHostFragment.findNavController(this).navigate(R.id.receiptFragment, args);
    }
}
