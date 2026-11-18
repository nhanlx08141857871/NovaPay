package com.novabank.novapay.presentation.customer.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.novabank.novapay.R;
import com.novabank.novapay.core.ui.BaseFragment;
import com.novabank.novapay.core.ui.StateLayoutHelper;
import com.novabank.novapay.databinding.FragmentTransactionHistoryBinding;
import com.novabank.novapay.domain.model.transaction.LedgerDirection;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Màn hình Lịch sử giao dịch (novapay://transactions?range=month):
 * Đọc sổ cái accounts/{accountId}/ledger, lọc theo chiều IN/OUT và tháng theo Asia/Ho_Chi_Minh.
 */
@AndroidEntryPoint
public class TransactionHistoryFragment extends BaseFragment<FragmentTransactionHistoryBinding> {

    private TransactionHistoryViewModel viewModel;
    private StateLayoutHelper stateLayoutHelper;
    private LedgerAdapter ledgerAdapter;

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

        ledgerAdapter = new LedgerAdapter();
        b.contentContainer.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.contentContainer.setAdapter(ledgerAdapter);

        b.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());

        Bundle args = getArguments();
        String range = args != null ? args.getString("range") : null;
        boolean monthOnly = range == null || range.isEmpty() || "month".equalsIgnoreCase(range);
        if (monthOnly) {
            b.chipThisMonth.setChecked(true);
        } else {
            b.chipRecent90Days.setChecked(true);
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

        b.chipGroupRange.setOnCheckedStateChangeListener((group, checkedIds) ->
                viewModel.setMonthOnlyRange(checkedIds.contains(R.id.chipThisMonth)));

        viewModel.initialize(monthOnly);
    }

    @Override
    protected void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                return;
            }
            stateLayoutHelper.render(state);
            TransactionHistoryUiState data = state.getData();
            if (data != null) {
                ledgerAdapter.submitList(data.getEntries());
            }
        });
    }
}
