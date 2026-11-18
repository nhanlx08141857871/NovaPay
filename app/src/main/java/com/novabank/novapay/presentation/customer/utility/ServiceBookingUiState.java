package com.novabank.novapay.presentation.customer.utility;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.utility.Order;
import com.novabank.novapay.domain.model.utility.ServiceCategory;
import com.novabank.novapay.domain.model.utility.ServiceItem;

import java.util.Collections;
import java.util.List;

/**
 * Trạng thái màn hình Đặt vé máy bay, xem phim, khách sạn, mua sắm & Đơn đã đặt (ServiceBookingFragment).
 */
public final class ServiceBookingUiState {

    private final List<Account> checkingAccounts;
    private final int selectedSourceIndex;
    private final boolean showingOrdersTab;
    private final ServiceCategory activeCategory;
    private final List<ServiceItem> catalogItems;
    private final List<Order> myOrders;
    private final ServiceItem selectedItem;
    private final int quantity;

    public ServiceBookingUiState(
            @NonNull List<Account> checkingAccounts,
            int selectedSourceIndex,
            boolean showingOrdersTab,
            @NonNull ServiceCategory activeCategory,
            @NonNull List<ServiceItem> catalogItems,
            @NonNull List<Order> myOrders,
            @Nullable ServiceItem selectedItem,
            int quantity
    ) {
        this.checkingAccounts = Collections.unmodifiableList(checkingAccounts);
        this.selectedSourceIndex = selectedSourceIndex;
        this.showingOrdersTab = showingOrdersTab;
        this.activeCategory = activeCategory;
        this.catalogItems = Collections.unmodifiableList(catalogItems);
        this.myOrders = Collections.unmodifiableList(myOrders);
        this.selectedItem = selectedItem;
        this.quantity = Math.max(1, quantity);
    }

    public long getComputedTotalAmountVnd() {
        if (selectedItem == null) {
            return 0L;
        }
        return Math.multiplyExact(selectedItem.getPrice(), (long) quantity);
    }

    @NonNull
    public List<Account> getCheckingAccounts() {
        return checkingAccounts;
    }

    public int getSelectedSourceIndex() {
        return selectedSourceIndex;
    }

    public boolean isShowingOrdersTab() {
        return showingOrdersTab;
    }

    @NonNull
    public ServiceCategory getActiveCategory() {
        return activeCategory;
    }

    @NonNull
    public List<ServiceItem> getCatalogItems() {
        return catalogItems;
    }

    @NonNull
    public List<Order> getMyOrders() {
        return myOrders;
    }

    @Nullable
    public ServiceItem getSelectedItem() {
        return selectedItem;
    }

    public int getQuantity() {
        return quantity;
    }
}
