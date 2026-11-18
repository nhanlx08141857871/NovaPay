package com.novabank.novapay.presentation.customer.home;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.novabank.novapay.R;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.core.security.SessionManager;
import com.novabank.novapay.core.ui.BaseFragment;
import com.novabank.novapay.core.ui.StateLayoutHelper;
import com.novabank.novapay.core.util.CurrencyFormatter;
import com.novabank.novapay.databinding.FragmentCustomerHomeBinding;
import com.novabank.novapay.domain.model.transaction.MonthlyCashflowSummary;
import com.novabank.novapay.domain.model.user.EkycStatus;
import com.novabank.novapay.domain.model.user.User;

import java.util.Arrays;
import java.util.Collections;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Màn hình Trang chủ Khách hàng (novapay://home):
 * Tổng tài sản (long), banner eKYC, Dịch vụ nhanh, Biểu đồ thu/chi MPAndroidChart & Danh sách tài khoản.
 */
@AndroidEntryPoint
public class CustomerHomeFragment extends BaseFragment<FragmentCustomerHomeBinding> {

    private CustomerHomeViewModel viewModel;
    private StateLayoutHelper stateLayoutHelper;
    private AccountCardAdapter accountAdapter;

    @NonNull
    @Override
    protected FragmentCustomerHomeBinding inflateBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentCustomerHomeBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(CustomerHomeViewModel.class);
        FragmentCustomerHomeBinding b = getBinding();

        b.stateEmpty.tvEmptyTitle.setText(R.string.core_home_accounts_title);
        b.stateEmpty.tvEmptySubtitle.setText(R.string.core_home_accounts_empty);

        stateLayoutHelper = new StateLayoutHelper(
                b.contentContainer,
                b.stateLoading,
                b.stateEmpty,
                b.stateError,
                () -> viewModel.loadHome()
        );

        accountAdapter = new AccountCardAdapter(account ->
                safeNavigateDeepLink(Constants.DEEPLINK_ACCOUNT_PREFIX + account.getAccountId())
        );
        b.rvAccounts.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvAccounts.setAdapter(accountAdapter);

        b.btnToggleBalance.setOnClickListener(v -> viewModel.toggleBalanceVisibility());
        b.btnProfile.setOnClickListener(v -> safeNavigateDeepLink(Constants.DEEPLINK_PROFILE));
        b.btnLogout.setOnClickListener(v -> performLogout());

        b.cardEkycBanner.setOnClickListener(v -> safeNavigateDeepLink(Constants.DEEPLINK_EKYC));
        b.btnEkycBannerAction.setOnClickListener(v -> safeNavigateDeepLink(Constants.DEEPLINK_EKYC));

        b.btnActionTransfer.setOnClickListener(v -> safeNavigateDeepLink(Constants.DEEPLINK_TRANSFER));
        b.btnActionBills.setOnClickListener(v -> safeNavigateDeepLink(Constants.DEEPLINK_BILLS));
        b.btnActionTopup.setOnClickListener(v -> safeNavigateDeepLink(Constants.DEEPLINK_TOPUP));
        b.btnActionServices.setOnClickListener(v -> safeNavigateDeepLink(Constants.DEEPLINK_SERVICES));
        b.btnActionHistory.setOnClickListener(v -> safeNavigateDeepLink(Constants.DEEPLINK_TRANSACTIONS_MONTH));
        b.btnActionBranches.setOnClickListener(v -> safeNavigateDeepLink(Constants.DEEPLINK_BRANCHES));
        b.btnActionChatbot.setOnClickListener(v -> safeNavigateDeepLink(SessionManager.DEEPLINK_CHATBOT));
        b.btnActionProfile.setOnClickListener(v -> safeNavigateDeepLink(Constants.DEEPLINK_PROFILE));

        configureChartAppearance(b.barChartCashflow);
        viewModel.loadHome();
    }

    @Override
    protected void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                return;
            }
            stateLayoutHelper.render(state);
            CustomerHomeUiState data = state.getData();
            if (data != null) {
                renderHome(data);
            }
        });
    }

    private void renderHome(@NonNull CustomerHomeUiState data) {
        FragmentCustomerHomeBinding b = getBinding();
        User user = data.getUser();

        String displayName = (user != null && !user.getFullName().isEmpty())
                ? user.getFullName()
                : getString(R.string.core_home_default_customer);
        b.tvGreeting.setText(getString(R.string.core_home_greeting, displayName));

        if (data.isBalanceHidden()) {
            b.tvTotalBalance.setText(R.string.core_home_masked_balance);
        } else {
            b.tvTotalBalance.setText(CurrencyFormatter.formatVnd(data.getTotalLiquidAssets()));
        }

        renderEkycBanner(user);
        renderCashflowChart(data.getCashflow());
        accountAdapter.submitList(data.getAccounts(), data.isBalanceHidden());
    }

    private void renderEkycBanner(@Nullable User user) {
        FragmentCustomerHomeBinding b = getBinding();
        if (user == null || user.isEkycVerified()) {
            b.cardEkycBanner.setVisibility(View.GONE);
            return;
        }
        b.cardEkycBanner.setVisibility(View.VISIBLE);
        EkycStatus status = user.getEkyc().getStatus();
        if (status == EkycStatus.PENDING) {
            b.tvEkycBannerTitle.setText(R.string.core_home_ekyc_banner_pending_title);
            b.tvEkycBannerDesc.setText(R.string.core_home_ekyc_banner_pending_desc);
            b.btnEkycBannerAction.setVisibility(View.GONE);
        } else if (status == EkycStatus.REJECTED) {
            b.tvEkycBannerTitle.setText(R.string.core_home_ekyc_banner_rejected_title);
            b.tvEkycBannerDesc.setText(R.string.core_home_ekyc_banner_rejected_desc);
            b.btnEkycBannerAction.setVisibility(View.VISIBLE);
        } else {
            b.tvEkycBannerTitle.setText(R.string.core_home_ekyc_banner_unverified_title);
            b.tvEkycBannerDesc.setText(R.string.core_home_ekyc_banner_unverified_desc);
            b.btnEkycBannerAction.setVisibility(View.VISIBLE);
        }
    }

    private void configureChartAppearance(@NonNull BarChart chart) {
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setPinchZoom(false);
        chart.setScaleEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setAxisMinimum(0f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(Arrays.asList(
                getString(R.string.core_home_chart_label_in),
                getString(R.string.core_home_chart_label_out)
        )));
    }

    private void renderCashflowChart(@NonNull MonthlyCashflowSummary summary) {
        FragmentCustomerHomeBinding b = getBinding();
        b.tvChartTitle.setText(getString(R.string.core_home_chart_title, summary.getMonthLabel()));
        b.tvChartIncome.setText(getString(
                R.string.core_home_chart_income,
                CurrencyFormatter.formatVnd(summary.getTotalIn())
        ));
        b.tvChartExpense.setText(getString(
                R.string.core_home_chart_expense,
                CurrencyFormatter.formatVnd(summary.getTotalOut())
        ));

        BarEntry inEntry = new BarEntry(0f, (float) summary.getTotalIn());
        BarEntry outEntry = new BarEntry(1f, (float) summary.getTotalOut());

        BarDataSet dataSet = new BarDataSet(Arrays.asList(inEntry, outEntry), "");
        dataSet.setColors(
                ContextCompat.getColor(requireContext(), R.color.novapay_income_positive),
                ContextCompat.getColor(requireContext(), R.color.novapay_expense_negative)
        );
        dataSet.setDrawValues(false);

        BarData barData = new BarData(Collections.singletonList(dataSet));
        barData.setBarWidth(0.45f);
        b.barChartCashflow.setData(barData);
        b.barChartCashflow.invalidate();
    }

    private void safeNavigateDeepLink(@NonNull String uriString) {
        try {
            navigateDeepLink(uriString);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showSnackbar(getString(R.string.core_guard_destination_unavailable));
        }
    }

    private void performLogout() {
        viewModel.signOut();
        NavOptions options = new NavOptions.Builder()
                .setPopUpTo(R.id.nav_core, true)
                .build();
        NavHostFragment.findNavController(this).navigate(R.id.loginFragment, null, options);
    }
}
