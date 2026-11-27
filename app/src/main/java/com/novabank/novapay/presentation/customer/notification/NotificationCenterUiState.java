package com.novabank.novapay.presentation.customer.notification;

import com.novabank.novapay.domain.model.notification.AppNotification;
import com.novabank.novapay.domain.model.notification.NotificationCategory;

import java.util.Collections;
import java.util.List;

public final class NotificationCenterUiState {

    private final List<AppNotification> items;
    private final NotificationCategory selectedCategory;
    private final int unreadCount;

    public NotificationCenterUiState(
            List<AppNotification> items,
            NotificationCategory selectedCategory,
            int unreadCount
    ) {
        this.items = items != null ? items : Collections.emptyList();
        this.selectedCategory = selectedCategory;
        this.unreadCount = unreadCount;
    }

    public List<AppNotification> getItems() {
        return items;
    }

    public NotificationCategory getSelectedCategory() {
        return selectedCategory;
    }

    public int getUnreadCount() {
        return unreadCount;
    }
}
