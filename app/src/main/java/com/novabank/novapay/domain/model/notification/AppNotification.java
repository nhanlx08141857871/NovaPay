package com.novabank.novapay.domain.model.notification;

import java.time.Instant;
import java.util.Objects;

public final class AppNotification {

    private final String notifId;
    private final NotificationCategory category;
    private final String direction;
    private final String title;
    private final String body;
    private final Long amount;
    private final Long balanceAfter;
    private final String accountNumber;
    private final String txId;
    private final String deepLink;
    private final boolean read;
    private final Instant createdAt;

    public AppNotification(
            String notifId,
            NotificationCategory category,
            String direction,
            String title,
            String body,
            Long amount,
            Long balanceAfter,
            String accountNumber,
            String txId,
            String deepLink,
            boolean read,
            Instant createdAt
    ) {
        this.notifId = Objects.requireNonNull(notifId, "notifId must not be null");
        this.category = category != null ? category : NotificationCategory.BALANCE;
        this.direction = direction != null && !direction.trim().isEmpty() ? direction.trim() : null;
        this.title = title != null ? title : "";
        this.body = body != null ? body : "";
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.accountNumber = accountNumber != null && !accountNumber.trim().isEmpty() ? accountNumber.trim() : null;
        this.txId = txId != null && !txId.trim().isEmpty() ? txId.trim() : null;
        this.deepLink = deepLink != null && !deepLink.trim().isEmpty() ? deepLink.trim() : null;
        this.read = read;
        this.createdAt = createdAt;
    }

    public String getNotifId() {
        return notifId;
    }

    public NotificationCategory getCategory() {
        return category;
    }

    public String getDirection() {
        return direction;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public Long getAmount() {
        return amount;
    }

    public Long getBalanceAfter() {
        return balanceAfter;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getTxId() {
        return txId;
    }

    public String getDeepLink() {
        return deepLink;
    }

    public boolean isRead() {
        return read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isInflow() {
        return "IN".equalsIgnoreCase(direction);
    }

    public boolean isOutflow() {
        return "OUT".equalsIgnoreCase(direction);
    }
}
