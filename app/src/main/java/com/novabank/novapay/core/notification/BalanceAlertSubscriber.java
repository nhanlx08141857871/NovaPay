package com.novabank.novapay.core.notification;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.novabank.novapay.data.mapper.NotificationMapper;
import com.novabank.novapay.domain.model.notification.AppNotification;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Lắng nghe realtime subcollection users/{uid}/notifications khi người dùng đang đăng nhập.
 * Chỉ phát System Notification cho các bản ghi mới tạo sau thời điểm đăng ký phiên.
 */
@Singleton
public class BalanceAlertSubscriber {

    private static final long CLOCK_SKEW_TOLERANCE_MS = 15_000L;

    private final FirebaseFirestore firestore;
    private final SystemNotificationHelper systemNotificationHelper;

    private ListenerRegistration registration;
    private String subscribedUid;
    private long subscribedAtEpochMs;
    private boolean initialSnapshotConsumed;
    private final Set<String> seenNotifIds = new HashSet<>();

    @Inject
    public BalanceAlertSubscriber(
            FirebaseFirestore firestore,
            SystemNotificationHelper systemNotificationHelper
    ) {
        this.firestore = firestore;
        this.systemNotificationHelper = systemNotificationHelper;
    }

    public synchronized void start(@NonNull String uid) {
        if (uid.trim().isEmpty()) {
            return;
        }
        if (uid.equals(subscribedUid) && registration != null) {
            return;
        }
        stop();
        subscribedUid = uid;
        subscribedAtEpochMs = System.currentTimeMillis();
        initialSnapshotConsumed = false;
        seenNotifIds.clear();

        registration = firestore.collection("users")
                .document(uid)
                .collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(20)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        return;
                    }
                    if (!initialSnapshotConsumed) {
                        initialSnapshotConsumed = true;
                        for (DocumentChange change : snapshots.getDocumentChanges()) {
                            seenNotifIds.add(change.getDocument().getId());
                        }
                        return;
                    }
                    for (DocumentChange change : snapshots.getDocumentChanges()) {
                        if (change.getType() != DocumentChange.Type.ADDED) {
                            continue;
                        }
                        String docId = change.getDocument().getId();
                        if (!seenNotifIds.add(docId)) {
                            continue;
                        }
                        AppNotification notification = NotificationMapper.fromSnapshot(change.getDocument());
                        if (notification == null || notification.isRead()) {
                            continue;
                        }
                        Instant createdAt = notification.getCreatedAt();
                        if (createdAt == null
                                || createdAt.toEpochMilli() >= (subscribedAtEpochMs - CLOCK_SKEW_TOLERANCE_MS)) {
                            systemNotificationHelper.showNotification(notification);
                        }
                    }
                });
    }

    public synchronized void stop() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
        subscribedUid = null;
        initialSnapshotConsumed = false;
        seenNotifIds.clear();
    }
}
