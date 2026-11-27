package com.novabank.novapay.presentation.customer.account;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.novabank.novapay.R;
import com.novabank.novapay.core.ui.BaseFragment;
import com.novabank.novapay.core.ui.StateLayoutHelper;
import com.novabank.novapay.databinding.FragmentAccountRouterBinding;
import com.novabank.novapay.domain.model.account.AccountType;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AccountRouterFragment extends BaseFragment<FragmentAccountRouterBinding> {

    private AccountRouterViewModel viewModel;
    private StateLayoutHelper stateLayoutHelper;
    private String accountId;

    @NonNull
    @Override
    protected FragmentAccountRouterBinding inflateBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentAccountRouterBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(AccountRouterViewModel.class);
        FragmentAccountRouterBinding b = getBinding();

        b.stateEmpty.tvEmptyTitle.setText(R.string.core_account_router_title);
        b.stateEmpty.tvEmptySubtitle.setText(R.string.core_account_not_found);

        stateLayoutHelper = new StateLayoutHelper(
                b.contentContainer,
                b.stateLoading,
                b.stateEmpty,
                b.stateError,
                () -> viewModel.resolveAccount(accountId)
        );

        Bundle args = getArguments();
        accountId = args != null ? args.getString("accountId") : null;
        viewModel.resolveAccount(accountId);
    }

    @Override
    protected void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state != null) {
                stateLayoutHelper.render(state);
            }
        });

        viewModel.getRouteEvent().observe(getViewLifecycleOwner(), target -> {
            if (target == null) {
                return;
            }
            Bundle navArgs = new Bundle();
            navArgs.putString("accountId", target.getAccountId());

            int actionId;
            if (target.getAccountType() == AccountType.SAVING) {
                actionId = R.id.action_accountRouter_to_savingDetail;
            } else if (target.getAccountType() == AccountType.MORTGAGE) {
                actionId = R.id.action_accountRouter_to_mortgageDetail;
            } else {
                actionId = R.id.action_accountRouter_to_checkingDetail;
            }

            NavController controller = NavHostFragment.findNavController(this);
            controller.navigate(actionId, navArgs);
        });
    }
}
