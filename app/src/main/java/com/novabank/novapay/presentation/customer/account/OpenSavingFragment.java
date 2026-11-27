package com.novabank.novapay.presentation.customer.account;

import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.novabank.novapay.R;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.core.ui.BaseFragment;
import com.novabank.novapay.core.ui.StateLayoutHelper;
import com.novabank.novapay.core.util.CurrencyFormatter;
import com.novabank.novapay.core.util.DateUtils;
import com.novabank.novapay.databinding.FragmentOpenSavingBinding;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.account.OpenSavingResult;
import com.novabank.novapay.domain.model.rate.InterestRate;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Màn hình Mở sổ tiết kiệm trực tuyến (novapay://savings/open).
 */
@AndroidEntryPoint
public class OpenSavingFragment extends BaseFragment<FragmentOpenSavingBinding> {

    private OpenSavingViewModel viewModel;
    private StateLayoutHelper stateLayoutHelper;
    private List<Account> boundCheckingAccounts = new ArrayList<>();
    private List<InterestRate> boundRates = new ArrayList<>();
    private boolean updatingAmountProgrammatically = false;

    @NonNull
    @Override
    protected FragmentOpenSavingBinding inflateBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentOpenSavingBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(OpenSavingViewModel.class);
        FragmentOpenSavingBinding b = getBinding();

        b.stateEmpty.tvEmptyTitle.setText(R.string.core_open_saving_title);
        b.stateEmpty.tvEmptySubtitle.setText(R.string.core_open_saving_no_checking);

        stateLayoutHelper = new StateLayoutHelper(
                b.contentContainer,
                b.stateLoading,
                b.stateEmpty,
                b.stateError,
                () -> viewModel.load()
        );

        b.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());

        b.etPrincipal.setText("5000000");
        b.etPrincipal.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (updatingAmountProgrammatically) {
                    return;
                }
                long amount = CurrencyFormatter.parseVndDigits(s != null ? s.toString() : "");
                viewModel.updatePrincipalAmount(amount);
            }
        });

        b.chipAmount1m.setOnClickListener(v -> setPrincipalInput(1_000_000L));
        b.chipAmount5m.setOnClickListener(v -> setPrincipalInput(5_000_000L));
        b.chipAmount10m.setOnClickListener(v -> setPrincipalInput(10_000_000L));
        b.chipAmount50m.setOnClickListener(v -> setPrincipalInput(50_000_000L));

        b.actvSourceAccount.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < boundCheckingAccounts.size()) {
                viewModel.selectCheckingAccount(boundCheckingAccounts.get(position).getAccountId());
            }
        });

        b.btnSubmitOpenSaving.setOnClickListener(v -> {
            String pin = b.etPin.getText() != null ? b.etPin.getText().toString() : null;
            viewModel.submitOpenSaving(pin);
        });

        viewModel.load();
    }

    @Override
    protected void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                return;
            }
            stateLayoutHelper.render(state);
            OpenSavingUiState data = state.getData();
            if (data != null) {
                renderUiState(data);
            }
        });

        viewModel.getOpenSuccessEvent().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                showSuccessDialog(result);
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

    private void setPrincipalInput(long amount) {
        updatingAmountProgrammatically = true;
        try {
            getBinding().etPrincipal.setText(String.valueOf(amount));
            getBinding().etPrincipal.setSelection(String.valueOf(amount).length());
        } finally {
            updatingAmountProgrammatically = false;
        }
        viewModel.updatePrincipalAmount(amount);
    }

    private void renderUiState(@NonNull OpenSavingUiState data) {
        FragmentOpenSavingBinding b = getBinding();

        if (!sameAccounts(boundCheckingAccounts, data.getCheckingAccounts())) {
            boundCheckingAccounts = new ArrayList<>(data.getCheckingAccounts());
            List<String> labels = new ArrayList<>();
            for (Account account : boundCheckingAccounts) {
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
            b.actvSourceAccount.setAdapter(adapter);
        }

        if (data.getSelectedAccount() != null) {
            String selectedText = getString(
                    R.string.core_open_saving_source_item,
                    data.getSelectedAccount().getAccountNumber(),
                    CurrencyFormatter.formatVnd(data.getSelectedAccount().getBalance())
            );
            if (!selectedText.equals(b.actvSourceAccount.getText().toString())) {
                b.actvSourceAccount.setText(selectedText, false);
            }
        }

        if (!boundRates.equals(data.getTermRates())) {
            boundRates = new ArrayList<>(data.getTermRates());
            buildTermChips(data.getTermRates(), data.getSelectedRate());
        }

        double rate = data.getSelectedRate() != null ? data.getSelectedRate().getAnnualRate() : 0.0;
        b.tvPreviewRate.setText(getString(R.string.core_saving_rate_value, rate));
        b.tvPreviewMonthlyInterest.setText(CurrencyFormatter.formatVnd(data.getMonthlyInterest()));
        b.tvPreviewTotalInterest.setText(CurrencyFormatter.formatVnd(data.getTotalMaturityInterest()));
        b.tvPreviewMaturityDate.setText(data.getEstimatedMaturityDate() != null
                ? DateUtils.formatDate(data.getEstimatedMaturityDate())
                : "-");
        b.tvPreviewTotalPayout.setText(CurrencyFormatter.formatVnd(data.getTotalMaturityPayout()));

        b.cardPinVerification.setVisibility(data.hasTransactionPin() ? View.VISIBLE : View.GONE);
        b.btnSubmitOpenSaving.setEnabled(!data.isSubmitting());
    }

    private void buildTermChips(
            @NonNull List<InterestRate> rates,
            @Nullable InterestRate selectedRate
    ) {
        FragmentOpenSavingBinding b = getBinding();
        b.chipGroupTerms.setOnCheckedStateChangeListener(null);
        b.chipGroupTerms.removeAllViews();

        int selectedChipId = View.NO_ID;
        for (InterestRate rate : rates) {
            Chip chip = new Chip(requireContext());
            int chipId = View.generateViewId();
            chip.setId(chipId);
            chip.setCheckable(true);
            chip.setText(getString(
                    R.string.core_open_saving_term_chip,
                    rate.getTermMonths(),
                    rate.getAnnualRate()
            ));
            chip.setTag(rate.getTermMonths());
            b.chipGroupTerms.addView(chip);
            if (selectedRate != null && selectedRate.getTermMonths() == rate.getTermMonths()) {
                selectedChipId = chipId;
            }
        }

        if (selectedChipId != View.NO_ID) {
            b.chipGroupTerms.check(selectedChipId);
        }

        b.chipGroupTerms.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            View checked = group.findViewById(checkedIds.get(0));
            if (checked != null && checked.getTag() instanceof Integer termMonths) {
                viewModel.selectTermMonths(termMonths);
            }
        });
    }

    private void showSuccessDialog(@NonNull OpenSavingResult result) {
        String maturityStr = result.getMaturityDate() != null
                ? DateUtils.formatDate(result.getMaturityDate())
                : "-";
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.core_open_saving_success_title)
                .setMessage(getString(
                        R.string.core_open_saving_success_desc,
                        result.getAccountNumber(),
                        result.getAnnualRate(),
                        maturityStr
                ))
                .setCancelable(false)
                .setPositiveButton(R.string.core_open_saving_view_account, (dialog, which) -> {
                    NavOptions options = new NavOptions.Builder()
                            .setPopUpTo(R.id.openSavingFragment, true)
                            .build();
                    NavHostFragment.findNavController(this).navigate(
                            Uri.parse(Constants.DEEPLINK_ACCOUNT_PREFIX + Uri.encode(result.getSavingAccountId())),
                            options
                    );
                })
                .setNegativeButton(R.string.core_open_saving_view_receipt, (dialog, which) -> {
                    if (!result.getTxId().isEmpty()) {
                        NavOptions options = new NavOptions.Builder()
                                .setPopUpTo(R.id.openSavingFragment, true)
                                .build();
                        NavHostFragment.findNavController(this).navigate(
                                Uri.parse(Constants.DEEPLINK_RECEIPT_PREFIX + Uri.encode(result.getTxId())),
                                options
                        );
                    } else {
                        NavHostFragment.findNavController(this).popBackStack();
                    }
                })
                .show();
    }

    private static boolean sameAccounts(@NonNull List<Account> a, @NonNull List<Account> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).getAccountId().equals(b.get(i).getAccountId())
                    || a.get(i).getBalance() != b.get(i).getBalance()) {
                return false;
            }
        }
        return true;
    }
}
