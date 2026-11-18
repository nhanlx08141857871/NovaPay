package com.novabank.novapay.presentation.officer;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputLayout;
import com.novabank.novapay.R;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.core.ui.BaseFragment;
import com.novabank.novapay.core.ui.StateLayoutHelper;
import com.novabank.novapay.databinding.FragmentCreateCustomerBinding;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CreateCustomerFragment extends BaseFragment<FragmentCreateCustomerBinding> {

    private static final DateTimeFormatter ISO_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US).withZone(ZoneOffset.UTC);

    private CreateCustomerViewModel viewModel;
    private StateLayoutHelper stateLayoutHelper;

    @NonNull
    @Override
    protected FragmentCreateCustomerBinding inflateBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentCreateCustomerBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(CreateCustomerViewModel.class);
        FragmentCreateCustomerBinding b = getBinding();

        stateLayoutHelper = new StateLayoutHelper(
                b.contentContainer,
                b.stateLoading,
                b.stateEmpty,
                b.stateError,
                () -> viewModel.restoreFormAfterError()
        );

        b.toolbarCreateCustomer.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack()
        );

        b.tilDob.setEndIconOnClickListener(v -> showDobPicker());

        b.btnSubmitCreateCustomer.setOnClickListener(v -> {
            String fullName = textOf(b.etFullName);
            String email = textOf(b.etEmail);
            String phone = textOf(b.etPhone);
            String nationalId = textOf(b.etNationalId);
            String dob = textOf(b.etDob);
            String address = textOf(b.etAddress);
            String password = textOf(b.etInitialPassword);
            viewModel.submitCreateCustomer(
                    fullName, email, phone, nationalId, dob, address, password
            );
        });
    }

    @Override
    protected void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null || !isAdded() || getView() == null) {
                return;
            }
            stateLayoutHelper.render(state);
            if (state.getStatus() == UiState.Status.SUCCESS && state.getData() != null) {
                applyFieldErrors(state.getData());
            }
        });

        viewModel.getCreatedSuccessUidEvent().observe(getViewLifecycleOwner(), uid -> {
            if (uid != null && isAdded()) {
                showSnackbar(getString(R.string.officer_create_customer_success, uid));
                NavHostFragment.findNavController(this).popBackStack();
            }
        });
    }

    private void showDobPicker() {
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now())
                .build();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.officer_customer_field_dob)
                .setCalendarConstraints(constraints)
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null && isAdded() && getView() != null) {
                String formatted = ISO_DATE.format(Instant.ofEpochMilli(selection));
                getBinding().etDob.setText(formatted);
                getBinding().tilDob.setError(null);
            }
        });
        picker.show(getChildFragmentManager(), "dob_date_picker");
    }

    private void applyFieldErrors(@NonNull CreateCustomerUiState data) {
        FragmentCreateCustomerBinding b = getBinding();
        setFieldError(b.tilFullName, data.getFullNameError());
        setFieldError(b.tilEmail, data.getEmailError());
        setFieldError(b.tilPhone, data.getPhoneError());
        setFieldError(b.tilNationalId, data.getNationalIdError());
        setFieldError(b.tilDob, data.getDobError());
        setFieldError(b.tilAddress, data.getAddressError());
        setFieldError(b.tilInitialPassword, data.getPasswordError());
    }

    private void setFieldError(@NonNull TextInputLayout layout, @Nullable Integer errResId) {
        layout.setError(errResId != null ? getString(errResId) : null);
    }

    private static String textOf(com.google.android.material.textfield.TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString() : "";
    }
}
