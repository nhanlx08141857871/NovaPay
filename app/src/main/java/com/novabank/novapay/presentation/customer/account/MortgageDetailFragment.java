package com.novabank.novapay.presentation.customer.account;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.novabank.novapay.R;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.core.ui.BaseFragment;
import com.novabank.novapay.core.ui.StateLayoutHelper;
import com.novabank.novapay.core.util.CurrencyFormatter;
import com.novabank.novapay.core.util.DateUtils;
import com.novabank.novapay.databinding.FragmentMortgageDetailBinding;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.account.AccountStatus;
import com.novabank.novapay.domain.model.account.MortgageInfo;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MortgageDetailFragment extends BaseFragment<FragmentMortgageDetailBinding> {

    private MortgageDetailViewModel viewModel;
    private StateLayoutHelper stateLayoutHelper;
    private String accountId;

    @NonNull
    @Override
    protected FragmentMortgageDetailBinding inflateBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentMortgageDetailBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(MortgageDetailViewModel.class);
        FragmentMortgageDetailBinding b = getBinding();

        b.stateEmpty.tvEmptyTitle.setText(R.string.core_mortgage_title);
        b.stateEmpty.tvEmptySubtitle.setText(R.string.core_account_not_found);

        stateLayoutHelper = new StateLayoutHelper(
                b.contentContainer,
                b.stateLoading,
                b.stateEmpty,
                b.stateError,
                () -> viewModel.observeMortgage(accountId)
        );

        b.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        b.btnPayInstallment.setOnClickListener(v -> {
            if (accountId == null || accountId.isEmpty()) {
                return;
            }
            String uri = Constants.DEEPLINK_TRANSFER
                    + "?type=MORTGAGE_PAYMENT&accountId="
                    + accountId;
            try {
                navigateDeepLink(uri);
            } catch (IllegalArgumentException | IllegalStateException ex) {
                showSnackbar(getString(R.string.core_guard_destination_unavailable));
            }
        });

        Bundle args = getArguments();
        accountId = args != null ? args.getString("accountId") : null;
        viewModel.observeMortgage(accountId);
    }

    @Override
    protected void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                return;
            }
            stateLayoutHelper.render(state);
            MortgageDetailUiState data = state.getData();
            if (data != null && data.getAccount() != null) {
                renderMortgage(data.getAccount());
            }
        });
    }

    private void renderMortgage(@NonNull Account account) {
        FragmentMortgageDetailBinding b = getBinding();
        MortgageInfo mortgage = account.getMortgage();

        b.tvAccountNumber.setText(account.getAccountNumber());
        b.tvStatus.setText(resolveStatusLabel(account.getStatus()));

        long outstanding = mortgage != null ? mortgage.getOutstandingBalance() : account.getBalance();
        b.tvOutstandingBalance.setText(CurrencyFormatter.formatVnd(outstanding));

        if (mortgage != null) {
            b.tvLoanAmount.setText(CurrencyFormatter.formatVnd(mortgage.getLoanAmount()));
            b.tvInstallmentAmount.setText(CurrencyFormatter.formatVnd(mortgage.getInstallmentAmount()));
            b.tvFrequency.setText(
                    MortgageInfo.FREQUENCY_BIWEEKLY.equalsIgnoreCase(mortgage.getPaymentFrequency())
                            ? R.string.core_mortgage_frequency_biweekly
                            : R.string.core_mortgage_frequency_monthly
            );
            b.tvAnnualRate.setText(getString(R.string.core_saving_rate_value, mortgage.getAnnualRate()));
            b.tvTermMonths.setText(getString(R.string.core_saving_term_months, mortgage.getTermMonths()));
            b.tvNextDueDate.setText(DateUtils.formatDate(mortgage.getNextDueDate()));
        } else {
            b.tvLoanAmount.setText(CurrencyFormatter.formatVnd(0L));
            b.tvInstallmentAmount.setText(CurrencyFormatter.formatVnd(0L));
            b.tvFrequency.setText(R.string.core_mortgage_frequency_monthly);
            b.tvAnnualRate.setText(getString(R.string.core_saving_rate_value, 0.0));
            b.tvTermMonths.setText(getString(R.string.core_saving_term_months, 0));
            b.tvNextDueDate.setText("-");
        }

        boolean canPay = account.getStatus() == AccountStatus.ACTIVE && outstanding > 0L;
        b.btnPayInstallment.setEnabled(canPay);
        b.tvSettledNotice.setVisibility(canPay ? View.GONE : View.VISIBLE);
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
