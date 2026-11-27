package com.novabank.novapay.data.remote.firebase;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.data.mapper.NotificationMapper;
import com.novabank.novapay.domain.model.notification.AppNotification;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

/**
 * Nguồn dữ liệu Firestore cho subcollection users/{uid}/notifications/{notifId}.
 */
@Singleton
public class FirestoreNotificationDataSource {

    private static final long MAX_NOTIFICATIONS_LIMIT = 100L;

    private final FirebaseFirestore firestore;

    @Inject
    public FirestoreNotificationDataSource(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public Observable<AppResult<List<AppNotification>>> observeNotifications(String uid) {
        return Observable.create(emitter -> {
            ListenerRegistration registration = firestore.collection("users")
                    .document(uid)
                    .collection("notifications")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(MAX_NOTIFICATIONS_LIMIT)
                    .addSnapshotListener((snapshots, error) -> {
                        if (emitter.isDisposed()) {
                            return;
                        }
                        if (error != null) {
                            emitter.onNext(AppResult.error(
                                    "FIRESTORE_ERROR",
                                    error.getMessage() != null ? error.getMessage() : "Không thể tải hộp thư thông báo."
                            ));
                            return;
                        }
                        List<AppNotification> items = new ArrayList<>();
                        if (snapshots != null) {
                            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                AppNotification mapped = NotificationMapper.fromSnapshot(doc);
                                if (mapped != null) {
                                    items.add(mapped);
                                }
                            }
                        }
                        emitter.onNext(AppResult.success(items));
                    });
            emitter.setCancellable(registration::remove);
        });
    }

    public Single<AppResult<Boolean>> markAsRead(String uid, String notifId) {
        return Single.create(emitter ->
                firestore.collection("users")
                        .document(uid)
                        .collection("notifications")
                        .document(notifId)
                        .update("read", true)
                        .addOnSuccessListener(unused -> {
                            if (!emitter.isDisposed()) {
                                emitter.onSuccess(AppResult.success(Boolean.TRUE));
                            }
                        })
                        .addOnFailureListener(ex -> {
                            if (!emitter.isDisposed()) {
                                emitter.onSuccess(AppResult.error(
                                        "FIRESTORE_ERROR",
                                        ex.getMessage() != null ? ex.getMessage() : "Không thể cập nhật trạng thái thông báo."
                                ));
                            }
                        })
        );
    }

    public Single<AppResult<Boolean>> markAllAsRead(String uid) {
        return Single.create(emitter ->
                firestore.collection("users")
                        .document(uid)
                        .collection("notifications")
                        .whereEqualTo("read", false)
                        .get()
                        .addOnSuccessListener(snapshots -> {
                            if (snapshots == null || snapshots.isEmpty()) {
                                if (!emitter.isDisposed()) {
                                    emitter.onSuccess(AppResult.success(Boolean.TRUE));
                                }
                                return;
                            }
                            WriteBatch batch = firestore.batch();
                            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                DocumentReference ref = doc.getReference();
                                batch.update(ref, "read", true);
                            }
                            batch.commit()
                                    .addOnSuccessListener(unused -> {
                                        if (!emitter.isDisposed()) {
                                            emitter.onSuccess(AppResult.success(Boolean.TRUE));
                                        }
                                    })
                                    .addOnFailureListener(ex -> {
                                        if (!emitter.isDisposed()) {
                                            emitter.onSuccess(AppResult.error(
                                                    "FIRESTORE_ERROR",
                                                    ex.getMessage() != null ? ex.getMessage() : "Không thể đánh dấu đã đọc tất cả."
                                            ));
                                        }
                                    });
                        })
                        .addOnFailureListener(ex -> {
                            if (!emitter.isDisposed()) {
                                emitter.onSuccess(AppResult.error(
                                        "FIRESTORE_ERROR",
                                        ex.getMessage() != null ? ex.getMessage() : "Không thể đánh dấu đã đọc tất cả."
                                ));
                            }
                        })
        );
    }
}
