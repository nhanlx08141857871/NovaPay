package com.novabank.novapay.presentation.customer.account;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.novabank.novapay.R;
import com.novabank.novapay.core.ui.BaseFragment;
import com.novabank.novapay.core.ui.StateLayoutHelper;
import com.novabank.novapay.core.util.CurrencyFormatter;
import com.novabank.novapay.core.util.DateUtils;
import com.novabank.novapay.databinding.FragmentSavingDetailBinding;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.account.AccountStatus;
import com.novabank.novapay.domain.model.account.SavingInfo;
import com.novabank.novapay.domain.model.account.SavingProjection;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Màn hình chi tiết tài khoản Tiết kiệm (SAVING):
 * Kết hợp realtime accounts/{id} + interest_rates theo termMonths, tính lãi dự kiến bằng long.
 */
@AndroidEntryPoint
public class SavingDetailFragment extends BaseFragment<FragmentSavingDetailBinding> {

    private SavingDetailViewModel viewModel;
    private StateLayoutHelper stateLayoutHelper;
    private String accountId;

    @NonNull
    @Override
    protected FragmentSavingDetailBinding inflateBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentSavingDetailBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(SavingDetailViewModel.class);
        FragmentSavingDetailBinding b = getBinding();

        b.stateEmpty.tvEmptyTitle.setText(R.string.core_saving_title);
        b.stateEmpty.tvEmptySubtitle.setText(R.string.core_account_not_found);

        stateLayoutHelper = new StateLayoutHelper(
                b.contentContainer,
                b.stateLoading,
                b.stateEmpty,
                b.stateError,
                () -> viewModel.observeSaving(accountId)
        );

        b.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());

        Bundle args = getArguments();
        accountId = args != null ? args.getString("accountId") : null;
        viewModel.observeSaving(accountId);
    }

    @Override
    protected void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                return;
            }
            stateLayoutHelper.render(state);
            SavingDetailUiState data = state.getData();
            if (data != null && data.getProjection() != null) {
                renderProjection(data.getProjection());
            }
        });
    }

    private void renderProjection(@NonNull SavingProjection projection) {
        FragmentSavingDetailBinding b = getBinding();
        Account account = projection.getAccount();
        SavingInfo saving = account.getSaving();

        b.tvBalance.setText(CurrencyFormatter.formatVnd(account.getBalance()));
        b.tvAccountNumber.setText(account.getAccountNumber());
        b.tvStatus.setText(resolveStatusLabel(account.getStatus()));
        b.tvMonthlyProfit.setText(CurrencyFormatter.formatVnd(projection.getMonthlyProfit()));
        b.tvAnnualRate.setText(getString(R.string.core_saving_rate_value, projection.getEffectiveAnnualRate()));

        if (saving != null) {
            b.tvPrincipal.setText(CurrencyFormatter.formatVnd(saving.getPrincipal()));
            if (saving.getTermMonths() <= 0) {
                b.tvTermMonths.setText(R.string.core_saving_term_demand);
            } else {
                b.tvTermMonths.setText(getString(R.string.core_saving_term_months, saving.getTermMonths()));
            }
            b.tvOpenedDate.setText(DateUtils.formatDate(saving.getOpenedAt()));
            b.tvMaturityDate.setText(DateUtils.formatDate(saving.getMaturityDate()));
        } else {
            b.tvPrincipal.setText(CurrencyFormatter.formatVnd(account.getBalance()));
            b.tvTermMonths.setText(R.string.core_saving_term_demand);
            b.tvOpenedDate.setText(DateUtils.formatDate(account.getCreatedAt()));
            b.tvMaturityDate.setText("-");
        }
    }

    private String resolveStatusLabel(AccountStatus status) {
        if (status == AccountStatus.FROZEN) {
            return getString(R.string.core_account_status_frozen);
        }
        if (status == AccountStatus.CLOSED) {
            return getString(R.string.core_account_status_closed);
        }
        return getString(R.string.core_account_status_active);
    }
}
