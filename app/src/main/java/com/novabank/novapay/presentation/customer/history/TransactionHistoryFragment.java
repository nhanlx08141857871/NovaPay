package com.novabank.novapay.presentation.customer.history;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.core.util.Pair;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.novabank.novapay.R;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.core.ui.BaseFragment;
import com.novabank.novapay.core.ui.StateLayoutHelper;
import com.novabank.novapay.core.util.CurrencyFormatter;
import com.novabank.novapay.core.util.DateUtils;
import com.novabank.novapay.core.util.StatementPdfExporter;
import com.novabank.novapay.databinding.FragmentTransactionHistoryBinding;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.account.AccountType;
import com.novabank.novapay.domain.model.transaction.LedgerDirection;
import com.novabank.novapay.domain.model.transaction.LedgerEntry;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Màn hình Lịch sử giao dịch nâng cao (novapay://transactions?range=month):
 * Lọc theo tài khoản, chiều IN/OUT, từ khóa, khoảng ngày tùy chỉnh, tổng kết Thu/Chi,
 * mở lại biên lai (novapay://receipt/{txId}) và Xuất Sao kê PDF chuẩn A4.
 */
@AndroidEntryPoint
public class TransactionHistoryFragment extends BaseFragment<FragmentTransactionHistoryBinding> {

    private TransactionHistoryViewModel viewModel;
    private StateLayoutHelper stateLayoutHelper;
    private LedgerAdapter ledgerAdapter;
    private List<Account> boundAccounts = new ArrayList<>();
    private TransactionHistoryUiState latestUiState;
    private boolean suppressRangeChipChange = false;

    @NonNull
    @Override
    protected FragmentTransactionHistoryBinding inflateBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentTransactionHistoryBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(TransactionHistoryViewModel.class);
        FragmentTransactionHistoryBinding b = getBinding();

        b.stateEmpty.tvEmptyTitle.setText(R.string.core_history_title);
        b.stateEmpty.tvEmptySubtitle.setText(R.string.core_history_empty);

        stateLayoutHelper = new StateLayoutHelper(
                b.contentContainer,
                b.stateLoading,
                b.stateEmpty,
                b.stateError,
                () -> viewModel.reload()
        );

        ledgerAdapter = new LedgerAdapter(this::onLedgerEntryClicked);
        b.rvLedger.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvLedger.setAdapter(ledgerAdapter);

        b.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        b.btnExportPdf.setOnClickListener(v -> exportStatementPdf());

        b.actvAccountFilter.setText(getString(R.string.core_history_account_all), false);
        b.actvAccountFilter.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0) {
                viewModel.setAccountFilter(null);
            } else {
                int accountIdx = position - 1;
                if (accountIdx >= 0 && accountIdx < boundAccounts.size()) {
                    viewModel.setAccountFilter(boundAccounts.get(accountIdx).getAccountId());
                }
            }
        });

        b.etSearchKeyword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setSearchKeyword(s != null ? s.toString() : "");
            }
        });

        Bundle args = getArguments();
        String range = args != null ? args.getString("range") : null;
        boolean monthOnly = range == null || range.isEmpty() || "month".equalsIgnoreCase(range);
        suppressRangeChipChange = true;
        try {
            if (monthOnly) {
                b.chipThisMonth.setChecked(true);
            } else {
                b.chipRecent90Days.setChecked(true);
            }
        } finally {
            suppressRangeChipChange = false;
        }

        b.chipGroupDirection.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipIn)) {
                viewModel.setDirectionFilter(LedgerDirection.IN);
            } else if (checkedIds.contains(R.id.chipOut)) {
                viewModel.setDirectionFilter(LedgerDirection.OUT);
            } else {
                viewModel.setDirectionFilter(null);
            }
        });

        b.chipGroupRange.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (suppressRangeChipChange || checkedIds.isEmpty()) {
                return;
            }
            if (checkedIds.contains(R.id.chipThisMonth)) {
                b.chipCustomDate.setText(R.string.core_history_range_custom);
                viewModel.setRangeMode(TransactionHistoryUiState.RangeMode.THIS_MONTH);
            } else if (checkedIds.contains(R.id.chipRecent90Days)) {
                b.chipCustomDate.setText(R.string.core_history_range_custom);
                viewModel.setRangeMode(TransactionHistoryUiState.RangeMode.RECENT_90_DAYS);
            } else if (checkedIds.contains(R.id.chipCustomDate)) {
                openCustomDatePicker();
            }
        });

        b.chipCustomDate.setOnClickListener(v -> {
            if (b.chipCustomDate.isChecked()) {
                openCustomDatePicker();
            }
        });

        viewModel.initialize(monthOnly);
    }

    @Override
    protected void observeViewModel() {
        viewModel.getAccountsLiveData().observe(getViewLifecycleOwner(), accounts -> {
            if (accounts != null) {
                bindAccountDropdown(accounts);
            }
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                return;
            }
            stateLayoutHelper.render(state);
            TransactionHistoryUiState data = state.getData();
            if (data != null) {
                latestUiState = data;
                ledgerAdapter.submitList(data.getEntries());
                FragmentTransactionHistoryBinding b = getBinding();
                b.tvSummaryIn.setText(CurrencyFormatter.formatVnd(data.getTotalIn()));
                b.tvSummaryOut.setText(CurrencyFormatter.formatVnd(data.getTotalOut()));
                b.tvSummaryCount.setText(getString(
                        R.string.core_history_summary_count,
                        data.getEntries().size()
                ));
            } else if (state.getStatus() != com.novabank.novapay.core.common.UiState.Status.LOADING) {
                latestUiState = null;
            }
        });
    }

    private void bindAccountDropdown(@NonNull List<Account> accounts) {
        boundAccounts = new ArrayList<>(accounts);
        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.core_history_account_all));
        for (Account account : boundAccounts) {
            String typeLabel = resolveShortTypeLabel(account.getType());
            labels.add(typeLabel + " • " + account.getAccountNumber());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                labels
        );
        getBinding().actvAccountFilter.setAdapter(adapter);
    }

    private void openCustomDatePicker() {
        MaterialDatePicker<Pair<Long, Long>> picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText(R.string.core_history_range_custom_picker_title)
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null || selection.first == null || selection.second == null) {
                return;
            }
            Instant start = Instant.ofEpochMilli(selection.first);
            Instant end = Instant.ofEpochMilli(selection.second);
            getBinding().chipCustomDate.setText(getString(
                    R.string.core_history_range_custom_label,
                    DateUtils.formatDate(start),
                    DateUtils.formatDate(end)
            ));
            viewModel.setCustomDateRange(start, end);
        });

        picker.addOnCancelListener(dialog -> revertCustomChipIfUnset());
        picker.addOnNegativeButtonClickListener(v -> revertCustomChipIfUnset());

        picker.show(getParentFragmentManager(), "novapay_history_date_range");
    }

    private void revertCustomChipIfUnset() {
        if (latestUiState != null
                && latestUiState.getRangeMode() == TransactionHistoryUiState.RangeMode.CUSTOM) {
            return;
        }
        suppressRangeChipChange = true;
        try {
            if (latestUiState != null
                    && latestUiState.getRangeMode() == TransactionHistoryUiState.RangeMode.RECENT_90_DAYS) {
                getBinding().chipRecent90Days.setChecked(true);
            } else {
                getBinding().chipThisMonth.setChecked(true);
            }
        } finally {
            suppressRangeChipChange = false;
        }
    }

    private void onLedgerEntryClicked(@NonNull LedgerEntry entry) {
        String txId = entry.getTxId();
        if (txId == null || txId.trim().isEmpty()) {
            showSnackbar(getString(R.string.core_history_receipt_unavailable));
            return;
        }
        try {
            navigateDeepLink(Constants.DEEPLINK_RECEIPT_PREFIX + Uri.encode(txId.trim()));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showSnackbar(getString(R.string.core_guard_destination_unavailable));
        }
    }

    private void exportStatementPdf() {
        if (latestUiState == null || latestUiState.getEntries().isEmpty()) {
            showSnackbar(getString(R.string.core_history_export_pdf_empty));
            return;
        }
        String customerName = !latestUiState.getCustomerName().isEmpty()
                ? latestUiState.getCustomerName()
                : getString(R.string.core_home_default_customer);
        String accountLabel = resolveSelectedAccountLabel(latestUiState);
        String periodLabel = resolvePeriodLabel(latestUiState);

        try {
            File pdfFile = StatementPdfExporter.exportStatementPdf(
                    requireContext(),
                    customerName,
                    accountLabel,
                    periodLabel,
                    latestUiState.getTotalIn(),
                    latestUiState.getTotalOut(),
                    latestUiState.getEntries()
            );
            Uri uri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    pdfFile
            );
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(
                    shareIntent,
                    getString(R.string.core_history_export_pdf_chooser)
            ));
        } catch (IOException | IllegalStateException ex) {
            showSnackbar(getString(R.string.core_history_export_pdf_failed));
        }
    }

    @NonNull
    private String resolveSelectedAccountLabel(@NonNull TransactionHistoryUiState state) {
        String selectedId = state.getSelectedAccountId();
        if (selectedId == null || selectedId.isEmpty()) {
            return getString(R.string.core_history_account_all);
        }
        for (Account account : state.getAccounts()) {
            if (selectedId.equals(account.getAccountId())) {
                return resolveShortTypeLabel(account.getType()) + " • " + account.getAccountNumber();
            }
        }
        return selectedId;
    }

    @NonNull
    private String resolvePeriodLabel(@NonNull TransactionHistoryUiState state) {
        if (state.getRangeMode() == TransactionHistoryUiState.RangeMode.CUSTOM
                && state.getCustomStart() != null
                && state.getCustomEnd() != null) {
            Instant inclusiveEnd = state.getCustomEnd().minusSeconds(1);
            return getString(
                    R.string.core_history_range_custom_label,
                    DateUtils.formatDate(state.getCustomStart()),
                    DateUtils.formatDate(inclusiveEnd)
            );
        }
        if (state.getRangeMode() == TransactionHistoryUiState.RangeMode.RECENT_90_DAYS) {
            return getString(R.string.core_history_range_recent);
        }
        return getString(R.string.core_history_range_month);
    }

    @NonNull
    private String resolveShortTypeLabel(@NonNull AccountType type) {
        if (type == AccountType.SAVING) {
            return getString(R.string.core_account_type_saving);
        }
        if (type == AccountType.MORTGAGE) {
            return getString(R.string.core_account_type_mortgage);
        }
        return getString(R.string.core_account_type_checking);
    }
}
