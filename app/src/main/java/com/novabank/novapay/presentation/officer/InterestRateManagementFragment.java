package com.novabank.novapay.presentation.officer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.novabank.novapay.R;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.core.ui.BaseFragment;
import com.novabank.novapay.core.ui.StateLayoutHelper;
import com.novabank.novapay.databinding.FragmentInterestRateManagementBinding;

import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class InterestRateManagementFragment
        extends BaseFragment<FragmentInterestRateManagementBinding> {

    private InterestRateManagementViewModel viewModel;
    private StateLayoutHelper stateLayoutHelper;
    private InterestRateAdapter adapter;

    @NonNull
    @Override
    protected FragmentInterestRateManagementBinding inflateBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentInterestRateManagementBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(InterestRateManagementViewModel.class);
        FragmentInterestRateManagementBinding b = getBinding();

        stateLayoutHelper = new StateLayoutHelper(
                b.contentContainer,
                b.stateLoading,
                b.stateEmpty,
                b.stateError,
                () -> viewModel.loadInterestRates()
        );
        b.stateEmpty.tvEmptyTitle.setText(R.string.officer_rate_empty_title);
        b.stateEmpty.tvEmptySubtitle.setText(R.string.officer_rate_empty_subtitle);

        adapter = new InterestRateAdapter(rate -> {
            b.etTermMonths.setText(String.valueOf(rate.getTermMonths()));
            b.etAnnualRate.setText(String.format(Locale.US, "%.2f", rate.getAnnualRate()));
            b.tilTermMonths.setError(null);
            b.tilAnnualRate.setError(null);
        });
        b.contentContainer.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.contentContainer.setAdapter(adapter);

        b.toolbarInterestRates.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack()
        );
        b.toolbarInterestRates.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_refresh) {
                viewModel.loadInterestRates();
                return true;
            }
            return false;
        });

        b.btnSaveInterestRate.setOnClickListener(v -> {
            b.tilTermMonths.setError(null);
            b.tilAnnualRate.setError(null);
            String termRaw = b.etTermMonths.getText() != null
                    ? b.etTermMonths.getText().toString() : "";
            String annualRaw = b.etAnnualRate.getText() != null
                    ? b.etAnnualRate.getText().toString() : "";
            viewModel.saveInterestRate(termRaw, annualRaw, R.string.officer_rate_save_success);
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
                adapter.submitList(state.getData().getRates());
            }
        });

        viewModel.getSubmitting().observe(getViewLifecycleOwner(), submitting -> {
            if (isAdded() && getView() != null) {
                boolean busy = Boolean.TRUE.equals(submitting);
                getBinding().progressRateSave.setVisibility(busy ? View.VISIBLE : View.GONE);
                getBinding().btnSaveInterestRate.setEnabled(!busy);
            }
        });

        viewModel.getTermErrorResEvent().observe(getViewLifecycleOwner(), resId -> {
            if (resId != null && isAdded() && getView() != null) {
                getBinding().tilTermMonths.setError(getString(resId));
            }
        });

        viewModel.getAnnualRateErrorResEvent().observe(getViewLifecycleOwner(), resId -> {
            if (resId != null && isAdded() && getView() != null) {
                getBinding().tilAnnualRate.setError(getString(resId));
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
}
