package com.novabank.novapay.presentation.officer;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.novabank.novapay.R;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.core.ui.BaseFragment;
import com.novabank.novapay.core.ui.StateLayoutHelper;
import com.novabank.novapay.databinding.DialogEditCustomerBinding;
import com.novabank.novapay.databinding.FragmentCustomerManagementBinding;
import com.novabank.novapay.domain.model.user.CustomerProfile;
import com.novabank.novapay.domain.model.user.CustomerValidation;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CustomerManagementFragment extends BaseFragment<FragmentCustomerManagementBinding> {

    private CustomerManagementViewModel viewModel;
    private StateLayoutHelper stateLayoutHelper;
    private CustomerAdapter adapter;

    @NonNull
    @Override
    protected FragmentCustomerManagementBinding inflateBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentCustomerManagementBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(CustomerManagementViewModel.class);
        FragmentCustomerManagementBinding b = getBinding();

        stateLayoutHelper = new StateLayoutHelper(
                b.contentContainer,
                b.stateLoading,
                b.stateEmpty,
                b.stateError,
                () -> viewModel.loadCustomers()
        );
        b.stateEmpty.tvEmptyTitle.setText(R.string.officer_customers_empty_title);
        b.stateEmpty.tvEmptySubtitle.setText(R.string.officer_customers_empty_subtitle);

        adapter = new CustomerAdapter(new CustomerAdapter.OnCustomerActionListener() {
            @Override
            public void onEditCustomer(@NonNull CustomerProfile customer) {
                showEditDialog(customer);
            }

            @Override
            public void onToggleLock(@NonNull CustomerProfile customer) {
                showToggleLockConfirmation(customer);
            }
        });
        b.contentContainer.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.contentContainer.setAdapter(adapter);

        b.toolbarCustomers.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack()
        );
        b.toolbarCustomers.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_refresh) {
                viewModel.loadCustomers();
                return true;
            }
            return false;
        });

        b.etSearchCustomer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.onSearchQueryChanged(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        b.fabCreateCustomer.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_customerManagement_to_createCustomer)
        );
    }

    @Override
    protected void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null || !isAdded() || getView() == null) {
                return;
            }
            stateLayoutHelper.render(state);
            if (state.getStatus() == UiState.Status.SUCCESS && state.getData() != null) {
                adapter.submitList(state.getData().getCustomers());
            }
        });

        viewModel.getActionInProgress().observe(getViewLifecycleOwner(), inProgress -> {
            if (isAdded() && getView() != null) {
                getBinding().progressCustomerAction.setVisibility(
                        Boolean.TRUE.equals(inProgress) ? View.VISIBLE : View.GONE
                );
            }
        });

        viewModel.getFeedbackMessageEvent().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                showSnackbar(msg);
            }
        });

        viewModel.getFeedbackResEvent().observe(getViewLifecycleOwner(), resId -> {
            if (resId != null) {
                showSnackbar(getString(resId));
            }
        });
    }

    private void showToggleLockConfirmation(@NonNull CustomerProfile customer) {
        boolean isLocked = customer.isLocked();
        int titleRes = isLocked
                ? R.string.officer_customer_unlock_confirm_title
                : R.string.officer_customer_lock_confirm_title;
        int msgRes = isLocked
                ? R.string.officer_customer_unlock_confirm_message
                : R.string.officer_customer_lock_confirm_message;
        int positiveRes = isLocked
                ? R.string.officer_customer_unlock_action
                : R.string.officer_customer_lock_action;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(titleRes)
                .setMessage(getString(msgRes, customer.getFullName()))
                .setNegativeButton(R.string.officer_action_cancel, null)
                .setPositiveButton(positiveRes, (dialog, which) ->
                        viewModel.toggleCustomerLock(
                                customer,
                                R.string.officer_customer_status_locked_success,
                                R.string.officer_customer_status_unlocked_success
                        )
                )
                .show();
    }

    private void showEditDialog(@NonNull CustomerProfile customer) {
        DialogEditCustomerBinding dialogBinding = DialogEditCustomerBinding.inflate(getLayoutInflater());
        dialogBinding.etEditFullName.setText(customer.getFullName());
        dialogBinding.etEditPhone.setText(customer.getPhone());
        dialogBinding.etEditAddress.setText(customer.getAddress());

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.officer_customer_edit_dialog_title)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.officer_action_cancel, null)
                .setPositiveButton(R.string.officer_action_save, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String fullName = dialogBinding.etEditFullName.getText() != null
                            ? dialogBinding.etEditFullName.getText().toString().trim() : "";
                    String phone = dialogBinding.etEditPhone.getText() != null
                            ? dialogBinding.etEditPhone.getText().toString().trim() : "";
                    String address = dialogBinding.etEditAddress.getText() != null
                            ? dialogBinding.etEditAddress.getText().toString().trim() : "";

                    boolean valid = true;
                    if (!CustomerValidation.isValidFullName(fullName)) {
                        dialogBinding.tilEditFullName.setError(getString(R.string.officer_validation_fullname));
                        valid = false;
                    } else {
                        dialogBinding.tilEditFullName.setError(null);
                    }
                    if (!CustomerValidation.isValidPhone(phone)) {
                        dialogBinding.tilEditPhone.setError(getString(R.string.officer_validation_phone));
                        valid = false;
                    } else {
                        dialogBinding.tilEditPhone.setError(null);
                    }
                    if (!CustomerValidation.isValidAddress(address)) {
                        dialogBinding.tilEditAddress.setError(getString(R.string.officer_validation_address));
                        valid = false;
                    } else {
                        dialogBinding.tilEditAddress.setError(null);
                    }

                    if (valid) {
                        viewModel.updateCustomerDetails(
                                customer.getUid(),
                                fullName,
                                phone,
                                address,
                                R.string.officer_customer_update_success
                        );
                        dialog.dismiss();
                    }
                }));

        dialog.show();
    }
}
