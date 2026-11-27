package com.novabank.novapay.presentation.customer.notification;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.novabank.novapay.R;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.core.ui.BaseFragment;
import com.novabank.novapay.core.ui.StateLayoutHelper;
import com.novabank.novapay.databinding.FragmentNotificationCenterBinding;
import com.novabank.novapay.domain.model.notification.AppNotification;
import com.novabank.novapay.domain.model.notification.NotificationCategory;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class NotificationCenterFragment extends BaseFragment<FragmentNotificationCenterBinding> {

    private NotificationCenterViewModel viewModel;
    private StateLayoutHelper stateLayoutHelper;
    private NotificationAdapter adapter;

    @NonNull
    @Override
    protected FragmentNotificationCenterBinding inflateBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentNotificationCenterBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(NotificationCenterViewModel.class);
        FragmentNotificationCenterBinding b = getBinding();

        b.stateEmpty.tvEmptyTitle.setText(R.string.core_notif_empty_title);
        b.stateEmpty.tvEmptySubtitle.setText(R.string.core_notif_empty_subtitle);

        stateLayoutHelper = new StateLayoutHelper(
                b.contentContainer,
                b.stateLoading,
                b.stateEmpty,
                b.stateError,
                () -> viewModel.loadNotifications()
        );

        adapter = new NotificationAdapter(this::onNotificationClicked);
        b.contentContainer.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.contentContainer.setAdapter(adapter);

        b.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        b.btnMarkAllRead.setOnClickListener(v -> viewModel.markAllAsRead());

        b.chipGroupCategory.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipCatBalance)) {
                viewModel.setCategoryFilter(NotificationCategory.BALANCE);
            } else if (checkedIds.contains(R.id.chipCatSaving)) {
                viewModel.setCategoryFilter(NotificationCategory.SAVING);
            } else if (checkedIds.contains(R.id.chipCatSecurity)) {
                viewModel.setCategoryFilter(NotificationCategory.SECURITY);
            } else if (checkedIds.contains(R.id.chipCatEkyc)) {
                viewModel.setCategoryFilter(NotificationCategory.EKYC);
            } else {
                viewModel.setCategoryFilter(null);
            }
        });

        viewModel.loadNotifications();
    }

    @Override
    protected void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                return;
            }
            stateLayoutHelper.render(state);
            NotificationCenterUiState data = state.getData();
            if (data != null) {
                adapter.submitList(data.getItems());
                getBinding().btnMarkAllRead.setEnabled(data.getUnreadCount() > 0);
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

    private void onNotificationClicked(@NonNull AppNotification notification) {
        if (!notification.isRead()) {
            viewModel.markAsRead(notification.getNotifId());
        }
        String deepLink = notification.getDeepLink();
        if (deepLink == null || deepLink.isEmpty()) {
            if (notification.getTxId() != null && !notification.getTxId().isEmpty()) {
                deepLink = Constants.DEEPLINK_RECEIPT_PREFIX + Uri.encode(notification.getTxId());
            } else {
                return;
            }
        }
        try {
            navigateDeepLink(deepLink);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showSnackbar(getString(R.string.core_guard_destination_unavailable));
        }
    }
}
