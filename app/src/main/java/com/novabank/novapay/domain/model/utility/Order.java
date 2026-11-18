package com.novabank.novapay.domain.model.utility;

import java.time.Instant;
import java.util.Objects;

/**
 * Đơn hàng / vé đã đặt trong collection orders/{orderId}.
 */
public final class Order {

    private final String orderId;
    private final String uid;
    private final String itemId;
    private final ServiceCategory category;
    private final int quantity;
    private final long totalAmount;
    private final String status;
    private final String txId;
    private final Instant createdAt;

    public Order(
            String orderId,
            String uid,
            String itemId,
            ServiceCategory category,
            int quantity,
            long totalAmount,
            String status,
            String txId,
            Instant createdAt
    ) {
        this.orderId = Objects.requireNonNull(orderId, "orderId must not be null");
        this.uid = uid != null ? uid : "";
        this.itemId = itemId != null ? itemId : "";
        this.category = Objects.requireNonNull(category, "category must not be null");
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.status = status != null ? status : "";
        this.txId = txId;
        this.createdAt = createdAt;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getUid() {
        return uid;
    }

    public String getItemId() {
        return itemId;
    }

    public ServiceCategory getCategory() {
        return category;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getTotalAmount() {
        return totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public String getTxId() {
        return txId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
