package com.novabank.novapay.presentation.officer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.novabank.novapay.R;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.core.ui.BaseFragment;
import com.novabank.novapay.core.ui.StateLayoutHelper;
import com.novabank.novapay.databinding.FragmentOfficerDashboardBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class OfficerDashboardFragment extends BaseFragment<FragmentOfficerDashboardBinding> {

    private OfficerDashboardViewModel viewModel;
    private StateLayoutHelper stateLayoutHelper;
    private FaceCheckAuditAdapter faceAuditAdapter;
    private AuditLogAdapter auditLogAdapter;

    @NonNull
    @Override
    protected FragmentOfficerDashboardBinding inflateBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentOfficerDashboardBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(OfficerDashboardViewModel.class);
        FragmentOfficerDashboardBinding b = getBinding();

        stateLayoutHelper = new StateLayoutHelper(
                b.contentContainer,
                b.stateLoading,
                b.stateEmpty,
                b.stateError,
                () -> viewModel.loadDashboard()
        );

        faceAuditAdapter = new FaceCheckAuditAdapter(item -> {
            Bundle args = new Bundle();
            args.putString(OfficerNavConstants.ARG_FOCUS_UID, item.getUid());
            NavHostFragment.findNavController(this).navigate(
                    R.id.action_officerDashboard_to_officerEkycReview,
                    args
            );
        });
        b.rvFaceCheckAudits.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvFaceCheckAudits.setAdapter(faceAuditAdapter);

        auditLogAdapter = new AuditLogAdapter();
        b.rvAuditLogs.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvAuditLogs.setAdapter(auditLogAdapter);

        b.toolbarOfficerDashboard.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_refresh) {
                viewModel.loadDashboard();
                return true;
            }
            if (item.getItemId() == R.id.action_logout) {
                confirmLogout();
                return true;
            }
            return false;
        });

        b.cardNavCustomers.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_officerDashboard_to_customerManagement)
        );
        b.cardNavEkycReview.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_officerDashboard_to_officerEkycReview)
        );
        b.btnOpenFaceAuditReview.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_officerDashboard_to_officerEkycReview)
        );
        b.cardNavInterestRates.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_officerDashboard_to_interestRateManagement)
        );
        b.cardNavBranches.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_officerDashboard_to_branchMap)
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
                renderDashboard(state.getData());
            }
        });

        viewModel.getMessageEvent().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                showSnackbar(message);
            }
        });

        viewModel.getLoggedOutEvent().observe(getViewLifecycleOwner(), loggedOut -> {
            if (Boolean.TRUE.equals(loggedOut) && isAdded()) {
                resetToRootLogin();
            }
        });
    }

    private void renderDashboard(@NonNull OfficerDashboardUiState data) {
        FragmentOfficerDashboardBinding b = getBinding();
        String email = data.getOfficerEmail().isEmpty()
                ? getString(R.string.officer_dashboard_email_fallback)
                : data.getOfficerEmail();
        b.tvOfficerEmail.setText(email);

        b.tvTotalCustomersCount.setText(String.valueOf(data.getTotalCustomers()));
        b.tvPendingEkycCount.setText(String.valueOf(data.getPendingEkycCount()));
        b.tvDailyTransactionsCount.setText(String.valueOf(data.getDailyTransactionsCount()));
        b.tvManualAuditCount.setText(
                getString(R.string.officer_post_audit_count_format, data.getManualFaceAuditCount())
        );

        boolean hasFaceAudits = !data.getPendingFaceAudits().isEmpty();
        b.rvFaceCheckAudits.setVisibility(hasFaceAudits ? View.VISIBLE : View.GONE);
        b.tvEmptyFaceAudits.setVisibility(hasFaceAudits ? View.GONE : View.VISIBLE);
        faceAuditAdapter.submitList(data.getPendingFaceAudits());

        boolean hasAuditLogs = !data.getRecentAuditLogs().isEmpty();
        b.rvAuditLogs.setVisibility(hasAuditLogs ? View.VISIBLE : View.GONE);
        b.tvEmptyAuditLogs.setVisibility(hasAuditLogs ? View.GONE : View.VISIBLE);
        auditLogAdapter.submitList(data.getRecentAuditLogs());
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.officer_logout_confirm_title)
                .setMessage(R.string.officer_logout_confirm_message)
                .setNegativeButton(R.string.officer_action_cancel, null)
                .setPositiveButton(R.string.officer_action_logout, (dialog, which) ->
                        viewModel.onLogoutConfirmed()
                )
                .show();
    }

    private void resetToRootLogin() {
        try {
            NavController navController = NavHostFragment.findNavController(this);
            NavGraph rootGraph = navController.getGraph();
            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(rootGraph.getId(), true)
                    .setLaunchSingleTop(true)
                    .build();
            navController.navigate(rootGraph.getStartDestinationId(), null, navOptions);
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            // SessionManager in MainActivity may have already reset navigation on signOut.
        }
    }
}
