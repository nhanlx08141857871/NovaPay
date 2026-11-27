package com.novabank.novapay.presentation.customer.notification;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novabank.novapay.R;
import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.core.common.SingleLiveEvent;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.domain.model.notification.AppNotification;
import com.novabank.novapay.domain.model.notification.NotificationCategory;
import com.novabank.novapay.domain.usecase.notification.MarkAllNotificationsReadUseCase;
import com.novabank.novapay.domain.usecase.notification.MarkNotificationReadUseCase;
import com.novabank.novapay.domain.usecase.notification.ObserveNotificationsUseCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

@HiltViewModel
public class NotificationCenterViewModel extends ViewModel {

    private final ObserveNotificationsUseCase observeNotificationsUseCase;
    private final MarkNotificationReadUseCase markNotificationReadUseCase;
    private final MarkAllNotificationsReadUseCase markAllNotificationsReadUseCase;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<UiState<NotificationCenterUiState>> uiState =
            new MutableLiveData<>(UiState.loading());
    private final SingleLiveEvent<Integer> messageResEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> messageTextEvent = new SingleLiveEvent<>();

    private List<AppNotification> allItems = Collections.emptyList();
    private NotificationCategory selectedCategory = null;

    @Inject
    public NotificationCenterViewModel(
            ObserveNotificationsUseCase observeNotificationsUseCase,
            MarkNotificationReadUseCase markNotificationReadUseCase,
            MarkAllNotificationsReadUseCase markAllNotificationsReadUseCase
    ) {
        this.observeNotificationsUseCase = observeNotificationsUseCase;
        this.markNotificationReadUseCase = markNotificationReadUseCase;
        this.markAllNotificationsReadUseCase = markAllNotificationsReadUseCase;
    }

    public void loadNotifications() {
        disposables.clear();
        uiState.setValue(UiState.loading());
        disposables.add(
                observeNotificationsUseCase.execute()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                this::handleResult,
                                throwable -> uiState.setValue(UiState.error(throwable.getMessage()))
                        )
        );
    }

    public void setCategoryFilter(@Nullable NotificationCategory categoryOrNull) {
        this.selectedCategory = categoryOrNull;
        renderFiltered();
    }

    public void markAsRead(@NonNull String notifId) {
        disposables.add(
                markNotificationReadUseCase.execute(notifId)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                            // SnapshotListener sẽ tự đồng bộ trạng thái đọc realtime
                        }, ignored -> {
                        })
        );
    }

    public void markAllAsRead() {
        disposables.add(
                markAllNotificationsReadUseCase.execute()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                            if (result.isSuccess()) {
                                messageResEvent.setValue(R.string.core_notif_mark_all_success);
                            } else if (result.getMessage() != null) {
                                messageTextEvent.setValue(result.getMessage());
                            }
                        }, throwable -> messageResEvent.setValue(R.string.core_error_network_generic))
        );
    }

    private void handleResult(AppResult<List<AppNotification>> result) {
        if (result.isError()) {
            uiState.setValue(UiState.error(result.getMessage()));
            return;
        }
        allItems = result.getData() != null ? result.getData() : Collections.emptyList();
        renderFiltered();
    }

    private void renderFiltered() {
        List<AppNotification> filtered = new ArrayList<>();
        int unread = 0;
        for (AppNotification item : allItems) {
            if (item == null) {
                continue;
            }
            if (!item.isRead()) {
                unread++;
            }
            if (selectedCategory == null || item.getCategory() == selectedCategory) {
                filtered.add(item);
            }
        }
        if (filtered.isEmpty()) {
            uiState.setValue(UiState.empty());
            return;
        }
        uiState.setValue(UiState.success(new NotificationCenterUiState(
                filtered,
                selectedCategory,
                unread
        )));
    }

    public LiveData<UiState<NotificationCenterUiState>> getUiState() {
        return uiState;
    }

    public SingleLiveEvent<Integer> getMessageResEvent() {
        return messageResEvent;
    }

    public SingleLiveEvent<String> getMessageTextEvent() {
        return messageTextEvent;
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}
