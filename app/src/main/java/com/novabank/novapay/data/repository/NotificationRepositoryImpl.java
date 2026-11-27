package com.novabank.novapay.data.repository;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.data.remote.firebase.FirebaseAuthDataSource;
import com.novabank.novapay.data.remote.firebase.FirestoreNotificationDataSource;
import com.novabank.novapay.domain.model.notification.AppNotification;
import com.novabank.novapay.domain.repository.NotificationRepository;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

@Singleton
public class NotificationRepositoryImpl implements NotificationRepository {

    private final FirebaseAuthDataSource authDataSource;
    private final FirestoreNotificationDataSource notificationDataSource;

    @Inject
    public NotificationRepositoryImpl(
            FirebaseAuthDataSource authDataSource,
            FirestoreNotificationDataSource notificationDataSource
    ) {
        this.authDataSource = authDataSource;
        this.notificationDataSource = notificationDataSource;
    }

    @Override
    public Observable<AppResult<List<AppNotification>>> observeNotifications() {
        String uid = authDataSource.getCurrentUid();
        if (uid == null || uid.isEmpty()) {
            return Observable.just(AppResult.error("UNAUTHENTICATED", "Vui lòng đăng nhập để xem thông báo."));
        }
        return notificationDataSource.observeNotifications(uid);
    }

    @Override
    public Observable<AppResult<Integer>> observeUnreadCount() {
        return observeNotifications().map(result -> {
            if (result.isError()) {
                return AppResult.error(result.getErrorCode(), result.getMessage());
            }
            int unread = 0;
            List<AppNotification> items = result.getData();
            if (items != null) {
                for (AppNotification item : items) {
                    if (item != null && !item.isRead()) {
                        unread++;
                    }
                }
            }
            return AppResult.success(unread);
        });
    }

    @Override
    public Single<AppResult<Boolean>> markAsRead(String notifId) {
        String uid = authDataSource.getCurrentUid();
        if (uid == null || uid.isEmpty()) {
            return Single.just(AppResult.error("UNAUTHENTICATED", "Phiên đăng nhập đã hết hạn."));
        }
        return notificationDataSource.markAsRead(uid, notifId);
    }

    @Override
    public Single<AppResult<Boolean>> markAllAsRead() {
        String uid = authDataSource.getCurrentUid();
        if (uid == null || uid.isEmpty()) {
            return Single.just(AppResult.error("UNAUTHENTICATED", "Phiên đăng nhập đã hết hạn."));
        }
        return notificationDataSource.markAllAsRead(uid);
    }
}
