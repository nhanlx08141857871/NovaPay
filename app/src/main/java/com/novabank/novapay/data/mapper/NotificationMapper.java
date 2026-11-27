package com.novabank.novapay.data.mapper;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.novabank.novapay.domain.model.notification.AppNotification;
import com.novabank.novapay.domain.model.notification.NotificationCategory;

import java.util.Map;

/**
 * Ánh xạ Document users/{uid}/notifications/{notifId} sang AppNotification (long VNĐ & Instant).
 */
public final class NotificationMapper {

    private NotificationMapper() {
    }

    @Nullable
    public static AppNotification fromSnapshot(@Nullable DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        return fromMap(snapshot.getId(), snapshot.getData());
    }

    @Nullable
    public static AppNotification fromMap(@Nullable String notifId, @Nullable Map<String, Object> data) {
        if (notifId == null || notifId.trim().isEmpty() || data == null) {
            return null;
        }
        Long amount = data.get("amount") != null ? FirestoreMapUtils.toLong(data.get("amount"), 0L) : null;
        Long balanceAfter = data.get("balanceAfter") != null
                ? FirestoreMapUtils.toLong(data.get("balanceAfter"), 0L)
                : null;

        return new AppNotification(
                notifId,
                NotificationCategory.fromString(FirestoreMapUtils.toStringOrNull(data.get("category"))),
                FirestoreMapUtils.toStringOrNull(data.get("direction")),
                FirestoreMapUtils.toStringOrEmpty(data.get("title")),
                FirestoreMapUtils.toStringOrEmpty(data.get("body")),
                amount,
                balanceAfter,
                FirestoreMapUtils.toStringOrNull(data.get("accountNumber")),
                FirestoreMapUtils.toStringOrNull(data.get("txId")),
                FirestoreMapUtils.toStringOrNull(data.get("deepLink")),
                FirestoreMapUtils.toBoolean(data.get("read"), false),
                FirestoreMapUtils.toInstant(data.get("createdAt"))
        );
    }
}
