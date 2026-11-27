package com.novabank.novapay.presentation.customer.home;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
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
 * Tổng tài sản (long), banner eKYC, 4 tác vụ nhanh chính + 4 tiện ích mở rộng,
 * Danh sách 3 loại tài khoản & Biểu đồ thu/chi MPAndroidChart tối giản.
 */
@AndroidEntryPoint
public class CustomerHomeFragment extends BaseFragment<FragmentCustomerHomeBinding> {

    private CustomerHomeViewModel viewModel;
    private StateLayoutHelper stateLayoutHelper;
    private AccountCardAdapter accountAdapter;
    private boolean notificationPermissionPrompted = false;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                // Không làm gián đoạn trải nghiệm nếu người dùng từ chối
            });

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
        b.btnNotifications.setOnClickListener(v -> safeNavigateDeepLink(Constants.DEEPLINK_NOTIFICATIONS));
        b.btnProfile.setOnClickListener(v -> safeNavigateDeepLink(Constants.DEEPLINK_PROFILE));
        b.btnLogout.setOnClickListener(v -> performLogout());
        b.btnOpenSaving.setOnClickListener(v -> safeNavigateDeepLink(Constants.DEEPLINK_SAVINGS_OPEN));

        b.cardEkycBanner.setOnClickListener(v -> safeNavigateDeepLink(Constants.DEEPLINK_EKYC));
        b.btnEkycBannerAction.setOnClickListener(v -> safeNavigateDeepLink(Constants.DEEPLINK_EKYC));

        b.btnActionTransfer.setOnClickListener(v -> safeNavigateDeepLink(Constants.DEEPLINK_TRANSFER));
        b.btnActionQrScan.setOnClickListener(v -> safeNavigateDeepLink(SessionManager.DEEPLINK_QR));
        b.btnActionDeposit.setOnClickListener(v -> safeNavigateDeepLink(SessionManager.DEEPLINK_DEPOSIT));
        b.btnActionBills.setOnClickListener(v -> safeNavigateDeepLink(Constants.DEEPLINK_BILLS));

        b.btnActionTopup.setOnClickListener(v -> safeNavigateDeepLink(Constants.DEEPLINK_TOPUP));
        b.btnActionServices.setOnClickListener(v -> safeNavigateDeepLink(Constants.DEEPLINK_SERVICES));
        b.btnActionHistory.setOnClickListener(v -> safeNavigateDeepLink(Constants.DEEPLINK_TRANSACTIONS_MONTH));
        b.btnActionBranches.setOnClickListener(v -> safeNavigateDeepLink(Constants.DEEPLINK_BRANCHES));
        b.btnActionChatbot.setOnClickListener(v -> safeNavigateDeepLink(SessionManager.DEEPLINK_CHATBOT));
        b.btnActionProfile.setOnClickListener(v -> safeNavigateDeepLink(Constants.DEEPLINK_PROFILE));

        configureChartAppearance(b.barChartCashflow);
        requestNotificationPermissionIfNeeded();
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
            b.btnToggleBalance.setIconResource(R.drawable.ic_np_eye_off);
        } else {
            b.tvTotalBalance.setText(CurrencyFormatter.formatVnd(data.getTotalLiquidAssets()));
            b.btnToggleBalance.setIconResource(R.drawable.ic_np_eye_open);
        }

        renderEkycBanner(user);
        renderNotificationBadge(data.getUnreadNotificationCount());
        renderCashflowChart(data.getCashflow());
        accountAdapter.submitList(data.getAccounts(), data.isBalanceHidden());
    }

    private void renderNotificationBadge(int unreadCount) {
        FragmentCustomerHomeBinding b = getBinding();
        if (unreadCount <= 0) {
            b.tvNotificationBadge.setVisibility(View.GONE);
            return;
        }
        b.tvNotificationBadge.setVisibility(View.VISIBLE);
        b.tvNotificationBadge.setText(unreadCount > 99
                ? getString(R.string.core_notif_badge_overflow)
                : String.valueOf(unreadCount));
    }

    private void requestNotificationPermissionIfNeeded() {
        if (notificationPermissionPrompted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationPermissionPrompted = true;
        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            com.google.android.material.snackbar.Snackbar.make(
                            getBinding().getRoot(),
                            R.string.core_notif_permission_rationale,
                            com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                    )
                    .setAction(R.string.core_notif_permission_allow,
                            v -> notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS))
                    .show();
        } else {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
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
        int mutedTextColor = ContextCompat.getColor(requireContext(), R.color.np_text_muted);
        int hairlineColor = ContextCompat.getColor(requireContext(), R.color.np_border_hairline);

        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setDrawBorders(false);
        chart.setPinchZoom(false);
        chart.setScaleEnabled(false);
        chart.setExtraBottomOffset(6f);
        chart.getAxisRight().setEnabled(false);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setGridColor(hairlineColor);
        leftAxis.setGridLineWidth(0.8f);
        leftAxis.setTextColor(mutedTextColor);
        leftAxis.setLabelCount(4, false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setAxisLineColor(hairlineColor);
        xAxis.setTextColor(mutedTextColor);
        xAxis.setTextSize(12f);
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
        dataSet.setHighLightAlpha(0);

        BarData barData = new BarData(Collections.singletonList(dataSet));
        barData.setBarWidth(0.38f);
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
                .setPopUpTo(R.id.nav_graph, true)
                .setLaunchSingleTop(true)
                .build();
        NavHostFragment.findNavController(this).navigate(R.id.loginFragment, null, options);
    }
}
